# 🔗 CONFIGURACIÓN DE URL DE PRODUCT SERVICE

**Pregunta:** ¿Dónde se define la URL con la que Order Service consume Product Service?

---

## 📍 UBICACIÓN DE LA CONFIGURACIÓN

La URL se configura en **múltiples lugares** siguiendo un orden de prioridad. Aquí está el flujo completo:

---

## 🔄 FLUJO DE CONFIGURACIÓN (Orden de Prioridad)

```
1. Código Java (ProductClient.java)
   ↓ Lee con @Value("${product.service.url}")
   
2. application.yaml (Valor por defecto LOCAL)
   ↓ product.service.url: http://localhost:8082
   
3. application-kubernetes.yaml (Valor por defecto K8s)
   ↓ product.service.url: http://product-service.product-service.svc.cluster.local
   
4. ConfigMap de Kubernetes (Sobrescribe)
   ↓ PRODUCT_SERVICE_URL: http://product-service.product-service.svc.cluster.local
   
5. Variable de Entorno en Deployment (Inyecta)
   ↓ env: PRODUCT_SERVICE_URL desde ConfigMap
   
6. Spring Boot lee la variable
   ↓ Convierte PRODUCT_SERVICE_URL → product.service.url
   
7. ProductClient usa la URL
   ↓ String url = productServiceUrl + "/api/products/" + productId
```

---

## 📝 PASO A PASO DETALLADO

### 1️⃣ **Código Java - ProductClient.java**

**Ubicación:** `order-service/src/main/java/.../infrastructure/client/ProductClient.java`

**Línea 24-25:**
```java
@Value("${product.service.url}")
private String productServiceUrl;
```

**¿Qué hace?**
- Spring inyecta el valor de la propiedad `product.service.url`
- Se busca en: variables de entorno, application.yaml, ConfigMap, etc.

**Uso (línea 33):**
```java
String url = this.productServiceUrl + "/api/products/" + productId;
// Ejemplo: "http://product-service.product-service.svc.cluster.local/api/products/1"
```

---

### 2️⃣ **application.yaml (Desarrollo Local)**

**Ubicación:** `order-service/src/main/resources/application.yaml`

**Líneas 72-74:**
```yaml
product:
  service:
    url: ${PRODUCT_SERVICE_URL:http://localhost:8082}
```

**¿Qué significa?**
- `PRODUCT_SERVICE_URL`: Variable de entorno (tiene prioridad)
- `:http://localhost:8082`: Valor por defecto si no existe la variable

**Cuándo se usa:**
- Cuando ejecutas la aplicación **localmente** (sin Kubernetes)
- Ejemplo: `java -jar order-service.jar`

---

### 3️⃣ **application-kubernetes.yaml (Kubernetes)**

**Ubicación:** `order-service/src/main/resources/application-kubernetes.yaml`

**Líneas 74-76:**
```yaml
product:
  service:
    url: ${PRODUCT_SERVICE_URL:http://product-service.product-service.svc.cluster.local}
```

**¿Qué significa?**
- Valor por defecto para Kubernetes
- URL del DNS interno de Kubernetes

**Cuándo se usa:**
- Cuando el perfil activo es `kubernetes`
- Se activa con: `SPRING_PROFILES_ACTIVE=kubernetes`

---

### 4️⃣ **ConfigMap de Kubernetes**

**Ubicación:** `order-service/k8s/01-configmap.yaml`

**Línea 15:**
```yaml
data:
  PRODUCT_SERVICE_URL: "http://product-service.product-service.svc.cluster.local"
```

**¿Qué es?**
- ConfigMap almacena configuración **no sensible**
- Se crea en Kubernetes con: `kubectl apply -f 01-configmap.yaml`

**Formato de la URL:**
```
http://<service-name>.<namespace>.svc.cluster.local
```

**Componentes:**
- `product-service`: Nombre del Service de Kubernetes
- `product-service`: Namespace donde está el Service
- `svc.cluster.local`: Dominio interno de Kubernetes

**¿Cómo saber cuál es la URL?**
1. Ver el Service de Product Service:
   ```bash
   kubectl get service -n product-service
   ```
   Salida:
   ```
   NAME              TYPE       CLUSTER-IP      PORT(S)
   product-service   NodePort   10.106.64.249   80:30082/TCP
   ```

2. El nombre del Service es: `product-service`
3. El namespace es: `product-service`
4. La URL completa es: `http://product-service.product-service.svc.cluster.local`

**Puerto:**
- El Service expone el puerto **80** (interno)
- El contenedor escucha en **8082** (targetPort)
- Kubernetes enruta automáticamente: `puerto 80 → puerto 8082`

---

### 5️⃣ **Deployment - Inyección de Variable**

**Ubicación:** `order-service/k8s/03-deployment.yaml`

**Líneas 59-63:**
```yaml
env:
  - name: PRODUCT_SERVICE_URL
    valueFrom:
      configMapKeyRef:
        name: order-service-config
        key: PRODUCT_SERVICE_URL
```

**¿Qué hace?**
- Inyecta la variable `PRODUCT_SERVICE_URL` en el contenedor
- Toma el valor del ConfigMap `order-service-config`
- Spring Boot convierte `PRODUCT_SERVICE_URL` → `product.service.url`

