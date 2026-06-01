package com.flashsale.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@Setter
public class Product {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;
}
