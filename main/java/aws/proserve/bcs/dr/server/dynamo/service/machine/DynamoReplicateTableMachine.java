// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dynamo.service.machine;

import aws.proserve.bcs.dr.dynamo.DynamoItem;
import aws.proserve.bcs.dr.dynamo.DynamoItem.State;
import aws.proserve.bcs.dr.machine.AbstractStateMachine;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.server.dynamo.dto.ImmutableReplicateDynamoRequest;
import aws.proserve.bcs.dr.server.dynamo.dto.ImmutableTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.AWSStepFunctionsException;
import com.amazonaws.services.stepfunctions.model.StopExecutionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;
import java.util.Date;

@Named
public class DynamoReplicateTableMachine extends AbstractStateMachine {

    private final DynamoDBMapper dbMapper;

    DynamoReplicateTableMachine(
            AWSStepFunctions machine,
            ObjectMapper mapper,
            DynamoDBMapper dbMapper) {
        super(machine, mapper);
        this.dbMapper = dbMapper;
    }

    public void start(Project project, DynamoItem item) {
        log.info("Schedule table replication from {} at {} to {} at {}",
                item.getSource(), project.getSourceRegion(),
                item.getTarget(), project.getTargetRegion());

        item.setExecutionArn(
                executeAsync(ImmutableReplicateDynamoRequest.builder()
                        .projectId(project.getId())
                        .source(ImmutableTable.builder()
                                .table(item.getSource())
                                .region(project.getSourceRegion().getName())
                                .build())
                        .target(ImmutableTable.builder()
                                .table(item.getTarget())
                                .region(project.getTargetRegion().getName())
                                .build())
                        .build()));
        item.setState(State.REPLICATING.name());
        item.setStartTime(new Date());
        dbMapper.save(project);
    }

    public void stop(Project project, DynamoItem config) {
        log.info("Stop table replication from {} at {} to {} at {}",
                config.getSource(), project.getSourceRegion(),
                config.getTarget(), project.getTargetRegion());

        try {
            machine.stopExecution(new StopExecutionRequest().withExecutionArn(config.getExecutionArn()));
        } catch (AWSStepFunctionsException e) {
            log.warn("Unable to stop execution", e);
        }

        config.setState(State.STOPPED.name());
        config.setEndTime(new Date());
        dbMapper.save(project);
    }
}
