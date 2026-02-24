package com.tecsup.app.micro.order.domain.repository;

import com.tecsup.app.micro.order.domain.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del repositorio de órdenes (Domain Layer)
 * Define las operaciones de persistencia sin depender de implementaciones específicas
 */
public interface OrderRepository {
    
    /**
     * Guarda una orden (crea o actualiza)
     */
    Order save(Order order);
    
    /**
     * Busca una orden por ID
     */
    Optional<Order> findById(Long id);
    
    /**
     * Obtiene todas las órdenes
     */
    List<Order> findAll();
    
    /**
     * Busca órdenes por ID de usuario
     */
    List<Order> findByUserId(Long userId);
    
    /**
     * Busca una orden por número de orden
     */
    Optional<Order> findByOrderNumber(String orderNumber);
    
    /**
     * Obtiene el último número de orden (para inicializar el contador)
     */
    Optional<String> findLastOrderNumber();
    
    /**
     * Elimina una orden por ID
     */
    void deleteById(Long id);
}
