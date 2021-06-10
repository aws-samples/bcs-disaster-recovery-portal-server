// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dynamo.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableReplicateDynamoRequest.class)
@JsonDeserialize(as = ImmutableReplicateDynamoRequest.class)
@Value.Immutable
public interface ReplicateDynamoRequest {

    String getProjectId();

    Table getSource();

    Table getTarget();

    @JsonSerialize(as = ImmutableTable.class)
    @JsonDeserialize(as = ImmutableTable.class)
    @Value.Immutable
    interface Table {
        String getTable();

        String getRegion();
    }
}
