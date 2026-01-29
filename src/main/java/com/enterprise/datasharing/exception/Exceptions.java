package com.enterprise.datasharing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exceptions for the application
 */
public class Exceptions {
    // This class is just a container for the exception classes below
}

/**
 * Exception thrown when access is denied
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
@Getter
class AccessDeniedExceptionInternal extends RuntimeException {
    private final String details;

    public AccessDeniedExceptionInternal(String message) {
        super(message);
        this.details = message;
    }

    public AccessDeniedExceptionInternal(String message, String details) {
        super(message);
        this.details = details;
    }
}
