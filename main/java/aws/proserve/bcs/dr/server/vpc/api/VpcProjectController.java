// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.vpc.api;

import aws.proserve.bcs.dr.aws.AwsVpc;
import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.dto.request.DeleteItemsRequest;
import aws.proserve.bcs.dr.server.project.service.ProjectService;
import aws.proserve.bcs.dr.server.vpc.dto.ManageVpcItemRequest;
import aws.proserve.bcs.dr.server.vpc.dto.ManageVpcItemsRequest;
import aws.proserve.bcs.dr.server.vpc.service.VpcService;
import aws.proserve.bcs.dr.vpc.AwsVpcItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vpc/projects")
class VpcProjectController {

    private final VpcService service;
    private final ProjectService projectService;

    VpcProjectController(
            VpcService service,
            ProjectService projectService) {
        this.service = service;
        this.projectService = projectService;
    }

    @GetMapping("/{projectId}/awsVpcs")
    ResponseEntity<AwsVpc[]> getAwsVpcs(@PathVariable String projectId) {
        return ResponseEntity.ok(service.getAwsVpcs(projectService.findOne(projectId)));
    }

    @GetMapping("/{projectId}/awsVpcItems")
    ResponseEntity<AwsVpcItem[]> getAwsVpcItems(@PathVariable String projectId) {
        return ResponseEntity.ok(service.getAwsVpcItems(projectService.findOne(projectId)));
    }

    @PutMapping("/{projectId}/items")
    ResponseEntity<Response> addItems(
            @PathVariable String projectId,
            @RequestBody ManageVpcItemsRequest request) {
        service.addItems(projectService.findOne(projectId), request.getItems());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @DeleteMapping("/{projectId}/items")
    ResponseEntity<Response> deleteItems(
            @PathVariable String projectId,
            @RequestBody DeleteItemsRequest request) {
        service.deleteItems(projectService.findOne(projectId), request.getIds());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }

    @PostMapping("/{projectId}/items")
    ResponseEntity<Response> replicateItem(
            @PathVariable String projectId,
            @RequestBody ManageVpcItemRequest request) {
        service.replicateItem(projectService.findOne(projectId), request.getItem());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }
}
