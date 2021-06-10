// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.boot.api;


import aws.proserve.bcs.dr.server.boot.service.BootService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/boot/projects")
class BootProjectController {

    private final BootService service;

    BootProjectController(BootService service) {
        this.service = service;
    }
}
