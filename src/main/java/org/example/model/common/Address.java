package org.example.model.common;

import java.util.Objects;

/**
 * Адрес
 */
public class Address {
    private final Long localityId;
    private final Long streetId;
    private final Long houseId;
    private final Long apartmentId;
    private final String fullAddress;
    
    public Address(Long localityId, Long streetId, Long houseId, Long apartmentId, String fullAddress) {
        this.localityId = localityId;
        this.streetId = streetId;
        this.houseId = houseId;
        this.apartmentId = apartmentId;
        this.fullAddress = fullAddress;
    }
    
    public Long getLocalityId() { return localityId; }
    public Long getStreetId() { return streetId; }
    public Long getHouseId() { return houseId; }
    public Long getApartmentId() { return apartmentId; }
    public String getFullAddress() { return fullAddress; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(apartmentId, address.apartmentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(apartmentId);
    }
    
    @Override
    public String toString() {
        return "Address{fullAddress='" + fullAddress + "'}";
    }
}
