package com.nexusbiz.nexusbiz.data.repository

import android.util.Log
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Repositorio para manejar notificaciones push.
 * 
 * IMPORTANTE: Este repositorio maneja el guardado de tokens FCM.
 * El envío real de notificaciones debe hacerse desde el backend (Supabase Edge Functions o Firebase Cloud Functions).
 * 
 * Para implementar el envío de notificaciones, necesitas:
 * 1. Una función en Supabase Edge Functions o Firebase Cloud Functions
 * 2. Triggers en Supabase que llamen a estas funciones cuando ocurran eventos
 * 3. O llamar manualmente a estas funciones desde el código cuando ocurran los eventos
 */
class NotificationRepository {
    private val supabase = SupabaseManager.client

    /**
     * Guarda o actualiza el token FCM de un usuario.
     * 
     * IMPORTANTE: Necesitas agregar una columna "fcm_token" a la tabla "usuarios" en Supabase:
     * ALTER TABLE usuarios ADD COLUMN fcm_token TEXT;
     */
    suspend fun saveFCMToken(userId: String, token: String): Result<Unit> {
        return try {
            supabase.from("usuarios")
                .update(mapOf("fcm_token" to token)) {
                    filter { eq("id", userId) }
                }
            Log.d("NotificationRepository", "Token FCM guardado para usuario: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error al guardar token FCM: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene el token FCM de un usuario.
     */
    suspend fun getFCMToken(userId: String): String? {
        return try {
            val user = supabase.from("usuarios")
                .select(columns = Columns.ALL) {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<Map<String, Any>>()
            
            @Suppress("UNCHECKED_CAST")
            user?.get("fcm_token") as? String
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error al obtener token FCM: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene todos los tokens FCM de clientes en un distrito específico.
     * Útil para notificar a clientes sobre nuevas ofertas.
     */
    suspend fun getClientTokensByDistrict(district: String): List<String> {
        return try {
            val users = supabase.from("usuarios")
                .select {
                    filter {
                        eq("district", district)
                        eq("user_type", "CONSUMER")
                    }
                }
                .decodeList<Map<String, Any>>()
            
            // Filtrar usuarios que tienen token FCM (no nulo)
            @Suppress("UNCHECKED_CAST")
            users.mapNotNull { user ->
                val token = user["fcm_token"] as? String
                token?.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error al obtener tokens por distrito: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene el token FCM de un bodeguero por su store_id.
     */
    suspend fun getStoreOwnerToken(storeId: String): String? {
        return try {
            // Primero obtener el owner_id de la bodega
            val store = supabase.from("bodegas")
                .select {
                    filter { eq("id", storeId) }
                }
                .decodeSingleOrNull<Map<String, Any>>()
            
            val ownerId = store?.get("owner_id") as? String ?: return null
            
            // Luego obtener el token del owner
            getFCMToken(ownerId)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error al obtener token del bodeguero: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene tokens FCM de todos los participantes de una oferta/grupo.
     */
    suspend fun getParticipantTokens(offerId: String): List<String> {
        return try {
            // Obtener todas las reservas de la oferta
            val reservations = supabase.from("reservas_completas")
                .select {
                    filter { eq("offer_id", offerId) }
                }
                .decodeList<Map<String, Any>>()
            
            val userIds = reservations.mapNotNull { it["user_id"] as? String }
            
            // Obtener tokens de esos usuarios
            // Como Supabase no tiene operador `in` directo, obtenemos los tokens uno por uno
            // o filtramos después
            if (userIds.isEmpty()) return emptyList()
            
            val tokens = mutableListOf<String>()
            for (userId in userIds) {
                val token = getFCMToken(userId)
                if (token != null && token.isNotBlank()) {
                    tokens.add(token)
                }
            }
            
            tokens
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error al obtener tokens de participantes: ${e.message}", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "NotificationRepository"
    }
}
