// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dbdump.service;

import aws.proserve.bcs.dr.aws.AwsDbInstance;
import aws.proserve.bcs.dr.aws.ImmutableAwsDbEndpoint;
import aws.proserve.bcs.dr.aws.ImmutableAwsDbInstance;
import aws.proserve.bcs.dr.dbdump.AwsDbDumpItem;
import aws.proserve.bcs.dr.dbdump.DbDumpItem;
import aws.proserve.bcs.dr.dbdump.DbDumpProject;
import aws.proserve.bcs.dr.dbdump.ImmutableAwsDbDumpItem;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.project.ProjectService;
import aws.proserve.bcs.dr.project.Region;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.secret.Credential;
import aws.proserve.bcs.dr.secret.SecretManager;
import aws.proserve.bcs.dr.secret.Secrets;
import aws.proserve.bcs.dr.server.dbdump.dto.CreateDbDumpProjectRequest;
import aws.proserve.bcs.dr.server.dbdump.dto.ManageDbDumpItemRequest;
import aws.proserve.bcs.dr.server.dbdump.service.machine.DbDumpMySqlGetDatabasesMachine;
import com.amazonaws.jmespath.ObjectMapperSingleton;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Subnet;
import com.amazonaws.services.rds.model.VpcSecurityGroupMembership;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
public class DbDumpService implements ProjectService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ProjectFinder projectFinder;
    private final SecretManager secretManager;
    private final DbDumpMySqlGetDatabasesMachine getDatabasesMachine;

    DbDumpService(
            ProjectFinder projectFinder, SecretManager secretManager,
            DbDumpMySqlGetDatabasesMachine getDatabasesMachine) {
        this.projectFinder = projectFinder;
        this.secretManager = secretManager;
        this.getDatabasesMachine = getDatabasesMachine;
    }

    public AwsDbInstance[] getAwsDbInstances(Project project, Side side, Component component) {
        return getAwsDbInstances(component,
                AmazonRDSClientBuilder.standard()
                        .withRegion(project.getRegion(side).toAwsRegion())
                        .withCredentials(Credential.toProvider(secretManager.getCredential(project, side)))
                        .build());
    }

    private Map<String, AwsDbInstance> getAwsDbInstanceMap(Project project, Side side) {
        return Stream.of(getAwsDbInstances(project, side, project.getType()))
                .collect(Collectors.toMap(AwsDbInstance::getId, i -> i));
    }

    private AwsDbInstance[] getAwsDbInstances(Component component, AmazonRDS rds) {
        final Predicate<DBInstance> filter = i -> {
            switch (component) {
                case DbDumpMySql:
                    return i.getEngine().contains("mysql") || i.getEngine().contains("mariadb");

                default:
                    return false;
            }
        };

        final var dbInstances = new ArrayList<DBInstance>();
        final var request = new DescribeDBInstancesRequest();
        DescribeDBInstancesResult result;
        do {
            result = rds.describeDBInstances(request);
            dbInstances.addAll(result.getDBInstances().stream()
                    .filter(filter)
                    .collect(Collectors.toList()));

            request.setMarker(result.getMarker());
        } while (result.getMarker() != null);

        return dbInstances.stream()
                .map(db -> ImmutableAwsDbInstance.builder()
                        .dBInstanceIdentifier(db.getDBInstanceIdentifier())
                        .name(db.getDBName() == null ? "" : db.getDBName())
                        .engine(db.getEngine())
                        .engineVersion(db.getEngineVersion())
                        .instanceClass(db.getDBInstanceClass())
                        .instanceStatus(db.getDBInstanceStatus())
                        .masterUsername(db.getMasterUsername())
                        .multiAZ(db.isMultiAZ())
                        .endpoint(ImmutableAwsDbEndpoint.builder()
                                .address(db.getEndpoint().getAddress())
                                .port(db.getEndpoint().getPort())
                                .build())
                        .subnetIds(db.getDBSubnetGroup().getSubnets().stream()
                                .map(Subnet::getSubnetIdentifier)
                                .toArray(String[]::new))
                        .securityGroupIds(db.getVpcSecurityGroups().stream()
                                .map(VpcSecurityGroupMembership::getVpcSecurityGroupId)
                                .toArray(String[]::new))
                        .build())
                .toArray(AwsDbInstance[]::new);
    }

    private boolean checkRegion(Project project, String id, boolean source) {
        final AmazonRDS rds;
        if (source) {
            rds = AmazonRDSClientBuilder.standard()
                    .withRegion(project.getSourceRegion().toAwsRegion())
                    .withCredentials(Credential.toProvider(secretManager.getCredential(project)))
                    .build();
        } else {
            rds = AmazonRDSClientBuilder.standard()
                    .withRegion(project.getTargetRegion().toAwsRegion())
                    .build();
        }

        try {
            rds.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(id));
            return true;
        } catch (Exception e) {
            log.warn("Unable to describe database.", e);
            return false;
        }
    }

    public AwsDbDumpItem[] getAwsDbInstanceItems(Project project) {
        final var sourceMap = getAwsDbInstanceMap(project, Side.source);
        final var targetMap = getAwsDbInstanceMap(project, Side.target);

        return project.getDbDumpProject().getItems()
                .stream()
                .map(item -> ImmutableAwsDbDumpItem.builder()
                        .item(item)
                        .source(sourceMap.get(item.getSource()))
                        .target(targetMap.get(item.getTarget()))
                        .sourceRegion(project.getSourceRegion().getName())
                        .targetRegion(project.getTargetRegion().getName())
                        .build())
                .toArray(AwsDbDumpItem[]::new);
    }

    public void create(Component component, CreateDbDumpProjectRequest request) {
        final var dump = new DbDumpProject();
        dump.setItems(Collections.emptyList());

        final var project = new Project();
        project.setName(request.getName());
        project.setType(component);
        project.setSourceRegion(new Region(Regions.fromName(request.getSourceRegion())));
        project.setTargetRegion(new Region(Regions.fromName(request.getTargetRegion())));
        project.setDbDumpProject(dump);

        log.debug("Save DB dump project [{}]", project.getName());
        projectFinder.save(project, request.getSourceCredential());
    }

    @Override
    public void delete(Project project) {
        projectFinder.delete(project);
    }

    public void addItem(Project project, ManageDbDumpItemRequest request) {
        log.info("Add DB dump items to project {}", project.getName());

        final var item = request.getItem();
        final var items = project.getDbDumpProject().getItems();
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
        secretManager.saveSecret(Secrets.idOfDb(project.getId(), Side.source, item.getSource()), request.getSourcePassword());
        secretManager.saveSecret(Secrets.idOfDb(project.getId(), Side.target, item.getTarget()), request.getTargetPassword());

        final var sourceClient = AWSSecretsManagerClientBuilder.standard()
                .withRegion(project.getSourceRegion().toAwsRegion())
                .withCredentials(Credential.toProvider(secretManager.getCredential(project)))
                .build();
        final var sourceManager = new SecretManager(sourceClient, ObjectMapperSingleton.getObjectMapper());
        sourceManager.saveSecret(Secrets.idOfDb(project.getId(), Side.source, item.getSource()), request.getSourcePassword());
    }

    public void deleteItems(Project project, String[] keys) {
        log.info("Delete {} DB dump items from project {}", keys.length, project.getName());

        final var sourceClient = AWSSecretsManagerClientBuilder.standard()
                .withRegion(project.getSourceRegion().toAwsRegion())
                .withCredentials(Credential.toProvider(secretManager.getCredential(project)))
                .build();
        final var sourceManager = new SecretManager(sourceClient, ObjectMapperSingleton.getObjectMapper());
        final var keySet = Set.of(keys);
        project.getDbDumpProject().getItems().removeIf(item -> {
            sourceManager.deleteSecret(Secrets.idOfDb(project.getId(), Side.source, item.getSource()));
            return keySet.contains(item.getId());
        });
        projectFinder.save(project);
    }

    public String[] getDatabases(Project project, String instanceId) {
        final var sourceMap = getAwsDbInstanceMap(project, Side.source);
        return getDatabasesMachine.getDatabases(project, instanceId, sourceMap.get(instanceId));
    }

    public void dumpItem(Project project, DbDumpItem item) {
        log.info("Dump item {} of project {}", item.getId(), project.getName());
        project.getDbDumpProject().find(item.getId());
    }

    public void restoreItem(Project project, DbDumpItem item) {
        log.info("Restore item {} of project {}", item.getId(), project.getName());
        project.getDbDumpProject().find(item.getId());
    }
}
