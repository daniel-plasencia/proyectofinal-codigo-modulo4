package com.tecsup.app.micro.order.domain.model;

/**
 * Estados válidos de una orden
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
