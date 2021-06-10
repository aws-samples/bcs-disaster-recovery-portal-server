// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dbdump.service.machine;

import aws.proserve.bcs.dr.aws.AwsDbInstance;
import aws.proserve.bcs.dr.dbdump.DbDumpItem;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.machine.AbstractStateMachine;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.server.dbdump.dto.ImmutableDbParameter;
import aws.proserve.bcs.dr.server.dbdump.dto.ImmutableMySqlGetDatabasesRequest;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;
import java.io.IOException;
import java.util.List;

@Named
public class DbDumpMySqlGetDatabasesMachine extends AbstractStateMachine {

    DbDumpMySqlGetDatabasesMachine(
            AWSStepFunctions machine,
            ObjectMapper mapper) {
        super(machine, mapper);
    }

    public String[] getDatabases(Project project, String instanceId, AwsDbInstance dbInstance) {
        log.info("Get databases of {} at {} ", instanceId, project.getSourceRegion());

        final DbDumpItem item = project.getDbDumpProject().getItems()
                .stream()
                .filter(i -> i.getSource().equals(instanceId))
                .findFirst()
                .orElseThrow(() -> new PortalException("Unable to find DB instance " + instanceId));

        final var output = execute(ImmutableMySqlGetDatabasesRequest.builder()
                .region(project.getSourceRegion().getName())
                .dbId(dbInstance.getDBInstanceIdentifier())
                .projectId(project.getId())
                .securityGroupIds(dbInstance.getSecurityGroupIds())
                .subnetIds(dbInstance.getSubnetIds())
                .dbParameter(ImmutableDbParameter.builder()
                        .host(dbInstance.getEndpoint().getAddress())
                        .port(dbInstance.getEndpoint().getPort())
                        .username(dbInstance.getMasterUsername())
                        .build())
                .build());
        log.debug("GetDatabases output  {}", output);

        try {
            final List<String> databases = mapper.readValue(output, List.class);
            return databases.toArray(new String[0]);
        } catch (IOException e) {
            log.warn("Unable to parse output", e);
            return null;
        }
    }
}
