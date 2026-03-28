package org.example.parser.model.dto;

/**
 * DTO для отображения квартиры в UI
 */
public class ApartmentDTO {
    private final Long id;
    private final String number;
    
    public ApartmentDTO(Long id, String number) {
        this.id = id;
        this.number = number;
    }
    
    public Long getId() { return id; }
    public String getNumber() { return number; }
    
    @Override
    public String toString() {
        return number;
    }
}
