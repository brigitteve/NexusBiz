package com.nexusbiz.nexusbiz.data.repository

import android.util.Log
import com.nexusbiz.nexusbiz.data.model.Store
import com.nexusbiz.nexusbiz.data.model.StorePlan
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import com.nexusbiz.nexusbiz.data.remote.model.Store as RemoteStore
import com.nexusbiz.nexusbiz.data.remote.model.Offer as RemoteOffer
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
        useNearbyStores: Boolean = false,
        userId: String? = null
    ): List<Store> {
        return try {
            // Obtener oferta para saber el distrito (en el nuevo esquema, los productos son ofertas)
            val offer = supabase.from("ofertas")
                .select {
                    filter { eq("id", productId) }
                }
                .decodeSingleOrNull<RemoteOffer>()
            
            val offerDistrict = offer?.district
            val district = userDistrict?.takeIf { it.isNotBlank() } ?: offerDistrict
            
            // Si se solicita bodegas cercanas pero no tenemos coordenadas del dispositivo,
            // intentar obtenerlas desde la BD del usuario
            var finalUserLat = userLat
            var finalUserLon = userLon
            
            if (useNearbyStores && (finalUserLat == null || finalUserLon == null) && userId != null) {
                val userCoords = getUserCoordinates(userId)
                if (userCoords != null) {
                    finalUserLat = userCoords.first
                    finalUserLon = userCoords.second
                    Log.d("StoreRepository", "Coordenadas obtenidas desde BD del usuario: lat=$finalUserLat, lon=$finalUserLon")
                }
            }
            
            val allStores = when {
                // Si se solicita bodegas cercanas y hay ubicación del usuario (del dispositivo o BD)
                useNearbyStores && finalUserLat != null && finalUserLon != null -> {
                    // Pasar el distrito también para filtrar ofertas por distrito
                    getStoresByRadius(productId, finalUserLat, finalUserLon, RADIUS_KM, district)
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
            
            // Calcular distancias y ordenar (solo si hay ubicación del usuario y la bodega tiene coordenadas)
            val hasUserLocation = userLat != null && userLon != null
            val storesWithDistance = allStores.map { store ->
                val distance = if (hasUserLocation && store.latitude != null && store.longitude != null) {
                    calculateDistance(
                        userLat ?: 0.0,
                        userLon ?: 0.0,
                        store.latitude ?: 0.0,
                        store.longitude ?: 0.0
                    )
                } else {
                    null
                }
                store.copy(distance = distance ?: 0.0) to distance
            }
                .sortedWith(
                    // Ordenar: primero las que tienen distancia calculada, luego las que no
                    // Dentro de cada grupo, ordenar por distancia (más cercana primero)
                    compareBy<Pair<Store, Double?>> { pair -> 
                        if (pair.second == null) 1 else 0 
                    }
                    .thenBy { pair -> 
                        pair.second ?: Double.MAX_VALUE 
                    }
                )
                .map { (store, _) -> store }
            
            storesWithDistance
        } catch (e: IllegalStateException) {
            Log.e("StoreRepository", "Supabase no inicializado", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error al obtener bodegas: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Obtiene bodegas con stock dentro de un radio específico desde la ubicación del usuario.
     * Usa la fórmula de Haversine para calcular la distancia y filtrar bodegas dentro del radio.
     * 
     * @param productId ID del producto (oferta) para filtrar bodegas relevantes
     * @param userLat Latitud del usuario
     * @param userLon Longitud del usuario
     * @param radiusKm Radio en kilómetros (5 km por defecto)
     * @return Lista de bodegas dentro del radio, ordenadas por distancia
     */
    private suspend fun getStoresByRadius(
        productId: String,
        userLat: Double,
        userLon: Double,
        radiusKm: Double,
        district: String? = null
    ): List<Store> {
        return try {
            // Primero obtener la oferta original para saber el nombre del producto y el distrito
            val originalOffer = supabase.from("ofertas")
                .select {
                    filter { eq("id", productId) }
                }
                .decodeSingleOrNull<RemoteOffer>()
            
            if (originalOffer == null) {
                Log.e("StoreRepository", "No se pudo obtener la oferta $productId")
                return emptyList()
            }
            
            val productName = originalOffer.productName
            val productKey = originalOffer.productKey // Usar product_key para agrupar productos similares
            val offerDistrict = district ?: originalOffer.district
            
            if (productName.isBlank() || productKey.isBlank()) {
                Log.e("StoreRepository", "No se pudo obtener el nombre o clave del producto de la oferta $productId")
                return emptyList()
            }
            
            Log.d("StoreRepository", "Buscando bodegas con producto: '$productName' (product_key: '$productKey', distrito sugerido: '$offerDistrict')")
            
            // Primero buscar ofertas con el mismo product_key (productos exactamente iguales normalizados)
            val offersWithSameKey = supabase.from("ofertas")
                .select {
                    filter {
                        eq("status", "ACTIVE")
                        eq("product_key", productKey) // Búsqueda exacta por product_key
                        // NO filtrar por distrito cuando se busca por radio - queremos todas las bodegas cercanas
                    }
                }
                .decodeList<RemoteOffer>()
            
            Log.d("StoreRepository", "Ofertas encontradas con product_key exacto '$productKey': ${offersWithSameKey.size}")
            
            // Si no hay suficientes resultados o queremos encontrar productos similares,
            // buscar por product_key parcial usando palabras clave
            val finalOffersWithKeywords = if (offersWithSameKey.size < 2) {
                Log.d("StoreRepository", "Buscando productos similares por palabras clave en product_key...")
                
                // Extraer palabras clave del product_key para búsqueda más flexible
                val keywords = productKey.split("\\s+".toRegex())
                    .filter { it.length > 2 } // Solo palabras de más de 2 caracteres
                    .take(3) // Tomar las 3 primeras palabras clave más importantes
                
                Log.d("StoreRepository", "Palabras clave extraídas del product_key: $keywords")
                
                // Buscar ofertas que contengan alguna de las palabras clave en el product_key
                val offersByKeywords = keywords.flatMap { keyword ->
                    try {
                        supabase.from("ofertas")
                            .select {
                                filter {
                                    eq("status", "ACTIVE")
                                    ilike("product_key", "%$keyword%") // Búsqueda parcial en product_key
                                    // NO filtrar por distrito cuando se busca por radio
                                }
                            }
                            .decodeList<RemoteOffer>()
                    } catch (e: Exception) {
                        Log.e("StoreRepository", "Error al buscar por palabra clave '$keyword': ${e.message}")
                        emptyList()
                    }
                }
                .distinctBy { it.id } // Eliminar ofertas duplicadas
                
                Log.d("StoreRepository", "Ofertas encontradas con palabras clave en product_key: ${offersByKeywords.size}")
                
                // Combinar resultados: primero los exactos, luego los similares
                (offersWithSameKey + offersByKeywords).distinctBy { it.id }
            } else {
                offersWithSameKey
            }
            
            Log.d("StoreRepository", "Ofertas finales encontradas: ${finalOffersWithKeywords.size}")
            
            // Obtener los IDs de las bodegas que tienen este producto
            val storeIds = finalOffersWithKeywords.mapNotNull { it.storeId }.distinct()
            
            if (storeIds.isEmpty()) {
                Log.d("StoreRepository", "No se encontraron bodegas con el producto '$productName'")
                return emptyList()
            }
            
            Log.d("StoreRepository", "Bodegas únicas con el producto: ${storeIds.size}, IDs: $storeIds")
            
            // Obtener TODAS las bodegas que publicaron esta oferta (sin filtrar por has_stock)
            // Si una bodega publicó la oferta, significa que tiene el producto disponible
            // Usar distinctBy para asegurar que cada bodega aparezca solo una vez
            val allStoresWithStock = storeIds.flatMap { storeId ->
                try {
                    supabase.from("bodegas")
                        .select {
                            filter {
                                eq("id", storeId)
                            }
                        }
                        .decodeList<RemoteStore>()
                } catch (e: Exception) {
                    Log.e("StoreRepository", "Error al obtener bodega $storeId: ${e.message}")
                    emptyList()
                }
            }
            .map { remoteStore -> mapRemoteStoreToStore(remoteStore) }
            .distinctBy { it.id } // Asegurar que cada bodega aparezca solo una vez
            
            Log.d("StoreRepository", "Bodegas únicas encontradas que publicaron la oferta: ${allStoresWithStock.size}")
            
            // Filtrar solo las que tienen coordenadas (necesarias para calcular distancia)
            val storesWithCoordinates = allStoresWithStock.filter { it.latitude != null && it.longitude != null }
            
            Log.d("StoreRepository", "Bodegas con coordenadas: ${storesWithCoordinates.size}")
            Log.d("StoreRepository", "Bodegas sin coordenadas: ${allStoresWithStock.size - storesWithCoordinates.size}")
            
            // Si no hay bodegas con coordenadas, retornar todas las bodegas (sin distancia)
            if (storesWithCoordinates.isEmpty() && allStoresWithStock.isNotEmpty()) {
                Log.d("StoreRepository", "No hay bodegas con coordenadas, retornando todas las bodegas sin distancia")
                return allStoresWithStock.map { it.copy(distance = 0.0) }
            }
            
            val allStoresWithStockFinal = storesWithCoordinates
            
            // Calcular distancia y filtrar bodegas dentro del radio
            // También ordenamos por distancia para mostrar las más cercanas primero
            val storesInRadius = allStoresWithStockFinal
                .mapNotNull { store ->
                    if (store.latitude != null && store.longitude != null) {
                        val distance = calculateDistance(
                            userLat,
                            userLon,
                            store.latitude ?: 0.0,
                            store.longitude ?: 0.0
                        )
                        
                        // Solo incluir bodegas dentro del radio
                        if (distance <= radiusKm) {
                            store.copy(distance = distance) to distance
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                .sortedBy { (_, distance) -> distance } // Ordenar por distancia (más cercana primero)
                .map { (store, _) -> store }
            
            Log.d("StoreRepository", "Bodegas dentro del radio de ${radiusKm}km: ${storesInRadius.size}")
            storesInRadius
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error al obtener bodegas por radio: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Obtiene bodegas con stock del distrito especificado que tengan el mismo producto
     */
    private suspend fun getStoresByDistrict(
        productId: String,
        district: String
    ): List<Store> {
        return try {
            // Primero obtener el nombre y clave del producto de la oferta
            val offer = supabase.from("ofertas")
                .select {
                    filter { eq("id", productId) }
                }
                .decodeSingleOrNull<RemoteOffer>()
            
            val productName = offer?.productName
            val productKey = offer?.productKey // Usar product_key para agrupar productos similares
            
            if (productName.isNullOrBlank() || productKey.isNullOrBlank()) {
                Log.e("StoreRepository", "No se pudo obtener el nombre o clave del producto de la oferta $productId")
                return emptyList()
            }
            
            Log.d("StoreRepository", "Buscando bodegas en distrito '$district' con producto: '$productName' (product_key: '$productKey')")
            
            // Primero buscar ofertas con el mismo product_key (productos exactamente iguales normalizados)
            val offersWithSameKey = supabase.from("ofertas")
                .select {
                    filter {
                        eq("status", "ACTIVE")
                        eq("product_key", productKey) // Búsqueda exacta por product_key
                        eq("district", district)
                    }
                }
                .decodeList<RemoteOffer>()
            
            Log.d("StoreRepository", "Ofertas encontradas con product_key exacto '$productKey' en distrito '$district': ${offersWithSameKey.size}")
            
            // Si no hay suficientes resultados, buscar por product_key parcial usando palabras clave
            val finalOffersWithKeywords = if (offersWithSameKey.size < 2) {
                Log.d("StoreRepository", "Buscando productos similares por palabras clave en product_key...")
                
                // Extraer palabras clave del product_key para búsqueda más flexible
                val keywords = productKey.split("\\s+".toRegex())
                    .filter { it.length > 2 } // Solo palabras de más de 2 caracteres
                    .take(3) // Tomar las 3 primeras palabras clave más importantes
                
                Log.d("StoreRepository", "Palabras clave extraídas del product_key: $keywords")
                
                // Buscar ofertas que contengan alguna de las palabras clave en el product_key
                val offersByKeywords = keywords.flatMap { keyword ->
                    try {
                        supabase.from("ofertas")
                            .select {
                                filter {
                                    eq("status", "ACTIVE")
                                    ilike("product_key", "%$keyword%") // Búsqueda parcial en product_key
                                    eq("district", district)
                                }
                            }
                            .decodeList<RemoteOffer>()
                    } catch (e: Exception) {
                        Log.e("StoreRepository", "Error al buscar por palabra clave '$keyword': ${e.message}")
                        emptyList()
                    }
                }
                .distinctBy { it.id } // Eliminar ofertas duplicadas
                
                Log.d("StoreRepository", "Ofertas encontradas con palabras clave en product_key: ${offersByKeywords.size}")
                
                // Combinar resultados: primero los exactos, luego los similares
                (offersWithSameKey + offersByKeywords).distinctBy { it.id }
            } else {
                offersWithSameKey
            }
            
            Log.d("StoreRepository", "Ofertas finales encontradas en distrito: ${finalOffersWithKeywords.size}")
            
            // Obtener los IDs de las bodegas que tienen este producto
            val storeIds = finalOffersWithKeywords.mapNotNull { it.storeId }.distinct()
            
            if (storeIds.isEmpty()) {
                Log.d("StoreRepository", "No se encontraron bodegas con el producto '$productName' en el distrito '$district'")
                return emptyList()
            }
            
            Log.d("StoreRepository", "Bodegas únicas con el producto en distrito: ${storeIds.size}, IDs: $storeIds")
            
            // Obtener TODAS las bodegas que publicaron esta oferta en el distrito
            // Si una bodega publicó la oferta, significa que tiene el producto disponible
            // Usar distinctBy para asegurar que cada bodega aparezca solo una vez
            val stores = storeIds.flatMap { storeId ->
                try {
                    supabase.from("bodegas")
                        .select {
                            filter {
                                eq("id", storeId)
                                eq("district", district)
                            }
                        }
                        .decodeList<RemoteStore>()
                } catch (e: Exception) {
                    Log.e("StoreRepository", "Error al obtener bodega $storeId: ${e.message}")
                    emptyList()
                }
            }
            .map { remoteStore -> mapRemoteStoreToStore(remoteStore) }
            .distinctBy { it.id } // Asegurar que cada bodega aparezca solo una vez
            
            Log.d("StoreRepository", "Bodegas encontradas: ${stores.size}")
            stores
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
    
    /**
     * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine.
     * Esta es una implementación más precisa que considera el radio de la Tierra en metros.
     * 
     * @param lat1 Latitud del primer punto (usuario)
     * @param lon1 Longitud del primer punto (usuario)
     * @param lat2 Latitud del segundo punto (bodega)
     * @param lon2 Longitud del segundo punto (bodega)
     * @return Distancia en kilómetros
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Radio de la Tierra en metros (6371000 metros = 6371 km)
        val earthRadiusMeters = 6371000.0
        
        // Convertir grados a radianes
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLatRad = Math.toRadians(lat2 - lat1)
        val deltaLonRad = Math.toRadians(lon2 - lon1)
        
        // Fórmula de Haversine completa
        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        // Distancia en metros, convertir a kilómetros
        val distanceMeters = earthRadiusMeters * c
        return distanceMeters / 1000.0
    }
    
    /**
     * Obtiene las coordenadas del usuario desde la base de datos.
     * Útil cuando no se tienen las coordenadas en memoria pero están guardadas en la BD.
     */
    suspend fun getUserCoordinates(userId: String): Pair<Double, Double>? {
        return try {
            val userData = supabase.from("usuarios")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<Map<String, Any>>()
            
            val latitude = (userData?.get("latitude") as? Number)?.toDouble()
            val longitude = (userData?.get("longitude") as? Number)?.toDouble()
            
            if (latitude != null && longitude != null) {
                Pair(latitude, longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StoreRepository", "Error al obtener coordenadas del usuario: ${e.message}", e)
            null
        }
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
