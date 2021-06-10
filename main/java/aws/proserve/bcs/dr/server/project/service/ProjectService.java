// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.project.service;

import aws.proserve.bcs.ce.dto.CreateCloudEndureProjectRequest;
import aws.proserve.bcs.ce.service.CloudEndureStateMachineService;
import aws.proserve.bcs.cem.dto.CreateCemProjectRequest;
import aws.proserve.bcs.cem.service.CemService;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectFinder;
import aws.proserve.bcs.dr.server.boot.dto.CreateBootProjectRequest;
import aws.proserve.bcs.dr.server.boot.service.BootService;
import aws.proserve.bcs.dr.server.dbreplica.dto.CreateDbReplicaProjectRequest;
import aws.proserve.bcs.dr.server.dbreplica.service.DbReplicaService;
import aws.proserve.bcs.dr.server.dynamo.dto.CreateDynamoProjectRequest;
import aws.proserve.bcs.dr.server.dynamo.service.DynamoService;
import aws.proserve.bcs.dr.server.dbdump.dto.CreateDbDumpProjectRequest;
import aws.proserve.bcs.dr.server.dbdump.service.DbDumpService;
import aws.proserve.bcs.dr.server.s3.dto.CreateS3ProjectRequest;
import aws.proserve.bcs.dr.server.s3.service.S3Service;
import aws.proserve.bcs.dr.server.vpc.dto.CreateVpcProjectRequest;
import aws.proserve.bcs.dr.server.vpc.service.VpcService;

import javax.inject.Named;
import java.util.List;

@Named
public class ProjectService {

    private final ProjectFinder finder;
    private final BootService bootService;
    private final CemService cemService;
    private final CloudEndureStateMachineService cloudEndureService;
    private final DynamoService dynamoService;
    private final DbDumpService dbDumpService;
    private final DbReplicaService dbReplicaService;
    private final S3Service s3Service;
    private final VpcService vpcService;

    ProjectService(
            ProjectFinder finder,
            BootService bootService,
            CemService cemService,
            CloudEndureStateMachineService cloudEndureService,
            DbDumpService dbDumpService,
            DynamoService dynamoService,
            DbReplicaService dbReplicaService,
            S3Service s3Service,
            VpcService vpcService) {
        this.finder = finder;
        this.bootService = bootService;
        this.cemService = cemService;
        this.cloudEndureService = cloudEndureService;
        this.dbDumpService = dbDumpService;
        this.dbReplicaService = dbReplicaService;
        this.dynamoService = dynamoService;
        this.s3Service = s3Service;
        this.vpcService = vpcService;
    }

    public Project findOne(String id) {
        return finder.findOne(id);
    }

    public List<Project> findAllBoot() {
        final var projects = finder.findByType(Component.Boot);
        projects.forEach(project -> project.setState("参考单项状态"));
        return projects;
    }

    public List<Project> findAllCem() {
        final var projects = finder.findByType(Component.CloudEndureManager);
        projects.forEach(project -> project.setState("参考单项状态"));
        return projects;
    }

    public List<Project> findAllCloudEndure() {
        final var projects = finder.findByType(Component.CloudEndure);
        projects.forEach(project -> project.setState("参考单项状态"));
        return projects;
    }

    public List<Project> findAllDbDump(Component component) {
        final var projects = finder.findByType(component);
        projects.forEach(project -> project.setState("参考单项状态"));
        return projects;
    }

    public List<Project> findAllDbReplica(Component component) {
        final var projects = finder.findByType(component);
        projects.forEach(project -> project.setState("参考单项状态"));
        return projects;
    }

    public List<Project> findAllS3() {
        final var projects = finder.findByType(Component.S3);
        projects.forEach(project -> project.setState("参考单项状态"));
        return projects;
    }

    public List<Project> findAllVpc() {
        final var projects = finder.findByType(Component.VPC);
        projects.forEach(project -> project.setState("参考单项状态"));
        return projects;
    }

    public List<Project> findAllDynamo() {
        final var projects = finder.findByType(Component.DynamoDB);
        projects.forEach(project -> project.setState("参考单项状态"));
        return projects;
    }

    public void save(CreateBootProjectRequest input) {
        bootService.create(input);
    }

    public void save(CreateCemProjectRequest input) {
        cemService.create(input);
    }

    public void save(CreateCloudEndureProjectRequest input) {
        cloudEndureService.create(input);
    }

    public void save(Component component, CreateDbDumpProjectRequest input) {
        dbDumpService.create(component, input);
    }

    public void save(Component component, CreateDbReplicaProjectRequest input) {
        dbReplicaService.create(component, input);
    }

    public void save(CreateS3ProjectRequest input) {
        s3Service.create(input);
    }

    public void save(CreateVpcProjectRequest input) {
        vpcService.create(input);
    }

    public void save(CreateDynamoProjectRequest input) {
        dynamoService.create(input);
    }

    public void delete(Project project) {
        switch (project.getType()) {
            case Boot:
                bootService.delete(project);
                break;

            case CloudEndure:
                cloudEndureService.delete(project);
                break;

            case CloudEndureManager:
                cemService.delete(project);
                break;

            case DynamoDB:
                dynamoService.delete(project);
                break;

            case DbDumpMySql:
            case DbDumpOracle:
                dbDumpService.delete(project);
                break;

            case DbReplicaOracleEc2:
                dbReplicaService.delete(project);

            case S3:
                s3Service.delete(project);
                break;

            case VPC:
                vpcService.delete(project);
                break;
        }
    }
}
