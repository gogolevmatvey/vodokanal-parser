package org.example.model.dto;

/**
 * DTO для отображения дома в UI
 */
public class HouseDTO {
    private final Long id;
    private final String displayNumber;
    
    public HouseDTO(Long id, String number, String building) {
        this.id = id;
        this.displayNumber = formatDisplayNumber(number, building);
    }
    
    private String formatDisplayNumber(String number, String building) {
        if (building == null || building.isEmpty()) {
            return number;
        }
        return number + " " + building;
    }
    
    public Long getId() { return id; }
    public String getDisplayNumber() { return displayNumber; }
    
    @Override
    public String toString() {
        return displayNumber;
    }
}
