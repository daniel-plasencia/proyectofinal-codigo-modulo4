package com.tecsup.app.micro.order.application.usecase;

import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Caso de uso: Obtener órdenes por ID de usuario
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetOrdersByUserIdUseCase {
    
    private final OrderRepository orderRepository;
    
    public List<Order> execute(Long userId) {
        log.debug("Executing GetOrdersByUserIdUseCase for userId: {}", userId);
        return orderRepository.findByUserId(userId);
    }
}
