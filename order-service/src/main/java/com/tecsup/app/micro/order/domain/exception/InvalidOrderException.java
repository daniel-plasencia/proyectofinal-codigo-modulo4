package com.tecsup.app.micro.order.domain.exception;

/**
 * Excepción lanzada cuando los datos de la orden son inválidos
 */
public class InvalidOrderException extends RuntimeException {
    
    public InvalidOrderException(String message) {
        super(message);
    }
}
