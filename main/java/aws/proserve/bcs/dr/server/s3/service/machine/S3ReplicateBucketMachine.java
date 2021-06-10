// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.s3.service.machine;

import aws.proserve.bcs.dr.machine.AbstractStateMachine;
import aws.proserve.bcs.dr.project.Project;
import aws.proserve.bcs.dr.s3.S3Item;
import aws.proserve.bcs.dr.s3.S3Item.State;
import aws.proserve.bcs.dr.server.s3.dto.ImmutableBucket;
import aws.proserve.bcs.dr.server.s3.dto.ImmutableReplicateS3Request;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Named;
import java.util.Date;
import java.util.concurrent.ExecutorService;

@Named
public class S3ReplicateBucketMachine extends AbstractStateMachine {
    private final DynamoDBMapper dbMapper;
    private final ExecutorService executor;

    S3ReplicateBucketMachine(
            AWSStepFunctions machine,
            ObjectMapper mapper,
            DynamoDBMapper dbMapper,
            ExecutorService executor) {
        super(machine, mapper);
        this.dbMapper = dbMapper;
        this.executor = executor;
    }

    public void replicate(Project project, S3Item item) {
        log.info("Schedule S3 replication from {} at {} to {} at {}",
                item.getSource(), project.getSourceRegion(),
                item.getTarget(), project.getTargetRegion());

        item.setState(State.REPLICATING.name());
        item.setStartTime(new Date());
        dbMapper.save(project);
        executor.submit(() -> {
            try {
                execute(ImmutableReplicateS3Request.builder()
                        .projectId(project.getId())
                        .source(ImmutableBucket.builder()
                                .bucket(item.getSource())
                                .region(project.getSourceRegion().getName())
                                .build())
                        .target(ImmutableBucket.builder()
                                .bucket(item.getTarget())
                                .region(project.getTargetRegion().getName())
                                .build())
                        .build());
                item.setState(State.REPLICATED.name());
                item.setEndTime(new Date());
                dbMapper.save(project);
            } catch (Exception e) {
                log.warn("S3 replication execution failed.", e);
                item.setState(State.FAILED.name());
                item.setEndTime(new Date());
                dbMapper.save(project);
            }
        });
    }
}
