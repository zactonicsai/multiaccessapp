package com.enterprise.datasharing.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when access is denied by access control
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
@Getter
public class AccessDeniedException extends RuntimeException {
    
    private final String details;

    public AccessDeniedException(String message) {
        super(message);
        this.details = message;
    }

    public AccessDeniedException(String message, String details) {
        super(message);
        this.details = details;
    }
}
