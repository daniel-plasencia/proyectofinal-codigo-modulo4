# KUBERNETES CON SPRING BOOT Y DOCKER

### 1.- Pre-requisitos

#### Verificar que product-service está ejecutándose
```
# Verificar que product-service está en Kubernetes
kubectl get all -n product-service

# Deberías ver:
# - 1 pods corriendo
# - 1 deployment
# - 1 service

# Probar que product-service funciona
curl http://localhost:30082/api/products
```

#### Verificar PostgreSQL de order-service
```
# Verificar que PostgreSQL de order está corriendo
docker ps | grep postgres-order

# Output esperado:
# postgres-order  postgres:15-alpine  Up  0.0.0.0:5435->5432/tcp
```

#### Crear las tablas en orderdb
```
# Conectarse a PostgreSQL de order
docker exec -it postgres-order psql -U postgres -d orderdb

# Ejecutar los scripts de migración (en orden):
# 1. V1__CREATE_TABLES.sql
# 2. V2__ADD_INDEXES.sql
# 3. V3__INSERT_DATA.sql

# O ejecutar desde archivos:
docker exec -i postgres-order psql -U postgres -d orderdb < database/V1__CREATE_TABLES.sql
docker exec -i postgres-order psql -U postgres -d orderdb < database/V2__ADD_INDEXES.sql
docker exec -i postgres-order psql -U postgres -d orderdb < database/V3__INSERT_DATA.sql
```

### 2.- Actualizar profile de Kubernetes `application-kubernetes.yaml`

``` yaml 
# ============================================
# APPLICATION CONFIGURATION FOR KUBERNETES
# ============================================
# Order Service - Configuración para Kubernetes

server:
  port: 8083

spring:
  application:
    name: order-service
  
  # ============================================
  # DATASOURCE - OrderDB
  # ============================================
  datasource:
    url: ${DB_URL:jdbc:postgresql://host.docker.internal:5435/orderdb}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${POOL_SIZE:10}
      minimum-idle: 5
      connection-timeout: 20000
  
  # ============================================
  # JPA CONFIGURATION
  # ============================================
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          lob:
            non_contextual_creation: true
    hibernate:
      ddl-auto: ${DDL_AUTO:validate}
    show-sql: ${SHOW_SQL:false}

# ============================================
# ACTUATOR (Para health checks de Kubernetes)
# ============================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

# ============================================
# LOGGING
# ============================================
logging:
  level:
    com.tecsup.app.micro.order: ${LOG_LEVEL:INFO}
    org.hibernate.SQL: ${SQL_LOG_LEVEL:WARN}

# ============================================
# PRODUCT SERVICE URL - Comunicación entre servicios
# ============================================
# En Kubernetes, usar DNS interno
# Formato: http://<service-name>.<namespace>.svc.cluster.local
product:
  service:
    url: ${PRODUCT_SERVICE_URL:http://product-service.product-service.svc.cluster.local}
```


### 3.- Agregar Actuator

- Agregar dependencia de Actuator en `pom.xml`

```xml

<!-- Spring Boot Actuator (para health checks de Kubernetes) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

```

- Compilar para verificar
```
cd order-service
mvn clean compile
```


### 4 - Iniciar PostgreSQL para orderdb

#### Iniciar PostgreSQL

```
# Desde el directorio raíz del proyecto
docker-compose up -d postgres-order

# Ver logs
docker logs postgres-order
```

#### Crear las tablas

```
# Conectarse a PostgreSQL
docker exec -it postgres-order psql -U postgres -d orderdb

# Ejecutar los scripts SQL en orden:
# 1. V1__CREATE_TABLES.sql
# 2. V2__ADD_INDEXES.sql
# 3. V3__INSERT_DATA.sql

# O desde archivos:
docker exec -i postgres-order psql -U postgres -d orderdb < database/V1__CREATE_TABLES.sql
docker exec -i postgres-order psql -U postgres -d orderdb < database/V2__ADD_INDEXES.sql
docker exec -i postgres-order psql -U postgres -d orderdb < database/V3__INSERT_DATA.sql
```



### 5.- Dockerizar order-service

#### Compilar con perfil Kubernetes

```
mvn clean package -DskipTests
```

#### Dockerfile

```
# ============================================
# ETAPA 2: RUNTIME
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar JAR desde etapa de build
COPY target/*.jar /app/order-service.jar

# Puerto
EXPOSE 8083

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/order-service.jar"]
```

#### Construir imagen

```
# Construir imagen
docker build -t order-service:1.0 .

# Este proceso toma 2-3 minutos la primera vez
# Ver progreso: [1/2] STEP X/Y...

# Verificar imagen creada
docker images | grep order-service

# Deberías ver:
# order-service   1.0   abc123def456   1 minute ago   230MB
```

#### Probar la imagen Docker

```
# Ejecutar contenedor
docker run -p 8083:8083 \
  -e SPRING_PROFILES_ACTIVE=kubernetes \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5435/orderdb \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e PRODUCT_SERVICE_URL=http://host.docker.internal:30082 \
  order-service:1.0

# En otra terminal, probar
curl http://localhost:8083/actuator/health

# Output esperado:
# {"status":"UP","groups":["liveness","readiness"]}

# Ctrl+C para detener
```

