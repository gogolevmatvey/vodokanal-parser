package org.example.model.domain;

import java.util.Objects;

/**
 * Квартира/помещение
 */
public class Apartment {
    private Long id;
    private Long houseId;
    private String number;
    
    public Apartment() {}
    
    public Apartment(Long id, Long houseId, String number) {
        this.id = id;
        this.houseId = houseId;
        this.number = number;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getHouseId() { return houseId; }
    public void setHouseId(Long houseId) { this.houseId = houseId; }
    
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Apartment apartment = (Apartment) o;
        return Objects.equals(id, apartment.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Apartment{id=" + id + ", number='" + number + "'}";
    }
}
