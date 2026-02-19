package com.tecsup.app.micro.order.presentation.mapper;

import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.model.OrderItem;
import com.tecsup.app.micro.order.domain.model.OrderStatus;
import com.tecsup.app.micro.order.domain.model.Product;
import com.tecsup.app.micro.order.presentation.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * Mapper entre DTOs de presentación y modelos de dominio
 */
@Mapper(componentModel = "spring")
public interface OrderDtoMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "items", source = "items", qualifiedByName = "itemRequestToItem")
    Order toDomain(CreateOrderRequest request);
    
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);
    
    List<OrderResponse> toResponseList(List<Order> orders);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "product", ignore = true)
    OrderItem toDomain(OrderItemRequest request);
    
    @Mapping(target = "product", source = "product")
    OrderItemResponse toResponse(OrderItem item);
    
    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);
    
    ProductResponse toResponse(Product product);
    
    @Named("itemRequestToItem")
    default List<OrderItem> itemRequestToItem(List<OrderItemRequest> requests) {
        if (requests == null) {
            return null;
        }
        return requests.stream()
                .map(this::toDomain)
                .toList();
    }
    
    @Named("statusToString")
    default String statusToString(OrderStatus status) {
        return status != null ? status.name() : null;
    }
}
