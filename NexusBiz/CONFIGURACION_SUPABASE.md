# 🔧 CONFIGURACIÓN DE SUPABASE - NEXUSBIZ

## ✅ Cambios Realizados

### 1. Dependencias Agregadas
- ✅ Supabase Client for Kotlin (v2.1.8)
- ✅ Kotlinx Serialization

### 2. Archivos Creados/Modificados

#### Nuevos Archivos:
- ✅ `data/remote/SupabaseClient.kt` - Cliente de Supabase
- ✅ `data/model/VerificationCode.kt` - Modelo para códigos de verificación

#### Archivos Actualizados:
- ✅ `build.gradle.kts` - Dependencias de Supabase
- ✅ `data/model/User.kt` - @Serializable con mapeo snake_case
- ✅ `data/model/Store.kt` - @Serializable con mapeo snake_case
- ✅ `data/model/Product.kt` - @Serializable con mapeo snake_case
- ✅ `data/model/Group.kt` - @Serializable con mapeo snake_case
- ✅ `data/model/Participant.kt` - @Serializable con mapeo snake_case
- ✅ `data/model/Category.kt` - @Serializable
- ✅ `data/repository/AuthRepository.kt` - Conectado a Supabase
- ✅ `data/repository/ProductRepository.kt` - Conectado a Supabase
- ✅ `data/repository/StoreRepository.kt` - Conectado a Supabase
- ✅ `data/repository/GroupRepository.kt` - Conectado a Supabase
- ✅ `MainActivity.kt` - Inicialización de Supabase

## 🔑 Configuración Requerida

### Paso 1: Obtener Credenciales de Supabase

1. Ve a tu proyecto en [Supabase Dashboard](https://app.supabase.com)
2. Ve a **Settings** → **API**
3. Copia:
   - **Project URL** (ej: `https://xxxxx.supabase.co`)
   - **anon/public key** (tu API key pública)

### Paso 2: Configurar en MainActivity.kt

Abre `app/src/main/java/com/nexusbiz/nexusbiz/MainActivity.kt` y reemplaza:

```kotlin
SupabaseManager.init(
    supabaseUrl = "TU_URL_SUPABASE", // Reemplazar con tu URL
    supabaseKey = "TU_API_KEY" // Reemplazar con tu API key
)
```

**Ejemplo:**
```kotlin
SupabaseManager.init(
    supabaseUrl = "https://abcdefghijklmnop.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
)
```

## 📋 Mapeo de Tablas

### Tablas de Supabase → Modelos Kotlin

| Tabla Supabase | Modelo Kotlin | Archivo |
|----------------|---------------|---------|
| `usuarios` | `User` | `data/model/User.kt` |
| `bodegas` | `Store` | `data/model/Store.kt` |
| `productos` | `Product` | `data/model/Product.kt` |
| `grupos` | `Group` | `data/model/Group.kt` |
| `participantes` | `Participant` | `data/model/Group.kt` |
| `categorias` | `Category` | `data/model/Category.kt` |
| `codigos_verificacion` | `VerificationCode` | `data/model/VerificationCode.kt` |

### Mapeo de Columnas (snake_case → camelCase)

Los modelos usan `@SerialName` para mapear automáticamente:
- `completed_groups` → `completedGroups`
- `user_type` → `userType`
- `created_at` → `createdAt`
- `image_url` → `imageUrl`
- etc.

## 🔄 Conversión de Timestamps

Los timestamps en Supabase son `TIMESTAMPTZ` (strings ISO 8601), pero en Kotlin usamos `Long` (millis).

Los repositorios manejan la conversión automáticamente:
- **BD → Kotlin**: `timestampToLong()` convierte string ISO a Long
- **Kotlin → BD**: `longToTimestamp()` convierte Long a string ISO

## ⚠️ Notas Importantes

### 1. Autenticación
Actualmente `AuthRepository` usa `password_hash` directamente. **Para producción**, se recomienda usar **Supabase Auth** en lugar de manejar passwords manualmente.

### 2. Triggers de Base de Datos
Los siguientes triggers están implementados en la BD (ver `nexusbiz_supabase_schema.sql`):
- ✅ Actualización automática de `current_size` cuando se agregan participantes
- ✅ Generación de QR cuando `current_size >= target_size`
- ✅ Cambio a PICKUP cuando se completa la meta
- ✅ Cambio a VALIDATED cuando todos los participantes retiran
- ✅ Expiración automática de grupos ACTIVE

### 3. Validación de Participantes
Para validar un retiro, usa:
```kotlin
groupRepository.validateParticipant(participantId)
```

Esto actualiza `is_validated = true` en la tabla `participantes`, y el trigger de la BD cambia el grupo a VALIDATED cuando todos están validados.

### 4. Códigos de Verificación
Los códigos SMS se almacenan en `codigos_verificacion` con expiración de 10 minutos. **TODO**: Implementar envío real de SMS (Twilio, Firebase, etc.)

## 🧪 Pruebas

Después de configurar las credenciales:

1. **Sincronizar Gradle** (Sync Now)
2. **Compilar el proyecto**
3. **Ejecutar la app**
4. **Verificar logs** para errores de conexión

## 🐛 Troubleshooting

### Error: "No such table"
- Verifica que ejecutaste el SQL schema en Supabase
- Verifica que los nombres de tablas coinciden exactamente

### Error: "Column not found"
- Verifica el mapeo `@SerialName` en los modelos
- Verifica que las columnas existen en la BD

### Error: "Connection refused"
- Verifica la URL de Supabase
- Verifica la API key
- Verifica que el proyecto de Supabase está activo

### Error: "Serialization"
- Verifica que todos los modelos tienen `@Serializable`
- Verifica que los enums tienen `@SerialName` para cada valor

## 📚 Recursos

- [Supabase Kotlin Documentation](https://github.com/supabase/supabase-kt)
- [PostgREST Query Builder](https://supabase.github.io/supabase-kt/docs/guides/postgrest-query-builder)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)

