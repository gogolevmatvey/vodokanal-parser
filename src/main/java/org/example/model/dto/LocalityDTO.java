package org.example.model.dto;

/**
 * DTO для отображения населенного пункта в UI
 */
public class LocalityDTO {
    private final Long id;
    private final String displayName;
    
    public LocalityDTO(Long id, String name, String type) {
        this.id = id;
        this.displayName = formatDisplayName(name, type);
    }
    
    private String formatDisplayName(String name, String type) {
        if (type == null || type.isEmpty()) {
            return name;
        }
        return type + ". " + name;
    }
    
    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    
    @Override
    public String toString() {
        return displayName;
    }
}
