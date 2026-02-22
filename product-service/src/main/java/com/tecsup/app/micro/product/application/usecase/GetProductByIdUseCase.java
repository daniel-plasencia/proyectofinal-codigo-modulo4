package com.tecsup.app.micro.product.application.usecase;

import com.tecsup.app.micro.product.domain.exception.ProductNotFoundException;
import com.tecsup.app.micro.product.domain.exception.UserNotFoundException;
import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.domain.model.User;
import com.tecsup.app.micro.product.domain.repository.ProductRepository;
import com.tecsup.app.micro.product.infrastructure.client.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caso de uso: Obtener producto por ID
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetProductByIdUseCase {
    
    private final ProductRepository productRepository;

    private final UserClient userClient;

    public Product execute(Long id) {
        log.debug("Executing GetProductByIdUseCase for id: {}", id);

        Product prod = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // --------------------------------------------------------
        // Llama al microservicio user-service
        // --------------------------------------------------------
        // Intentar obtener el usuario creador, pero no fallar si no se puede
        try {
            User user = userClient.getUserById(prod.getCreatedBy());
            log.info("Fetching user from userdb: {}", user);
            
            if(user != null) {
                prod.setCreatedByUser(user);
            } else {
                log.warn("User with id {} not found in userdb, continuing without user info", prod.getCreatedBy());
            }
        } catch (Exception e) {
            log.warn("Could not fetch user {} from userdb: {}. Continuing without user info.", 
                    prod.getCreatedBy(), e.getMessage());
            // Continuar sin el usuario creador - no es crítico para obtener el producto
        }

        return prod;
    }
}
