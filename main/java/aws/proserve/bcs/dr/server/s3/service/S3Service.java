// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.s3.service;

import aws.proserve.bcs.dr.aws.AwsBucket;
import aws.proserve.bcs.dr.aws.ImmutableAwsBucket;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.project.ProjectService;
import aws.proserve.bcs.dr.project.Region;
import aws.proserve.bcs.dr.s3.AwsBucketItem;
import aws.proserve.bcs.dr.s3.ImmutableAwsBucketItem;
import aws.proserve.bcs.dr.s3.S3Item;
import aws.proserve.bcs.dr.s3.S3Project;
import aws.proserve.bcs.dr.server.s3.dto.CreateS3ProjectRequest;
import aws.proserve.bcs.dr.server.s3.service.machine.S3ReplicateBucketMachine;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Collections;
import java.util.Set;

@Named
public class S3Service implements ProjectService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AmazonS3 s3;
    private final ProjectFinder projectFinder;
    private final S3ReplicateBucketMachine replicateBucketMachine;

    S3Service(
            AmazonS3 s3,
            ProjectFinder projectFinder,
            S3ReplicateBucketMachine replicateBucketMachine) {
        this.s3 = s3;
        this.projectFinder = projectFinder;
        this.replicateBucketMachine = replicateBucketMachine;
    }

    public AwsBucket[] getAwsBuckets() {
        return s3.listBuckets()
                .stream().parallel()
                .map(Bucket::getName)
                .map(bucket -> ImmutableAwsBucket.builder()
                        .id(bucket)
                        .name(bucket)
                        .region(getRegion(bucket))
                        .build())
                .toArray(AwsBucket[]::new);
    }

    private String getRegion(String bucket) {
        return com.amazonaws.services.s3.model.Region
                .fromValue(s3.getBucketLocation(bucket))
                .toAWSRegion()
                .getName();
    }

    public AwsBucketItem[] getAwsBucketItems(Project project) {
        return project.getS3Project().getItems()
                .stream()
                .map(item -> ImmutableAwsBucketItem.builder()
                        .item(item)
                        .source(ImmutableAwsBucket.builder()
                                .id(item.getSource())
                                .name(item.getSource())
                                .region(project.getSourceRegion().getName())
                                .build())
                        .target(ImmutableAwsBucket.builder()
                                .id(item.getTarget())
                                .name(item.getTarget())
                                .region(project.getTargetRegion().getName())
                                .build())
                        .sourceRegion(project.getSourceRegion().getName())
                        .targetRegion(project.getTargetRegion().getName())
                        .build())
                .toArray(AwsBucketItem[]::new);
    }

    public void create(CreateS3ProjectRequest request) {
        final var s3Project = new S3Project();
        s3Project.setItems(Collections.emptyList());

        final var project = new Project();
        project.setName(request.getName());
        project.setType(Component.S3);
        project.setSourceRegion(new Region(Regions.fromName(request.getSourceRegion())));
        project.setTargetRegion(new Region(Regions.fromName(request.getTargetRegion())));
        project.setS3Project(s3Project);

        log.debug("Save S3 project [{}]", project.getName());
        projectFinder.save(project, request.getSourceCredential());
    }

    @Override
    public void delete(Project project) {
        projectFinder.delete(project);
    }

    public void addItem(Project project, S3Item item) {
        log.info("Add S3 items to project {}", project.getName());

        final var items = project.getS3Project().getItems();
        for (var i : items) {
            if (i.getId().equals(item.getId())) {
                log.info("This item exists in the project already.");
                throw new PortalException(String.format("重复添加的复制项（从 %s 到 %s）",
                        item.getSource(), item.getTarget()));
            }
        }

        try {
            if (!getRegion(item.getSource()).equals(project.getSourceRegion().getName())) {
                throw new PortalException("源桶所属区域不是 " + project.getSourceRegion());
            }
        } catch (Exception e) {
            throw new PortalException(String.format("在 %s 找不到源桶 %s",
                    project.getSourceRegion(), item.getSource()));
        }

        try {
            if (!getRegion(item.getTarget()).equals(project.getTargetRegion().getName())) {
                throw new PortalException("目的桶所属区域不是 " + project.getTargetRegion());
            }
        } catch (Exception e) {
            throw new PortalException(String.format("在 %s 找不到目的桶 %s",
                    project.getTargetRegion(), item.getTarget()));
        }

        items.add(item);
        replicateBucketMachine.replicate(project, item); // will save project
    }

    public void deleteItems(Project project, String[] keys) {
        log.info("Delete {} S3 items from project {}", keys.length, project.getName());
        final var keySet = Set.of(keys);
        project.getS3Project().getItems().removeIf(config -> keySet.contains(config.getId()));
        projectFinder.save(project);
    }

    public void replicateItem(Project project, S3Item item) {
        log.info("Replicate S3 item {} of project {}", item.getId(), project.getName());
        replicateBucketMachine.replicate(project, project.getS3Project().find(item.getId()));
    }
}
