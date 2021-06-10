// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.project.api;

import aws.proserve.bcs.ce.dto.CreateCloudEndureProjectRequest;
import aws.proserve.bcs.ce.service.CloudEndureInstanceService;
import aws.proserve.bcs.cem.dto.CreateCemProjectRequest;
import aws.proserve.bcs.dr.aws.AwsInstance;
import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.server.boot.dto.CreateBootProjectRequest;
import aws.proserve.bcs.dr.server.dbdump.dto.CreateDbDumpProjectRequest;
import aws.proserve.bcs.dr.server.dbreplica.dto.CreateDbReplicaProjectRequest;
import aws.proserve.bcs.dr.server.dynamo.dto.CreateDynamoProjectRequest;
import aws.proserve.bcs.dr.server.project.service.ProjectService;
import aws.proserve.bcs.dr.server.s3.dto.CreateS3ProjectRequest;
import aws.proserve.bcs.dr.server.vpc.dto.CreateVpcProjectRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Named;
import java.util.List;

@RestController
@RequestMapping("/projects")
class ProjectController {

    private final ProjectService service;
    private final CloudEndureInstanceService cloudEndureInstanceService;

    ProjectController(
            @Named ProjectService service,
            CloudEndureInstanceService cloudEndureInstanceService) {
        this.service = service;
        this.cloudEndureInstanceService = cloudEndureInstanceService;
    }

    @GetMapping("/boot")
    ResponseEntity<List<Project>> findAllBoot() {
        return ResponseEntity.ok(service.findAllCloudEndure());
    }

    @PostMapping("/boot")
    ResponseEntity<Response> saveBoot(@RequestBody CreateBootProjectRequest request) {
        service.save(request);
        return ResponseEntity.ok(Response.SUCCESS);
    }

    @GetMapping("/cem")
    ResponseEntity<List<Project>> findAllCem() {
        return ResponseEntity.ok(service.findAllCem());
    }

    @PostMapping("/cem")
    ResponseEntity<Response> saveCem(@RequestBody CreateCemProjectRequest request) {
        service.save(request);
        return ResponseEntity.ok(Response.SUCCESS);
    }

    @GetMapping("/cloudendure")
    ResponseEntity<List<Project>> findAllCloudEndure() {
        return ResponseEntity.ok(service.findAllCloudEndure());
    }

    @PostMapping("/cloudendure")
    ResponseEntity<Response> saveCloudEndure(@RequestBody CreateCloudEndureProjectRequest request) {
        service.save(request);
        return ResponseEntity.ok(Response.SUCCESS);
    }

    @GetMapping("/s3")
    ResponseEntity<List<Project>> findAllS3() {
        return ResponseEntity.ok(service.findAllS3());
    }

    @PostMapping("/s3")
    ResponseEntity<Response> saveS3(@RequestBody CreateS3ProjectRequest request) {
        service.save(request);
        return ResponseEntity.ok(Response.SUCCESS);
    }

    @GetMapping("/vpc")
    ResponseEntity<List<Project>> findAllVpc() {
        return ResponseEntity.ok(service.findAllVpc());
    }

    @PostMapping("/vpc")
    ResponseEntity<Response> saveVpc(@RequestBody CreateVpcProjectRequest request) {
        service.save(request);
        return ResponseEntity.ok(Response.SUCCESS);
    }

    @GetMapping("/dynamo")
    ResponseEntity<List<Project>> findAllDynamo() {
        return ResponseEntity.ok(service.findAllDynamo());
    }

    @PostMapping("/dynamo")
    ResponseEntity<Response> saveDynamo(@RequestBody CreateDynamoProjectRequest request) {
        service.save(request);
        return ResponseEntity.ok(Response.SUCCESS);
    }

    @GetMapping("/dbdump/{component}")
    ResponseEntity<List<Project>> findAllDbDump(@PathVariable Component component) {
        return ResponseEntity.ok(service.findAllDbDump(component));
    }

    @PostMapping("/dbdump/{component}")
    ResponseEntity<Response> saveDbDump(
            @PathVariable Component component,
            @RequestBody CreateDbDumpProjectRequest request) {
        service.save(component, request);
        return ResponseEntity.ok(Response.SUCCESS);
    }

    @GetMapping("/dbreplica/{component}")
    ResponseEntity<List<Project>> findAllDbReplica(@PathVariable Component component) {
        return ResponseEntity.ok(service.findAllDbReplica(component));
    }

    @PostMapping("/dbreplica/{component}")
    ResponseEntity<Response> saveDbReplica(
            @PathVariable Component component,
            @RequestBody CreateDbReplicaProjectRequest request) {
        service.save(component, request);
        return ResponseEntity.ok(Response.SUCCESS);
    }

    @GetMapping("/{projectId}")
    ResponseEntity<Project> findOne(@PathVariable String projectId) {
        return ResponseEntity.ok(service.findOne(projectId));
    }

    @GetMapping("/{projectId}/instances/{side}")
    ResponseEntity<AwsInstance[]> findAllMachines(
            @PathVariable String projectId,
            @PathVariable Side side) {
        return ResponseEntity.ok(cloudEndureInstanceService.findAllQualifiedInstances(service.findOne(projectId), side));
    }

    @DeleteMapping("/{projectId}")
    ResponseEntity<Response> delete(@PathVariable String projectId) {
        service.delete(service.findOne(projectId));
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }
}
