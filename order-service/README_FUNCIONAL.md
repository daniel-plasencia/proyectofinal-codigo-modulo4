# 📚 ORDER SERVICE - Documentación Funcional del Código

**Propósito:** Este documento explica **funcionalmente** cómo funciona todo el código del Order Service y el cambio realizado en Product Service.

---

## 📋 TABLA DE CONTENIDOS

1. [Arquitectura General](#arquitectura-general)
2. [Capa de Dominio (Domain)](#capa-de-dominio-domain)
3. [Capa de Aplicación (Application)](#capa-de-aplicación-application)
4. [Capa de Infraestructura (Infrastructure)](#capa-de-infraestructura-infrastructure)
5. [Capa de Presentación (Presentation)](#capa-de-presentación-presentation)
6. [Flujo Completo de Datos](#flujo-completo-de-datos)
7. [Cambio en Product Service](#cambio-en-product-service)
8. [Mappers y Conversiones](#mappers-y-conversiones)

---

## 🏗️ ARQUITECTURA GENERAL

### Clean Architecture - 4 Capas

```
┌─────────────────────────────────────────────────────────┐
│  PRESENTATION (Controladores REST, DTOs)                 │
│  - Recibe requests HTTP                                  │
│  - Valida datos de entrada                               │
│  - Convierte DTOs ↔ Dominio                             │
└─────────────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────────────┐
│  APPLICATION (Casos de Uso, Servicios)                  │
│  - Orquesta la lógica de negocio                        │
│  - Coordina entre dominio e infraestructura             │
│  - Maneja transacciones                                 │
└─────────────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────────────┐
│  DOMAIN (Modelos, Reglas de Negocio)                   │
│  - Entidades puras sin dependencias                     │
│  - Validaciones de negocio                              │
│  - Interfaces de repositorios                          │
└─────────────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────────────┐
│  INFRASTRUCTURE (BD, HTTP, Configuración)              │
│  - Implementa repositorios                             │
│  - Clientes HTTP para otros servicios                   │
│  - Configuración de Spring                              │
└─────────────────────────────────────────────────────────┘
```

**Principio:** Las capas internas (Domain) NO dependen de las externas (Infrastructure).

---

## 🎯 CAPA DE DOMINIO (DOMAIN)

**Propósito:** Contiene la lógica de negocio pura, sin dependencias de frameworks.

### 📁 `domain/model/`

#### `Order.java`
**Función:** Modelo de dominio que representa una orden de compra.

**Responsabilidades:**
- Almacena datos de la orden: `id`, `orderNumber`, `userId`, `status`, `totalAmount`
- Contiene lista de `items` (OrderItem)
- **Métodos de negocio:**
  - `calculateTotal()`: Calcula el total sumando subtotales de items
  - `isValid()`: Valida que la orden tenga `userId` válido e items no vacíos
  - `generateOrderNumber(int)`: Genera número único (ej: "ORD-2026-001")

**Ejemplo de uso:**
```java
Order order = Order.builder()
    .userId(1L)
    .status(OrderStatus.PENDING)
    .items(items)
    .build();

if (order.isValid()) {
    BigDecimal total = order.calculateTotal();
}
```

---

#### `OrderItem.java`
**Función:** Representa un item dentro de una orden.

**Responsabilidades:**
- Almacena: `productId`, `quantity`, `unitPrice`, `subtotal`
- Referencia al `Product` completo (obtenido de Product Service)
- **Métodos:**
  - `calculateSubtotal()`: Calcula `quantity × unitPrice`
  - `isValid()`: Valida que tenga `productId` y `quantity` válidos
    - **IMPORTANTE:** `unitPrice` es opcional al crear (se asigna después)

**Cambio importante:**
```java
// ANTES: Requería unitPrice (causaba error 400)
public boolean isValid() {
    return productId != null && quantity != null 
        && unitPrice != null; // ❌ Falla si es null
}

// AHORA: unitPrice es opcional al crear
public boolean isValid() {
    if (productId == null || productId <= 0) return false;
    if (quantity == null || quantity <= 0) return false;
    // unitPrice se asigna después de validar con Product Service
    return true; // ✅ Funciona sin unitPrice
}
```

---

#### `OrderStatus.java`
**Función:** Enum que define los estados posibles de una orden.

**Valores:**
- `PENDING`: Orden creada, pendiente de confirmación
- `CONFIRMED`: Orden confirmada
- `SHIPPED`: Orden enviada
- `DELIVERED`: Orden entregada
- `CANCELLED`: Orden cancelada

---

#### `Product.java`
**Función:** Modelo de dominio que representa un producto (obtenido de Product Service).

**Uso:** Se usa en `OrderItem` para almacenar información completa del producto.

**Campos:** `id`, `name`, `description`, `price`, `stock`, `category`

---

### 📁 `domain/exception/`

#### `InvalidOrderException.java`
**Función:** Excepción lanzada cuando los datos de la orden son inválidos.

**Cuándo se lanza:**
- `userId` es null o <= 0
- `items` está vacío o es null
- Algún item no es válido

**Ejemplo:**
```java
if (!order.isValid()) {
    throw new InvalidOrderException("Invalid order data. User ID and items are required.");
}
```

---

#### `OrderNotFoundException.java`
**Función:** Excepción lanzada cuando no se encuentra una orden por ID.

**Cuándo se lanza:** Al consultar una orden que no existe.

---

#### `ProductNotFoundException.java`
**Función:** Excepción lanzada cuando un producto no existe en Product Service.

**Cuándo se lanza:** Al crear una orden con un `productId` que no existe.

---

### 📁 `domain/repository/`

#### `OrderRepository.java`
**Función:** Interfaz que define las operaciones de persistencia (patrón Repository).

**Métodos:**
- `save(Order)`: Guarda o actualiza una orden
- `findById(Long)`: Busca orden por ID
- `findAll()`: Obtiene todas las órdenes
- `findByUserId(Long)`: Obtiene órdenes de un usuario
- `findByOrderNumber(String)`: Busca por número de orden
- `deleteById(Long)`: Elimina una orden

**Nota:** Esta es una **interfaz de dominio**, la implementación está en Infrastructure.

---

## 🔧 CAPA DE APLICACIÓN (APPLICATION)

**Propósito:** Orquesta la lógica de negocio y coordina entre dominio e infraestructura.

### 📁 `application/usecase/`

#### `CreateOrderUseCase.java`
**Función:** Caso de uso para crear una nueva orden.

**Flujo:**
1. Valida que la orden sea válida (`order.isValid()`)
2. Para cada item:
   - Llama a `ProductClient.getProductById()` para obtener el producto
   - Valida que el producto existe
   - Obtiene el precio actual del producto
   - Calcula el subtotal (`quantity × price`)
3. Calcula el total sumando todos los subtotales
4. Genera un número de orden único (`ORD-2026-001`)
5. Guarda la orden usando `OrderRepository.save()`

**Código clave:**
```java
// Valida y enriquece items con información del producto
List<OrderItem> validatedItems = validateAndEnrichItems(order.getItems());

// Calcula total
BigDecimal totalAmount = calculateTotal(validatedItems);

// Genera número único
String orderNumber = Order.generateOrderNumber(orderSequence.getAndIncrement());
```

---

#### `GetOrderByIdUseCase.java`
**Función:** Obtiene una orden por ID y enriquece los items con información del producto.

**Flujo:**
1. Busca la orden en el repositorio
2. Si no existe, lanza `OrderNotFoundException`
3. **Enriquece items:** Para cada item, llama a Product Service para obtener el `Product` completo
4. Asigna el `Product` a cada `OrderItem`
5. Retorna la orden con productos enriquecidos

**Código clave:**
```java
Order order = orderRepository.findById(id)
    .orElseThrow(() -> new OrderNotFoundException(id));

// Enriquece items con información del producto
enrichItemsWithProducts(order);

return order;
```

**Método `enrichItemsWithProducts()`:**
```java
private void enrichItemsWithProducts(Order order) {
    if (order.getItems() != null) {
        order.getItems().forEach(item -> {
            try {
                Product product = productClient.getProductById(item.getProductId());
                item.setProduct(product); // ✅ Enriquece el item
            } catch (Exception e) {
                log.warn("Could not fetch product {}", item.getProductId());
                // Continúa sin producto si hay error
            }
        });
    }
}
```

---

#### `GetAllOrdersUseCase.java`
**Función:** Obtiene todas las órdenes y enriquece los items.

**Flujo:**
1. Obtiene todas las órdenes del repositorio
2. Para cada orden, enriquece los items con información del producto
3. Retorna la lista completa

---

#### `GetOrdersByUserIdUseCase.java`
**Función:** Obtiene órdenes de un usuario específico.

**Flujo:**
1. Busca órdenes por `userId`
2. Enriquece los items de cada orden
3. Retorna la lista

---

### 📁 `application/service/`

#### `OrderApplicationService.java`
**Función:** Servicio de aplicación que orquesta los casos de uso.

**Responsabilidades:**
- Coordina los casos de uso
- Maneja transacciones (`@Transactional`)
- Punto de entrada desde la capa de presentación

**Métodos:**
- `createOrder(Order)`: Crea una orden (transaccional)
- `getOrderById(Long)`: Obtiene orden por ID (solo lectura)
- `getAllOrders()`: Obtiene todas (solo lectura)
- `getOrdersByUserId(Long)`: Obtiene órdenes de usuario (solo lectura)

---

## 🔌 CAPA DE INFRAESTRUCTURA (INFRASTRUCTURE)

**Propósito:** Implementa las interfaces del dominio y maneja detalles técnicos.

### 📁 `infrastructure/persistence/`

#### `entity/OrderEntity.java` y `OrderItemEntity.java`
**Función:** Entidades JPA que representan las tablas de la base de datos.

**Responsabilidades:**
- Mapean a tablas: `orders` y `order_items`
- Definen relaciones JPA (`@OneToMany`, `@ManyToOne`)
- Anotaciones JPA: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`

**Diferencia con Domain:**
- `OrderEntity`: Entidad JPA con anotaciones de persistencia
- `Order`: Modelo de dominio puro, sin anotaciones JPA

---

#### `repository/JpaOrderRepository.java`
**Función:** Interfaz Spring Data JPA que extiende `JpaRepository`.

**Responsabilidades:**
- Proporciona métodos CRUD automáticos
- Permite queries personalizadas con `@Query`
- Spring Data genera la implementación automáticamente

---

#### `repository/OrderRepositoryImpl.java`
**Función:** Implementación del `OrderRepository` del dominio.

**Responsabilidades:**
- Implementa los métodos definidos en `OrderRepository` (dominio)
- Usa `JpaOrderRepository` para acceder a la BD
- Convierte entre `OrderEntity` ↔ `Order` usando `OrderPersistenceMapper`

**Flujo:**
```java
// 1. Recibe Order (dominio)
Order order = ...;

// 2. Convierte a OrderEntity (JPA)
OrderEntity entity = mapper.toEntity(order);

// 3. Guarda usando JPA
OrderEntity saved = jpaOrderRepository.save(entity);

// 4. Convierte de vuelta a Order (dominio)
Order savedOrder = mapper.toDomain(saved);
```

**Método importante `mapEntityToDomainWithItems()`:**
```java
private Order mapEntityToDomainWithItems(OrderEntity entity) {
    Order order = mapper.toDomain(entity);
    if (entity.getItems() != null) {
        List<OrderItem> items = entity.getItems().stream()
            .map(mapper::toDomain)
            .toList();
        order.setItems(items);
    }
    return order;
}
```

---

#### `mapper/OrderPersistenceMapper.java`
**Función:** Mapper MapStruct que convierte entre entidades JPA y modelos de dominio.

**Conversiones:**
- `OrderEntity` ↔ `Order`
- `OrderItemEntity` ↔ `OrderItem`
- `OrderStatus` (enum) ↔ `String` (BD)

**Configuración:**
```java
@Mapping(target = "items", ignore = true) // Se mapea manualmente
@Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
Order toDomain(OrderEntity entity);
```

---

### 📁 `infrastructure/client/`

#### `ProductClient.java`
**Función:** Cliente HTTP que se comunica con Product Service.

**Responsabilidades:**
- Realiza llamadas HTTP a Product Service
- Usa `RestTemplate` para hacer requests
- Convierte respuestas JSON a `ProductDto`
- Mapea `ProductDto` → `Product` (dominio)

**Método principal:**
```java
public Product getProductById(Long productId) {
    String url = productServiceUrl + "/api/products/" + productId;
    ProductDto dto = restTemplate.getForObject(url, ProductDto.class);
    return productDtoMapper.toDomain(dto);
}
```

**URL configurada:**
- En Kubernetes: `http://product-service.product-service.svc.cluster.local`
- En local: `http://localhost:8082`

---

#### `dto/ProductDto.java`
**Función:** DTO que representa la respuesta JSON de Product Service.

**Campos:** Coinciden con el JSON que retorna Product Service.

---

#### `mapper/ProductDtoMapper.java`
**Función:** Mapper que convierte `ProductDto` → `Product` (dominio).

---

### 📁 `infrastructure/config/`

#### `BeanConfig.java`
**Función:** Configuración de beans de Spring.

**Beans configurados:**
- `RestTemplate`: Cliente HTTP para llamar a Product Service
  - Timeout de conexión: 5 segundos
  - Timeout de lectura: 5 segundos

---

## 🎨 CAPA DE PRESENTACIÓN (PRESENTATION)

**Propósito:** Maneja las peticiones HTTP y expone la API REST.

### 📁 `presentation/controller/`

#### `OrderController.java`
**Función:** Controlador REST que expone los endpoints HTTP.

**Endpoints:**
- `POST /api/orders`: Crea una orden
- `GET /api/orders`: Obtiene todas las órdenes
- `GET /api/orders/{id}`: Obtiene orden por ID
- `GET /api/orders/user/{userId}`: Obtiene órdenes de un usuario
- `GET /api/orders/health`: Health check

**Flujo de un request:**
```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
    @Valid @RequestBody CreateOrderRequest request) {
    
    // 1. Convierte DTO → Dominio
    Order order = orderDtoMapper.toDomain(request);
    
    // 2. Ejecuta caso de uso
    Order createdOrder = orderApplicationService.createOrder(order);
    
    // 3. Convierte Dominio → DTO de respuesta
    OrderResponse response = orderDtoMapper.toResponse(createdOrder);
    
    // 4. Retorna HTTP 201 Created
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

---

#### `GlobalExceptionHandler.java`
**Función:** Maneja excepciones globalmente y las convierte a respuestas HTTP.

**Excepciones manejadas:**
- `InvalidOrderException` → HTTP 400 Bad Request
- `OrderNotFoundException` → HTTP 404 Not Found
- `ProductNotFoundException` → HTTP 404 Not Found
- `Exception` genérica → HTTP 500 Internal Server Error

**Ejemplo:**
```java
@ExceptionHandler(InvalidOrderException.class)
public ResponseEntity<ErrorResponse> handleInvalidOrder(
    InvalidOrderException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(400, ex.getMessage()));
}
```

---

### 📁 `presentation/dto/`

#### `CreateOrderRequest.java`
**Función:** DTO que representa el request para crear una orden.

**Campos:**
- `userId` (Long, @NotNull, @Min(1))
- `items` (List<OrderItemRequest>, @NotEmpty, @Valid)

**Validaciones:**
- `userId` debe ser positivo
- `items` no puede estar vacío
- Cada item se valida con `OrderItemRequest`

---

#### `OrderItemRequest.java`
**Función:** DTO que representa un item en el request.

**Campos:**
- `productId` (Long, @NotNull, @Min(1))
- `quantity` (Integer, @NotNull, @Min(1))

---

#### `OrderResponse.java`
**Función:** DTO que representa la respuesta de una orden.

**Campos:**
- `id`, `orderNumber`, `userId`, `status`, `totalAmount`
- `items` (List<OrderItemResponse>)
- `createdAt`, `updatedAt`

---

#### `OrderItemResponse.java`
**Función:** DTO que representa un item en la respuesta.

**Campos:**
- `id`, `product` (ProductResponse), `quantity`, `unitPrice`, `subtotal`

**Nota:** El campo `product` puede ser `null` en la respuesta de creación, pero se enriquece al consultar la orden.

---

#### `ProductResponse.java`
**Función:** DTO que representa un producto en la respuesta.

**Campos:** `id`, `name`, `description`, `price`, `stock`, `category`

---

### 📁 `presentation/mapper/`

#### `OrderDtoMapper.java`
**Función:** Mapper MapStruct que convierte entre DTOs y modelos de dominio.

**Conversiones principales:**
- `CreateOrderRequest` → `Order` (dominio)
- `Order` (dominio) → `OrderResponse`
- `OrderItemRequest` → `OrderItem` (dominio)
- `OrderItem` (dominio) → `OrderItemResponse`
- `Product` (dominio) → `ProductResponse`

**Método especial `itemRequestToItem()`:**
```java
@Named("itemRequestToItem")
default List<OrderItem> itemRequestToItem(List<OrderItemRequest> requests) {
    if (requests == null) return null;
    return requests.stream()
        .map(this::toDomain) // Convierte cada OrderItemRequest → OrderItem
        .toList();
}
```

**Configuración importante:**
```java
@Mapping(target = "unitPrice", ignore = true) // Se asigna después
@Mapping(target = "subtotal", ignore = true)  // Se calcula después
@Mapping(target = "product", ignore = true)   // Se obtiene después
OrderItem toDomain(OrderItemRequest request);
```

---

## 🔄 FLUJO COMPLETO DE DATOS

### Flujo: Crear Orden (POST /api/orders)

```
1. Cliente HTTP
   ↓ POST /api/orders
   { "userId": 1, "items": [...] }

2. OrderController.createOrder()
   ↓ Valida @Valid
   CreateOrderRequest (DTO)

3. OrderDtoMapper.toDomain()
   ↓ Convierte DTO → Dominio
   Order (dominio) - sin unitPrice todavía

4. OrderApplicationService.createOrder()
   ↓ @Transactional
   CreateOrderUseCase.execute()

5. CreateOrderUseCase
   ↓ order.isValid() - ✅ Pasa (unitPrice es opcional)
   ↓ Para cada item:
   ProductClient.getProductById()
   ↓ HTTP GET /api/products/{id}
   Product Service
   ↓ Retorna ProductDto
   Product (dominio) con precio
   ↓ Calcula subtotal
   OrderItem enriquecido

6. OrderRepository.save()
   ↓ OrderRepositoryImpl.save()
   OrderPersistenceMapper.toEntity()
   ↓ Convierte Dominio → JPA
   OrderEntity
   ↓ JpaOrderRepository.save()
   PostgreSQL (orderdb)
   ↓ INSERT INTO orders, order_items

7. OrderDtoMapper.toResponse()
   ↓ Convierte Dominio → DTO
   OrderResponse (DTO)
   ↓ HTTP 201 Created
   Cliente recibe respuesta
```

---

### Flujo: Consultar Orden (GET /api/orders/{id})

```
1. Cliente HTTP
   ↓ GET /api/orders/4

2. OrderController.getOrderById()
   ↓
   OrderApplicationService.getOrderById()

3. GetOrderByIdUseCase.execute()
   ↓
   OrderRepository.findById(4)
   ↓ OrderRepositoryImpl.findById()
   JpaOrderRepository.findById(4)
   ↓ SELECT * FROM orders WHERE id = 4
   PostgreSQL
   ↓ Retorna OrderEntity
   OrderPersistenceMapper.toDomain()
   ↓ Convierte JPA → Dominio
   Order (dominio) - items sin product

4. enrichItemsWithProducts()
   ↓ Para cada item:
   ProductClient.getProductById(item.productId)
   ↓ HTTP GET /api/products/{id}
   Product Service
   ↓ Retorna Product
   item.setProduct(product) ✅

5. OrderDtoMapper.toResponse()
   ↓ Convierte Dominio → DTO
   OrderResponse con productos enriquecidos
   ↓ HTTP 200 OK
   Cliente recibe orden completa
```

---

## 🔧 CAMBIO EN PRODUCT SERVICE

### Archivo modificado: `GetProductByIdUseCase.java`

### Problema original:
Cuando Order Service llamaba a `GET /api/products/{id}`, Product Service intentaba obtener el usuario creador del User Service. Si el User Service no estaba disponible o requería autenticación, Product Service lanzaba una excepción y retornaba HTTP 500.

### Solución implementada:

**ANTES:**
```java
public Product execute(Long id) {
    Product prod = productRepository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
    
    User user = userClient.getUserById(prod.getCreatedBy());
    if(user == null) {
        throw new UserNotFoundException(id); // ❌ Falla si no hay usuario
    }
    prod.setCreatedByUser(user);
    return prod;
}
```

**DESPUÉS:**
```java
public Product execute(Long id) {
    Product prod = productRepository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
    
    // Intentar obtener usuario, pero NO fallar si no se puede
    try {
        User user = userClient.getUserById(prod.getCreatedBy());
        if(user != null) {
            prod.setCreatedByUser(user);
        } else {
            log.warn("User not found, continuing without user info");
        }
    } catch (Exception e) {
        log.warn("Could not fetch user: {}. Continuing without user info.", 
            e.getMessage());
        // ✅ Continúa sin el usuario - no es crítico
    }
    
    return prod; // ✅ Siempre retorna el producto
}
```

### Beneficios:
1. **Resiliencia:** Product Service no falla si User Service no está disponible
2. **Compatibilidad:** Order Service puede obtener productos sin problemas
3. **Degradación elegante:** El producto se retorna sin información del usuario creador

---

## 🗺️ MAPPERS Y CONVERSIONES

### Resumen de Mappers:

| Mapper | Función | Conversiones |
|---------|---------|---------------|
| `OrderDtoMapper` | DTOs ↔ Dominio | `CreateOrderRequest` ↔ `Order`<br>`OrderResponse` ↔ `Order`<br>`OrderItemRequest` ↔ `OrderItem`<br>`OrderItemResponse` ↔ `OrderItem` |
| `OrderPersistenceMapper` | Dominio ↔ JPA | `Order` ↔ `OrderEntity`<br>`OrderItem` ↔ `OrderItemEntity`<br>`OrderStatus` ↔ `String` |
| `ProductDtoMapper` | DTO ↔ Dominio | `ProductDto` ↔ `Product` |

### Flujo de conversiones completo:

```
HTTP Request (JSON)
    ↓
CreateOrderRequest (DTO)
    ↓ OrderDtoMapper
Order (Dominio)
    ↓ OrderPersistenceMapper
OrderEntity (JPA)
    ↓ JPA
PostgreSQL (BD)
    ↓ JPA
OrderEntity (JPA)
    ↓ OrderPersistenceMapper
Order (Dominio)
    ↓ OrderDtoMapper
OrderResponse (DTO)
    ↓
HTTP Response (JSON)
```

---

## 📝 RESUMEN DE ARCHIVOS

### Domain Layer (7 archivos)
- `Order.java`: Modelo de orden con lógica de negocio
- `OrderItem.java`: Modelo de item con validación
- `OrderStatus.java`: Enum de estados
- `Product.java`: Modelo de producto
- `InvalidOrderException.java`: Excepción de orden inválida
- `OrderNotFoundException.java`: Excepción de orden no encontrada
- `ProductNotFoundException.java`: Excepción de producto no encontrado
- `OrderRepository.java`: Interfaz de repositorio

### Application Layer (5 archivos)
- `CreateOrderUseCase.java`: Crea órdenes y valida productos
- `GetOrderByIdUseCase.java`: Obtiene orden y enriquece productos
- `GetAllOrdersUseCase.java`: Obtiene todas las órdenes
- `GetOrdersByUserIdUseCase.java`: Obtiene órdenes por usuario
- `OrderApplicationService.java`: Orquesta casos de uso

### Infrastructure Layer (11 archivos)
- `OrderEntity.java`: Entidad JPA de orden
- `OrderItemEntity.java`: Entidad JPA de item
- `JpaOrderRepository.java`: Repositorio JPA
- `OrderRepositoryImpl.java`: Implementación del repositorio
- `OrderPersistenceMapper.java`: Mapper JPA ↔ Dominio
- `ProductClient.java`: Cliente HTTP para Product Service
- `ProductDto.java`: DTO de respuesta de Product Service
- `ProductDtoMapper.java`: Mapper ProductDto ↔ Product
- `BeanConfig.java`: Configuración de beans

### Presentation Layer (8 archivos)
- `OrderController.java`: Controlador REST
- `GlobalExceptionHandler.java`: Manejo de excepciones
- `CreateOrderRequest.java`: DTO de request
- `OrderItemRequest.java`: DTO de item en request
- `OrderResponse.java`: DTO de respuesta
- `OrderItemResponse.java`: DTO de item en respuesta
- `ProductResponse.java`: DTO de producto en respuesta
- `OrderDtoMapper.java`: Mapper DTOs ↔ Dominio

**Total: 31 archivos Java**

---

## ✅ PRINCIPIOS APLICADOS

1. **Clean Architecture:** Separación clara de capas
2. **Dependency Inversion:** Dominio no depende de infraestructura
3. **Single Responsibility:** Cada clase tiene una responsabilidad
4. **DRY (Don't Repeat Yourself):** Mappers reutilizables
5. **Fail Fast:** Validaciones tempranas
6. **Resiliencia:** Manejo de errores sin fallar completamente

---

**Fin del documento funcional**
