package org.example.model.dto;

/**
 * DTO для отображения улицы в UI
 */
public class StreetDTO {
    private final Long id;
    private final String name;

    public StreetDTO(Long id, String name) {
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
