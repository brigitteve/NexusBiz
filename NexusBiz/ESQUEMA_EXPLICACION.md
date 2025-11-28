# 📋 EXPLICACIÓN DEL ESQUEMA SQL - NEXUSBIZ

## 🔗 RELACIÓN ENTRE TABLAS Y PANTALLAS

### 1. **usuarios** (users)
**Archivos relacionados:**
- `data/model/User.kt` - Modelo de datos
- `data/repository/AuthRepository.kt` - Login, registro, verificación
- `ui/viewmodel/AuthViewModel.kt` - Gestión de sesión
- `ui/screens/auth/LoginScreen.kt` - Pantalla de login
- `ui/screens/auth/RegisterScreen.kt` - Pantalla de registro
- `ui/screens/home/HomeScreen.kt` - Muestra alias del usuario
- `ui/components/NexusDrawer.kt` - Drawer con información del usuario
- `ui/screens/profile/ProfileScreen.kt` - Perfil del usuario

**Campos clave:**
- `phone`: Usado para login (único)
- `password_hash`: Autenticación
- `user_type`: 'CONSUMER' o 'STORE_OWNER' (enum)
- `completed_groups`: Contador usado en `GroupCompletedConsumerScreen.kt`
- `total_savings`: Mostrado en pantallas de grupos completados

**Flujos:**
1. **Registro**: `RegisterScreen.kt` → crea usuario con `user_type = 'CONSUMER'`
2. **Login Cliente**: `LoginScreen.kt` → `AuthRepository.login()` → establece `currentClient`
3. **Login Bodeguero**: `LoginScreen.kt` (modo bodega) → `AuthRepository.loginStore()` → establece `currentStore`

---

### 2. **bodegas** (stores)
**Archivos relacionados:**
- `data/model/Store.kt` - Modelo de datos
- `data/repository/StoreRepository.kt` - CRUD de bodegas
- `ui/screens/store/BodegaRegistrationScreens.kt` - Registro de bodega
- `ui/screens/store/StoreDashboardScreen.kt` - Dashboard del bodeguero
- `ui/screens/store/StoreProfileScreen.kt` - Perfil de bodega
- `ui/screens/store/PublishProductScreen.kt` - Publicar ofertas

**Campos clave:**
- `owner_id`: Relación con `usuarios` (el bodeguero)
- `ruc`: RUC de la bodega (validado en `BodegaValidateRucScreen.kt`)
- `commercial_name`: Nombre comercial (capturado en registro)
- `latitude/longitude`: Para búsquedas geográficas en `QuickBuyScreen.kt`

**Flujos:**
1. **Registro Bodega**: `BodegaRegistrationScreens.kt` → valida RUC → crea bodega → establece `currentStore`
2. **Dashboard**: `StoreDashboardScreen.kt` → muestra ofertas y grupos de la bodega

---

### 3. **categorias** (categories)
**Archivos relacionados:**
- `data/model/Category.kt` - Modelo de datos
- `data/repository/ProductRepository.kt` - `getCategories()`
- `ui/screens/home/HomeScreen.kt` - Filtros por categoría

**Uso:**
- Filtrado de productos en `HomeScreen.kt`
- Categorías: "Todos", "Alimentos", "Limpieza", "Bebidas", "Snacks"

---

### 4. **productos** (products) - Ofertas publicadas
**Archivos relacionados:**
- `data/model/Product.kt` - Modelo de datos
- `data/repository/ProductRepository.kt` - CRUD de productos
- `ui/screens/store/PublishProductScreen.kt` - Publicar oferta
- `ui/screens/home/HomeScreen.kt` - Lista de ofertas
- `ui/screens/product/ProductDetailScreen.kt` - Detalle de oferta
- `ui/screens/store/StoreDashboardScreen.kt` - Ofertas de la bodega

