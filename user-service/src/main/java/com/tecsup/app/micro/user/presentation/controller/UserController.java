package com.tecsup.app.micro.user.presentation.controller;

import com.tecsup.app.micro.user.application.service.UserApplicationService;
import com.tecsup.app.micro.user.domain.model.User;
import com.tecsup.app.micro.user.presentation.dto.CreateUserRequest;
import com.tecsup.app.micro.user.presentation.dto.UpdateUserRequest;
import com.tecsup.app.micro.user.presentation.dto.UserResponse;
import com.tecsup.app.micro.user.presentation.mapper.UserDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de Usuarios
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserApplicationService userApplicationService;
    private final UserDtoMapper userDtoMapper;
    
    /**
     * Obtiene todos los usuarios
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("REST request to get all users");
        List<User> users = userApplicationService.getAllUsers();
        return ResponseEntity.ok(userDtoMapper.toResponseList(users));
    }
    
    /**
     * Obtiene un usuario por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("REST request to get user by id: {}", id);
        User user = userApplicationService.getUserById(id);
        return ResponseEntity.ok(userDtoMapper.toResponse(user));
    }
    
    /**
     * Crea un nuevo usuario
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("REST request to create user: {}", request.getEmail());
        User user = userDtoMapper.toDomain(request);
        User createdUser = userApplicationService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userDtoMapper.toResponse(createdUser));
    }
    
    /**
     * Actualiza un usuario existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("REST request to update user with id: {}", id);
        User user = userDtoMapper.toDomain(request);
        User updatedUser = userApplicationService.updateUser(id, user);
        return ResponseEntity.ok(userDtoMapper.toResponse(updatedUser));
    }
    
    /**
     * Elimina un usuario
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("REST request to delete user with id: {}", id);
        userApplicationService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Endpoint de salud
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service running with Clean Architecture!");
    }
}
