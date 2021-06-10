// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dbreplica.api;

import aws.proserve.bcs.dr.aws.AwsInstance;
import aws.proserve.bcs.dr.dbdump.AwsDbDumpItem;
import aws.proserve.bcs.dr.dbreplica.AwsDbReplicaEc2Item;
import aws.proserve.bcs.dr.dbreplica.AwsDbReplicaItem;
import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.dto.request.DeleteItemsRequest;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.server.dbreplica.dto.ManageDbReplicaItemRequest;
import aws.proserve.bcs.dr.server.dbreplica.service.DbReplicaService;
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
@RequestMapping("/dbreplica/projects")
public class DbReplicaProjectController {

    private final DbReplicaService service;
    private final ProjectService projectService;

    DbReplicaProjectController(
            DbReplicaService service,
            ProjectService projectService) {
        this.service = service;
        this.projectService = projectService;
    }

    @GetMapping("/{projectId}/awsInstances/{side}")
    ResponseEntity<AwsInstance[]> getAwsInstances(
            @PathVariable String projectId,
            @PathVariable Side side) {
        return ResponseEntity.ok(service.getAwsInstances(projectService.findOne(projectId), side));
    }

    @GetMapping("/{projectId}/awsDbReplicaEc2Items")
    ResponseEntity<AwsDbReplicaEc2Item[]> getAwsDbReplicaEc2Items(@PathVariable String projectId) {
        return ResponseEntity.ok(service.getAwsDbReplicaEc2Items(projectService.findOne(projectId)));
    }

    @PutMapping("/{projectId}/items")
    ResponseEntity<Response> addItems(
            @PathVariable String projectId,
            @RequestBody ManageDbReplicaItemRequest request) {
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

}
