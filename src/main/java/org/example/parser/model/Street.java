package org.example.parser.model;

import java.util.Objects;

/**
 * Улица
 */
public class Street {
    private Long id;
    private Long localityId;
    private String name;
    private String type; // "ул", "пер", "пр-кт"
    
    public Street() {}
    
    public Street(Long id, Long localityId, String name, String type) {
        this.id = id;
        this.localityId = localityId;
        this.name = name;
        this.type = type;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getLocalityId() { return localityId; }
    public void setLocalityId(Long localityId) { this.localityId = localityId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Street street = (Street) o;
        return Objects.equals(id, street.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Street{id=" + id + ", name='" + name + "', type='" + type + "'}";
    }
}
