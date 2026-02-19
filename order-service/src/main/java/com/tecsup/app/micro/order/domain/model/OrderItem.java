package com.tecsup.app.micro.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * OrderItem Domain Model
 * Representa un item dentro de una orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    private Long id;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    
    // Referencia al producto (obtenido del Product Service)
    private Product product;
    
    /**
     * Calcula el subtotal (quantity × unitPrice)
     */
    public BigDecimal calculateSubtotal() {
        if (quantity == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Valida que el item tenga los datos mínimos requeridos
     */
    public boolean isValid() {
        return productId != null && productId > 0
            && quantity != null && quantity > 0
            && unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) >= 0;
    }
}
