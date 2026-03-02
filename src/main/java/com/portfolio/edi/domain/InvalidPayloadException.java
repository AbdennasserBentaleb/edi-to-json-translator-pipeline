package com.portfolio.edi.domain;

/**
 * Exception thrown when the incoming flat file or XML does
 * not meet basic validation requirements.
 * Used to trigger the Apache Camel Dead Letter Channel.
 */
public class InvalidPayloadException extends RuntimeException {

    public InvalidPayloadException(String message) {
        super(message);
    }
    
    public InvalidPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
