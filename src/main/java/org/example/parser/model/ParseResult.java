package org.example.parser.model;

import java.util.Objects;

/**
 * Результат парсинга записи
 */
public class ParseResult {
    private final boolean valid;
    private final String errorMessage;
    private final ParsedRecord recordData;
    
    public ParseResult(boolean valid, String errorMessage, ParsedRecord recordData) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.recordData = recordData;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public ParsedRecord getRecordData() {
        return recordData;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParseResult that = (ParseResult) o;
        return valid == that.valid &&
               Objects.equals(errorMessage, that.errorMessage) &&
               Objects.equals(recordData, that.recordData);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, errorMessage, recordData);
    }
    
    @Override
    public String toString() {
        return "ParseResult{valid=" + valid + ", errorMessage='" + errorMessage + "'}";
    }
}