**Campos clave:**
- `normal_price` / `group_price`: Validación en `ProductRepository.createProduct()` (group_price < normal_price)
- `min_group_size` / `max_group_size`: Validación en `PublishProductScreen.kt`
- `duration_hours`: Duración de la oferta (capturado en `PublishProductScreen.kt` línea 80)
- `is_active`: Para desactivar ofertas

**Flujos:**
1. **Publicar Oferta**: `PublishProductScreen.kt` → valida precios → crea producto
2. **Ver Ofertas**: `HomeScreen.kt` → filtra por distrito y categoría
3. **Detalle**: `ProductDetailScreen.kt` → muestra producto y grupos activos

---

### 5. **grupos** (groups) - Grupos de compra colectiva
**Archivos relacionados:**
- `data/model/Group.kt` - Modelo de datos
- `data/repository/GroupRepository.kt` - Lógica completa de grupos
- `ui/screens/groups/MyGroupsScreen.kt` - Lista de grupos del usuario
- `ui/screens/groups/GroupReservedScreen.kt` - Grupo en reserva (ACTIVE)
- `ui/screens/groups/GroupReadyForPickupScreen.kt` - Grupo listo para retiro (PICKUP)
- `ui/screens/groups/GroupCompletedConsumerScreen.kt` - Grupo completado (VALIDATED/COMPLETED)
- `ui/screens/groups/GroupExpiredConsumerScreen.kt` - Grupo expirado (EXPIRED)
- `ui/screens/groups/PickupQRScreen.kt` - Muestra QR de retiro

**Máquina de Estados:**
```
ACTIVE (reserva) 
  ↓ (cuando current_size >= target_size)
PICKUP (meta cumplida, QR generado)
  ↓ (cuando todos los participantes validan)
VALIDATED (todos retirados)
  ↓
COMPLETED

ACTIVE (reserva)
  ↓ (si expires_at < NOW())
EXPIRED
```

**Campos clave:**
- `status`: Enum `group_status` (ACTIVE, PICKUP, VALIDATED, COMPLETED, EXPIRED)
- `qr_code`: Generado automáticamente cuando `current_size >= target_size` (trigger)
- `expires_at`: Fecha de expiración (calculada con `duration_hours` del producto)
- `validated_at`: Fecha cuando todos los participantes retiraron

**Flujos:**
1. **Crear Grupo**: `ProductDetailScreen.kt` → `GroupRepository.createGroup()` → estado ACTIVE
2. **Unirse a Grupo**: `ProductDetailScreen.kt` → `GroupRepository.createReservation()` → actualiza `current_size`
3. **Completar Meta**: Trigger automático → cambia a PICKUP → genera QR
4. **Ver QR**: `PickupQRScreen.kt` → muestra QR solo si status = PICKUP o VALIDATED
5. **Validar Retiro**: `ScanQRScreen.kt` → `GroupRepository.validateGroup()` → cambia a VALIDATED
6. **Expirar**: Trigger automático → cambia ACTIVE a EXPIRED si `expires_at < NOW()`

---

### 6. **participantes** (participants)
**Archivos relacionados:**
- `data/model/Participant.kt` - Modelo de datos
- `data/repository/GroupRepository.kt` - `createReservation()` crea participantes
- `ui/screens/groups/GroupDetailScreen.kt` - Lista de participantes
- `ui/screens/store/StoreGroupDetailScreen.kt` - Participantes para bodeguero
- `ui/screens/store/ScanQRScreen.kt` - Validación de participantes

**Campos clave:**
- `is_validated`: Indica si el participante ya retiró (usado en `ScanQRScreen.kt`)
- `validated_at`: Fecha de retiro
- `UNIQUE(group_id, user_id)`: Previene unirse dos veces (validación en `GroupRepository.createReservation()` línea 110)

**Flujos:**
1. **Unirse**: `GroupRepository.createReservation()` → crea registro en `participantes`
2. **Validar Retiro**: `ScanQRScreen.kt` → actualiza `is_validated = true`
3. **Completar Grupo**: Trigger `check_group_completion()` → si todos validados, grupo → VALIDATED

---

