// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.vpc.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@JsonSerialize(as = ImmutableReplicateVpcRequest.class)
@JsonDeserialize(as = ImmutableReplicateVpcRequest.class)
@Value.Immutable
public interface ReplicateVpcRequest {

    VpcInfo getSource();

    VpcInfo getTarget();

    @Nullable
    String getCidr();

    /**
     * @apiNote Immutables does not recognise <code>is</code> prefix for boolean.
     */
    boolean getContinuous();

    @JsonSerialize(as = ImmutableVpcInfo.class)
    @JsonDeserialize(as = ImmutableVpcInfo.class)
    @Value.Immutable
    interface VpcInfo {
        @Value.Default
        @Nullable
        default String getVpcId() {
            return null;
        }

        String getRegion();
    }
}
