package com.nexusbiz.nexusbiz.data.repository

import android.util.Log
import com.nexusbiz.nexusbiz.data.model.Store
import com.nexusbiz.nexusbiz.data.model.User
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.UUID

class AuthRepository {
    private val supabase: io.github.jan.supabase.SupabaseClient
        get() = SupabaseManager.client
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()
    
    private val _isOnboardingComplete = MutableStateFlow(false)
    val isOnboardingComplete: Flow<Boolean> = _isOnboardingComplete.asStateFlow()
    
    suspend fun completeOnboarding() {
        _isOnboardingComplete.value = true
    }
    
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        // Usar UTF-8 explícitamente para asegurar consistencia
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Normaliza un hash eliminando espacios y convirtiendo a minúsculas
     */
    private fun normalizeHash(hash: String): String {
        return hash.trim().lowercase().replace(" ", "").replace("\n", "").replace("\r", "")
    }
    
    /**
     * Valida que la fecha de nacimiento indique una edad mínima de 18 años
     */
    private fun isValidAge(fechaNacimiento: String): Boolean {
        return try {
            val formatter = DateTimeFormatter.ISO_DATE
            val birthDate = LocalDate.parse(fechaNacimiento, formatter)
            val age = Period.between(birthDate, LocalDate.now()).years
            age >= 18
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun login(alias: String, password: String): Result<User> {
        return try {
            val passwordHash = hashPassword(password)
            Log.d("AuthRepository", "Intentando login con alias: $alias")
            Log.d("AuthRepository", "Password hash generado (longitud ${passwordHash.length}): ${passwordHash.take(20)}...")
            
            // Primero buscar el usuario por alias
            val user = supabase.from("usuarios")
                .select {
                    filter { eq("alias", alias) }
                }
                .decodeSingleOrNull<User>()
            
            if (user == null) {
                Log.w("AuthRepository", "Usuario con alias '$alias' no encontrado")
                return Result.failure(Exception("Alias o contraseña incorrectos"))
            }
            
            Log.d("AuthRepository", "Usuario encontrado: ${user.id}, tipo: ${user.userType}")
            Log.d("AuthRepository", "Password hash en BD (longitud ${user.passwordHash.length}): ${user.passwordHash.take(20)}...")
            
            // Normalizar ambos hashes antes de comparar (por si hay espacios o diferencias de case)
            val normalizedStoredHash = normalizeHash(user.passwordHash)
            val normalizedGeneratedHash = normalizeHash(passwordHash)
            
            Log.d("AuthRepository", "Hash normalizado generado: $normalizedGeneratedHash")
            Log.d("AuthRepository", "Hash normalizado en BD: $normalizedStoredHash")
            
            // Comparar hashes normalizados
            if (normalizedStoredHash != normalizedGeneratedHash) {
                Log.w("AuthRepository", "Los hashes NO coinciden después de normalizar")
                Log.d("AuthRepository", "Hash generado completo: $passwordHash")
                Log.d("AuthRepository", "Hash en BD completo: ${user.passwordHash}")
                Log.d("AuthRepository", "Hash generado normalizado completo: $normalizedGeneratedHash")
                Log.d("AuthRepository", "Hash en BD normalizado completo: $normalizedStoredHash")
                
                // Intentar con diferentes encodings como último recurso
                val hashUtf8 = hashPassword(password)
                val hashIso8859 = try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    val hash = digest.digest(password.toByteArray(Charsets.ISO_8859_1))
                    hash.joinToString("") { "%02x".format(it) }
                } catch (e: Exception) { "" }
                
                Log.d("AuthRepository", "Hash UTF-8: $hashUtf8")
                Log.d("AuthRepository", "Hash ISO-8859-1: $hashIso8859")
                
                return Result.failure(Exception("Alias o contraseña incorrectos"))
            }
            
            Log.d("AuthRepository", "Login exitoso para usuario: ${user.id}")
            _currentUser.value = user
            Result.success(user)
        } catch (e: IllegalStateException) {
            Log.e("AuthRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al hacer login: ${e.message}", e)
            Log.e("AuthRepository", "Stack trace: ${e.stackTraceToString()}")
            Result.failure(Exception("Error al iniciar sesión: ${e.message ?: "Error desconocido"}"))
        }
    }
    
    suspend fun loginStore(alias: String, password: String): Result<Store> {
        return try {
            val passwordHash = hashPassword(password)
            Log.d("AuthRepository", "Intentando login de bodega con alias: $alias")
            Log.d("AuthRepository", "Password hash generado (longitud ${passwordHash.length}): ${passwordHash.take(20)}...")
            
            // Buscar usuario bodeguero por alias y tipo
            val user = supabase.from("usuarios")
                .select {
                    filter {
                        eq("alias", alias)
                        eq("user_type", "STORE_OWNER")
                    }
                }
                .decodeSingleOrNull<User>()
            
            if (user == null) {
                Log.w("AuthRepository", "Bodeguero con alias '$alias' no encontrado")
                return Result.failure(Exception("Bodeguero no encontrado o credenciales incorrectas"))
            }
            
            Log.d("AuthRepository", "Usuario bodeguero encontrado: ${user.id}")
            Log.d("AuthRepository", "Password hash en BD (longitud ${user.passwordHash.length}): ${user.passwordHash.take(20)}...")
            
            // Normalizar ambos hashes antes de comparar
            val normalizedStoredHash = normalizeHash(user.passwordHash)
            val normalizedGeneratedHash = normalizeHash(passwordHash)
            
            Log.d("AuthRepository", "Hash normalizado generado: $normalizedGeneratedHash")
            Log.d("AuthRepository", "Hash normalizado en BD: $normalizedStoredHash")
            
            // Comparar hashes normalizados
            if (normalizedStoredHash != normalizedGeneratedHash) {
                Log.w("AuthRepository", "Los hashes NO coinciden para bodeguero después de normalizar")
                Log.d("AuthRepository", "Hash generado completo: $passwordHash")
                Log.d("AuthRepository", "Hash en BD completo: ${user.passwordHash}")
                Log.d("AuthRepository", "Hash generado normalizado completo: $normalizedGeneratedHash")
                Log.d("AuthRepository", "Hash en BD normalizado completo: $normalizedStoredHash")
                
                // Intentar con diferentes encodings como último recurso
                val hashUtf8 = hashPassword(password)
                val hashIso8859 = try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    val hash = digest.digest(password.toByteArray(Charsets.ISO_8859_1))
                    hash.joinToString("") { "%02x".format(it) }
                } catch (e: Exception) { "" }
                
                Log.d("AuthRepository", "Hash UTF-8: $hashUtf8")
                Log.d("AuthRepository", "Hash ISO-8859-1: $hashIso8859")
                
                return Result.failure(Exception("Bodeguero no encontrado o credenciales incorrectas"))
            }
            
            // Buscar bodega del propietario
            val store = supabase.from("bodegas")
                .select {
                    filter { eq("owner_id", user.id) }
                }
                .decodeSingleOrNull<Store>()
            
            if (store == null) {
                Log.w("AuthRepository", "Bodega no encontrada para usuario: ${user.id}")
                return Result.failure(Exception("Bodega no encontrada"))
            }
            
            Log.d("AuthRepository", "Bodega encontrada: ${store.id}")
            _currentUser.value = user
            Result.success(store)
        } catch (e: IllegalStateException) {
            Log.e("AuthRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al hacer login de bodega: ${e.message}", e)
            Log.e("AuthRepository", "Stack trace: ${e.stackTraceToString()}")
            Result.failure(Exception("Error al iniciar sesión: ${e.message ?: "Error desconocido"}"))
        }
    }
    
