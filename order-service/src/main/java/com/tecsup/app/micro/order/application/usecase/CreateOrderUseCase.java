package com.tecsup.app.micro.order.application.usecase;

import com.tecsup.app.micro.order.domain.exception.InvalidOrderException;
import com.tecsup.app.micro.order.domain.exception.ProductNotFoundException;
import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.model.OrderItem;
import com.tecsup.app.micro.order.domain.model.OrderStatus;
import com.tecsup.app.micro.order.domain.model.Product;
import com.tecsup.app.micro.order.domain.repository.OrderRepository;
import com.tecsup.app.micro.order.infrastructure.client.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Caso de uso: Crear una nueva orden
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderUseCase {
    
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    
    // Contador para generar números de orden únicos
    // Se inicializa en 1, pero se ajusta al iniciar basándose en la BD
    private static final AtomicInteger orderSequence = new AtomicInteger(1);
    private static volatile boolean initialized = false;
    
    public Order execute(Order order) {
        log.debug("Executing CreateOrderUseCase for user: {}", order.getUserId());
        
        // Inicializar contador si no se ha hecho
        initializeOrderSequence();
        
        // Validar datos de la orden
        if (!order.isValid()) {
            throw new InvalidOrderException("Invalid order data. User ID and items are required.");
        }
        
        // Validar y obtener productos del Product Service
        List<OrderItem> validatedItems = validateAndEnrichItems(order.getItems());
        
        // Calcular subtotales y total
        BigDecimal totalAmount = calculateTotal(validatedItems);
        
        // Generar número de orden único
        String orderNumber = Order.generateOrderNumber(orderSequence.getAndIncrement());
        
        // Crear orden completa
        Order newOrder = Order.builder()
                .orderNumber(orderNumber)
                .userId(order.getUserId())
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .items(validatedItems)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Guardar orden
        Order savedOrder = orderRepository.save(newOrder);
        log.info("Order created successfully with id: {} and orderNumber: {}", savedOrder.getId(), savedOrder.getOrderNumber());
        
        return savedOrder;
    }
    
    /**
     * Valida cada item consultando el Product Service y obtiene el precio actual
     */
    private List<OrderItem> validateAndEnrichItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> {
                    try {
                        // Obtener producto del Product Service
                        Product product = productClient.getProductById(item.getProductId());
                        
                        // Validar que el producto existe
                        if (product == null) {
                            throw new ProductNotFoundException(item.getProductId());
                        }
                        
                        // Usar el precio actual del producto
                        BigDecimal unitPrice = product.getPrice();
                        
                        // Calcular subtotal
                        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                        
                        // Crear item enriquecido
                        OrderItem enrichedItem = OrderItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .unitPrice(unitPrice)
                                .subtotal(subtotal)
                                .product(product)
                                .build();
                        
                        log.debug("Item validated: productId={}, quantity={}, unitPrice={}, subtotal={}", 
                                item.getProductId(), item.getQuantity(), unitPrice, subtotal);
                        
                        return enrichedItem;
                    } catch (ProductNotFoundException e) {
                        log.error("Product not found: {}", item.getProductId());
                        throw e;
                    } catch (Exception e) {
                        log.error("Error validating product {}: {}", item.getProductId(), e.getMessage());
                        throw new InvalidOrderException("Error validating product " + item.getProductId() + ": " + e.getMessage());
                    }
                })
                .toList();
    }
    
    /**
     * Calcula el total de la orden sumando todos los subtotales
     */
    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Inicializa el contador de secuencia basándose en el último número de orden en la BD
     * Esto evita duplicados cuando el servicio se reinicia
     */
    private void initializeOrderSequence() {
        if (!initialized) {
            synchronized (CreateOrderUseCase.class) {
                if (!initialized) {
                    try {
                        Optional<String> lastOrderNumber = orderRepository.findLastOrderNumber();
                        if (lastOrderNumber.isPresent()) {
                            String lastNumber = lastOrderNumber.get();
                            // Extraer el número de secuencia del formato ORD-YYYY-NNN
                            // Ejemplo: ORD-2026-004 -> 4
                            try {
                                String[] parts = lastNumber.split("-");
                                if (parts.length == 3 && parts[0].equals("ORD")) {
                                    int lastSequence = Integer.parseInt(parts[2]);
                                    // Inicializar con el siguiente número
                                    orderSequence.set(lastSequence + 1);
                                    log.info("Order sequence initialized from database. Last order: {}, Next sequence: {}", 
                                            lastNumber, lastSequence + 1);
                                } else {
                                    log.warn("Could not parse last order number: {}. Starting from 1.", lastNumber);
                                }
                            } catch (NumberFormatException e) {
                                log.warn("Could not parse sequence from order number: {}. Starting from 1.", lastNumber);
                            }
                        } else {
                            log.info("No existing orders found. Starting sequence from 1.");
                        }
                    } catch (Exception e) {
                        log.error("Error initializing order sequence from database: {}", e.getMessage());
                        // Continuar con el valor por defecto (1)
                    }
                    initialized = true;
                }
            }
        }
    }
}
