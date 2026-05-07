package com.example.securityanalyze.portfolio.api;

public class InsufficientPositionException extends RuntimeException {
    public InsufficientPositionException(String message) {
        super(message);
    }
}
