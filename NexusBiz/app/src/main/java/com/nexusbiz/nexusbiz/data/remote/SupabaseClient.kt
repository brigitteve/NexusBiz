package com.nexusbiz.nexusbiz.data.remote

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseManager {
    @Volatile
    private var _client: SupabaseClient? = null
    
    @Volatile
    private var _supabaseUrl: String? = null
    
    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException("Supabase no ha sido inicializado. Llama a SupabaseManager.init() primero.")
    
    val supabaseUrl: String
        get() = _supabaseUrl ?: throw IllegalStateException("Supabase no ha sido inicializado. Llama a SupabaseManager.init() primero.")

    fun init(supabaseUrl: String, supabaseKey: String) {
        _supabaseUrl = supabaseUrl
        try {
            _client = createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey
            ) {
                // Permite ignorar campos inesperados que lleguen desde la API (por ejemplo password_hash)
                defaultSerializer = KotlinXSerializer(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                )
                install(Auth)
                install(Postgrest)
                install(Storage)
                install(Realtime) // Plugin de Realtime para escuchar cambios en tiempo real
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error al inicializar Supabase: ${e.message}", e)
            throw e
        }
    }
}

