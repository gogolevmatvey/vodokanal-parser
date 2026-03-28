package org.example.exception;

/**
 * Исключение, возникающее при ошибке парсинга записи
 */
public class RecordParseException extends RuntimeException {
    
    public RecordParseException(String message) {
        super(message);
    }
    
    public RecordParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
