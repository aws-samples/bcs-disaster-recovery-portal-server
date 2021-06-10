// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.server.exception;

import aws.proserve.bcs.ce.exception.CloudEndureException;
import aws.proserve.bcs.dr.dto.Response;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.exception.ProjectNotFoundException;
import aws.proserve.bcs.dr.machine.StateMachineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class PortalExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(StateMachineException.class)
    public ResponseEntity<Response> handle(StateMachineException e) {
        log.warn("StateMachineException", e);
        return ResponseEntity.badRequest().body(Response.unsuccessful(e.getLocalizedMessage()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CloudEndureException.class)
    public ResponseEntity<Response> handle(CloudEndureException e) {
        log.warn("CloudEndureException", e);
        return ResponseEntity.badRequest().body(Response.unsuccessful(e.getLocalizedMessage()));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<Response> handle(ProjectNotFoundException e) {
        log.warn("ProjectNotFoundException", e);
        return ResponseEntity.badRequest().body(Response.unsuccessful(e.getLocalizedMessage()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PortalException.class)
    public ResponseEntity<Response> handle(PortalException e) {
        log.warn("PortalException", e);
        return ResponseEntity.badRequest().body(Response.unsuccessful(e.getLocalizedMessage()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handle(Exception e) {
        log.warn("Exception", e);
        return ResponseEntity.badRequest().body(Response.fail(e.getLocalizedMessage()));
    }
}
