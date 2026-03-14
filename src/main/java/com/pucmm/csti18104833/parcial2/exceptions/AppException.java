package com.pucmm.csti18104833.parcial2.exceptions;

public class AppException extends RuntimeException {
    private final int statusCode;

    public AppException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
