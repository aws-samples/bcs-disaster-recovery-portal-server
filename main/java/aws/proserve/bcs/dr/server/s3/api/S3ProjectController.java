// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.s3.api;

import aws.proserve.bcs.dr.aws.AwsBucket;
import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.dto.request.DeleteItemsRequest;
import aws.proserve.bcs.dr.s3.AwsBucketItem;
import aws.proserve.bcs.dr.server.project.service.ProjectService;
import aws.proserve.bcs.dr.server.s3.dto.ManageS3ItemRequest;
import aws.proserve.bcs.dr.server.s3.service.S3Service;
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
@RequestMapping("/s3/projects")
class S3ProjectController {

    private final S3Service service;
    private final ProjectService projectService;

    S3ProjectController(
            S3Service service,
            ProjectService projectService) {
        this.service = service;
        this.projectService = projectService;
    }

    @GetMapping("/{projectId}/awsBuckets")
    ResponseEntity<AwsBucket[]> getAwsBuckets(@PathVariable String projectId) {
        projectService.findOne(projectId);
        return ResponseEntity.ok(service.getAwsBuckets());
    }

    @GetMapping("/{projectId}/awsBucketItems")
    ResponseEntity<AwsBucketItem[]> getAwsBucketItems(@PathVariable String projectId) {
        return ResponseEntity.ok(service.getAwsBucketItems(projectService.findOne(projectId)));
    }

    @PutMapping("/{projectId}/items")
    ResponseEntity<Response> addItems(
            @PathVariable String projectId,
            @RequestBody ManageS3ItemRequest request) {
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

    @PostMapping("/{projectId}/items")
    ResponseEntity<Response> replicateItem(
            @PathVariable String projectId,
            @RequestBody ManageS3ItemRequest request) {
        service.replicateItem(projectService.findOne(projectId), request.getItem());
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }
}
