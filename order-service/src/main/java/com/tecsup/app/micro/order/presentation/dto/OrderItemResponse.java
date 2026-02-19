package com.tecsup.app.micro.order.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta para un item de orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    
    private Long id;
    private ProductResponse product;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
