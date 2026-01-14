package com.alura.churnnsight.exception;

public class DownstreamException extends RuntimeException {
    private final String service;
    private final int status;
    private final String body;

    public DownstreamException(String service, int status, String body) {
        super(service + " error " + status);
        this.service = service;
        this.status = status;
        this.body = body;
    }

    public String getService() {
        return service;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }
}
