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
            val items = supabase.from("productos")
                .select {
                    filter {
                        eq("is_active", true)
                        if (district.isNotBlank()) {
                            eq("district", district)
                        }
                        if (category != null && category != "Todos") {
                            eq("category_name", category)
                        }
                        if (searchQuery != null && searchQuery.isNotBlank()) {
                            ilike("name", "%$searchQuery%")
                        }
                    }
                }
                .decodeList<RemoteProduct>()
                .map { remoteProduct ->
                    // Convertir store_plan String a StorePlan enum
                    val plan = when (remoteProduct.storePlan) {
                        "PRO" -> StorePlan.PRO
                        else -> StorePlan.FREE
                    }
                    // Convertir RemoteProduct a Product
                    Product(
                        id = remoteProduct.id,
                        name = remoteProduct.name,
                        description = remoteProduct.description,
                        imageUrl = remoteProduct.imageUrl,
                        categoryId = remoteProduct.categoryId,
                        category = remoteProduct.categoryName,
                        normalPrice = remoteProduct.normalPrice,
                        groupPrice = remoteProduct.groupPrice,
                        minGroupSize = remoteProduct.minGroupSize,
                        maxGroupSize = remoteProduct.maxGroupSize,
                        storeId = remoteProduct.storeId,
                        storeName = remoteProduct.storeName,
                        district = remoteProduct.district,
                        isActive = remoteProduct.isActive,
                        durationHours = remoteProduct.durationHours,
                        storePlan = plan,
                        createdAt = remoteProduct.createdAt,
                        updatedAt = remoteProduct.updatedAt
                    )
                }
                .sortedWith(
                    compareByDescending<Product> { it.storePlan == StorePlan.PRO }
                        .thenByDescending { it.createdAt ?: "" }
                )

            mutex.withLock { _products.value = items }
            Log.d("ProductRepository", "Productos obtenidos: ${items.size}")
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
            val remoteProduct = supabase.from("productos")
                .select {
                    filter { eq("id", productId) }
                }
                .decodeSingleOrNull<RemoteProduct>() ?: return null
            
            // Convertir store_plan String a StorePlan enum
            val plan = when (remoteProduct.storePlan) {
                "PRO" -> StorePlan.PRO
                else -> StorePlan.FREE
            }
            // Convertir RemoteProduct a Product
            Product(
                id = remoteProduct.id,
                name = remoteProduct.name,
                description = remoteProduct.description,
                imageUrl = remoteProduct.imageUrl,
                categoryId = remoteProduct.categoryId,
                category = remoteProduct.categoryName,
                normalPrice = remoteProduct.normalPrice,
                groupPrice = remoteProduct.groupPrice,
                minGroupSize = remoteProduct.minGroupSize,
                maxGroupSize = remoteProduct.maxGroupSize,
                storeId = remoteProduct.storeId,
                storeName = remoteProduct.storeName,
                district = remoteProduct.district,
                isActive = remoteProduct.isActive,
                durationHours = remoteProduct.durationHours,
                storePlan = plan,
                createdAt = remoteProduct.createdAt,
                updatedAt = remoteProduct.updatedAt
            )
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getCategories(): List<Category> {
        return try {
            supabase.from("categorias")
                .select()
                .decodeList<Category>()
                .sortedBy { it.name }
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
                    
                    val planValue = storeData?.get("plan")?.jsonPrimitive?.contentOrNull
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
            supabase.from("productos").insert(productInsert)
            
            Log.d("ProductRepository", "Producto insertado exitosamente en BD")
            
            // refrescar lista
            fetchProducts(district = newProduct.district, category = null, searchQuery = null)
            Result.success(newProduct)
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
        return try {
            supabase.from("productos")
                .update(product) {
                    filter { eq("id", product.id) }
                }
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            supabase.from("productos")
                .update(mapOf("is_active" to false)) {
                    filter { eq("id", productId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
