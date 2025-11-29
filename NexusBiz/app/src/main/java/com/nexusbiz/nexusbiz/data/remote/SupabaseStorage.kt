package com.nexusbiz.nexusbiz.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nexusbiz.nexusbiz.util.ImageUriHelper
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utilidad para subir imágenes a Supabase Storage y obtener la URL pública
 * para almacenarla en la base de datos relacional.
 * 
 * REFACTORIZADO: Ahora garantiza que todas las URIs sean legibles antes de subir.
 */
object SupabaseStorage {
    private const val TAG = "SupabaseStorage"

    /**
     * Sube una imagen al primer bucket disponible y devuelve la URL pública.
     * 
     * MEJORAS:
     * - Convierte automáticamente URIs file:// a content:// usando FileProvider
     * - Valida que la URI sea legible antes de intentar leer bytes
     * - Mejora la detección de MIME type
     * - Maneja errores de forma controlada
     *
     * @param context Contexto para leer el archivo desde la URI
     * @param imageUri URI local de la imagen (content:// o file://)
     * @param pathBuilder Función que construye la ruta completa (carpeta/archivo.ext)
     * @param bucketPriority Lista de buckets a probar en orden de prioridad
     * @return URL pública de la imagen subida o null si hay error
     */
    suspend fun uploadPublicImage(
        context: Context,
        imageUri: Uri,
        pathBuilder: (extension: String) -> String,
        bucketPriority: List<String> = listOf("product-images", "public")
    ): String? = withContext(Dispatchers.IO) {
        try {
            // PASO 1: Convertir URI a una URI legible (content://)
            val readableUri = ImageUriHelper.convertToReadableUri(context, imageUri)
            if (readableUri == null) {
                Log.e(TAG, "No se pudo convertir URI a legible: $imageUri")
                return@withContext null
            }
            
            // PASO 2: Verificar que la URI sea realmente legible
            if (!ImageUriHelper.isUriReadable(context, readableUri)) {
                Log.e(TAG, "URI convertida no es legible: $readableUri")
                return@withContext null
            }
            
            // PASO 3: Obtener MIME type mejorado
            val mimeType = ImageUriHelper.getMimeType(context, readableUri)
            val extension = ImageUriHelper.getExtensionFromMimeType(mimeType)
            Log.d(TAG, "MIME type detectado: $mimeType, extensión: $extension")

            val filePath = pathBuilder(extension)

            // PASO 4: Leer bytes de la URI legible
            val bytes = try {
                context.contentResolver.openInputStream(readableUri)?.use { it.readBytes() }
            } catch (e: Exception) {
                Log.e(TAG, "Error al leer bytes desde URI: $readableUri", e)
                null
            }
            
            if (bytes == null || bytes.isEmpty()) {
                Log.e(TAG, "No se pudieron leer bytes desde la URI: $readableUri")
                return@withContext null
            }
            
            Log.d(TAG, "Bytes leídos exitosamente: ${bytes.size} bytes")

            // PASO 5: Intentar subir a cada bucket en orden de prioridad
            // Si el bucket no existe, intentar crearlo automáticamente
            var bucketUsed: String? = null
            var lastError: Exception? = null
            
            for (bucket in bucketPriority.distinct()) {
                try {
                    Log.d(TAG, "Intentando subir a bucket: $bucket, ruta: $filePath, tamaño: ${bytes.size} bytes")
                    
                    // Intentar subir directamente con diferentes configuraciones
                    try {
                        // Primera opción: upload con upsert
                        SupabaseManager.client.storage.from(bucket).upload(filePath, bytes, upsert = true)
                        bucketUsed = bucket
                        Log.d(TAG, "Imagen subida exitosamente a bucket: $bucket")
                        break
                    } catch (uploadError: Exception) {
                        // Si falla con upsert, intentar sin upsert
                        Log.w(TAG, "Error con upsert=true, intentando sin upsert: ${uploadError.message}")
                        try {
                            SupabaseManager.client.storage.from(bucket).upload(filePath, bytes, upsert = false)
                            bucketUsed = bucket
                            Log.d(TAG, "Imagen subida exitosamente a bucket (sin upsert): $bucket")
                            break
                        } catch (uploadError2: Exception) {
                            // Si también falla, lanzar el error original
                            throw uploadError
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                    val errorMessage = e.message ?: "Sin mensaje"
                    val errorClass = e.javaClass.simpleName
                    
                    // Log detallado del error
                    Log.e(TAG, "Error detallado al subir a bucket '$bucket':", e)
                    Log.e(TAG, "Tipo de error: $errorClass")
                    Log.e(TAG, "Mensaje: $errorMessage")
                    Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                    
                    val isBucketNotFound = errorMessage.contains("Bucket not found", ignoreCase = true) ||
                                          errorMessage.contains("not found", ignoreCase = true) ||
                                          errorMessage.contains("404", ignoreCase = false)
                    
                    val isPermissionError = errorMessage.contains("permission", ignoreCase = true) ||
                                           errorMessage.contains("unauthorized", ignoreCase = true) ||
                                           errorMessage.contains("403", ignoreCase = false) ||
                                           errorMessage.contains("forbidden", ignoreCase = true)
                    
                    if (isBucketNotFound) {
                        Log.w(TAG, "Bucket '$bucket' no existe o no es accesible. ${e.message}")
                    } else if (isPermissionError) {
                        Log.w(TAG, "Error de permisos al acceder al bucket '$bucket'. Verifica las políticas de Storage. ${e.message}")
                    } else {
                        Log.w(TAG, "Error al subir a bucket '$bucket': ${e.message}")
                    }
                    // Continuar con el siguiente bucket
                }
            }

            if (bucketUsed == null) {
                val errorMsg = lastError?.message ?: "Desconocido"
                val isBucketNotFound = errorMsg.contains("Bucket not found", ignoreCase = true) ||
                                     errorMsg.contains("not found", ignoreCase = true) ||
                                     errorMsg.contains("404", ignoreCase = false)
                
                val isPermissionError = errorMsg.contains("permission", ignoreCase = true) ||
                                       errorMsg.contains("unauthorized", ignoreCase = true) ||
                                       errorMsg.contains("403", ignoreCase = false) ||
                                       errorMsg.contains("forbidden", ignoreCase = true)
                
                val errorMessage = when {
                    isBucketNotFound -> {
                        buildString {
                            append("Los buckets de Storage no existen o no son accesibles.\n\n")
                            append("SOLUCIÓN:\n")
                            append("1. Ve a https://app.supabase.com\n")
                            append("2. Selecciona tu proyecto\n")
                            append("3. Ve a Storage en el menú lateral\n")
                            append("4. Verifica que el bucket 'product-images' exista y sea público\n")
                            append("5. Si no existe, créalo con:\n")
                            append("   - Nombre: product-images\n")
                            append("   - Público: Sí\n")
                            append("\nError: $errorMsg")
                        }
                    }
                    isPermissionError -> {
                        buildString {
                            append("Error de permisos al subir imagen.\n\n")
                            append("SOLUCIÓN:\n")
                            append("1. Ve a Storage → Policies en Supabase Dashboard\n")
                            append("2. Verifica que exista una política que permita INSERT para el bucket 'product-images'\n")
                            append("3. La política debe permitir a usuarios autenticados o anónimos subir archivos\n")
                            append("\nError: $errorMsg")
                        }
                    }
                    else -> {
                        "No se pudo subir la imagen. Error: $errorMsg\n\nVerifica los logs para más detalles."
                    }
                }
                
                Log.e(TAG, errorMessage)
                Log.e(TAG, "Último error completo: ${lastError?.stackTraceToString()}")
                return@withContext null
            }

            // PASO 6: Construir y retornar URL pública
            val publicUrl = "${SupabaseManager.supabaseUrl}/storage/v1/object/public/$bucketUsed/$filePath"
            Log.d(TAG, "Imagen subida correctamente. URL pública: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al subir imagen: ${e.message}", e)
            null
        }
    }
}
