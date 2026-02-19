package com.tecsup.app.micro.order.infrastructure.client;

import com.tecsup.app.micro.order.domain.exception.ProductNotFoundException;
import com.tecsup.app.micro.order.domain.model.Product;
import com.tecsup.app.micro.order.infrastructure.client.dto.ProductDto;
import com.tecsup.app.micro.order.infrastructure.client.mapper.ProductDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP para comunicarse con Product Service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClient {

    private final RestTemplate restTemplate;
    private final ProductDtoMapper productDtoMapper;

    @Value("${product.service.url}")
    private String productServiceUrl;

    /**
     * Obtiene un producto por ID desde el Product Service
     */
    public Product getProductById(Long productId) {
        log.info("Calling Product Service to get product with id: {}", productId);

        String url = this.productServiceUrl + "/api/products/" + productId;

        try {
            ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);
            
            if (productDto == null) {
                log.error("Product with id {} not found in Product Service", productId);
                throw new ProductNotFoundException(productId);
            }
            
            log.info("Product retrieved successfully from Product Service: {}", productDto);
            return productDtoMapper.toDomain(productDto);
        } catch (ProductNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Product Service: {}", e.getMessage());
            throw new ProductNotFoundException(productId);
        }
    }
}
