// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.setting.service;

import aws.proserve.bcs.ce.service.SessionService;

import javax.inject.Named;

@Named
public class SettingService {

    private final SessionService sessionService;

    SettingService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public String getApiToken() {
        return sessionService.getToken();
    }

    public void setApiToken(String token) {
        sessionService.setToken(token);
    }
}
