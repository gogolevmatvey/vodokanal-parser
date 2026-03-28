package org.example.exception;

/**
 * Исключение, возникающее при ошибке работы с базой данных
 */
public class DatabaseException extends RuntimeException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
