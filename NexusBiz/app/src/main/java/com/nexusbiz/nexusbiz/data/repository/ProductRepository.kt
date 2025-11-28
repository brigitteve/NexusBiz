package com.nexusbiz.nexusbiz.data.repository

import android.util.Log
import com.nexusbiz.nexusbiz.data.model.Product
import com.nexusbiz.nexusbiz.data.model.Category
import com.nexusbiz.nexusbiz.data.model.StorePlan
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import com.nexusbiz.nexusbiz.data.remote.model.Product as RemoteProduct
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.util.UUID

@Serializable
private data class ProductInsert(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("category_id") val categoryId: String = "",
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("normal_price") val normalPrice: Double,
    @SerialName("group_price") val groupPrice: Double,
    @SerialName("min_group_size") val minGroupSize: Int,
    @SerialName("max_group_size") val maxGroupSize: Int,
    @SerialName("store_id") val storeId: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("district") val district: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("duration_hours") val durationHours: Int = 24,
    @SerialName("store_plan") val storePlan: String = "FREE"
)

class ProductRepository {
    private val supabase: io.github.jan.supabase.SupabaseClient
        get() = SupabaseManager.client
    private val mutex = Mutex()
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()
    
    suspend fun getProducts(district: String, category: String? = null, searchQuery: String? = null): List<Product> {
        return try {
            fetchProducts(district, category, searchQuery)
            products.value
        } catch (e: IllegalStateException) {
            Log.e("ProductRepository", "Supabase no inicializado", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error al obtener productos: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchProducts(district: String, category: String? = null, searchQuery: String? = null) {
        try {
            Log.d("ProductRepository", "Buscando productos desde ofertas - distrito: '$district', categoría: $category, búsqueda: $searchQuery")
            // En el nuevo esquema, los productos se obtienen desde las ofertas activas
            // Obtener ofertas activas y convertirlas a productos
            val remoteOffers = supabase.from("ofertas")
                .select {
                    filter {
                        eq("status", "ACTIVE")
                        if (district.isNotBlank()) {
                            eq("district", district)
                        }
                        if (searchQuery != null && searchQuery.isNotBlank()) {
                            ilike("product_name", "%$searchQuery%")
                        }
                    }
                }
                .decodeList<com.nexusbiz.nexusbiz.data.remote.model.Offer>()
            
            Log.d("ProductRepository", "Ofertas encontradas: ${remoteOffers.size}")
            
            // Obtener el plan de la bodega para cada oferta
            val mappedItems = remoteOffers.mapNotNull { offer ->
                try {
                    // Obtener el plan de la bodega
                    val storeData = supabase.from("bodegas")
                        .select(columns = Columns.ALL) {
                            filter { eq("id", offer.storeId) }
                        }
                        .decodeSingleOrNull<JsonObject>()
                    
                    val planValue = storeData?.get("plan_type")?.jsonPrimitive?.contentOrNull ?: "FREE"
                    val plan = when (planValue.uppercase()) {
                        "PRO" -> StorePlan.PRO
                        else -> StorePlan.FREE
                    }
                    
                    // Convertir Offer directamente a Product
                    Product(
                        id = offer.id,
                        name = offer.productName,
                        description = offer.description,
                        imageUrl = offer.imageUrl,
                        categoryId = "",
                        category = category ?: "",
                        normalPrice = offer.normalPrice,
                        groupPrice = offer.groupPrice,
                        minGroupSize = offer.targetUnits,
                        maxGroupSize = offer.targetUnits,
                        storeId = offer.storeId,
                        storeName = offer.storeName,
                        district = offer.district,
                        isActive = true,
                        durationHours = offer.durationHours,
                        storePlan = plan,
                        createdAt = offer.createdAt,
                        updatedAt = offer.updatedAt
                    )
                } catch (e: Exception) {
                    Log.e("ProductRepository", "Error al mapear oferta a producto: ${e.message}", e)
                    null
                }
            }
            Log.d("ProductRepository", "Productos mapeados: ${mappedItems.size}")
            mappedItems.forEach { product ->
                Log.d("ProductRepository", "Producto: ${product.name}, distrito: ${product.district}, store: ${product.storeName}")
            }
            
            val sortedItems = mappedItems.sortedWith(
                compareByDescending<Product> { it.storePlan == StorePlan.PRO }
                    .thenByDescending { it.createdAt ?: "" }
            )

            mutex.withLock { _products.value = sortedItems }
            Log.d("ProductRepository", "Productos mapeados y guardados: ${sortedItems.size}")
        } catch (e: IllegalStateException) {
            Log.e("ProductRepository", "Supabase no inicializado", e)
            mutex.withLock { _products.value = emptyList() }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error al obtener productos: ${e.message}", e)
            mutex.withLock { _products.value = emptyList() }
        }
    }
    
    fun getProductsFlow(district: String, category: String? = null, searchQuery: String? = null): Flow<List<Product>> {
        return flowOf(runBlocking { getProducts(district, category, searchQuery) })
    }
    
    suspend fun getProductById(productId: String): Product? {
        return try {
            // En el nuevo esquema, buscar en ofertas
            val remoteOffer = supabase.from("ofertas")
                .select {
                    filter { eq("id", productId) }
                }
                .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.Offer>() ?: return null
            
            // Obtener el plan de la bodega
            val storeData = supabase.from("bodegas")
                .select(columns = Columns.ALL) {
                    filter { eq("id", remoteOffer.storeId) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            val planValue = storeData?.get("plan_type")?.jsonPrimitive?.contentOrNull ?: "FREE"
            val plan = when (planValue.uppercase()) {
                "PRO" -> StorePlan.PRO
                else -> StorePlan.FREE
            }
            
            // Convertir Offer a Product
            val remoteProduct = Product(
                id = remoteOffer.id,
                name = remoteOffer.productName,
                description = remoteOffer.description,
                imageUrl = remoteOffer.imageUrl,
                categoryId = "",
                category = "",
                normalPrice = remoteOffer.normalPrice,
                groupPrice = remoteOffer.groupPrice,
                minGroupSize = remoteOffer.targetUnits,
                maxGroupSize = remoteOffer.targetUnits,
                storeId = remoteOffer.storeId,
                storeName = remoteOffer.storeName,
                district = remoteOffer.district,
                isActive = true,
                durationHours = remoteOffer.durationHours,
                storePlan = plan,
                createdAt = remoteOffer.createdAt,
                updatedAt = remoteOffer.updatedAt
            )
            
            return remoteProduct
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error al obtener producto por ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * Obtiene las categorías disponibles.
     * 
     * NOTA: En el esquema actual de Supabase no existe la tabla 'categorias'.
     * Este método retorna una lista vacía o categorías hardcodeadas.
     * Si en el futuro se agrega la tabla de categorías, se puede actualizar este método.
     */
    suspend fun getCategories(): List<Category> {
        return try {
            // CORRECCIÓN: La tabla 'categorias' no existe en el esquema actual
            // Retornar lista vacía o categorías hardcodeadas según sea necesario
            Log.d("ProductRepository", "getCategories() llamado - tabla 'categorias' no existe, retornando lista vacía")
            emptyList()
            
            // Si se necesitan categorías hardcodeadas, descomentar lo siguiente:
            /*
            listOf(
                Category(id = "1", name = "Alimentos", description = "Productos alimenticios"),
                Category(id = "2", name = "Bebidas", description = "Bebidas y líquidos"),
                Category(id = "3", name = "Limpieza", description = "Productos de limpieza"),
                Category(id = "4", name = "Otros", description = "Otros productos")
            )
            */
        } catch (e: IllegalStateException) {
            Log.e("ProductRepository", "Supabase no inicializado", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error al obtener categorías: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun createProduct(product: Product): Result<Product> {
        return try {
            Log.d("ProductRepository", "Iniciando creación de producto: ${product.name}")
            
            // Validaciones de consistencia
            if (product.normalPrice <= 0 || product.groupPrice <= 0) {
                Log.e("ProductRepository", "Error de validación: precios inválidos")
                return Result.failure(Exception("Los precios deben ser mayores a 0"))
            }
            if (product.groupPrice >= product.normalPrice) {
                Log.e("ProductRepository", "Error de validación: precio grupal >= precio normal")
                return Result.failure(Exception("El precio grupal debe ser menor al precio normal"))
            }
            if (product.minGroupSize < 1) {
                Log.e("ProductRepository", "Error de validación: minGroupSize < 1")
                return Result.failure(Exception("El tamaño mínimo del grupo debe ser al menos 1"))
            }
            if (product.maxGroupSize < product.minGroupSize) {
                Log.e("ProductRepository", "Error de validación: maxGroupSize < minGroupSize")
                return Result.failure(Exception("El tamaño máximo debe ser mayor o igual al mínimo"))
            }
            
            val newProduct = product.copy(id = UUID.randomUUID().toString())
            Log.d("ProductRepository", "Creando producto: ${newProduct.name} con ID: ${newProduct.id}, storeId: ${newProduct.storeId}")
            
            // Insertar producto con el plan de la bodega
            // Obtener el plan de la bodega si no está en el producto
            val storePlanToInsert: String = if (newProduct.storePlan != null) {
                newProduct.storePlan.name // Convertir enum a String
            } else {
                try {
                    val storeData = supabase.from("bodegas")
                        .select(columns = Columns.ALL) {
                            filter { eq("id", newProduct.storeId) }
                        }
                        .decodeSingleOrNull<JsonObject>()
                    
                    val planValue = storeData?.get("plan_type")?.jsonPrimitive?.contentOrNull
                    planValue ?: "FREE"
                } catch (e: Exception) {
                    Log.w("ProductRepository", "No se pudo obtener el plan de la bodega, usando FREE: ${e.message}")
                    "FREE"
                }
            }
            
            // Insertar usando data class serializable para evitar problemas de serialización
            val productInsert = ProductInsert(
                id = newProduct.id,
                name = newProduct.name,
                description = newProduct.description ?: "",
                imageUrl = newProduct.imageUrl?.takeIf { it.isNotBlank() }, // Permitir null si no hay imagen
                categoryId = newProduct.categoryId ?: "",
                categoryName = newProduct.category ?: "",
                normalPrice = newProduct.normalPrice,
                groupPrice = newProduct.groupPrice,
                minGroupSize = newProduct.minGroupSize,
                maxGroupSize = newProduct.maxGroupSize,
                storeId = newProduct.storeId,
                storeName = newProduct.storeName,
                district = newProduct.district,
                isActive = newProduct.isActive,
                durationHours = newProduct.durationHours,
                storePlan = storePlanToInsert
            )
            
            Log.d("ProductRepository", "Insertando producto en BD: ${productInsert.name}, imageUrl: ${productInsert.imageUrl}")
            // En el nuevo esquema, los productos se crean como ofertas
            // Este método ya no debería usarse directamente, usar OfferRepository.createOffer en su lugar
            Log.w("ProductRepository", "createProduct está deprecado. Usar OfferRepository.createOffer en su lugar.")
            Result.failure(Exception("Los productos ahora se crean como ofertas. Usa OfferRepository.createOffer."))
        } catch (e: IllegalStateException) {
            Log.e("ProductRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error al crear producto: ${e.message}", e)
            Log.e("ProductRepository", "Stack trace: ${e.stackTraceToString()}")
            Result.failure(Exception("Error al crear producto: ${e.message ?: "Error desconocido"}"))
        }
    }

    suspend fun publishProduct(product: Product): Result<Product> = createProduct(product)
    
    suspend fun updateProduct(product: Product): Result<Product> {
        // En el nuevo esquema, los productos son ofertas, usar OfferRepository para actualizar
        Log.w("ProductRepository", "updateProduct está deprecado. Los productos ahora son ofertas.")
        return Result.failure(Exception("Los productos ahora son ofertas. Usa OfferRepository para actualizar."))
    }
    
    suspend fun deleteProduct(productId: String): Result<Unit> {
        // En el nuevo esquema, los productos son ofertas, usar OfferRepository para eliminar
        Log.w("ProductRepository", "deleteProduct está deprecado. Los productos ahora son ofertas.")
        return Result.failure(Exception("Los productos ahora son ofertas. Usa OfferRepository para eliminar."))
    }
}