### 7. **codigos_verificacion** (verification_codes)
**Archivos relacionados:**
- `data/repository/AuthRepository.kt` - `sendVerificationCode()`, `verifyCode()`
- `ui/screens/auth/RegisterScreen.kt` - Verificación de teléfono
- `ui/screens/store/BodegaVerifyPhoneScreen.kt` - Verificación de bodega

**Uso:**
- Códigos SMS temporales (expiran en 10 minutos)
- Un código por teléfono, no reutilizable

---

## 🔄 FLUJOS DE NEGOCIO IMPLEMENTADOS

### Flujo 1: Publicar Oferta (Bodeguero)
```
PublishProductScreen.kt
  → valida: nombre, precios (group < normal), target, duración
  → ProductRepository.createProduct()
  → INSERT en productos
```

### Flujo 2: Reservar Producto (Cliente)
```
ProductDetailScreen.kt
  → verifica grupo activo existente
  → si no existe: GroupRepository.createGroup() → INSERT en grupos
  → si existe: GroupRepository.createReservation() → INSERT en participantes
  → trigger actualiza current_size
  → si current_size >= target_size: trigger cambia a PICKUP y genera QR
```

### Flujo 3: Retirar Producto (Cliente)
```
PickupQRScreen.kt
  → muestra QR solo si status = PICKUP o VALIDATED
  → ScanQRScreen.kt (bodeguero)
  → GroupRepository.validateGroup()
  → UPDATE participantes SET is_validated = true
  → trigger check_group_completion()
  → si todos validados: grupo → VALIDATED
```

### Flujo 4: Expiración Automática
```
Trigger expire_active_groups() (ejecutar periódicamente con cron)
  → UPDATE grupos SET status = 'EXPIRED'
  WHERE status = 'ACTIVE' AND expires_at < NOW()
```

---

## 📊 ÍNDICES Y RENDIMIENTO

### Índices críticos:
1. **usuarios.phone**: Búsquedas de login (muy frecuente)
2. **grupos(product_id, status)**: Validar grupos activos únicos por producto
3. **grupos.expires_at**: Para expiración automática
4. **participantes(group_id, user_id)**: Validar duplicados al unirse
5. **bodegas.location (GIST)**: Búsquedas geográficas en `QuickBuyScreen.kt`

---

## 🛡️ VALIDACIONES IMPLEMENTADAS

### A nivel de base de datos:
1. **CHECK constraints**: Precios positivos, group_price < normal_price
2. **UNIQUE constraints**: phone único, qr_code único, (group_id, user_id) único
3. **FOREIGN KEYS**: Integridad referencial
4. **Triggers**: 
   - Prevenir grupos activos duplicados por producto
   - Actualizar current_size automáticamente
   - Generar QR cuando se completa meta
   - Cambiar a VALIDATED cuando todos retiran

### A nivel de aplicación (Kotlin):
- `Validators.kt`: Validación de teléfono, nombres, precios
- `GroupRepository`: Validaciones de negocio (no duplicados, no lleno, no expirado)
- `ProductRepository`: Validación de precios y tamaños

---

## 🎯 PRÓXIMOS PASOS PARA INTEGRACIÓN

1. **Ejecutar el SQL** en Supabase SQL Editor
2. **Configurar RLS** (Row Level Security) según necesidades
3. **Conectar repositorios Kotlin** con Supabase Client
4. **Configurar triggers de expiración** con Supabase Edge Functions o cron
5. **Implementar autenticación** con Supabase Auth (reemplazar password_hash manual)

---

## 📝 NOTAS IMPORTANTES

- **QR único**: Se genera automáticamente cuando `current_size >= target_size`
- **Estados del grupo**: La máquina de estados está implementada en triggers
- **Denormalización**: Algunos campos (store_name, product_name) están denormalizados para rendimiento
- **Expiración**: Los grupos ACTIVE expiran automáticamente si `expires_at < NOW()`
- **Validación completa**: Un grupo pasa a VALIDATED solo cuando TODOS los participantes retiran

