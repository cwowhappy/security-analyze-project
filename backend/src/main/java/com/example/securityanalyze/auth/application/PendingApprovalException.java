package com.example.securityanalyze.auth.application;

public class PendingApprovalException extends RuntimeException {
    public PendingApprovalException(String message) {
        super(message);
    }
}
