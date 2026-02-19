package com.tecsup.app.micro.order.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta para un producto (simplificado)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
}
