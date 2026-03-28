package org.example.exception;

/**
 * Исключение, возникающее когда сущность не найдена
 */
public class NotFoundException extends RuntimeException {
    
    public NotFoundException(String message) {
        super(message);
    }
    
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
