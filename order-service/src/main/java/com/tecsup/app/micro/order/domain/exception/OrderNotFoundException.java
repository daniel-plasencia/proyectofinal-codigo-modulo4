package com.tecsup.app.micro.order.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra una orden
 */
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(Long orderId) {
        super("Order with id " + orderId + " not found");
    }
    
    public OrderNotFoundException(String orderNumber) {
        super("Order with number " + orderNumber + " not found");
    }
}
