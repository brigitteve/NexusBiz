package com.nexusbiz.nexusbiz.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utilidades para manejo seguro de URIs de imágenes.
 * 
 * Garantiza que todas las URIs sean legibles por contentResolver
 * y maneja correctamente FileProvider para Android 7.0+
 */
object ImageUriHelper {
    private const val TAG = "ImageUriHelper"
    
    /**
     * Crea un archivo temporal para guardar una foto de la cámara.
     * El archivo se guarda en el directorio de caché de la app.
     * 
     * @return File temporal o null si hay error
     */
    fun createTempImageFile(context: Context, prefix: String = "image"): File? {
        return try {
            val cacheDir = context.cacheDir
            val imageDir = File(cacheDir, "images")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }
            val timestamp = System.currentTimeMillis()
            File(imageDir, "${prefix}_${timestamp}.jpg")
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear archivo temporal: ${e.message}", e)
            null
        }
    }
    
    /**
     * Obtiene una URI válida usando FileProvider para un archivo.
     * Esta URI puede ser compartida con otras apps de forma segura.
     * 
     * @param file Archivo para el cual crear la URI
     * @param context Contexto de la aplicación
     * @return URI válida con permisos o null si hay error
     */
    fun getUriForFile(context: Context, file: File): Uri? {
        return try {
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener URI con FileProvider: ${e.message}", e)
            null
        }
    }
    
    /**
     * Convierte cualquier URI a una URI legible por contentResolver.
     * 
     * - Si es content://: la retorna tal cual
     * - Si es file://: la copia a caché y retorna content://
     * - Si es otra: intenta leerla y copiarla
     * 
     * @param uri URI original
     * @param context Contexto de la aplicación
     * @return URI legible (content://) o null si no se puede convertir
     */
    suspend fun convertToReadableUri(context: Context, uri: Uri?): Uri? {
        if (uri == null) return null
        
        return try {
            when (uri.scheme) {
                "content" -> {
                    // Ya es una URI content://, verificar que sea legible
                    if (isUriReadable(context, uri)) {
                        uri
                    } else {
                        Log.w(TAG, "URI content:// no es legible, intentando copiar: $uri")
                        copyToCacheFile(context, uri)
                    }
                }
                "file" -> {
                    // Convertir file:// a content:// usando FileProvider
                    val file = File(uri.path ?: return null)
                    if (file.exists()) {
                        getUriForFile(context, file) ?: copyToCacheFile(context, uri)
                    } else {
                        Log.e(TAG, "Archivo no existe: ${file.absolutePath}")
                        null
                    }
                }
                else -> {
                    // Intentar leer y copiar
                    Log.w(TAG, "URI con esquema desconocido: ${uri.scheme}, intentando copiar")
                    copyToCacheFile(context, uri)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir URI a legible: ${e.message}", e)
            null
        }
    }
    
    /**
     * Copia una imagen desde cualquier URI a un archivo temporal en caché
     * y retorna una URI content:// legible.
     * 
     * @param uri URI de origen
     * @param context Contexto de la aplicación
     * @return URI content:// del archivo copiado o null si falla
     */
    suspend fun copyToCacheFile(context: Context, uri: Uri): Uri? {
        return try {
            val tempFile = createTempImageFile(context, "copied_image")
            if (tempFile == null) {
                Log.e(TAG, "No se pudo crear archivo temporal")
                return null
            }
            
            // Leer desde la URI original
            val inputStream: InputStream? = when (uri.scheme) {
                "content" -> context.contentResolver.openInputStream(uri)
                "file" -> FileInputStream(File(uri.path ?: return null))
                else -> {
                    Log.e(TAG, "No se puede leer URI con esquema: ${uri.scheme}")
                    return null
                }
            }
            
            if (inputStream == null) {
                Log.e(TAG, "No se pudo abrir InputStream para URI: $uri")
                return null
            }
            
            // Copiar a archivo temporal
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Retornar URI con FileProvider
            getUriForFile(context, tempFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error al copiar archivo a caché: ${e.message}", e)
            null
        }
    }
    
    /**
     * Verifica si una URI es legible por contentResolver.
     * 
     * @param uri URI a verificar
     * @param context Contexto de la aplicación
     * @return true si la URI es legible, false en caso contrario
     */
    fun isUriReadable(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "URI no es legible: $uri, error: ${e.message}")
            false
        }
    }
    
    /**
     * Valida si una URI es una URL remota (http/https).
     * 
     * @param uri URI o string a validar
     * @return true si es una URL remota
     */
    fun isRemoteUrl(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        return uri.startsWith("http://") || uri.startsWith("https://")
    }
    
    /**
     * Valida si una URI es local (content:// o file://).
     * 
     * @param uri URI a validar
     * @return true si es una URI local
     */
    fun isLocalUri(uri: Uri?): Boolean {
        if (uri == null) return false
        return uri.scheme == "content" || uri.scheme == "file"
    }
    
    /**
     * Obtiene el MIME type de una URI.
     * 
     * @param uri URI de la imagen
     * @param context Contexto de la aplicación
     * @return MIME type o "image/jpeg" por defecto
     */
    fun getMimeType(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.getType(uri) ?: "image/jpeg"
        } catch (e: Exception) {
            Log.w(TAG, "Error al obtener MIME type: ${e.message}")
            "image/jpeg"
        }
    }
    
    /**
     * Obtiene la extensión del archivo basado en el MIME type.
     * 
     * @param mimeType MIME type de la imagen
     * @return Extensión sin punto (ej: "jpg", "png")
     */
    fun getExtensionFromMimeType(mimeType: String): String {
        return when {
            mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> "jpg"
            mimeType.contains("png", ignoreCase = true) -> "png"
            mimeType.contains("webp", ignoreCase = true) -> "webp"
            mimeType.contains("gif", ignoreCase = true) -> "gif"
            else -> "jpg"
        }
    }
}

