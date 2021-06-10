// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dynamo.api;

import aws.proserve.bcs.dr.aws.AwsTable;
import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.dto.request.DeleteItemsRequest;
import aws.proserve.bcs.dr.dynamo.AwsTableItem;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.server.dynamo.dto.ManageDynamoItemRequest;
import aws.proserve.bcs.dr.server.dynamo.service.DynamoService;
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
@RequestMapping("/dynamo/projects")
class DynamoProjectController {

    private final DynamoService service;
    private final ProjectService projectService;

    DynamoProjectController(
            DynamoService service,
            ProjectService projectService) {
        this.service = service;
        this.projectService = projectService;
    }

    @GetMapping("/{projectId}/awsTables/{side}")
    ResponseEntity<AwsTable[]> getAwsTables(
            @PathVariable String projectId,
            @PathVariable Side side) {
        return ResponseEntity.ok(service.getAwsTables(projectService.findOne(projectId), side));
    }

    @GetMapping("/{projectId}/awsTableItems")
    ResponseEntity<AwsTableItem[]> getAwsTableItems(@PathVariable String projectId) {
        return ResponseEntity.ok(service.getAwsTableItems(projectService.findOne(projectId)));
    }

    @PutMapping("/{projectId}/items")
    ResponseEntity<Response> addItems(
            @PathVariable String projectId,
            @RequestBody ManageDynamoItemRequest request) {
        service.addItem(projectService.findOne(projectId), request.getItem());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @DeleteMapping("/{projectId}/items")
    ResponseEntity<Response> deleteItems(
            @PathVariable String projectId,
            @RequestBody DeleteItemsRequest request) {
        service.deleteItems(projectService.findOne(projectId), request.getIds());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @PutMapping("/{projectId}/items/start")
    ResponseEntity<Response> startItem(
            @PathVariable String projectId,
            @RequestBody ManageDynamoItemRequest request) {
        service.startItem(projectService.findOne(projectId), request.getItem());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @PutMapping("/{projectId}/items/stop")
    ResponseEntity<Response> stopItem(
            @PathVariable String projectId,
            @RequestBody ManageDynamoItemRequest request) {
        service.stopItem(projectService.findOne(projectId), request.getItem());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }
}