### 6.- Desplegar en Kubernetes

#### Crear Namespace en Kubernetes

- Aplicar namespace
```
kubectl apply -f k8s/00-namespace.yaml

# Output:
# namespace/order-service created
```
- Verificar namespace
```
kubectl get namespaces  

# Deberías ver:
# user-service       Active   X minutes
# product-service    Active   X minutes
# order-service      Active   5s
```

#### Crear ConfigMap

- Aplicar ConfigMap
```
kubectl apply -f k8s/01-configmap.yaml

# Output:
# configmap/order-service-config created
```
- Verificar ConfigMap
```
kubectl get configmap -n order-service

# Ver contenido
kubectl describe configmap order-service-config -n order-service
```


#### Entender la URL de product-service

```
http://product-service.product-service.svc.cluster.local
      └─────┬─────┘ └────┬─────┘ └┬┘ └────┬────┘
        Service       Namespace  Tipo   Cluster

Explicación:
    product-service: Nombre del Service en K8s
    product-service: Namespace donde está
    svc: Tipo "service"
    cluster.local: Dominio del cluster
```

#### Crear Secret
```
# Aplicar
kubectl apply -f k8s/02-secret.yaml

# Output:
# secret/order-service-secret created

# Verificar
kubectl get secret -n order-service

# Ver detalle 
kubectl describe secret order-service-secret -n order-service
```

#### Desplegar Order-Service

- Aplicar Deployment
```
kubectl apply -f k8s/03-deployment.yaml

# Output:
# deployment.apps/order-service created
```


- En caso necesites redesplegar (por ejemplo, después de corregir un error en el Deployment):
```
 kubectl rollout restart deployment order-service -n order-service
```


- Verificar pods
```
kubectl get pods -n order-service 
```

- Ver logs
```
# Ver logs
kubectl logs -f <POD_NAME> -n order-service

# Ver descripción completa del pod
kubectl describe pod <POD_NAME> -n order-service
```

- Verificar variables de entorno

```
# Entrar al pod
kubectl exec -it <POD_NAME> -n order-service -- /bin/sh

# Ver variables
env | grep DB_
env | grep PRODUCT_SERVICE_URL

# Deberías ver:
# PRODUCT_SERVICE_URL=http://product-service.product-service.svc.cluster.local

# Salir
exit
```

#### Exponer con Service

- Aplicar Service

```
kubectl apply -f k8s/04-service.yaml

# Output:
# service/order-service created
```

- Verificar Service
```

kubectl get service -n order-service

# Output:
# NAME              TYPE       CLUSTER-IP      PORT(S)        AGE
# order-service     NodePort   10.96.xxx.xxx   80:30083/TCP   5s
```

- Probar order-service
```
# Health check
curl http://localhost:30083/actuator/health

# Output esperado:
# {"status":"UP"}
```
# Listar órdenes
```
curl http://localhost:30083/api/orders
```

### 7.- Probar Comunicación Entre Servicios

#### Ver todos los servicios

```
# En namespace user-service
kubectl get all -n user-service

# En namespace product-service
kubectl get all -n product-service

# En namespace order-service
kubectl get all -n order-service
```

#### Probar comunicación desde order-service a product-service

```
# Crear una orden (debería validar productos en product-service)
curl -X POST http://localhost:30083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
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
  }'

# Output esperado (ejemplo):
# {
#   "id": 1,
#   "orderNumber": "ORD-2025-001",
#   "userId": 1,
#   "items": [
#     {
#       "id": 1,
#       "product": {
#         "id": 1,
#         "name": "Laptop Dell XPS 15",
#         "price": 1299.99
#       },
#       "quantity": 2,
#       "unitPrice": 1299.99,
#       "subtotal": 2599.98
#     }
#   ],
#   "totalAmount": 2999.97,
#   "status": "PENDING",
#   "createdAt": "2025-01-20T10:30:00"
# }
```

#### Ver logs de comunicación
```
# Ver logs de order-service
kubectl logs -f <POD_NAME> -n order-service

# Deberías ver:
# Calling Product Service to get product with id: 1
# Product retrieved successfully from Product Service: ProductDto(id=1, name=Laptop Dell XPS 15, ...)
```

#### Probar otros endpoints

```
# Obtener orden por ID
curl http://localhost:30083/api/orders/1

# Obtener órdenes por usuario
curl http://localhost:30083/api/orders/user/1

# Health check
curl http://localhost:30083/api/orders/health
```

### 8.- Endpoints disponibles

- `POST /api/orders` - Crear una nueva orden
- `GET /api/orders` - Listar todas las órdenes
- `GET /api/orders/{id}` - Obtener orden por ID
- `GET /api/orders/user/{userId}` - Obtener órdenes por usuario
- `GET /api/orders/health` - Health check del servicio
- `GET /actuator/health` - Health check de Kubernetes

### 9.- Notas importantes

- **Order Service NO tiene Spring Security**: Todos los endpoints son públicos
- **Depende de Product Service**: Debe estar corriendo para validar productos
- **Base de datos**: orderdb en puerto 5435
- **Puerto local**: 8083
- **Puerto Kubernetes**: 30083 (NodePort)
