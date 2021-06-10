// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dbreplica.dto;

import aws.proserve.bcs.dr.dbreplica.DbReplicaItem;
import aws.proserve.bcs.dr.dto.request.ManageItemRequest;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableManageDbReplicaItemRequest.class)
@JsonDeserialize(as = ImmutableManageDbReplicaItemRequest.class)
@Value.Immutable
public interface ManageDbReplicaItemRequest extends ManageItemRequest<DbReplicaItem> {

}