    suspend fun register(
        alias: String,
        password: String,
        fechaNacimiento: String,
        distrito: String
    ): Result<User> {
        return try {
            // Validar edad mínima
            if (!isValidAge(fechaNacimiento)) {
                return Result.failure(Exception("Debes ser mayor de 18 años"))
            }
            
            val passwordHash = hashPassword(password)
            
            // Verificar si el alias ya existe
            val existingUser = supabase.from("usuarios")
                .select {
                    filter { eq("alias", alias) }
                }
                .decodeSingleOrNull<User>()
            
            if (existingUser != null) {
                return Result.failure(Exception("El alias ya está en uso"))
            }
            
            val user = User(
                id = UUID.randomUUID().toString(),
                alias = alias,
                passwordHash = passwordHash,
                fechaNacimiento = fechaNacimiento,
                district = distrito,
                userType = com.nexusbiz.nexusbiz.data.model.UserType.CONSUMER
            )
            
            supabase.from("usuarios").insert(user)
            _currentUser.value = user
            Result.success(user)
        } catch (e: IllegalStateException) {
            Log.e("AuthRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al registrar usuario: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun logout() {
        _currentUser.value = null
    }
    
    suspend fun updateProfile(user: User): Result<User> {
        return try {
            supabase.from("usuarios")
                .update(user) {
                    filter { eq("id", user.id) }
                }
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al actualizar perfil: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val oldPasswordHash = hashPassword(oldPassword)
            val newPasswordHash = hashPassword(newPassword)
            
            // Verificar contraseña actual
            val user = supabase.from("usuarios")
                .select {
                    filter {
                        eq("id", userId)
                        eq("password_hash", oldPasswordHash)
                    }
                }
                .decodeSingleOrNull<User>()
            
            if (user == null) {
                return Result.failure(Exception("Contraseña actual incorrecta"))
            }
            
            // Actualizar contraseña
            supabase.from("usuarios")
                .update(mapOf("password_hash" to newPasswordHash)) {
                    filter { eq("id", userId) }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al cambiar contraseña: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Función de utilidad para actualizar el password hash de un usuario por alias.
     * SOLO PARA DESARROLLO/TESTING - NO USAR EN PRODUCCIÓN
     * 
     * Esta función actualiza el password_hash en la BD con el hash de la contraseña proporcionada.
     * Útil cuando necesitas sincronizar el hash en la BD con una contraseña conocida.
     */
    suspend fun updatePasswordHashByAlias(alias: String, newPassword: String): Result<Unit> {
        return try {
            val newPasswordHash = hashPassword(newPassword)
            Log.d("AuthRepository", "Actualizando password hash para alias: $alias")
            Log.d("AuthRepository", "Nuevo hash: $newPasswordHash")
            
            // Buscar usuario
            val user = supabase.from("usuarios")
                .select {
                    filter { eq("alias", alias) }
                }
                .decodeSingleOrNull<User>()
            
            if (user == null) {
                return Result.failure(Exception("Usuario no encontrado"))
            }
            
            // Actualizar password hash
            supabase.from("usuarios")
                .update(mapOf("password_hash" to newPasswordHash)) {
                    filter { eq("id", user.id) }
                }
            
            Log.d("AuthRepository", "Password hash actualizado exitosamente para usuario: ${user.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al actualizar password hash: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    fun setOnboardingComplete(complete: Boolean) {
        _isOnboardingComplete.value = complete
    }
    
    suspend fun checkAliasExists(alias: String): Boolean {
        return try {
            val existingUser = supabase.from("usuarios")
                .select {
                    filter { eq("alias", alias) }
                }
                .decodeSingleOrNull<User>()
            existingUser != null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al verificar alias: ${e.message}", e)
            false
        }
    }
    
    suspend fun registerStoreOwner(
        alias: String,
        password: String,
        fechaNacimiento: String,
        ruc: String,
        razonSocial: String,
        nombreComercial: String,
        district: String,
        address: String
    ): Result<Store> {
        return try {
            if (!isValidAge(fechaNacimiento)) {
                return Result.failure(Exception("Debes ser mayor de 18 años"))
            }

            val passwordHash = hashPassword(password)
            
            // Verificar si el alias ya existe
            val existingUser = supabase.from("usuarios")
                .select {
                    filter { eq("alias", alias) }
                }
                .decodeSingleOrNull<User>()
            
            if (existingUser != null) {
                return Result.failure(Exception("El alias ya está en uso"))
            }
            
            // Crear usuario
            val userId = UUID.randomUUID().toString()
            val user = User(
                id = userId,
                alias = alias,
                passwordHash = passwordHash,
                fechaNacimiento = fechaNacimiento,
                district = district,
                userType = com.nexusbiz.nexusbiz.data.model.UserType.STORE_OWNER
            )
            
            supabase.from("usuarios").insert(user)
            
            // Crear bodega
            val storeId = UUID.randomUUID().toString()
            val store = Store(
                id = storeId,
                name = nombreComercial,
                address = address,
                district = district,
                phone = "",
                ownerId = userId,
                hasStock = false,
                rating = null,
                totalSales = 0,
                ruc = ruc,
                commercialName = nombreComercial,
                ownerAlias = alias
            )
            
            supabase.from("bodegas").insert(store)
            
            _currentUser.value = user
            Result.success(store)
        } catch (e: IllegalStateException) {
            Log.e("AuthRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al registrar bodeguero: ${e.message}", e)
            Result.failure(e)
        }
    }
}
