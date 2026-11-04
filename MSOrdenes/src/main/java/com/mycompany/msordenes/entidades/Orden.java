package com.mycompany.msordenes.entidades;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import java.util.List;

/**
 *
 * @author katia
 */
@Entity
public class Orden extends PanacheEntity {
    @ElementCollection(fetch = FetchType.EAGER)
    public List<Long> productos;
    public double total;
}
