package org.example.parser.model;

import java.util.Objects;

/**
 * Дом
 */
public class House {
    private Long id;
    private Long streetId;
    private String number;
    private String building; // "корп.", "стр."
    
    public House() {}
    
    public House(Long id, Long streetId, String number, String building) {
        this.id = id;
        this.streetId = streetId;
        this.number = number;
        this.building = building;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getStreetId() { return streetId; }
    public void setStreetId(Long streetId) { this.streetId = streetId; }
    
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        House house = (House) o;
        return Objects.equals(id, house.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "House{id=" + id + ", number='" + number + "', building='" + building + "'}";
    }
}
