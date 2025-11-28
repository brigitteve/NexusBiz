# 🔧 Solución para Dependencias de Supabase

## Problema
Las dependencias de Supabase no se encuentran en Maven Central.

## Solución 1: Verificar Versión Correcta

Si la versión 2.5.2 no funciona, prueba con estas versiones alternativas:

### Opción A: Versión 2.3.3 (Estable)
```kotlin
implementation("io.github.jan-tennert.supabase:gotrue-kt:2.3.3")
implementation("io.github.jan-tennert.supabase:postgrest-kt:2.3.3")
implementation("io.github.jan-tennert.supabase:storage-kt:2.3.3")
```

### Opción B: Usar BOM (Bill of Materials)
```kotlin
implementation(platform("io.github.jan-tennert.supabase:bom:2.3.3"))
implementation("io.github.jan-tennert.supabase:gotrue-kt")
implementation("io.github.jan-tennert.supabase:postgrest-kt")
implementation("io.github.jan-tennert.supabase:storage-kt")
```

## Solución 2: Verificar Repositorios

Asegúrate de que `settings.gradle.kts` tenga Maven Central:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()  // ← Debe estar presente
    }
}
```

## Solución 3: Sincronizar Gradle

1. En Android Studio: **File → Sync Project with Gradle Files**
2. O desde terminal: `./gradlew --refresh-dependencies`

## Solución 4: Verificar Versión en Maven Central

Visita: https://mvnrepository.com/artifact/io.github.jan-tennert.supabase/gotrue-kt

Busca la versión más reciente disponible y úsala en `build.gradle.kts`.

## Solución 5: Usar Versión Alternativa

Si ninguna versión funciona, prueba con:

```kotlin
// Versión alternativa
implementation("io.github.jan-tennert.supabase:gotrue-kt:2.2.0")
implementation("io.github.jan-tennert.supabase:postgrest-kt:2.2.0")
implementation("io.github.jan-tennert.supabase:storage-kt:2.2.0")
```

## Verificación

Después de cambiar las versiones:
1. **Sync Gradle** (File → Sync Project with Gradle Files)
2. **Clean Project** (Build → Clean Project)
3. **Rebuild Project** (Build → Rebuild Project)

## Nota

Si el problema persiste, verifica:
- ✅ Conexión a Internet
- ✅ Configuración de proxy (si aplica)
- ✅ Versión de Gradle (debe ser 8.0+)
- ✅ Versión de Android Gradle Plugin

