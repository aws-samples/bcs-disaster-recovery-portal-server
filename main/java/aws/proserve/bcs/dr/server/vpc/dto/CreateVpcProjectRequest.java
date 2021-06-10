// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.vpc.dto;

import aws.proserve.bcs.dr.dto.request.CreateProjectRequest;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * From GWT.
 */
@JsonSerialize(as = ImmutableCreateVpcProjectRequest.class)
@JsonDeserialize(as = ImmutableCreateVpcProjectRequest.class)
@Value.Immutable
public interface CreateVpcProjectRequest extends CreateProjectRequest {

}
