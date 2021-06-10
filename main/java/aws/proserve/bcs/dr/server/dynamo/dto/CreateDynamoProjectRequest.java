// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dynamo.dto;

import aws.proserve.bcs.dr.dto.request.CreateProjectRequest;
import aws.proserve.bcs.dr.secret.Credential;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * From GWT.
 */
@JsonSerialize(as = ImmutableCreateDynamoProjectRequest.class)
@JsonDeserialize(as = ImmutableCreateDynamoProjectRequest.class)
@Value.Immutable
public interface CreateDynamoProjectRequest extends CreateProjectRequest {

    Credential getSourceCredential();
}
