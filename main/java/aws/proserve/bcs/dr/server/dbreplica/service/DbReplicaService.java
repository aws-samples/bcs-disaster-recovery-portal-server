// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dbreplica.service;

import aws.proserve.bcs.ce.service.CloudEndureInstanceService;
import aws.proserve.bcs.dr.aws.AwsInstance;
import aws.proserve.bcs.dr.dbreplica.AwsDbReplicaEc2Item;
import aws.proserve.bcs.dr.dbreplica.DbReplicaItem;
import aws.proserve.bcs.dr.dbreplica.DbReplicaProject;
import aws.proserve.bcs.dr.dbreplica.ImmutableAwsDbReplicaEc2Item;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.project.ProjectService;
import aws.proserve.bcs.dr.project.Region;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.secret.Credential;
import aws.proserve.bcs.dr.secret.SecretManager;
import aws.proserve.bcs.dr.server.dbreplica.dto.CreateDbReplicaProjectRequest;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
public class DbReplicaService implements ProjectService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CloudEndureInstanceService instanceService;
    private final ProjectFinder projectFinder;
    private final SecretManager secretManager;

    DbReplicaService(
            CloudEndureInstanceService instanceService,
            ProjectFinder projectFinder,
            SecretManager secretManager) {
        this.instanceService = instanceService;
        this.projectFinder = projectFinder;
        this.secretManager = secretManager;
    }

    public AwsInstance[] getAwsInstances(Project project, Side side) {
        final var credential = Credential.toProvider(secretManager.getCredential(project, side));
        return getAwsInstances(
                AmazonEC2ClientBuilder.standard()
                        .withRegion(project.getRegion(side).toAwsRegion())
                        .withCredentials(credential)
                        .build(),
                AmazonIdentityManagementClientBuilder.standard()
                        .withRegion(project.getRegion(side).toAwsRegion())
                        .withCredentials(credential)
                        .build());
    }

    private Map<String, AwsInstance> getAwsInstanceMap(Project project, Side side) {
        return Stream.of(getAwsInstances(project, side))
                .collect(Collectors.toMap(AwsInstance::getId, i -> i));
    }

    private AwsInstance[] getAwsInstances(AmazonEC2 ec2, AmazonIdentityManagement iam) {
        final var instances = new ArrayList<AwsInstance>();
        final var request = new DescribeInstancesRequest();
        DescribeInstancesResult result;
        do {
            result = ec2.describeInstances(request);
            for (var r : result.getReservations()) {
                for (var instance : r.getInstances()) {
                    if (!instanceService.isQualified(instance, null, iam)) {
                        continue;
                    }

                    instances.add(AwsInstance.convert(instance));
                }
            }

            request.setNextToken(result.getNextToken());
        } while (result.getNextToken() != null);

        return instances.toArray(AwsInstance[]::new);
    }

    private boolean checkRegion(Project project, String id, boolean source) {
        final AmazonEC2 ec2;
        if (source) {
            ec2 = AmazonEC2ClientBuilder.standard()
                    .withRegion(project.getSourceRegion().toAwsRegion())
                    .withCredentials(Credential.toProvider(secretManager.getCredential(project)))
                    .build();
        } else {
            ec2 = AmazonEC2ClientBuilder.standard()
                    .withRegion(project.getTargetRegion().toAwsRegion())
                    .build();
        }

        try {
            ec2.describeInstances(new DescribeInstancesRequest().withInstanceIds(id));
            return true;
        } catch (Exception e) {
            log.warn("Unable to describe instance.", e);
            return false;
        }
    }

    public AwsDbReplicaEc2Item[] getAwsDbReplicaEc2Items(Project project) {
        final var sourceMap = getAwsInstanceMap(project, Side.source);
        final var targetMap = getAwsInstanceMap(project, Side.target);

        return project.getDbReplicaProject().getItems()
                .stream()
                .map(item -> ImmutableAwsDbReplicaEc2Item.builder()
                        .item(item)
                        .source(sourceMap.get(item.getSource()))
                        .target(targetMap.get(item.getTarget()))
                        .sourceRegion(project.getSourceRegion().getName())
                        .targetRegion(project.getTargetRegion().getName())
                        .build())
                .toArray(AwsDbReplicaEc2Item[]::new);
    }

    public void create(Component component, CreateDbReplicaProjectRequest request) {
        final var replica = new DbReplicaProject();
        replica.setItems(Collections.emptyList());

        final var project = new Project();
        project.setName(request.getName());
        project.setType(component);
        project.setSourceRegion(new Region(Regions.fromName(request.getSourceRegion())));
        project.setTargetRegion(new Region(Regions.fromName(request.getTargetRegion())));
        project.setDbReplicaProject(replica);

        log.debug("Save DB replica project [{}]", project.getName());
        projectFinder.save(project, request.getSourceCredential());
    }

    @Override
    public void delete(Project project) {
        projectFinder.delete(project);
    }

    public void addItem(Project project, DbReplicaItem item) {
        log.info("Add DB replica items to project {}", project.getName());

        final var items = project.getDbReplicaProject().getItems();
        for (var i : items) {
            if (i.getId().equals(item.getId())) {
                log.info("This item exists in the project already.");
                throw new PortalException(String.format("重复添加的复制项（从 %s 到 %s）",
                        item.getSource(), item.getTarget()));
            }
        }

        if (!checkRegion(project, item.getSource(), true)) {
            throw new PortalException("源数据库所属区域不是 " + project.getSourceRegion());
        }

        if (!checkRegion(project, item.getTarget(), false)) {
            throw new PortalException("目的数据库所属区域不是 " + project.getTargetRegion());
        }

        items.add(item);
        projectFinder.save(project);
    }

    public void deleteItems(Project project, String[] keys) {
        log.info("Delete {} DB replica items from project {}", keys.length, project.getName());
        final var keySet = Set.of(keys);
        project.getDbReplicaProject().getItems().removeIf(config -> keySet.contains(config.getId()));
        projectFinder.save(project);
    }
}
