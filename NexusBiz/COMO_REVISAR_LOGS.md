# 🔍 Cómo Revisar los Logs de Error en Android Studio

Cuando la app se cierra o crashea, los logs te mostrarán exactamente qué está pasando. Sigue estos pasos:

## 📱 Paso 1: Abrir Logcat en Android Studio

1. **Abre Android Studio**
2. En la parte inferior de la ventana, busca la pestaña **"Logcat"**
3. Si no la ves, ve a: `View` → `Tool Windows` → `Logcat` (o presiona `Alt + 6`)

## 🔎 Paso 2: Filtrar los Logs

### Opción A: Filtrar por Tag (Recomendado)
En el campo de búsqueda de Logcat, escribe:
```
tag:NexusBiz OR tag:AndroidRuntime OR tag:crash
```

### Opción B: Filtrar por Nivel de Error
1. En el dropdown de nivel, selecciona **"Error"** o **"Assert"**
2. Esto mostrará solo los errores críticos

### Opción C: Filtrar por Paquete
Escribe en el campo de búsqueda:
```
package:com.nexusbiz.nexusbiz
```

## 🚨 Paso 3: Buscar Errores Específicos

### Errores Comunes que Debes Buscar:

1. **Supabase no inicializado:**
   ```
   Supabase no ha sido inicializado
   ```

2. **Errores de conexión:**
   ```
   java.net.UnknownHostException
   java.net.SocketTimeoutException
   ```

3. **Errores de serialización:**
   ```
   kotlinx.serialization.SerializationException
   ```

4. **Errores de NullPointer:**
   ```
   java.lang.NullPointerException
   ```

5. **Errores de IllegalState:**
   ```
   java.lang.IllegalStateException
   ```

## 📋 Paso 4: Copiar el Stack Trace Completo

Cuando encuentres un error:

1. **Haz clic derecho** en la línea del error
2. Selecciona **"Copy"** o **"Copy Stack Trace"**
3. Pega el error completo aquí para que pueda ayudarte

## 🎯 Paso 5: Ver Logs en Tiempo Real

1. **Conecta tu dispositivo** o inicia el emulador
2. **Ejecuta la app** desde Android Studio
3. Los logs aparecerán en tiempo real en Logcat
4. Cuando la app crashee, el error aparecerá en rojo

## 🔧 Comandos Útiles de Logcat

### Limpiar logs:
- Botón **"Clear Logcat"** (icono de papelera) o presiona `Ctrl + L`

### Guardar logs:
- Botón **"Export Logs"** (icono de guardar) para guardar los logs en un archivo

### Buscar texto específico:
- Presiona `Ctrl + F` para buscar texto en los logs

## 📝 Tags de Logging en NexusBiz

La app usa estos tags para facilitar el debugging:

- `SupabaseManager` - Errores de inicialización de Supabase
- `AuthRepository` - Errores de autenticación
- `ProductRepository` - Errores al obtener productos/categorías
- `GroupRepository` - Errores relacionados con grupos
- `StoreRepository` - Errores relacionados con bodegas
- `MainActivity` - Errores en la actividad principal

## 🐛 Ejemplo de Error que Debes Buscar

```
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: com.nexusbiz.nexusbiz, PID: 12345
    java.lang.IllegalStateException: Supabase no ha sido inicializado
        at com.nexusbiz.nexusbiz.data.remote.SupabaseClient$client$2.invoke(SupabaseClient.kt:15)
        at com.nexusbiz.nexusbiz.data.repository.AuthRepository.getSupabase(AuthRepository.kt:18)
        ...
```

## 💡 Tips

1. **Siempre revisa el error más reciente** - Los errores aparecen en orden cronológico
2. **Busca "FATAL EXCEPTION"** - Estos son los errores que crashean la app
3. **Revisa el stack trace completo** - La línea que dice "Caused by:" es muy importante
4. **Filtra por tu paquete** - Usa `package:com.nexusbiz.nexusbiz` para ver solo tus errores

## 📞 Si Necesitas Ayuda

Cuando encuentres un error, copia:
1. El mensaje de error completo
2. El stack trace (al menos las primeras 10 líneas)
3. En qué momento ocurre (al abrir la app, al hacer login, etc.)

Y compártelo para que pueda ayudarte a solucionarlo.

