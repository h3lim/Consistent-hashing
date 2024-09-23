package com.example.consistent_hashing.exception;


public class ShardException extends RuntimeException {
    public ShardException(String message) {
        super(message);
    }

    public ShardException(String message, Throwable cause) {
        super(message, cause);
    }
}

