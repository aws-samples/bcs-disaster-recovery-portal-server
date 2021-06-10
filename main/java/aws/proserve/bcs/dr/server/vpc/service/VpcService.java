// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.vpc.service;

import aws.proserve.bcs.dr.aws.AwsVpc;
import aws.proserve.bcs.dr.aws.ImmutableAwsVpc;
import aws.proserve.bcs.dr.dynamo.DynamoConstants;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.project.ProjectService;
import aws.proserve.bcs.dr.server.vpc.dto.CreateVpcProjectRequest;
import aws.proserve.bcs.dr.server.vpc.service.machine.VpcReplicateVpcMachine;
import aws.proserve.bcs.dr.vpc.AwsVpcItem;
import aws.proserve.bcs.dr.vpc.ImmutableAwsVpcItem;
import aws.proserve.bcs.dr.vpc.VpcItem;
import aws.proserve.bcs.dr.vpc.VpcProject;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Named
public class VpcService implements ProjectService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper;
    private final AWSLambda lambda;
    private final DynamoDB dynamoDB;
    private final DynamoDBMapper dbMapper;
    private final ProjectFinder projectFinder;
    private final VpcReplicateVpcMachine replicateVpcMachine;

    VpcService(
            ObjectMapper mapper,
            AWSLambda lambda,
            DynamoDB dynamoDB,
            DynamoDBMapper dbMapper, ProjectFinder projectFinder,
            VpcReplicateVpcMachine replicateVpcMachine) {
        this.mapper = mapper;
        this.lambda = lambda;
        this.dynamoDB = dynamoDB;
        this.dbMapper = dbMapper;
        this.projectFinder = projectFinder;
        this.replicateVpcMachine = replicateVpcMachine;
    }

    public AwsVpc[] getAwsVpcs(Project project) {
        return getAwsVpcs(project.getSourceRegion().getName());
    }

    public AwsVpc[] getAwsVpcs(String region) {
        final var ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(region)
                .build();

        return AwsVpc.getVpcs(ec2).stream().map(AwsVpc::convert).toArray(AwsVpc[]::new);
    }

    public AwsVpcItem[] getAwsVpcItems(Project project) {
        final var ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(project.getSourceRegion().toAwsRegion())
                .build();

        return project.getVpcProject().getItems()
                .stream()
                .map(item -> ImmutableAwsVpcItem.builder()
                        .item(item)
                        .source(getAwsVpc(ec2, item.getSource()))
                        .sourceRegion(project.getSourceRegion().getName())
                        .targetRegion(project.getTargetRegion().getName())
                        .build())
                .toArray(AwsVpcItem[]::new);
    }

    private AwsVpc getAwsVpc(AmazonEC2 ec2, String vpcId) {
        return ec2.describeVpcs(new DescribeVpcsRequest().withVpcIds(vpcId))
                .getVpcs()
                .stream()
                .map(AwsVpc::convert)
                .findFirst()
                .orElse(ImmutableAwsVpc.builder().vpcId(vpcId).name("").state("").build());
    }

    public void create(CreateVpcProjectRequest request) {
        try {
            final var invoke = lambda.invoke(new InvokeRequest()
                    .withFunctionName("DRPVpcCreateVpcProject")
                    .withPayload(mapper.writeValueAsString(request)));
            final var output = StandardCharsets.UTF_8.decode(invoke.getPayload()).toString();
            log.info("VPC project created [{}]", output);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create VPC project.", e);
        }
    }

    @Override
    public void delete(Project project) {
        final var table = dynamoDB.getTable(DynamoConstants.TABLE_VPC);
        for (var vpcItem : project.getVpcProject().getItems()) {
            for (var item : table.scan("#source.vpcId = :vpcId",
                    Map.of("#source", "source"),
                    Map.of(":vpcId", vpcItem.getSource()))) {
                table.deleteItem(DynamoConstants.KEY_ID, item.getString(DynamoConstants.KEY_ID));
            }
        }

        final var vpcIds = projectFinder.findByType(Component.VPC).stream()
                .map(Project::getVpcProject)
                .map(VpcProject::getItems).flatMap(List::stream)
                .map(VpcItem::getSource)
                .collect(Collectors.toSet());

        for (var item : table.scan("attribute_exists(#source.vpcId)",
                Map.of("#source", "source"), null)) {
            final var vpcId = (String) item.getMap("source").get("vpcId");
            if (!vpcIds.contains(vpcId)) {
                table.deleteItem(DynamoConstants.KEY_ID, item.getString(DynamoConstants.KEY_ID));
            }
        }

        dbMapper.delete(project);
    }

    public void addItems(Project project, VpcItem[] items) {
        log.info("Add {} vpc items to project {}", items.length, project.getName());

        if (items[0].isContinuous() && !isContinuousReady(project)) {
            throw new PortalException("源区域不支持 VPC 持续复制");
        }

        final var vpcItems = project.getVpcProject().getItems();
        final var vpcIds = vpcItems.stream().map(VpcItem::getId).collect(Collectors.toSet());

        for (var i : items) {
            if (vpcIds.contains(i.getSource())) {
                log.warn("Skip duplicated item {}", i);
                continue;
            }

            replicateVpcMachine.replicate(project, i);
            vpcItems.add(i);
        }

        dbMapper.save(project);
    }

    public void deleteItems(Project project, String[] vpcIds) {
        log.info("Delete {} vpc items from project {}", vpcIds.length, project.getName());
        final var vpcIdSet = Set.of(vpcIds);
        project.getVpcProject().getItems().removeIf(config -> vpcIdSet.contains(config.getSource()));
        dbMapper.save(project);

        for (var vpcId : vpcIds) {
            lambda.invoke(new InvokeRequest()
                    .withFunctionName("DRPVpcDeleteVpc")
                    .withPayload("\"" + vpcId + "\""));
        }
    }

    public boolean isContinuousReady(Project project) {
        final var invoke = lambda.invoke(new InvokeRequest()
                .withFunctionName("DRPVpcCheckWatchReady")
                .withPayload("\"" + project.getSourceRegion().getName() + "\""));
        return Boolean.parseBoolean(StandardCharsets.UTF_8.decode(invoke.getPayload()).toString());
    }

    public void replicateItem(Project project, VpcItem item) {
        log.info("Replicate VPC config {} of project {}", item.getSource(), project.getName());
        replicateVpcMachine.replicate(project, project.getVpcProject().find(item.getId()));
    }
}