**Conversión automática:**
- Spring Boot convierte `PRODUCT_SERVICE_URL` (snake_case) → `product.service.url` (dot notation)
- O puedes usar directamente: `@Value("${PRODUCT_SERVICE_URL}")`

---

## 🔍 ¿CÓMO SABER QUÉ URL SE ESTÁ USANDO?

### Opción 1: Ver logs del pod

```bash
kubectl logs -n order-service deployment/order-service | grep "Product Service"
```

Verás:
```
Calling Product Service to get product with id: 1
```

### Opción 2: Ver variables de entorno del pod

```bash
kubectl exec -n order-service deployment/order-service -- env | grep PRODUCT
```

Salida:
```
PRODUCT_SERVICE_URL=http://product-service.product-service.svc.cluster.local
```

### Opción 3: Ver ConfigMap

```bash
kubectl get configmap order-service-config -n order-service -o yaml
```

---

## 🌐 DIFERENCIAS: LOCAL vs KUBERNETES

### 🔵 Desarrollo Local (sin Kubernetes)

**URL usada:** `http://localhost:8082`

**Configuración:**
- `application.yaml` → `product.service.url: http://localhost:8082`
- Product Service debe estar corriendo en `localhost:8082`

**Cómo probar:**
```bash
# Ejecutar Order Service localmente
java -jar order-service.jar

# O con Maven
mvn spring-boot:run
```

---

### 🟢 Kubernetes (Producción/Desarrollo)

**URL usada:** `http://product-service.product-service.svc.cluster.local`

**Configuración:**
1. ConfigMap define: `PRODUCT_SERVICE_URL`
2. Deployment inyecta la variable
3. Spring Boot lee: `product.service.url`

**Cómo funciona:**
```
Order Service Pod
  ↓
Llama a: http://product-service.product-service.svc.cluster.local
  ↓
Kubernetes DNS resuelve el nombre
  ↓
Enruta al Service: product-service
  ↓
Service enruta al Pod: product-service-xxx
  ↓
Pod responde en puerto 8082
```

---

## 📋 RESUMEN: Dónde se define la URL

| Ubicación | Archivo | Propósito |
|-----------|---------|-----------|
| **Código** | `ProductClient.java` | Lee la URL con `@Value` |
| **Local** | `application.yaml` | Valor por defecto local |
| **K8s** | `application-kubernetes.yaml` | Valor por defecto K8s |
| **Config** | `01-configmap.yaml` | **URL real en Kubernetes** |
| **Deploy** | `03-deployment.yaml` | Inyecta variable al pod |

---

## ✅ CONFIGURACIÓN ACTUAL

### En Kubernetes (lo que estás usando):

1. **ConfigMap** (`01-configmap.yaml`):
   ```yaml
   PRODUCT_SERVICE_URL: "http://product-service.product-service.svc.cluster.local"
   ```

2. **Deployment** (`03-deployment.yaml`):
   ```yaml
   env:
     - name: PRODUCT_SERVICE_URL
       valueFrom:
         configMapKeyRef:
           name: order-service-config
           key: PRODUCT_SERVICE_URL
   ```

3. **ProductClient.java**:
   ```java
   @Value("${product.service.url}")
   private String productServiceUrl;
   ```

4. **Resultado:**
   - Spring Boot lee `PRODUCT_SERVICE_URL` del entorno
   - Lo convierte a `product.service.url`
   - `ProductClient` usa: `http://product-service.product-service.svc.cluster.local`

---

## 🔧 CÓMO CAMBIAR LA URL

### Opción 1: Modificar ConfigMap

```bash
# Editar ConfigMap
kubectl edit configmap order-service-config -n order-service

# Cambiar PRODUCT_SERVICE_URL
# Reiniciar deployment
kubectl rollout restart deployment order-service -n order-service
```

### Opción 2: Modificar archivo YAML

1. Editar `order-service/k8s/01-configmap.yaml`
2. Cambiar el valor de `PRODUCT_SERVICE_URL`
3. Aplicar: `kubectl apply -f 01-configmap.yaml`
4. Reiniciar: `kubectl rollout restart deployment order-service -n order-service`

---

## 🎯 FORMATO DE URL EN KUBERNETES

### DNS Interno de Kubernetes:

```
http://<service-name>.<namespace>.svc.cluster.local
```

**Ejemplo:**
- Service name: `product-service`
- Namespace: `product-service`
- URL completa: `http://product-service.product-service.svc.cluster.local`

### ¿Por qué este formato?

1. **Service name:** Nombre del Service de Kubernetes
2. **Namespace:** Namespace donde está el Service
3. **svc.cluster.local:** Dominio interno de Kubernetes para Services

### Verificar Service:

```bash
# Ver Services en el namespace
kubectl get service -n product-service

# Ver detalles del Service
kubectl describe service product-service -n product-service
```

---

## 📚 REFERENCIAS

- **Spring @Value:** https://docs.spring.io/spring-framework/reference/core/beans/annotation-value.html
- **Kubernetes DNS:** https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/
- **ConfigMap:** https://kubernetes.io/docs/concepts/configuration/configmap/

---

**Fin del documento**
