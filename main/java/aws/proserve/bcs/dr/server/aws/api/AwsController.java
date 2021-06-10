// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.aws.api;

import aws.proserve.bcs.dr.aws.AwsInstanceType;
import aws.proserve.bcs.dr.aws.AwsSecurityGroup;
import aws.proserve.bcs.dr.project.Region;
import aws.proserve.bcs.dr.server.aws.service.AwsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/aws")
@RestController
class AwsController {

    private final AwsService service;

    AwsController(AwsService service) {
        this.service = service;
    }

    @GetMapping("/regions")
    ResponseEntity<Region[]> getRegions() {
        return ResponseEntity.ok(service.getRegions());
    }

    @GetMapping("/regions/{region}/instanceTypes")
    ResponseEntity<AwsInstanceType[]> getRegions(@PathVariable String region) {
        return ResponseEntity.ok(service.getInstanceTypes(region));
    }

    /**
     * @return security groups of a VPC in a region.
     */
    @GetMapping("/regions/{region}/vpcs/{vpcId}/securityGroups")
    ResponseEntity<AwsSecurityGroup[]> getSecurityGroups(
            @PathVariable String region,
            @PathVariable String vpcId) {
        return ResponseEntity.ok(service.getSecurityGroups(region, vpcId));
    }
}
