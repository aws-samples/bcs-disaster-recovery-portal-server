// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dbdump.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableMySqlGetDatabasesRequest.class)
@JsonDeserialize(as = ImmutableMySqlGetDatabasesRequest.class)
@Value.Immutable
public interface MySqlGetDatabasesRequest {

    String getDbId();

    String getProjectId();

    String getRegion();

    DbParameter getDbParameter();

    String[] getSubnetIds();

    String[] getSecurityGroupIds();

    @JsonSerialize(as = ImmutableDbParameter.class)
    @JsonDeserialize(as = ImmutableDbParameter.class)
    @Value.Immutable
    interface DbParameter {
        String getHost();

        int getPort();

        String getUsername();
    }
}
