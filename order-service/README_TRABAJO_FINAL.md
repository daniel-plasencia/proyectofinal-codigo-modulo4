# 📄 ORDER SERVICE - Documentación Completa del Trabajo Final

**Módulo:** 4  
**Fecha:** 23/02/2026  
**Microservicio:** Order Service - Gestión de Órdenes de Compra

---

## 📋 TABLA DE CONTENIDOS

1. [Cumplimiento de Requerimientos](#cumplimiento-de-requerimientos)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Prerequisitos](#prerequisitos)
4. [Instalación y Despliegue](#instalación-y-despliegue)
5. [Pruebas de Endpoints](#pruebas-de-endpoints-con-postman)
6. [Flujo Completo del Sistema](#flujo-completo-del-sistema-con-postman)
7. [Modelo de Datos](#modelo-de-datos)
8. [Troubleshooting](#troubleshooting)

---

## 📌 NOTA IMPORTANTE

**Todas las pruebas de endpoints se realizan usando Postman.**  
Asegúrate de tener Postman instalado antes de comenzar las pruebas.

**Descargar Postman:** https://www.postman.com/downloads/

---

## ✅ CUMPLIMIENTO DE REQUERIMIENTOS

### 🎯 Objetivo del Trabajo Final

✅ **Desarrollar un microservicio de Gestión de Órdenes (Order Service)** que se integre con el microservicio **Product Service** y se despliegue localmente en Kubernetes.

### 📋 Funcionalidades Requeridas

| Requerimiento | Estado | Implementación |
|--------------|--------|----------------|
| **1. Registrar órdenes de compra** con uno o más productos | ✅ **COMPLETADO** | Endpoint `POST /api/orders` |
| **2. Asociar cada orden a un usuario** específico | ✅ **COMPLETADO** | Campo `userId` en request y BD |
| **3. Calcular automáticamente el monto total** | ✅ **COMPLETADO** | Cálculo de `subtotal` y `totalAmount` |
| **4. Integración con Product Service** | ✅ **COMPLETADO** | Validación de productos y obtención de precios |
| **5. Despliegue en Kubernetes** | ✅ **COMPLETADO** | Manifiestos K8s completos |

### 🏗️ Arquitectura Requerida

✅ **Arquitectura de Microservicios:**
- Order Service en puerto **8083**
- Base de datos **orderdb** en puerto **5435**
- Comunicación HTTP con Product Service

✅ **Modelo de Datos:**
- Tabla `orders` con todos los campos requeridos
- Tabla `order_items` con relación 1:N
- Restricciones y validaciones implementadas

---

## 🏗️ ARQUITECTURA DEL SISTEMA

### Diagrama de Arquitectura Completa

```
┌─────────────────────────────────────────────────────────────┐
│               ARQUITECTURA COMPLETA (CON ORDER)              │
└─────────────────────────────────────────────────────────────┘

     ┌────────────┐ ┌────────────┐ ┌────────────┐
     │   User     │ │  Product   │ │   Order    │
     │  Service   │ │  Service   │ │  Service   │ ◄── NUEVO
     │   :8081    │ │   :8082    │ │   :8083    │
     │  K8s:30081 │ │  K8s:30082 │ │  K8s:30083 │
     └──────┬─────┘ └──────┬─────┘ └──────┬─────┘
            │              │              │
            │              │              │
            ▼              ▼              ▼
       ┌────────┐     ┌─────────┐    ┌────────┐
       │userdb  │     │productdb│    │orderdb │ ◄── NUEVA BD
       │ :5434  │     │ :5433   │    │ :5435 │
       └────────┘     └─────────┘    └────────┘

COMUNICACIÓN:
Order Service ──(HTTP GET /api/products/{id})──► Product Service
```

### Flujo de Datos: Crear Orden

```
┌─────────────────────────────────────────────────────────────┐
│  FLUJO: Crear Orden (RF-01)                                  │
└─────────────────────────────────────────────────────────────┘

Cliente
  │
  │ POST /api/orders
  │ {
  │   "userId": 1,
  │   "items": [
  │     { "productId": 1, "quantity": 2 },
  │     { "productId": 3, "quantity": 1 }
  │   ]
  │ }
  ▼
Order Service (Puerto 8083)
  │
  ├─► Para cada item:
  │   │
  │   ├─► Llamar a Product Service
  │   │   GET http://product-service.product-service.svc.cluster.local/api/products/1
  │   │   ✅ Validar que producto existe
  │   │   ✅ Obtener precio actual: 1299.99
  │   │
  │   ├─► Calcular subtotal
  │   │   quantity (2) × unit_price (1299.99) = subtotal (2599.98)
  │   │
  │   └─► Repetir para cada producto
  │
  ├─► Calcular total_amount
  │   Σ subtotals = 2599.98 + 399.99 = 2999.97
  │
  ├─► Generar order_number único
  │   Formato: ORD-YYYY-NNN (ej: ORD-2025-001)
  │
  ├─► Guardar en orderdb
  │   INSERT INTO orders (order_number, user_id, status, total_amount, ...)
  │   INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal, ...)
  │
  ▼
Respuesta 201 Created
{
  "id": 1,
  "orderNumber": "ORD-2025-001",
  "userId": 1,
  "items": [
    {
      "id": 1,
      "product": {
        "id": 1,
        "name": "Laptop Dell XPS 15",
        "price": 1299.99
      },
      "quantity": 2,
      "unitPrice": 1299.99,
      "subtotal": 2599.98
    },
    {
      "id": 2,
      "product": {
        "id": 3,
        "name": "Teclado Mecánico Keychron K8",
        "price": 399.99
      },
      "quantity": 1,
      "unitPrice": 399.99,
      "subtotal": 399.99
    }
  ],
  "totalAmount": 2999.97,
  "status": "PENDING",
  "createdAt": "2025-02-21T10:30:00"
}
```

---

## 📋 PREREQUISITOS

### 1. Software Requerido

| Software | Versión | Verificación |
|----------|---------|--------------|
| **Docker Desktop** | Latest | `docker --version` |
| **Kubernetes** (Docker Desktop) | Enabled | `kubectl version` |
| **Java** | 21+ | `java -version` |
| **PostgreSQL** (Docker) | 15+ | `docker ps | grep postgres` |

### 2. Servicios Dependientes

#### ✅ Product Service debe estar corriendo

**Verificar en Kubernetes:**
```powershell
# Verificar que product-service está en Kubernetes
kubectl get all -n product-service

# Deberías ver:
# - 1 pod corriendo (STATUS: Running)
# - 1 deployment
# - 1 service (NodePort 30082)
```

**Probar que product-service funciona (en Postman):**
- **Method:** `GET`
- **URL:** `http://localhost:30082/api/products`
- **Send**

**Response esperado:**
```json
[
  {
    "id": 1,
    "name": "Laptop Dell XPS 15",
    "price": 1299.99,
    ...
  },
  ...
]
```

#### ✅ PostgreSQL de order-service debe estar corriendo

```powershell
# Verificar contenedor PostgreSQL
docker ps | Select-String "postgres-order"

# Output esperado:
# postgres-order   postgres:15-alpine   Up  0.0.0.0:5435->5432/tcp
```

### 3. Base de Datos: Tablas Creadas

Las tablas deben estar creadas en `orderdb`. Si no lo están, ejecutar:

```powershell
# Desde el directorio raíz del proyecto
cd order-service

# Ejecutar migraciones SQL
Get-Content database\V1__CREATE_TABLES.sql | docker exec -i postgres-order psql -U postgres -d orderdb
Get-Content database\V2__ADD_INDEXES.sql | docker exec -i postgres-order psql -U postgres -d orderdb
Get-Content database\V3__INSERT_DATA.sql | docker exec -i postgres-order psql -U postgres -d orderdb

# Verificar tablas creadas
docker exec -i postgres-order psql -U postgres -d orderdb -c "\dt"
```

**Output esperado:**
```
          List of relations
 Schema |    Name     | Type  |  Owner   
--------+-------------+-------+----------
 public | order_items | table | postgres
 public | orders      | table | postgres
```

---

## 🚀 INSTALACIÓN Y DESPLIEGUE

### Paso 1: Generar Maven Wrapper (si no existe)

**¿Qué es el Maven Wrapper?**
- Permite usar Maven sin instalarlo globalmente
- Crea archivos `mvnw.cmd` (Windows) y `.mvn/` (configuración)

**Verificar si existe:**
```powershell
cd order-service
ls mvnw*
```

**Si no existe, generarlo:**
```powershell
cd order-service
docker run --rm -v ${PWD}:/usr/src/mymaven -w /usr/src/mymaven maven:3.9-eclipse-temurin-21 mvn wrapper:wrapper
```

Esto creará:
- `mvnw.cmd` (script para Windows)
- `.mvn/wrapper/` (configuración de Maven)

---

### Paso 2: Compilar el Proyecto

```powershell
# Desde el directorio order-service
cd order-service

# Compilar (esto descarga Maven automáticamente si es necesario)
.\mvnw.cmd clean package -DskipTests
```

**Tiempo estimado:** 2-5 minutos (primera vez, descarga dependencias)

**Verificar compilación exitosa:**
```powershell
# Verificar que se creó el JAR
ls target\*.jar

# Deberías ver:
# order-service-0.0.1-SNAPSHOT.jar
```

---

### Paso 3: Construir Imagen Docker

```powershell
# Desde el directorio order-service
docker build -t order-service:1.0 .
```

**Tiempo estimado:** 1-2 minutos

**Verificar imagen creada:**
```powershell
docker images | Select-String "order-service"

# Deberías ver:
# order-service   1.0   abc123def456   2 minutes ago   230MB
```

---

### Paso 4: (Opcional) Probar Imagen Localmente

```powershell
# Ejecutar contenedor Docker
docker run -p 8083:8083 `
  -e SPRING_PROFILES_ACTIVE=kubernetes `
  -e DB_URL=jdbc:postgresql://host.docker.internal:5435/orderdb `
  -e DB_USERNAME=postgres `
  -e DB_PASSWORD=postgres `
  -e PRODUCT_SERVICE_URL=http://host.docker.internal:30082 `
  order-service:1.0
```

**En Postman, probar:**
- **Health check:**
  - Method: `GET`
  - URL: `http://localhost:8083/actuator/health`
  
- **Listar órdenes:**
  - Method: `GET`
  - URL: `http://localhost:8083/api/orders`

**Detener contenedor:** `Ctrl+C` en la terminal donde corre

---

### Paso 5: Desplegar en Kubernetes

#### 5.1. Verificar Contexto de Kubernetes

```powershell
# Ver contextos disponibles
kubectl config get-contexts

# Cambiar a Docker Desktop (si es necesario)
kubectl config use-context docker-desktop
```

#### 5.2. Aplicar Manifiestos de Kubernetes (en orden)

```powershell
# Desde el directorio order-service
cd order-service

# 1. Crear Namespace
kubectl apply -f k8s/00-namespace.yaml
# Output: namespace/order-service created

# 2. Crear ConfigMap
kubectl apply -f k8s/01-configmap.yaml
# Output: configmap/order-service-config created

# 3. Crear Secret
kubectl apply -f k8s/02-secret.yaml
# Output: secret/order-service-secret created

# 4. Crear Deployment
kubectl apply -f k8s/03-deployment.yaml
# Output: deployment.apps/order-service created

# 5. Crear Service
kubectl apply -f k8s/04-service.yaml
# Output: service/order-service created
```

---

### Paso 6: Verificar Despliegue

#### 6.1. Verificar Namespace

```powershell
kubectl get namespaces | Select-String "order-service"

# Deberías ver:
# order-service   Active   30s
```

#### 6.2. Verificar Deployment

```powershell
kubectl get deployments -n order-service

# Deberías ver:
# NAME           READY   UP-TO-DATE   AVAILABLE   AGE
# order-service  1/1     1            1           1m
```

#### 6.3. Verificar Pods

```powershell
kubectl get pods -n order-service

# Deberías ver:
# NAME                            READY   STATUS    RESTARTS   AGE
# order-service-xxxxx-xxxxx        1/1     Running   0          1m
```

**Si el pod está en `Pending` o `Error`:**
```powershell
# Ver detalles del pod
$POD_NAME = (kubectl get pods -n order-service -o jsonpath='{.items[0].metadata.name}')
kubectl describe pod $POD_NAME -n order-service

# Ver logs
kubectl logs $POD_NAME -n order-service
```

#### 6.4. Verificar Service

```powershell
kubectl get service -n order-service

# Deberías ver:
# NAME           TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
# order-service  NodePort   10.105.xx.xx   <none>        80:30083/TCP   1m
```

**Puerto importante:** `30083` es el puerto NodePort para acceder desde fuera del cluster.

---

### Paso 7: Verificar Logs de la Aplicación

```powershell
# Obtener nombre del pod
$POD_NAME = (kubectl get pods -n order-service -o jsonpath='{.items[0].metadata.name}')

# Ver logs en tiempo real
kubectl logs -f $POD_NAME -n order-service
```

**Buscar en los logs:**
- ✅ `Started OrderServiceApplication in X seconds` → Aplicación iniciada correctamente
- ✅ `HikariPool-1 - Start completed` → Conexión a BD exitosa
- ✅ `Calling Product Service to get product with id: X` → Comunicación con Product Service

**Si hay errores:**
- Verificar que Product Service está corriendo
- Verificar que PostgreSQL está corriendo
- Verificar variables de entorno en el pod

---

## 🧪 PRUEBAS DE ENDPOINTS CON POSTMAN

> **Nota:** Todas las pruebas se realizan usando **Postman**. Asegúrate de tener Postman instalado.

### Configuración Inicial en Postman

1. **Crear una nueva Collection:**
   - Abre Postman
   - Click en "New" → "Collection"
   - Nombre: `Order Service - Trabajo Final`

2. **Crear una Variable de Entorno (opcional pero recomendado):**
   - Click en "Environments" → "Create Environment"
   - Nombre: `Order Service Local`
   - Agregar variable:
     - Variable: `base_url`
     - Initial Value: `http://localhost:30083`
   - Click "Save"

---

### Endpoint 1: Health Check (Público)

**Verificar que el servicio está corriendo**

#### Configuración en Postman:

- **Method:** `GET`
- **URL:** `http://localhost:30083/actuator/health`
- **Headers:** (ninguno necesario)

#### Pasos:

1. Selecciona método **GET**
2. Ingresa la URL: `http://localhost:30083/actuator/health`
3. Click en **Send**

#### Response esperado (200 OK):

```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

---

### Endpoint 2: Listar Todas las Órdenes

**GET** `/api/orders`

#### Configuración en Postman:

- **Method:** `GET`
- **URL:** `http://localhost:30083/api/orders`
- **Headers:** (ninguno necesario)

#### Pasos:

1. Selecciona método **GET**
2. Ingresa la URL: `http://localhost:30083/api/orders`
3. Click en **Send**

#### Response esperado (200 OK):

```json
[
  {
    "id": 1,
    "orderNumber": "ORD-2025-001",
    "userId": 1,
    "status": "CONFIRMED",
    "totalAmount": 2849.97,
    "items": [
      {
        "id": 1,
        "productId": 1,
        "quantity": 1,
        "unitPrice": 1299.99,
        "subtotal": 1299.99
      }
    ],
    "createdAt": "2025-02-21T06:56:30",
    "updatedAt": "2025-02-21T06:56:30"
  },
  {
    "id": 2,
    "orderNumber": "ORD-2025-002",
    "userId": 2,
    "status": "PENDING",
    "totalAmount": 1199.98,
    "items": [...],
    "createdAt": "2025-02-21T06:56:30",
    "updatedAt": "2025-02-21T06:56:30"
  },
  {
    "id": 3,
    "orderNumber": "ORD-2025-003",
    "userId": 1,
    "status": "SHIPPED",
    "totalAmount": 149.99,
    "items": [...],
    "createdAt": "2025-02-21T06:56:30",
    "updatedAt": "2025-02-21T06:56:30"
  }
]
```

---

### Endpoint 3: Obtener Orden por ID

**GET** `/api/orders/{id}`

#### Configuración en Postman:

- **Method:** `GET`
- **URL:** `http://localhost:30083/api/orders/1`
- **Headers:** (ninguno necesario)

#### Pasos:

1. Selecciona método **GET**
2. Ingresa la URL: `http://localhost:30083/api/orders/1`
3. Click en **Send**

#### Response esperado (200 OK):

```json
{
  "id": 1,
  "orderNumber": "ORD-2025-001",
  "userId": 1,
  "status": "CONFIRMED",
  "totalAmount": 2849.97,
  "items": [
    {
      "id": 1,
      "product": {
        "id": 1,
        "name": "Laptop Dell XPS 15",
        "price": 1299.99
      },
      "quantity": 1,
      "unitPrice": 1299.99,
      "subtotal": 1299.99
    },
    {
      "id": 2,
      "product": {
        "id": 2,
        "name": "Mouse Logitech MX Master 3",
        "price": 99.99
      },
      "quantity": 1,
      "unitPrice": 99.99,
      "subtotal": 99.99
    },
    {
      "id": 3,
      "product": {
        "id": 3,
        "name": "Teclado Mecánico Keychron K8",
        "price": 89.99
      },
      "quantity": 1,
      "unitPrice": 89.99,
      "subtotal": 89.99
    }
  ],
  "createdAt": "2025-02-21T06:56:30",
  "updatedAt": "2025-02-21T06:56:30"
}
```

#### Prueba con ID inexistente:

- **URL:** `http://localhost:30083/api/orders/999`

#### Response esperado (404 Not Found):

```json
{
  "error": "Order not found",
  "message": "Order with id 999 not found",
  "status": 404
}
```

---

### Endpoint 4: Obtener Órdenes por Usuario

**GET** `/api/orders/user/{userId}`

#### Configuración en Postman:

- **Method:** `GET`
- **URL:** `http://localhost:30083/api/orders/user/1`
- **Headers:** (ninguno necesario)

#### Pasos:

1. Selecciona método **GET**
2. Ingresa la URL: `http://localhost:30083/api/orders/user/1`
3. Click en **Send**

#### Response esperado (200 OK):

```json
[
  {
    "id": 1,
    "orderNumber": "ORD-2025-001",
    "userId": 1,
    "status": "CONFIRMED",
    "totalAmount": 2849.97,
    "items": [...],
    "createdAt": "2025-02-21T06:56:30",
    "updatedAt": "2025-02-21T06:56:30"
  },
  {
    "id": 3,
    "orderNumber": "ORD-2025-003",
    "userId": 1,
    "status": "SHIPPED",
    "totalAmount": 149.99,
    "items": [...],
    "createdAt": "2025-02-21T06:56:30",
    "updatedAt": "2025-02-21T06:56:30"
  }
]
```

---

### Endpoint 5: Crear Nueva Orden ⭐ (RF-01)

**POST** `/api/orders`

Este es el endpoint principal que cumple con el requerimiento funcional RF-01.

#### Configuración en Postman:

- **Method:** `POST`
- **URL:** `http://localhost:30083/api/orders`
- **Headers:**
  - `Content-Type: application/json`
- **Body:** (seleccionar `raw` y `JSON`)

#### Body JSON:

```json
{
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ]
}
```

#### Pasos:

1. Selecciona método **POST**
2. Ingresa la URL: `http://localhost:30083/api/orders`
3. Ve a la pestaña **Headers**
4. Agrega header: `Content-Type: application/json`
5. Ve a la pestaña **Body**
6. Selecciona **raw** y **JSON** (dropdown)
7. Pega el JSON del body mostrado arriba
8. Click en **Send**

#### Response esperado (201 Created):

```json
{
  "id": 4,
  "orderNumber": "ORD-2025-004",
  "userId": 1,
  "status": "PENDING",
  "totalAmount": 2689.97,
  "items": [
    {
      "id": 7,
      "product": null,
      "quantity": 2,
      "unitPrice": 1299.99,
      "subtotal": 2599.98
    },
    {
      "id": 8,
      "product": null,
      "quantity": 1,
      "unitPrice": 89.99,
      "subtotal": 89.99
    }
  ],
  "createdAt": "2025-02-21T10:30:00",
  "updatedAt": "2025-02-21T10:30:00"
}
```

**Nota importante:** El campo `product` aparece como `null` en la respuesta de creación porque se enriquece dinámicamente cuando se consulta la orden (GET). Para ver la información completa del producto, consulta la orden creada usando `GET /api/orders/{id}`.

#### Verificar en Base de Datos (opcional):

```powershell
# Conectarse a PostgreSQL
docker exec -it postgres-order psql -U postgres -d orderdb

# Verificar orden creada
SELECT id, order_number, user_id, status, total_amount FROM orders WHERE id = 4;

# Verificar items creados
SELECT id, order_id, product_id, quantity, unit_price, subtotal 
FROM order_items WHERE order_id = 4;
```

---

### Endpoint 6: Health Endpoint Personalizado

**GET** `/api/orders/health`

#### Configuración en Postman:

- **Method:** `GET`
- **URL:** `http://localhost:30083/api/orders/health`
- **Headers:** (ninguno necesario)

#### Pasos:

1. Selecciona método **GET**
2. Ingresa la URL: `http://localhost:30083/api/orders/health`
3. Click en **Send**

#### Response esperado (200 OK):

```
Order Service running with Clean Architecture!
```

---

## 🔄 FLUJO COMPLETO DEL SISTEMA CON POSTMAN

### Escenario Completo: Simulación de E-commerce

Este flujo demuestra el funcionamiento completo del sistema según el TRABAJO_FINAL.md usando **Postman**.

---

### **Paso 1: Verificar Servicios Dependientes**

#### 1.1. Verificar Product Service

**En Postman:**
- **Method:** `GET`
- **URL:** `http://localhost:30082/api/products`
- **Send**

**Response esperado:** Lista de productos disponibles

**Ejemplo de productos:**
- ID 1: Laptop Dell XPS 15 - $1299.99
- ID 2: Mouse Logitech MX Master 3 - $99.99
- ID 3: Teclado Mecánico Keychron K8 - $89.99
- ID 4: Monitor LG UltraWide 34" - $449.99
- ID 5: Auriculares Sony WH-1000XM5 - $349.99

#### 1.2. Verificar Order Service

**En Postman:**
- **Method:** `GET`
- **URL:** `http://localhost:30083/actuator/health`
- **Send**

**Response esperado:**
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

---

### **Paso 2: Consultar Productos Disponibles**

**En Postman:**
- **Method:** `GET`
- **URL:** `http://localhost:30082/api/products`
- **Send**

**Response esperado:** Array con todos los productos disponibles

**Anotar los IDs de productos que usarás para crear la orden.**

---

### **Paso 3: Crear Orden de Compra (RF-01)** ⭐

**Escenario:** Usuario ID 1 quiere comprar:
- 2x Laptop Dell XPS 15 (productId: 1)
- 1x Teclado Mecánico (productId: 3)

**En Postman:**
- **Method:** `POST`
- **URL:** `http://localhost:30083/api/orders`
- **Headers:**
  - `Content-Type: application/json`
- **Body** (raw, JSON):
```json
{
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ]
}
```
- **Send**

**Lo que sucede internamente:**

1. ✅ Order Service recibe el request
2. ✅ Para cada item:
   - Llama a Product Service: `GET /api/products/1`
   - Valida que producto existe
   - Obtiene precio actual: `1299.99`
   - Calcula subtotal: `2 × 1299.99 = 2599.98`
   - Llama a Product Service: `GET /api/products/3`
   - Obtiene precio: `89.99`
   - Calcula subtotal: `1 × 89.99 = 89.99`
3. ✅ Calcula total: `2599.98 + 89.99 = 2689.97`
4. ✅ Genera order_number único: `ORD-2025-004`
5. ✅ Guarda en BD:
   - `INSERT INTO orders (...)`
   - `INSERT INTO order_items (...)`
6. ✅ Retorna orden completa con todos los datos

**Response esperado (201 Created):**
```json
{
  "id": 4,
  "orderNumber": "ORD-2025-004",
  "userId": 1,
  "status": "PENDING",
  "totalAmount": 2689.97,
  "items": [
    {
      "id": 7,
      "product": null,
      "quantity": 2,
      "unitPrice": 1299.99,
      "subtotal": 2599.98
    },
    {
      "id": 8,
      "product": null,
      "quantity": 1,
      "unitPrice": 89.99,
      "subtotal": 89.99
    }
  ],
  "createdAt": "2025-02-21T10:30:00",
  "updatedAt": "2025-02-21T10:30:00"
}
```

**Nota:** El campo `product` aparece como `null` en la respuesta de creación. Se enriquece automáticamente cuando consultas la orden usando `GET /api/orders/{id}` (ver Paso 4).

**⚠️ IMPORTANTE:** Anota el `id` de la orden creada (ej: `4`) para los siguientes pasos.

---

### **Paso 4: Consultar Orden Creada**

**En Postman:**
- **Method:** `GET`
- **URL:** `http://localhost:30083/api/orders/4` (usar el ID de la orden creada)
- **Send**

**Response esperado (200 OK):**
```json
{
  "id": 4,
  "orderNumber": "ORD-2025-004",
  "userId": 1,
  "status": "PENDING",
  "totalAmount": 2689.97,
  "items": [
    {
      "id": 7,
      "product": {
        "id": 1,
        "name": "Laptop Dell XPS 15",
        "price": 1299.99
      },
      "quantity": 2,
      "unitPrice": 1299.99,
      "subtotal": 2599.98
    },
    {
      "id": 8,
      "product": {
        "id": 3,
        "name": "Teclado Mecánico Keychron K8",
        "price": 89.99
      },
      "quantity": 1,
      "unitPrice": 89.99,
      "subtotal": 89.99
    }
  ],
  "createdAt": "2025-02-21T10:30:00",
  "updatedAt": "2025-02-21T10:30:00"
}
```

**Verificar que incluye:**
- ✅ Información completa del producto (`product.id`, `product.name`, `product.price`)
- ✅ Cálculos correctos (`subtotal`, `totalAmount`)
- ✅ Número de orden único (`orderNumber`)

---

### **Paso 5: Consultar Todas las Órdenes del Usuario**

**En Postman:**
- **Method:** `GET`
- **URL:** `http://localhost:30083/api/orders/user/1`
- **Send**

**Response esperado (200 OK):**
```json
[
  {
    "id": 1,
    "orderNumber": "ORD-2025-001",
    "userId": 1,
    "status": "CONFIRMED",
    "totalAmount": 2849.97,
    ...
  },
  {
    "id": 3,
    "orderNumber": "ORD-2025-003",
    "userId": 1,
    "status": "SHIPPED",
    "totalAmount": 149.99,
    ...
  },
  {
    "id": 4,
    "orderNumber": "ORD-2025-004",
    "userId": 1,
    "status": "PENDING",
    "totalAmount": 2689.97,
    ...
  }
]
```

**Verificar que:**
- ✅ Aparece la orden recién creada (ID 4)
- ✅ Todas las órdenes pertenecen al usuario 1

---

### **Paso 6: Listar Todas las Órdenes del Sistema**

**En Postman:**
- **Method:** `GET`
- **URL:** `http://localhost:30083/api/orders`
- **Send**

**Response esperado:** Array con todas las órdenes (de todos los usuarios)

---

### **Paso 7: Verificar Comunicación Entre Servicios**

**Para verificar que Order Service se comunica con Product Service:**

```powershell
# Ver logs de Order Service
$POD_NAME = (kubectl get pods -n order-service -o jsonpath='{.items[0].metadata.name}')
kubectl logs $POD_NAME -n order-service | Select-String "Product Service"
```

**Deberías ver en los logs:**
```
Calling Product Service to get product with id: 1
Product retrieved successfully from Product Service: ProductDto(...)
Calling Product Service to get product with id: 3
Product retrieved successfully from Product Service: ProductDto(...)
```

---

### **Paso 8: Probar Casos de Error**

#### 8.1. Producto Inexistente

**En Postman:**
- **Method:** `POST`
- **URL:** `http://localhost:30083/api/orders`
- **Headers:**
  - `Content-Type: application/json`
- **Body** (raw, JSON):
```json
{
  "userId": 1,
  "items": [
    {
      "productId": 999,
      "quantity": 1
    }
  ]
}
```
- **Send**

**Response esperado (404 Not Found):**
```json
{
  "error": "Product not found",
  "message": "Product with id 999 not found in Product Service",
  "status": 404
}
```

#### 8.2. Orden con ID Inexistente

**En Postman:**
- **Method:** `GET`
- **URL:** `http://localhost:30083/api/orders/999`
- **Send**

**Response esperado (404 Not Found):**
```json
{
  "error": "Order not found",
  "message": "Order with id 999 not found",
  "status": 404
}
```

#### 8.3. Request Inválido (sin items)

**En Postman:**
- **Method:** `POST`
- **URL:** `http://localhost:30083/api/orders`
- **Headers:**
  - `Content-Type: application/json`
- **Body** (raw, JSON):
```json
{
  "userId": 1,
  "items": []
}
```
- **Send**

**Response esperado (400 Bad Request):**
```json
{
  "error": "Invalid order",
  "message": "Order must have at least one item",
  "status": 400
}
```

---

### **Resumen del Flujo Completo**

1. ✅ **Verificar servicios** → Product Service y Order Service funcionando
2. ✅ **Consultar productos** → Ver productos disponibles
3. ✅ **Crear orden** → POST con userId e items
4. ✅ **Validación automática** → Order Service valida productos con Product Service
5. ✅ **Cálculo automático** → Subtotal y total calculados
6. ✅ **Guardado en BD** → Orden y items guardados
7. ✅ **Consulta de orden** → Verificar orden creada
8. ✅ **Consulta por usuario** → Ver todas las órdenes del usuario
9. ✅ **Manejo de errores** → Probar casos de error

**Este flujo demuestra que el Order Service cumple con todos los requerimientos del TRABAJO_FINAL.md:**
- ✅ Registra órdenes de compra
- ✅ Asocia órdenes a usuarios
- ✅ Calcula montos automáticamente
- ✅ Se integra con Product Service
- ✅ Maneja errores correctamente

---

## 📊 MODELO DE DATOS

### Diagrama Entidad-Relación

```
┌─────────────────────────────┐
│            ORDERS           │
├─────────────────────────────┤
│ PK  id                      │
│     order_number (UNIQUE)   │
│     user_id                 │
│     status                  │
│     total_amount            │
│     created_at              │
│     updated_at              │
└─────────────┬───────────────┘
              │ 1      
              │         
              │                         
              │ N                        
              ▼                        
┌─────────────────────────────┐
│        ORDER_ITEMS          │
├─────────────────────────────┤
│ PK  id                      │
│ FK  order_id                │
│     product_id              │
│     quantity                │
│     unit_price              │
│     subtotal                │
└─────────────────────────────┘
                               
     product_id    ────────────────────┐
                                       │
        user_id    ──────────┐         │
                             │         │
                             ▼         ▼
                    User Service   Product Service
                      (userdb)      (productdb)
```

### Estructura de Tablas

#### Tabla: `orders`

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único de la orden |
| `order_number` | VARCHAR(50) | UNIQUE, NOT NULL | Número de orden (ej: ORD-2025-001) |
| `user_id` | BIGINT | NOT NULL | ID del usuario (ref. externa a userdb) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | Estado de la orden |
| `total_amount` | NUMERIC(10,2) | NOT NULL, >= 0 | Monto total calculado |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Fecha de creación |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Fecha de actualización |

**Estados válidos:** `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`

#### Tabla: `order_items`

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | ID único del item |
| `order_id` | BIGINT | NOT NULL, FK → orders(id) CASCADE | ID de la orden |
| `product_id` | BIGINT | NOT NULL | ID del producto (ref. externa a productdb) |
| `quantity` | INTEGER | NOT NULL, > 0 | Cantidad |
| `unit_price` | NUMERIC(10,2) | NOT NULL, >= 0 | Precio unitario (del Product Service) |
| `subtotal` | NUMERIC(10,2) | NOT NULL, >= 0 | Subtotal (quantity × unit_price) |

### Scripts de Migración

Los scripts están organizados en migraciones Flyway:

1. **V1__CREATE_TABLES.sql**: Crea tablas `orders` y `order_items` con todas las restricciones
2. **V2__ADD_INDEXES.sql**: Crea índices para optimizar consultas
3. **V3__INSERT_DATA.sql**: Inserta datos de prueba

**Ejecutar migraciones:**
```powershell
Get-Content database\V1__CREATE_TABLES.sql | docker exec -i postgres-order psql -U postgres -d orderdb
Get-Content database\V2__ADD_INDEXES.sql | docker exec -i postgres-order psql -U postgres -d orderdb
Get-Content database\V3__INSERT_DATA.sql | docker exec -i postgres-order psql -U postgres -d orderdb
```

---

## 🔧 TROUBLESHOOTING

### Problema 1: Pod no inicia (Status: Pending/Error)

**Solución:**
```powershell
# Ver detalles del pod
$POD_NAME = (kubectl get pods -n order-service -o jsonpath='{.items[0].metadata.name}')
kubectl describe pod $POD_NAME -n order-service

# Ver logs
kubectl logs $POD_NAME -n order-service

# Posibles causas:
# - Imagen Docker no encontrada → Reconstruir: docker build -t order-service:1.0 .
# - Variables de entorno incorrectas → Verificar ConfigMap y Secret
# - Product Service no disponible → Verificar que product-service está corriendo
```

---

### Problema 2: Error de conexión a Base de Datos

**Síntomas:**
- Logs muestran: `Connection refused` o `Connection timeout`
- Pod en estado `CrashLoopBackOff`

**Solución:**
```powershell
# 1. Verificar que PostgreSQL está corriendo
docker ps | Select-String "postgres-order"

# 2. Verificar que las tablas existen
docker exec -i postgres-order psql -U postgres -d orderdb -c "\dt"

# 3. Verificar variables de entorno en el pod
$POD_NAME = (kubectl get pods -n order-service -o jsonpath='{.items[0].metadata.name}')
kubectl exec $POD_NAME -n order-service -- env | Select-String "DB_"
```

---

### Problema 3: Error al llamar a Product Service

**Síntomas:**
- Al crear orden, error: `Product not found` o `Connection refused`
- Logs muestran: `Error calling Product Service`

**Solución:**
```powershell
# 1. Verificar que Product Service está corriendo
kubectl get pods -n product-service

# 2. Probar Product Service directamente
Invoke-WebRequest -Uri http://localhost:30082/api/products/1 -UseBasicParsing

# 3. Verificar URL de Product Service en ConfigMap
kubectl get configmap order-service-config -n order-service -o yaml

# Debería tener:
# PRODUCT_SERVICE_URL: http://product-service.product-service.svc.cluster.local
```

---

### Problema 4: Error 400 "Invalid order data. User ID and items are required"

**Síntomas:**
- Al crear orden con POST, recibe error 400
- Mensaje: `"Invalid order data. User ID and items are required"`
- Aunque el request tiene `userId` e `items` correctamente

**Causa:**
- Este error ya fue resuelto en la versión actual
- Ocurría cuando la validación de `OrderItem` requería `unitPrice` antes de obtenerlo del Product Service

**Solución (si ocurre):**
- Verificar que estás usando la versión más reciente del código
- El método `OrderItem.isValid()` ahora solo valida `productId` y `quantity` al crear
- Los campos `unitPrice` y `subtotal` se asignan automáticamente después de validar con Product Service

---

### Problema 5: Maven Wrapper no funciona

**Síntomas:**
- Error: `.\mvnw.cmd no se reconoce`

**Solución:**
```powershell
# Regenerar wrapper
cd order-service
docker run --rm -v ${PWD}:/usr/src/mymaven -w /usr/src/mymaven maven:3.9-eclipse-temurin-21 mvn wrapper:wrapper

# Verificar que se creó
ls mvnw*
ls .mvn\wrapper\
```

---

### Problema 6: Puerto 30083 no responde

**Solución:**
```powershell
# 1. Verificar que el Service está creado
kubectl get service -n order-service

# 2. Verificar que el pod está Running
kubectl get pods -n order-service

# 3. Verificar logs del pod
$POD_NAME = (kubectl get pods -n order-service -o jsonpath='{.items[0].metadata.name}')
kubectl logs $POD_NAME -n order-service

# 4. Probar desde dentro del cluster
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- curl http://order-service.order-service.svc.cluster.local/api/orders/health
```

---

## 📝 RESUMEN DE CUMPLIMIENTO

### ✅ Requerimientos Funcionales

| RF | Descripción | Estado | Endpoint |
|----|-------------|--------|----------|
| **RF-01** | Crear Orden de Compra | ✅ **COMPLETADO** | `POST /api/orders` |

### ✅ Funcionalidades Implementadas

- ✅ Registrar órdenes de compra con uno o más productos
- ✅ Asociar cada orden a un usuario específico
- ✅ Calcular automáticamente el monto total
- ✅ Integración con Product Service para validar productos y obtener precios
- ✅ Despliegue en Kubernetes local
- ✅ Base de datos PostgreSQL con tablas `orders` y `order_items`
- ✅ Endpoints REST completos (GET, POST)
- ✅ Manejo de errores y validaciones

### ✅ Arquitectura

- ✅ Microservicio independiente (puerto 8083)
- ✅ Base de datos propia (orderdb en puerto 5435)
- ✅ Comunicación HTTP con Product Service
- ✅ Clean Architecture (Domain, Application, Infrastructure, Presentation)
- ✅ Sin Spring Security (como se solicitó)

---

## 📚 REFERENCIAS

- **TRABAJO_FINAL.md**: Especificación completa del trabajo
- **README.md**: Documentación técnica del servicio
- **Kubernetes Manifests**: `k8s/` directory
- **Database Migrations**: `database/` directory

---

## 👨‍💻 AUTOR

**Desarrollado para:** Módulo 4 - Arquitectura de Microservicios  
**Fecha:** Febrero 2025

---

**¡Order Service está completamente funcional y cumple con todos los requerimientos del TRABAJO_FINAL.md!** 🎉
