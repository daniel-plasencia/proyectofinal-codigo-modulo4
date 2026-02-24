package com.tecsup.app.micro.order.infrastructure.persistence.repository;

import com.tecsup.app.micro.order.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de OrderEntity
 * Interface de Spring Data JPA
 */
@Repository
public interface JpaOrderRepository extends JpaRepository<OrderEntity, Long> {
    
    List<OrderEntity> findByUserId(Long userId);
    
    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    
    /**
     * Obtiene el último número de orden ordenado por ID descendente
     */
    @Query("SELECT o.orderNumber FROM OrderEntity o ORDER BY o.id DESC")
    List<String> findAllOrderNumbersOrderedByIdDesc();
}
