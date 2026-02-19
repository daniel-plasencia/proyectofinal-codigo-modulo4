package com.tecsup.app.micro.order.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para crear una orden
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be positive")
    private Long userId;
    
    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<OrderItemRequest> items;
}
