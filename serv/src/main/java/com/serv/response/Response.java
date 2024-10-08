package com.serv.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Response {

    public Response(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    private Status status;
    private String message;

    public interface IStatus {

        String getDescription();

        int getRequestStatus();
    }

    /**
     * Some HTTP response status codes
     */
    @Getter
    public enum Status implements IStatus, Serializable {
        SWITCH_PROTOCOL(101, "Switching Protocols"),
        OK(200, "OK"),
        CREATED(201, "Created"),
        ACCEPTED(202, "Accepted"),
        NO_CONTENT(204, "No Content"),
        PARTIAL_CONTENT(206, "Partial Content"),
        REDIRECT(301, "Moved Permanently"),
        NOT_MODIFIED(304, "Not Modified"),
        BAD_REQUEST(400, "Bad Request"),
        UNAUTHORIZED(401, "Unauthorized"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
        NOT_ACCEPTABLE(406, "Not Acceptable"),
        REQUEST_TIMEOUT(408, "Request Timeout"),
        CONFLICT(409, "Conflict"),
        RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
        INTERNAL_ERROR(500, "Internal Server Error"),
        NOT_IMPLEMENTED(501, "Not Implemented"),
        UNSUPPORTED_HTTP_VERSION(505, "HTTP Version Not Supported");

        private final int requestStatus;

        private final String description;

        Status(int requestStatus, String description) {
            this.requestStatus = requestStatus;
            this.description = description;
        }

        @Override
        public String getDescription() {
            return this.requestStatus + " " + this.description;
        }

    }
}
