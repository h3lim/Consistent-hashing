package com.example.consistent_hashing.exception;

public class UserMigrationException extends RuntimeException {
    public UserMigrationException(String message) {
        super(message);
    }

    public UserMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
