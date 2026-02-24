# ⏸️ PAUSAR Y REACTIVAR SERVICIOS EN KUBERNETES

## ✅ ESTADO ACTUAL: **PAUSADOS** (0 réplicas)

Los servicios están pausados para liberar memoria. La configuración se mantiene intacta.

---

## ⏸️ PAUSAR SERVICIOS (Liberar Memoria)

### Pausar todos los servicios:

```powershell
# Order Service
kubectl scale deployment order-service --replicas=0 -n order-service

# Product Service
kubectl scale deployment product-service --replicas=0 -n product-service

# User Service
kubectl scale deployment user-service --replicas=0 -n user-service
```

### Verificar que están pausados:

```powershell
kubectl get deployments --all-namespaces
```

Deberías ver:
```
order-service     0/0     0            0
product-service   0/0     0            0
user-service      0/0     0            0
```

---

## ▶️ REACTIVAR SERVICIOS

### Reactivar todos los servicios:

```powershell
# Order Service
kubectl scale deployment order-service --replicas=1 -n order-service

# Product Service
kubectl scale deployment product-service --replicas=1 -n product-service

# User Service
kubectl scale deployment user-service --replicas=1 -n user-service
```

### Verificar que están corriendo:

```powershell
kubectl get pods --all-namespaces
```

Deberías ver los pods en estado `Running`:
```
order-service     order-service-xxx    1/1     Running
product-service   product-service-xxx  1/1     Running
user-service      user-service-xxx     1/1     Running
```

### Esperar a que estén listos:

```powershell
kubectl wait --for=condition=ready pod -l app=order-service -n order-service --timeout=120s
kubectl wait --for=condition=ready pod -l app=product-service -n product-service --timeout=120s
kubectl wait --for=condition=ready pod -l app=user-service -n user-service --timeout=120s
```

---

## 🔍 VER ESTADO ACTUAL

### Ver deployments:

```powershell
kubectl get deployments --all-namespaces
```

### Ver pods:

```powershell
kubectl get pods --all-namespaces
```

### Ver solo tus servicios:

```powershell
kubectl get pods -n order-service
kubectl get pods -n product-service
kubectl get pods -n user-service
```

---

## 💾 LIBERAR MÁS MEMORIA

Si aún necesitas más memoria, puedes:

### 1. Deshabilitar Kubernetes en Docker Desktop

1. Abre **Docker Desktop**
2. Ve a **Settings** → **Kubernetes**
3. Desmarca **"Enable Kubernetes"**
4. Click en **"Apply & Restart"**

**⚠️ Nota:** Esto detendrá completamente Kubernetes. Tendrás que reactivarlo cuando quieras usar los servicios.

### 2. Reducir recursos de Docker Desktop

1. Abre **Docker Desktop**
2. Ve a **Settings** → **Resources**
3. Reduce:
   - **CPUs**: De 4 a 2
   - **Memory**: De 4GB a 2GB
4. Click en **"Apply & Restart"**

---

## 📊 VERIFICAR USO DE MEMORIA

### Ver uso de recursos de los pods:

```powershell
kubectl top pods --all-namespaces
```

### Ver uso de recursos de los nodes:

```powershell
kubectl top nodes
```

---

## 🚀 COMANDOS RÁPIDOS

### Pausar todo:

```powershell
kubectl scale deployment order-service --replicas=0 -n order-service; kubectl scale deployment product-service --replicas=0 -n product-service; kubectl scale deployment user-service --replicas=0 -n user-service
```

### Reactivar todo:

```powershell
kubectl scale deployment order-service --replicas=1 -n order-service; kubectl scale deployment product-service --replicas=1 -n product-service; kubectl scale deployment user-service --replicas=1 -n user-service
```

---

## ⚠️ IMPORTANTE

- **Los datos NO se pierden**: Las bases de datos siguen corriendo en Docker Compose
- **La configuración se mantiene**: Todos los ConfigMaps, Secrets, Services, etc. siguen existiendo
- **Solo se pausan los pods**: Cuando reactives, los pods se recrearán automáticamente

---

**Última actualización:** Servicios pausados ✅
