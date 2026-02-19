package com.tecsup.app.micro.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Domain Model (Core Business Entity)
 * Esta es la entidad de dominio pura, sin dependencias de frameworks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    private Long id;
    private String orderNumber;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
    
    /**
     * Calcula el total de la orden basándose en los items
     */
    public BigDecimal calculateTotal() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Valida que la orden tenga los datos mínimos requeridos
     */
    public boolean isValid() {
        return userId != null && userId > 0
            && items != null && !items.isEmpty()
            && items.stream().allMatch(OrderItem::isValid);
    }
    
    /**
     * Genera un número de orden único basado en el año y un contador
     */
    public static String generateOrderNumber(int sequence) {
        int year = LocalDateTime.now().getYear();
        return String.format("ORD-%d-%03d", year, sequence);
    }
}
