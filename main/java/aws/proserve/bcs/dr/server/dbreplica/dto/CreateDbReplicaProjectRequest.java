// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.dbreplica.dto;

import aws.proserve.bcs.dr.dto.request.CreateProjectRequest;
import aws.proserve.bcs.dr.secret.Credential;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * From GWT.
 */
@JsonSerialize(as = ImmutableCreateDbReplicaProjectRequest.class)
@JsonDeserialize(as = ImmutableCreateDbReplicaProjectRequest.class)
@Value.Immutable
public interface CreateDbReplicaProjectRequest extends CreateProjectRequest {

    Credential getSourceCredential();
}
