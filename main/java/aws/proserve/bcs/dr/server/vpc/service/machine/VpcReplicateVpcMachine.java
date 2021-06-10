// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.vpc.service.machine;

import aws.proserve.bcs.dr.machine.AbstractStateMachine;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.server.vpc.dto.ImmutableReplicateVpcRequest;
import aws.proserve.bcs.dr.server.vpc.dto.ImmutableVpcInfo;
import aws.proserve.bcs.dr.vpc.VpcItem;
import aws.proserve.bcs.dr.vpc.VpcItem.State;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;
import java.util.Date;
import java.util.concurrent.ExecutorService;

@Named
public class VpcReplicateVpcMachine extends AbstractStateMachine {
    private final DynamoDBMapper dbMapper;
    private final ExecutorService executor;

    VpcReplicateVpcMachine(
            AWSStepFunctions machine,
            ObjectMapper mapper,
            DynamoDBMapper dbMapper,
            ExecutorService executor) {
        super(machine, mapper);
        this.dbMapper = dbMapper;
        this.executor = executor;
    }

    public void replicate(Project project, VpcItem item) {
        log.info("Schedule VPC replication for {}", item.getSource());
        item.setState(State.REPLICATING.name());
        item.setStartTime(new Date());
        dbMapper.save(project);
        executor.submit(() -> {
            try {
                final String output = execute(ImmutableReplicateVpcRequest.builder()
                        .cidr(item.getCidr())
                        .continuous(item.isContinuous())
                        .source(ImmutableVpcInfo.builder()
                                .vpcId(item.getSource())
                                .region(project.getSourceRegion().getName())
                                .build())
                        .target(ImmutableVpcInfo.builder()
                                .region(project.getTargetRegion().getName())
                                .build())
                        .build());
                final String result = mapper.readValue(output, String.class);
                item.setTarget(result);
                item.setState(State.REPLICATED.name());
                item.setEndTime(new Date());
                dbMapper.save(project);
            } catch (Exception e) {
                log.warn("VPC replication execution failed.", e);
                item.setState(State.FAILED.name());
                item.setEndTime(new Date());
                dbMapper.save(project);
            }
        });
    }
}
