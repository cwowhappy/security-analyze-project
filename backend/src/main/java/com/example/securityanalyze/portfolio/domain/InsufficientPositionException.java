package com.example.securityanalyze.portfolio.domain;

public class InsufficientPositionException extends RuntimeException {
    public InsufficientPositionException(String message) {
        super(message);
    }
}
