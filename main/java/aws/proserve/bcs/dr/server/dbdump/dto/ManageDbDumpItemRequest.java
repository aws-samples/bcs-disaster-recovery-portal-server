// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dbdump.dto;

import aws.proserve.bcs.dr.dto.request.ManageItemRequest;
import aws.proserve.bcs.dr.dbdump.DbDumpItem;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableManageDbDumpItemRequest.class)
@JsonDeserialize(as = ImmutableManageDbDumpItemRequest.class)
@Value.Immutable
public interface ManageDbDumpItemRequest extends ManageItemRequest<DbDumpItem> {

    String getSourcePassword();

    String getTargetPassword();
}
