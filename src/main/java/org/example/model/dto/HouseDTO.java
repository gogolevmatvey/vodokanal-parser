package org.example.model.dto;

/**
 * DTO для отображения дома в UI
 */
public class HouseDTO {
    private final Long id;
    private final String number;

    public HouseDTO(Long id, String number) {
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
