// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.s3.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableReplicateS3Request.class)
@JsonDeserialize(as = ImmutableReplicateS3Request.class)
@Value.Immutable
public interface ReplicateS3Request {

    String getProjectId();

    Bucket getSource();

    Bucket getTarget();

    @JsonSerialize(as = ImmutableBucket.class)
    @JsonDeserialize(as = ImmutableBucket.class)
    @Value.Immutable
    interface Bucket {
        String getBucket();

        String getRegion();
    }
}
