package com.crewmeister.forex.exception;

/**
 * Exception thrown when an external API call fails.
 * Allows callers to decide how to handle external service failures.
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
