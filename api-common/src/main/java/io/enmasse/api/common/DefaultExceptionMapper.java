/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.common;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.enmasse.address.model.UnresolvedAddressException;
import io.enmasse.address.model.UnresolvedAddressSpaceException;
import io.enmasse.address.model.v1.DeserializeException;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {
    protected static final Logger log = LoggerFactory.getLogger(DefaultExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        Response.StatusType status;
        Response response;
        if (exception instanceof WebApplicationException) {
            WebApplicationException webApplicationException = (WebApplicationException) exception;
            status = Response.Status.fromStatusCode(webApplicationException.getResponse().getStatus());
            response = Response.status(status)
                    .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), exception.getMessage()))
                    .build();
        } else if (exception instanceof UnresolvedAddressException || exception instanceof UnresolvedAddressSpaceException || exception instanceof DeserializeException) {
            status = Response.Status.BAD_REQUEST;
            response = Response.status(status)
                    .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), exception.getMessage()))
                    .build();
        } else if (exception instanceof KubernetesClientException) {
            Status kubeStatus = ((KubernetesClientException) exception).getStatus();
            status = Response.Status.fromStatusCode(kubeStatus.getCode());
            response = Response.status(kubeStatus.getCode())
                    .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), exception.getMessage()))
                    .build();
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            response = Response.status(status)
                    .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), exception.getMessage()))
                    .build();
        }

        if (status.getFamily() == Response.Status.Family.CLIENT_ERROR) {
            log.info("Returning client error HTTP status {}: {}", status.getStatusCode(), exception.getMessage());
        } else {
            log.warn("Returning server error HTTP status " + status.getStatusCode(), exception);
        }

        return response;
    }

}
