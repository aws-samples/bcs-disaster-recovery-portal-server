// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.vpc.dto;

import aws.proserve.bcs.dr.dto.request.ManageItemsRequest;
import aws.proserve.bcs.dr.vpc.VpcItem;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableManageVpcItemsRequest.class)
@JsonDeserialize(as = ImmutableManageVpcItemsRequest.class)
@Value.Immutable
public interface ManageVpcItemsRequest extends ManageItemsRequest<VpcItem> {

    /**
     * @apiNote non-reified array is not supported.
     */
    @Override
    VpcItem[] getItems();
}
