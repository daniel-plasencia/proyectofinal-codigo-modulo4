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
     * Para items nuevos (antes de validar con Product Service), solo requiere productId y quantity
     * Para items validados, también requiere unitPrice y subtotal
     */
    public boolean isValid() {
        // Validación básica: productId y quantity son obligatorios
        if (productId == null || productId <= 0) {
            return false;
        }
        if (quantity == null || quantity <= 0) {
            return false;
        }
        // unitPrice y subtotal son opcionales al crear (se asignan después de validar con Product Service)
        // Si están presentes, deben ser válidos
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        return true;
    }
}
