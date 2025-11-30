package com.nexusbiz.nexusbiz.data.repository

import android.util.Log
import com.nexusbiz.nexusbiz.data.model.Store
import com.nexusbiz.nexusbiz.data.model.StorePlan
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import com.nexusbiz.nexusbiz.data.remote.model.Store as RemoteStore
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.util.UUID

class StoreRepository {
    private val supabase: io.github.jan.supabase.SupabaseClient
        get() = SupabaseManager.client
    
    companion object {
        private const val RADIUS_KM = 5.0 // Radio de 5 km para bodegas cercanas
    }
    
    suspend fun getStoresWithStock(
        productId: String,
        userDistrict: String?,
        userLat: Double?,
        userLon: Double?,
        useNearbyStores: Boolean = false
    ): List<Store> {
        return try {
            // Obtener oferta para saber el distrito (en el nuevo esquema, los productos son ofertas)
            val offer = supabase.from("ofertas")
                .select {
                    filter { eq("id", productId) }
                }
                .decodeSingleOrNull<Map<String, Any>>()
            
            val offerDistrict = offer?.get("district") as? String
            val district = userDistrict?.takeIf { it.isNotBlank() } ?: offerDistrict
            
            val allStores = when {
                // Si se solicita bodegas cercanas y hay ubicación del usuario
                useNearbyStores && userLat != null && userLon != null -> {
                    getStoresByRadius(productId, userLat, userLon, RADIUS_KM)
                }
                // Si hay distrito seleccionado, filtrar por distrito
                district != null -> {
                    getStoresByDistrict(productId, district)
                }
                // Caso por defecto (sin distrito ni ubicación)
                else -> {
                    emptyList()
                }
            }
            
            // Calcular distancias y ordenar
            val hasUserLocation = userLat != null && userLon != null
            allStores.map { store ->
                val canCalculateDistance = hasUserLocation && store.latitude != null && store.longitude != null
                val distance = if (canCalculateDistance) {
                    calculateDistance(
                        userLat ?: 0.0,
                        userLon ?: 0.0,
                        store.latitude ?: 0.0,
                        store.longitude ?: 0.0
                    )
                } else {
                    null
                }
                store to distance
            }
                .sortedWith(
                    compareBy<Pair<Store, Double?>> { pair -> if (pair.second == null) 1 else 0 }
                        .thenBy { pair -> pair.second ?: Double.MAX_VALUE }
                )
                .map { (store, distance) -> store.copy(distance = distance ?: 0.0) }
        } catch (e: IllegalStateException) {
            Log.e("StoreRepository", "Supabase no inicializado", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error al obtener bodegas: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Obtiene bodegas con stock dentro de un radio específico desde la ubicación del usuario
     */
    private suspend fun getStoresByRadius(
        productId: String,
        userLat: Double,
        userLon: Double,
        radiusKm: Double
    ): List<Store> {
        return try {
            // Obtener todas las bodegas con stock y GPS activado
            val allStoresWithStock = supabase.from("bodegas")
                .select {
                    filter {
                        eq("has_stock", true)
                    }
                }
                .decodeList<RemoteStore>()
                .map { remoteStore -> mapRemoteStoreToStore(remoteStore) }
            
            // Filtrar solo las que tienen GPS activado y están dentro del radio
            allStoresWithStock.filter { store ->
                store.latitude != null && store.longitude != null && run {
                    val distance = calculateDistance(
                        userLat,
                        userLon,
                        store.latitude ?: 0.0,
                        store.longitude ?: 0.0
                    )
                    distance <= radiusKm
                }
            }
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error al obtener bodegas por radio: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Obtiene bodegas con stock del distrito especificado
     */
    private suspend fun getStoresByDistrict(
        productId: String,
        district: String
    ): List<Store> {
        return try {
            supabase.from("bodegas")
                .select {
                    filter {
                        eq("district", district)
                        eq("has_stock", true)
                    }
                }
                .decodeList<RemoteStore>()
                .map { remoteStore -> mapRemoteStoreToStore(remoteStore) }
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error al obtener bodegas por distrito: ${e.message}", e)
            emptyList()
        }
    }
    
    fun getStoresWithStockFlow(
        productId: String,
        userDistrict: String?,
        userLat: Double?,
        userLon: Double?,
        useNearbyStores: Boolean = false
    ): Flow<List<Store>> {
        return flowOf(runBlocking { getStoresWithStock(productId, userDistrict, userLat, userLon, useNearbyStores) })
    }
    
    suspend fun getStoreById(storeId: String): Store? {
        return try {
            supabase.from("bodegas")
                .select {
                    filter { eq("id", storeId) }
                }
                .decodeSingleOrNull<RemoteStore>()
                ?.let { mapRemoteStoreToStore(it) }
        } catch (e: IllegalStateException) {
            Log.e("StoreRepository", "Supabase no inicializado", e)
            null
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error al obtener bodega: ${e.message}", e)
            null
        }
    }
    
    suspend fun getStoresByOwner(ownerId: String): List<Store> {
        return try {
            supabase.from("bodegas")
                .select {
                    filter { eq("owner_id", ownerId) }
                }
                .decodeList<Store>()
        } catch (e: IllegalStateException) {
            Log.e("StoreRepository", "Supabase no inicializado", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error al obtener bodegas del propietario: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun createStore(store: Store): Result<Store> {
        return try {
            val newStore = store.copy(id = UUID.randomUUID().toString())
            supabase.from("bodegas").insert(newStore)
            Result.success(newStore)
        } catch (e: IllegalStateException) {
            Log.e("StoreRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error al crear bodega: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Fórmula de Haversine simplificada
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
    
    private fun mapRemoteStoreToStore(remoteStore: RemoteStore): Store {
        val plan = when (remoteStore.plan) {
            "PRO" -> StorePlan.PRO
            else -> StorePlan.FREE
        }
        return Store(
            id = remoteStore.id,
            name = remoteStore.name,
            address = remoteStore.address,
            district = remoteStore.district,
            latitude = remoteStore.latitude,
            longitude = remoteStore.longitude,
            phone = remoteStore.phone,
            imageUrl = remoteStore.imageUrl,
            hasStock = remoteStore.hasStock,
            ownerId = remoteStore.ownerId,
            rating = remoteStore.rating.takeIf { it > 0 },
            totalSales = remoteStore.totalSales,
            ruc = remoteStore.ruc,
            commercialName = remoteStore.commercialName,
            ownerAlias = remoteStore.ownerAlias,
            plan = plan,
            createdAt = remoteStore.createdAt,
            updatedAt = remoteStore.updatedAt
        )
    }
}
