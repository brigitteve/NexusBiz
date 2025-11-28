package com.nexusbiz.nexusbiz.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utilidad para subir imágenes a Supabase Storage y obtener la URL pública
 * para almacenarla en la base de datos relacional.
 */
object SupabaseStorage {

    /**
     * Sube una imagen al primer bucket disponible y devuelve la URL pública.
     *
     * @param context Contexto para leer el archivo desde la URI
     * @param imageUri URI local de la imagen (content:// o file://)
     * @param pathBuilder Función que construye la ruta completa (carpeta/archivo.ext)
     * @param bucketPriority Lista de buckets a probar en orden de prioridad
     */
    suspend fun uploadPublicImage(
        context: Context,
        imageUri: Uri,
        pathBuilder: (extension: String) -> String,
        bucketPriority: List<String> = listOf("product-images", "public")
    ): String? = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> "jpg"
                mimeType.contains("png", ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }

            val filePath = pathBuilder(extension)

            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            if (bytes == null) {
                Log.e("SupabaseStorage", "No se pudo abrir el archivo desde la URI: $imageUri")
                return@withContext null
            }

            var bucketUsed: String? = null
            for (bucket in bucketPriority.distinct()) {
                try {
                    SupabaseManager.client.storage.from(bucket).upload(filePath, bytes, upsert = true)
                    bucketUsed = bucket
                    break
                } catch (e: Exception) {
                    Log.w("SupabaseStorage", "Error al subir a bucket '$bucket': ${e.message}")
                }
            }

            if (bucketUsed == null) {
                Log.e("SupabaseStorage", "No se pudo subir la imagen a ningún bucket configurado")
                return@withContext null
            }

            val publicUrl = "${SupabaseManager.supabaseUrl}/storage/v1/object/public/$bucketUsed/$filePath"
            Log.d("SupabaseStorage", "Imagen subida correctamente a '$bucketUsed': $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e("SupabaseStorage", "Error al subir imagen: ${e.message}", e)
            null
        }
    }
}
