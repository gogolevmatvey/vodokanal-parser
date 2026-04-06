package org.example.model.domain;

import java.util.Objects;

/**
 * Населенный пункт (город, село, поселок, деревня)
 * Тип (г, с, п, дер) хранится вместе с названием в name.
 */
public class Locality {
    private Long id;
    private String name;

    public Locality() {}

    public Locality(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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
        return "Locality{id=" + id + ", name='" + name + "'}";
    }
}
