// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.vpc.api;

import aws.proserve.bcs.dr.aws.AwsVpc;
import aws.proserve.bcs.dr.server.vpc.service.VpcService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vpc")
class VpcController {

    private final VpcService service;

    VpcController(VpcService service) {
        this.service = service;
    }

    @GetMapping("/regions/{region}/awsVpcs")
    ResponseEntity<AwsVpc[]> getAwsVpcs(@PathVariable String region) {
        return ResponseEntity.ok(service.getAwsVpcs(region));
    }
}
