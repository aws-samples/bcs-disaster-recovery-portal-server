// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.s3.dto;

import aws.proserve.bcs.dr.dto.request.ManageItemRequest;
import aws.proserve.bcs.dr.s3.S3Item;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableManageS3ItemRequest.class)
@JsonDeserialize(as = ImmutableManageS3ItemRequest.class)
@Value.Immutable
public interface ManageS3ItemRequest extends ManageItemRequest<S3Item> {

}
