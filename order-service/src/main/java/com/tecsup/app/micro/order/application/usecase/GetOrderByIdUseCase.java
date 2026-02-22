package com.tecsup.app.micro.order.application.usecase;

import com.tecsup.app.micro.order.domain.exception.OrderNotFoundException;
import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.model.OrderItem;
import com.tecsup.app.micro.order.domain.model.Product;
import com.tecsup.app.micro.order.domain.repository.OrderRepository;
import com.tecsup.app.micro.order.infrastructure.client.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Caso de uso: Obtener orden por ID
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetOrderByIdUseCase {
    
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    
    public Order execute(Long id) {
        log.debug("Executing GetOrderByIdUseCase for id: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        
        // Enriquecer items con información del producto
        enrichItemsWithProducts(order);
        
        return order;
    }
    
    /**
     * Enriquece los items de la orden con información del producto desde Product Service
     */
    private void enrichItemsWithProducts(Order order) {
        if (order.getItems() != null) {
            List<OrderItem> enrichedItems = order.getItems().stream()
                    .map(item -> {
                        try {
                            Product product = productClient.getProductById(item.getProductId());
                            item.setProduct(product);
                            log.debug("Product enriched for item: productId={}, productName={}", 
                                    item.getProductId(), product != null ? product.getName() : "null");
                        } catch (Exception e) {
                            log.warn("Could not fetch product {} for order item: {}", 
                                    item.getProductId(), e.getMessage());
                            // Continuar sin producto si hay error
                        }
                        return item;
                    })
                    .toList();
            order.setItems(enrichedItems);
        }
    }
}
