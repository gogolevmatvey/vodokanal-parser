package org.example.model.domain;

import java.util.Objects;

/**
 * Населенный пункт (город, село, поселок, деревня)
 */
public class Locality {
    private Long id;
    private String name;
    private String type; // "г", "с", "п", "дер"
    
    public Locality() {}
    
    public Locality(Long id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Locality locality = (Locality) o;
        return Objects.equals(id, locality.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Locality{id=" + id + ", name='" + name + "', type='" + type + "'}";
    }
}
