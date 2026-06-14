package com.flashsale.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "sale_stock")
@Getter
@Setter
public class SaleStock {

    @Id
    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "initial_stock", nullable = false)
    private int initialStock;

    @Column(nullable = false)
    private int sold;
}
