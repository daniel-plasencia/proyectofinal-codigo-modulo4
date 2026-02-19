package com.tecsup.app.micro.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product Domain Model
 * Representa un producto obtenido del Product Service
 * Esta es una copia simplificada del modelo de Product Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
}
