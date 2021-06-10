// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dynamo.service;

import aws.proserve.bcs.dr.aws.AwsTable;
import aws.proserve.bcs.dr.aws.ImmutableAwsTable;
import aws.proserve.bcs.dr.dynamo.AwsTableItem;
import aws.proserve.bcs.dr.dynamo.DynamoItem;
import aws.proserve.bcs.dr.dynamo.DynamoProject;
import aws.proserve.bcs.dr.dynamo.ImmutableAwsTableItem;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.project.ProjectService;
import aws.proserve.bcs.dr.project.Region;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.secret.Credential;
import aws.proserve.bcs.dr.secret.SecretManager;
import aws.proserve.bcs.dr.server.dynamo.dto.CreateDynamoProjectRequest;
import aws.proserve.bcs.dr.server.dynamo.service.machine.DynamoReplicateTableMachine;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

@Named
public class DynamoService implements ProjectService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ProjectFinder projectFinder;
    private final SecretManager secretManager;
    private final DynamoReplicateTableMachine replicateTableMachine;

    DynamoService(
            ProjectFinder projectFinder,
            SecretManager secretManager,
            DynamoReplicateTableMachine replicateTableMachine) {
        this.projectFinder = projectFinder;
        this.secretManager = secretManager;
        this.replicateTableMachine = replicateTableMachine;
    }

    public AwsTable[] getAwsTables(Project project, Side side) {
        return listTables(AmazonDynamoDBClientBuilder.standard()
                .withRegion(project.getRegion(side).toAwsRegion())
                .withCredentials(Credential.toProvider(secretManager.getCredential(project, side)))
                .build());
    }

    private AwsTable[] listTables(AmazonDynamoDB dynamoDB) {
        final var tableNames = new ArrayList<String>();
        final var request = new ListTablesRequest();
        ListTablesResult result;
        do {
            result = dynamoDB.listTables(request);
            tableNames.addAll(result.getTableNames());

            request.setExclusiveStartTableName(result.getLastEvaluatedTableName());
        } while (result.getLastEvaluatedTableName() != null);

        return tableNames.stream()
                .map(name -> ImmutableAwsTable.builder()
                        .id(name)
                        .name(name)
                        .build())
                .toArray(AwsTable[]::new);
    }

    private boolean checkRegion(Project project, String table, boolean source) {
        final AmazonDynamoDB dynamoDB;
        if (source) {
            dynamoDB = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(project.getSourceRegion().toAwsRegion())
                    .withCredentials(Credential.toProvider(secretManager.getCredential(project)))
                    .build();
        } else {
            dynamoDB = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(project.getTargetRegion().toAwsRegion())
                    .build();
        }

        try {
            dynamoDB.describeTable(table);
            return true;
        } catch (Exception e) {
            log.warn("Unable to describe table.", e);
            return false;
        }
    }

    public AwsTableItem[] getAwsTableItems(Project project) {
        return project.getDynamoProject().getItems()
                .stream()
                .map(item -> ImmutableAwsTableItem.builder()
                        .item(item)
                        .source(ImmutableAwsTable.builder()
                                .id(item.getSource())
                                .name(item.getSource())
                                .build())
                        .target(ImmutableAwsTable.builder()
                                .id(item.getTarget())
                                .name(item.getTarget())
                                .build())
                        .sourceRegion(project.getSourceRegion().getName())
                        .targetRegion(project.getTargetRegion().getName())
                        .build())
                .toArray(AwsTableItem[]::new);
    }

    public void create(CreateDynamoProjectRequest request) {
        final var dynamoProject = new DynamoProject();
        dynamoProject.setItems(Collections.emptyList());

        final var project = new Project();
        project.setName(request.getName());
        project.setType(Component.DynamoDB);
        project.setSourceRegion(new Region(Regions.fromName(request.getSourceRegion())));
        project.setTargetRegion(new Region(Regions.fromName(request.getTargetRegion())));
        project.setDynamoProject(dynamoProject);

        log.debug("Save Dynamo project [{}]", project.getName());
        projectFinder.save(project, request.getSourceCredential());
    }

    @Override
    public void delete(Project project) {
        projectFinder.delete(project);
    }

    public void addItem(Project project, DynamoItem item) {
        log.info("Add Dynamo items to project {}", project.getName());

        final var items = project.getDynamoProject().getItems();
        for (var i : items) {
            if (i.getId().equals(item.getId())) {
                log.info("This item exists in the project already.");
                throw new PortalException(String.format("重复添加的复制项（从 %s 到 %s）",
                        item.getSource(), item.getTarget()));
            }
        }

        if (!checkRegion(project, item.getSource(), true)) {
            throw new PortalException("源表所属区域不是 " + project.getSourceRegion());
        }

        if (!checkRegion(project, item.getTarget(), false)) {
            throw new PortalException("目的表所属区域不是 " + project.getTargetRegion());
        }

        items.add(item);
        replicateTableMachine.start(project, item); // will save project
    }

    public void deleteItems(Project project, String[] keys) {
        log.info("Delete {} Dynamo items from project {}", keys.length, project.getName());
        final var keySet = Set.of(keys);
        project.getDynamoProject().getItems().removeIf(config -> keySet.contains(config.getId()));
        projectFinder.save(project);
    }

    public void startItem(Project project, DynamoItem item) {
        log.info("Start replicating item {} of project {}", item.getId(), project.getName());
        replicateTableMachine.start(project, project.getDynamoProject().find(item.getId()));
    }

    public void stopItem(Project project, DynamoItem item) {
        log.info("Stop replicating item {} of project {}", item.getId(), project.getName());
        replicateTableMachine.stop(project, project.getDynamoProject().find(item.getId()));
    }
}
