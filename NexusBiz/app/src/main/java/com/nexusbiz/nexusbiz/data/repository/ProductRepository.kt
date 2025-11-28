package com.nexusbiz.nexusbiz.data.repository

import android.util.Log
import com.nexusbiz.nexusbiz.data.model.Product
import com.nexusbiz.nexusbiz.data.model.Category
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.util.UUID

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
                .decodeList<Product>()
                .sortedByDescending { it.createdAt ?: "" }

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
            supabase.from("productos")
                .select {
                    filter { eq("id", productId) }
                }
                .decodeSingleOrNull<Product>()
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
            // Validaciones de consistencia
            if (product.normalPrice <= 0 || product.groupPrice <= 0) {
                return Result.failure(Exception("Los precios deben ser mayores a 0"))
            }
            if (product.groupPrice >= product.normalPrice) {
                return Result.failure(Exception("El precio grupal debe ser menor al precio normal"))
            }
            if (product.minGroupSize < 1) {
                return Result.failure(Exception("El tamaño mínimo del grupo debe ser al menos 1"))
            }
            if (product.maxGroupSize < product.minGroupSize) {
                return Result.failure(Exception("El tamaño máximo debe ser mayor o igual al mínimo"))
            }
            
            val newProduct = product.copy(id = UUID.randomUUID().toString())
            Log.d("ProductRepository", "Creando producto: ${newProduct.name} con ID: ${newProduct.id}")
            
            supabase.from("productos").insert(newProduct)
            
            Log.d("ProductRepository", "Producto insertado exitosamente")
            
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
