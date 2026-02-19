package com.tecsup.app.micro.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Order Service Application
 * Microservicio de órdenes con Clean Architecture
 */
@SpringBootApplication
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
