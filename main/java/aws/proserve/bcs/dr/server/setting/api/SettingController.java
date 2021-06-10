// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.setting.api;

import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.server.setting.service.SettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/setting")
class SettingController {

    private final SettingService service;

    SettingController(SettingService service) {
        this.service = service;
    }

    @GetMapping("/cloudendure/apiToken")
    ResponseEntity<String> getApiToken() {
        return ResponseEntity.ok(service.getApiToken());
    }

    @PutMapping("/cloudendure/apiToken/{token}")
    ResponseEntity<Response> setApiToken(@PathVariable String token) {
        service.setApiToken(token);
        return ResponseEntity.accepted().body(Response.SUCCESS);
    }
}
