// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dynamo.dto;

import aws.proserve.bcs.dr.dto.request.ManageItemRequest;
import aws.proserve.bcs.dr.dynamo.DynamoItem;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableManageDynamoItemRequest.class)
@JsonDeserialize(as = ImmutableManageDynamoItemRequest.class)
@Value.Immutable
public interface ManageDynamoItemRequest extends ManageItemRequest<DynamoItem> {

}
