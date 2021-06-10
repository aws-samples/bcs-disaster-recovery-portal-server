// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dbdump.api;

import aws.proserve.bcs.dr.aws.AwsDbInstance;
import aws.proserve.bcs.dr.dbdump.AwsDbDumpItem;
import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.dto.request.DeleteItemsRequest;
import aws.proserve.bcs.dr.project.Component;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.server.dbdump.dto.ManageDbDumpItemRequest;
import aws.proserve.bcs.dr.server.dbdump.service.DbDumpService;
import aws.proserve.bcs.dr.server.project.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dbdump/projects")
public class DbDumpProjectController {

    private final DbDumpService service;
    private final ProjectService projectService;

    DbDumpProjectController(
            DbDumpService service,
            ProjectService projectService) {
        this.service = service;
        this.projectService = projectService;
    }

    @GetMapping("/{projectId}/awsDbInstances/{side}/{component}")
    ResponseEntity<AwsDbInstance[]> getAwsDbInstances(
            @PathVariable String projectId,
            @PathVariable Side side,
            @PathVariable Component component) {
        return ResponseEntity.ok(service.getAwsDbInstances(projectService.findOne(projectId), side, component));
    }

    @GetMapping("/{projectId}/awsDbDumpItems")
    ResponseEntity<AwsDbDumpItem[]> getAwsDbDumpItems(@PathVariable String projectId) {
        return ResponseEntity.ok(service.getAwsDbInstanceItems(projectService.findOne(projectId)));
    }

    @PutMapping("/{projectId}/items")
    ResponseEntity<Response> addItems(
            @PathVariable String projectId,
            @RequestBody ManageDbDumpItemRequest request) {
        service.addItem(projectService.findOne(projectId), request);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @DeleteMapping("/{projectId}/items")
    ResponseEntity<Response> deleteItems(
            @PathVariable String projectId,
            @RequestBody DeleteItemsRequest request) {
        service.deleteItems(projectService.findOne(projectId), request.getIds());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @GetMapping("/{projectId}/instances/{instanceId}/databases")
    ResponseEntity<String[]> getDatabases(
            @PathVariable String projectId,
            @PathVariable String instanceId) {
        return ResponseEntity.ok(service.getDatabases(projectService.findOne(projectId), instanceId));
    }

    @PutMapping("/{projectId}/items/dump")
    ResponseEntity<Response> dumpItem(
            @PathVariable String projectId,
            @RequestBody ManageDbDumpItemRequest request) {
        service.dumpItem(projectService.findOne(projectId), request.getItem());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @PutMapping("/{projectId}/items/restore")
    ResponseEntity<Response> restoreItem(
            @PathVariable String projectId,
            @RequestBody ManageDbDumpItemRequest request) {
        service.restoreItem(projectService.findOne(projectId), request.getItem());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }
}
