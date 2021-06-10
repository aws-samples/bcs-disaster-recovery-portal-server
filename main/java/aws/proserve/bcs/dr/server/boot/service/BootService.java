// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.boot.service;

import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.project.ProjectService;
import aws.proserve.bcs.dr.server.boot.dto.CreateBootProjectRequest;

import javax.inject.Named;

@Named
public class BootService implements ProjectService {

    BootService() {
    }

    public void create(CreateBootProjectRequest request) {
    }

    @Override
    public void delete(Project project) {
    }
}
