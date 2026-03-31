package org.example.model.dto;

/**
 * DTO для отображения населенного пункта в UI
 */
public class LocalityDTO {
    private final Long id;
    private final String name;

    public LocalityDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
