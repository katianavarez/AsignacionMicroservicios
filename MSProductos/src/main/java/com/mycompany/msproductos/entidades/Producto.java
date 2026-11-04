package com.mycompany.msproductos.entidades;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

/**
 *
 * @author katia
 */
@Entity
public class Producto extends PanacheEntity {
    public String name;
    public double price;
}
