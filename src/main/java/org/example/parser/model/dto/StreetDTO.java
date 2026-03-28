package org.example.parser.model.dto;

/**
 * DTO для отображения улицы в UI
 */
public class StreetDTO {
    private final Long id;
    private final String displayName;
    
    public StreetDTO(Long id, String name, String type) {
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
