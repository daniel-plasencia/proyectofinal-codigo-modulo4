package com.tecsup.app.micro.order.infrastructure.persistence.repository;

import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.model.OrderItem;
import com.tecsup.app.micro.order.domain.repository.OrderRepository;
import com.tecsup.app.micro.order.infrastructure.persistence.entity.OrderEntity;
import com.tecsup.app.micro.order.infrastructure.persistence.entity.OrderItemEntity;
import com.tecsup.app.micro.order.infrastructure.persistence.mapper.OrderPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del repositorio de Order (Adaptador)
 * Conecta el dominio con la infraestructura de persistencia usando MapStruct
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class OrderRepositoryImpl implements OrderRepository {
    
    private final JpaOrderRepository jpaOrderRepository;
    private final OrderPersistenceMapper mapper;
    
    @Override
    public Order save(Order order) {
        log.debug("Saving order: {}", order.getOrderNumber());
        
        // Convertir a entidad
        OrderEntity entity = mapper.toEntity(order);
        
        // Mapear items manualmente para mantener la relación bidireccional
        if (order.getItems() != null) {
            List<OrderItemEntity> itemEntities = order.getItems().stream()
                    .map(item -> {
                        OrderItemEntity itemEntity = mapper.toEntity(item);
                        itemEntity.setOrderEntity(entity);
                        return itemEntity;
                    })
                    .toList();
            entity.setItems(itemEntities);
        }
        
        // Guardar
        OrderEntity savedEntity = jpaOrderRepository.save(entity);
        
        // Convertir de vuelta a dominio
        Order savedOrder = mapper.toDomain(savedEntity);
        
        // Mapear items de vuelta
        if (savedEntity.getItems() != null) {
            List<OrderItem> items = savedEntity.getItems().stream()
                    .map(mapper::toDomain)
                    .toList();
            savedOrder.setItems(items);
        }
        
        return savedOrder;
    }
    
    @Override
    public Optional<Order> findById(Long id) {
        log.debug("Finding order by id: {}", id);
        return jpaOrderRepository.findById(id)
                .map(this::mapEntityToDomainWithItems);
    }
    
    @Override
    public List<Order> findAll() {
        log.debug("Finding all orders");
        return jpaOrderRepository.findAll().stream()
                .map(this::mapEntityToDomainWithItems)
                .toList();
    }
    
    @Override
    public List<Order> findByUserId(Long userId) {
        log.debug("Finding orders by userId: {}", userId);
        return jpaOrderRepository.findByUserId(userId).stream()
                .map(this::mapEntityToDomainWithItems)
                .toList();
    }
    
    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        log.debug("Finding order by orderNumber: {}", orderNumber);
        return jpaOrderRepository.findByOrderNumber(orderNumber)
                .map(this::mapEntityToDomainWithItems);
    }
    
    @Override
    public Optional<String> findLastOrderNumber() {
        log.debug("Finding last order number");
        List<String> orderNumbers = jpaOrderRepository.findAllOrderNumbersOrderedByIdDesc();
        return orderNumbers.isEmpty() ? Optional.empty() : Optional.of(orderNumbers.get(0));
    }
    
    @Override
    public void deleteById(Long id) {
        log.debug("Deleting order by id: {}", id);
        jpaOrderRepository.deleteById(id);
    }
    
    /**
     * Mapea una entidad a dominio incluyendo los items
     */
    private Order mapEntityToDomainWithItems(OrderEntity entity) {
        Order order = mapper.toDomain(entity);
        if (entity.getItems() != null) {
            List<OrderItem> items = entity.getItems().stream()
                    .map(mapper::toDomain)
                    .toList();
            order.setItems(items);
        }
        return order;
    }
}
