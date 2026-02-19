package com.tecsup.app.micro.order.infrastructure.persistence.mapper;

import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.model.OrderItem;
import com.tecsup.app.micro.order.domain.model.OrderStatus;
import com.tecsup.app.micro.order.infrastructure.persistence.entity.OrderEntity;
import com.tecsup.app.micro.order.infrastructure.persistence.entity.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * Mapper entre entidades de dominio y entidades JPA
 * Usa MapStruct para generar el código de conversión
 */
@Mapper(componentModel = "spring")
public interface OrderPersistenceMapper {
    
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "items", source = "items", ignore = true)
    OrderEntity toEntity(Order order);
    
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "items", source = "items", ignore = true)
    Order toDomain(OrderEntity entity);
    
    List<Order> toDomainList(List<OrderEntity> entities);
    
    @Mapping(target = "orderEntity", ignore = true)
    @Mapping(target = "id", ignore = true)
    OrderItemEntity toEntity(OrderItem item);
    
    @Mapping(target = "orderId", source = "orderEntity.id")
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "id", source = "id")
    OrderItem toDomain(OrderItemEntity entity);
    
    List<OrderItem> toDomainItemList(List<OrderItemEntity> entities);
    
    @Named("statusToString")
    default String statusToString(OrderStatus status) {
        return status != null ? status.name() : null;
    }
    
    @Named("stringToStatus")
    default OrderStatus stringToStatus(String status) {
        return status != null ? OrderStatus.valueOf(status) : null;
    }
}
