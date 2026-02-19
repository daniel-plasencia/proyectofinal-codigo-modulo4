package com.tecsup.app.micro.order.infrastructure.client.mapper;

import com.tecsup.app.micro.order.domain.model.Product;
import com.tecsup.app.micro.order.infrastructure.client.dto.ProductDto;
import org.mapstruct.Mapper;

/**
 * Mapper entre DTOs del Product Service y modelo de dominio
 */
@Mapper(componentModel = "spring")
public interface ProductDtoMapper {
    
    Product toDomain(ProductDto dto);
}
