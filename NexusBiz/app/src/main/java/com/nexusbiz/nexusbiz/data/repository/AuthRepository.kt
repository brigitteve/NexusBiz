package com.nexusbiz.nexusbiz.data.repository

import android.util.Log
import com.nexusbiz.nexusbiz.data.model.Store
import com.nexusbiz.nexusbiz.data.model.User
import com.nexusbiz.nexusbiz.data.model.UserTier
import com.nexusbiz.nexusbiz.data.model.GamificationLevel
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import com.nexusbiz.nexusbiz.service.RealtimeService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.security.MessageDigest
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Repositorio que maneja autenticación y usuarios, integrado con Supabase Realtime.
 * 
 * Este repositorio:
 * - Mantiene el usuario actual actualizado automáticamente cuando hay cambios en la BD
 * - Escucha eventos de RealtimeService para cambios en puntos y nivel de gamificación
 * - Actualiza _currentUser cuando el usuario gana puntos o sube de nivel
 * 
 * Cómo funciona la sincronización en tiempo real:
 * 1. RealtimeService escucha cambios en la tabla "usuarios" (solo UPDATE)
 * 2. Cuando hay un cambio en puntos o gamification_level, RealtimeService emite un evento
 * 3. Este repositorio recibe el evento y actualiza _currentUser
 * 4. Los ViewModels y pantallas que observan currentUser se actualizan automáticamente
 * 5. Las pantallas que muestran puntos/nivel se recomponen automáticamente
 */
class AuthRepository {
    private val supabase: io.github.jan.supabase.SupabaseClient
        get() = SupabaseManager.client
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()
    
    private val _isOnboardingComplete = MutableStateFlow(false)
    val isOnboardingComplete: Flow<Boolean> = _isOnboardingComplete.asStateFlow()
    
    // Scope para corrutinas de Realtime
    private val realtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Flag para saber si ya se inició la escucha de eventos
    private var isListeningToRealtime = false
    
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
            
            // CORRECCIÓN: Buscar el usuario por alias y filtrar por user_type = 'CONSUMER'
            // Esto asegura que solo se obtengan usuarios clientes, no bodegueros
            val remoteUser = supabase.from("usuarios")
                .select {
                    filter {
                        eq("alias", alias)
                        eq("user_type", "CONSUMER")
                    }
                }
                .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
            
            if (remoteUser == null) {
                Log.w("AuthRepository", "Usuario con alias '$alias' no encontrado")
                return Result.failure(Exception("Alias o contraseña incorrectos"))
            }
            
            Log.d("AuthRepository", "Usuario encontrado: ${remoteUser.id}, tipo: ${remoteUser.userType}, alias: ${remoteUser.alias}, district: ${remoteUser.district}")
            Log.d("AuthRepository", "Password hash en BD (longitud ${remoteUser.passwordHash?.length ?: 0}): ${remoteUser.passwordHash?.take(20)}...")
            
            // Normalizar ambos hashes antes de comparar (por si hay espacios o diferencias de case)
            val storedHash = remoteUser.passwordHash ?: ""
            val normalizedStoredHash = normalizeHash(storedHash)
            val normalizedGeneratedHash = normalizeHash(passwordHash)
            
            Log.d("AuthRepository", "Hash normalizado generado: $normalizedGeneratedHash")
            Log.d("AuthRepository", "Hash normalizado en BD: $normalizedStoredHash")
            
            // Comparar hashes normalizados
            if (normalizedStoredHash != normalizedGeneratedHash) {
                Log.w("AuthRepository", "Los hashes NO coinciden después de normalizar")
                Log.d("AuthRepository", "Hash generado completo: $passwordHash")
                Log.d("AuthRepository", "Hash en BD completo: $storedHash")
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
            
            // Convertir User remoto a User local
            val localUser = User(
                id = remoteUser.id,
                alias = remoteUser.alias.ifBlank { alias },
                passwordHash = storedHash,
                fechaNacimiento = remoteUser.fechaNacimiento ?: "",
                district = remoteUser.district.ifBlank { "Trujillo" },
                email = remoteUser.email,
                avatar = remoteUser.avatar,
                latitude = remoteUser.latitude,
                longitude = remoteUser.longitude,
                points = remoteUser.points,
                gamificationLevel = remoteUser.gamificationLevel?.let {
                    when (it) {
                        com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.BRONCE -> 
                            com.nexusbiz.nexusbiz.data.model.GamificationLevel.BRONCE
                        com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA -> 
                            com.nexusbiz.nexusbiz.data.model.GamificationLevel.PLATA
                        com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO -> 
                            com.nexusbiz.nexusbiz.data.model.GamificationLevel.ORO
                    }
                },
                badges = remoteUser.badges,
                streak = remoteUser.streak,
                completedGroups = remoteUser.completedGroups,
                totalSavings = remoteUser.totalSavings,
                userType = when (remoteUser.userType) {
                    com.nexusbiz.nexusbiz.data.remote.model.UserType.CONSUMER -> com.nexusbiz.nexusbiz.data.model.UserType.CONSUMER
                    com.nexusbiz.nexusbiz.data.remote.model.UserType.STORE_OWNER -> com.nexusbiz.nexusbiz.data.model.UserType.STORE_OWNER
                },
                createdAt = remoteUser.createdAt
            )
            
            Log.d("AuthRepository", "Login exitoso para usuario: ${localUser.id}, alias: ${localUser.alias}, district: ${localUser.district}")
            _currentUser.value = localUser
            Result.success(localUser)
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
                .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
            
            if (user == null) {
                Log.w("AuthRepository", "Bodeguero con alias '$alias' no encontrado")
                return Result.failure(Exception("Bodeguero no encontrado o credenciales incorrectas"))
            }
            
            Log.d("AuthRepository", "Usuario bodeguero encontrado: ${user.id}")
            val storedPasswordHash = user.passwordHash ?: ""
            Log.d("AuthRepository", "Password hash en BD (longitud ${storedPasswordHash.length}): ${storedPasswordHash.take(20)}...")
            
            // Normalizar ambos hashes antes de comparar
            val normalizedStoredHash = normalizeHash(storedPasswordHash)
            val normalizedGeneratedHash = normalizeHash(passwordHash)
            
            Log.d("AuthRepository", "Hash normalizado generado: $normalizedGeneratedHash")
            Log.d("AuthRepository", "Hash normalizado en BD: $normalizedStoredHash")
            
            // Comparar hashes normalizados
            if (normalizedStoredHash != normalizedGeneratedHash) {
                Log.w("AuthRepository", "Los hashes NO coinciden para bodeguero después de normalizar")
                Log.d("AuthRepository", "Hash generado completo: $passwordHash")
                Log.d("AuthRepository", "Hash en BD completo: $storedPasswordHash")
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
            val remoteStore = supabase.from("bodegas")
                .select {
                    filter { eq("owner_id", user.id) }
                }
                .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.Store>()
            
            val store = remoteStore?.let { remote ->
                val plan = when (remote.plan) {
                    "PRO" -> com.nexusbiz.nexusbiz.data.model.StorePlan.PRO
                    else -> com.nexusbiz.nexusbiz.data.model.StorePlan.FREE
                }
                Store(
                    id = remote.id,
                    name = remote.name,
                    address = remote.address,
                    district = remote.district,
                    latitude = remote.latitude,
                    longitude = remote.longitude,
                    phone = remote.phone,
                    imageUrl = remote.imageUrl,
                    hasStock = remote.hasStock,
                    ownerId = remote.ownerId,
                    rating = remote.rating.takeIf { it > 0 },
                    totalSales = remote.totalSales,
                    ruc = remote.ruc,
                    commercialName = remote.commercialName,
                    ownerAlias = remote.ownerAlias,
                    plan = plan,
                    createdAt = remote.createdAt,
                    updatedAt = remote.updatedAt
                )
            }
            
            if (store == null) {
                Log.w("AuthRepository", "Bodega no encontrada para usuario: ${user.id}")
                return Result.failure(Exception("Bodega no encontrada"))
            }
            
            Log.d("AuthRepository", "Bodega encontrada: ${store.id}")
            // Convertir RemoteUser a User local
            val localUser = User(
                id = user.id,
                alias = user.alias,
                passwordHash = user.passwordHash ?: "",
                fechaNacimiento = user.fechaNacimiento ?: "",
                district = user.district,
                email = user.email,
                avatar = user.avatar,
                latitude = user.latitude,
                longitude = user.longitude,
                points = user.points,
                tier = when (user.gamificationLevel) {
                    com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO -> UserTier.GOLD
                    com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA -> UserTier.SILVER
                    else -> UserTier.BRONZE
                },
                gamificationLevel = when (user.gamificationLevel) {
                    com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO -> GamificationLevel.ORO
                    com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA -> GamificationLevel.PLATA
                    else -> GamificationLevel.BRONCE
                },
                badges = user.badges,
                streak = user.streak,
                completedGroups = user.completedGroups,
                totalSavings = user.totalSavings,
                userType = when (user.userType) {
                    com.nexusbiz.nexusbiz.data.remote.model.UserType.STORE_OWNER -> com.nexusbiz.nexusbiz.data.model.UserType.STORE_OWNER
                    else -> com.nexusbiz.nexusbiz.data.model.UserType.CONSUMER
                }
            )
            _currentUser.value = localUser
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
                .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
            
            if (existingUser != null) {
                return Result.failure(Exception("El alias ya está en uso"))
            }
            
            val userId = UUID.randomUUID().toString()
            
            // Insertar usuario usando mapa explícito
            // IMPORTANTE: Asegúrate de que la columna fecha_nacimiento existe en Supabase
            // Ejecuta el archivo migration_add_fecha_nacimiento.sql si recibes un error
            // Nota: phone no se incluye porque no existe en el nuevo esquema de la base de datos
            supabase.from("usuarios").insert(
                mapOf(
                    "id" to userId,
                    "alias" to alias,
                    "password_hash" to passwordHash,
                    "fecha_nacimiento" to fechaNacimiento,
                    "district" to distrito,
                    "user_type" to "CONSUMER"
                )
            )
            
            // Crear objeto User para el estado local (sin fecha_nacimiento por ahora)
            val user = User(
                id = userId,
                alias = alias,
                passwordHash = passwordHash,
                fechaNacimiento = fechaNacimiento, // Se mantiene en el objeto local para validación
                district = distrito,
                userType = com.nexusbiz.nexusbiz.data.model.UserType.CONSUMER
            )
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
    
    /**
     * Agrega puntos al usuario y actualiza el tier si es necesario
     */
    suspend fun addPoints(userId: String, pointsToAdd: Int, reason: String = ""): Result<Int> {
        return try {
            // Obtener usuario actual
            val currentUser = _currentUser.value
            if (currentUser == null || currentUser.id != userId) {
                // Si no está en memoria, obtenerlo de la BD
                val user = supabase.from("usuarios")
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
                
                if (user == null) {
                    return Result.failure(Exception("Usuario no encontrado"))
                }
                
                val newPoints = user.points + pointsToAdd
                val newGamificationLevel = when {
                    newPoints >= 200 -> com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO
                    newPoints >= 100 -> com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA
                    else -> com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.BRONCE
                }
                
                // Actualizar en BD - usar objeto User serializable
                val updatedUser = user.copy(
                    points = newPoints,
                    gamificationLevel = newGamificationLevel
                )
                supabase.from("usuarios")
                    .update(updatedUser) {
                        filter { eq("id", userId) }
                    }
                
                // Convertir RemoteUser a User local y actualizar usuario en memoria
                val newTier = when {
                    newPoints >= 200 -> com.nexusbiz.nexusbiz.data.model.UserTier.GOLD
                    newPoints >= 100 -> com.nexusbiz.nexusbiz.data.model.UserTier.SILVER
                    else -> com.nexusbiz.nexusbiz.data.model.UserTier.BRONZE
                }
                val localUser = User(
                    id = user.id,
                    alias = user.alias,
                    passwordHash = user.passwordHash ?: "",
                    fechaNacimiento = user.fechaNacimiento ?: "",
                    district = user.district,
                    email = user.email,
                    avatar = user.avatar,
                    latitude = user.latitude,
                    longitude = user.longitude,
                    points = newPoints,
                    tier = newTier,
                    gamificationLevel = when (newGamificationLevel) {
                        com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO -> GamificationLevel.ORO
                        com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA -> GamificationLevel.PLATA
                        else -> GamificationLevel.BRONCE
                    },
                    badges = user.badges,
                    streak = user.streak,
                    completedGroups = user.completedGroups,
                    totalSavings = user.totalSavings,
                    userType = when (user.userType) {
                        com.nexusbiz.nexusbiz.data.remote.model.UserType.STORE_OWNER -> com.nexusbiz.nexusbiz.data.model.UserType.STORE_OWNER
                        else -> com.nexusbiz.nexusbiz.data.model.UserType.CONSUMER
                    }
                )
                _currentUser.value = localUser
                
                Log.d("AuthRepository", "Puntos agregados: +$pointsToAdd ($reason). Total: $newPoints")
                Result.success(newPoints)
            } else {
                val newPoints = currentUser.points + pointsToAdd
                val newTier = when {
                    newPoints >= 200 -> com.nexusbiz.nexusbiz.data.model.UserTier.GOLD
                    newPoints >= 100 -> com.nexusbiz.nexusbiz.data.model.UserTier.SILVER
                    else -> com.nexusbiz.nexusbiz.data.model.UserTier.BRONZE
                }
                
                // Obtener el usuario remoto para actualizar
                val remoteUser = supabase.from("usuarios")
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
                
                if (remoteUser != null) {
                    val newGamificationLevel: com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel = when {
                        newPoints >= 200 -> com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO
                        newPoints >= 100 -> com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA
                        else -> com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.BRONCE
                    }
                    // Actualizar en BD - usar objeto User serializable
                    val updatedUser = remoteUser.copy(
                        points = newPoints,
                        gamificationLevel = newGamificationLevel
                    )
                    supabase.from("usuarios")
                        .update(updatedUser) {
                            filter { eq("id", userId) }
                        }
                }
                
                // Actualizar usuario en memoria
                _currentUser.value = currentUser.copy(
                    points = newPoints,
                    tier = newTier
                )
                
                Log.d("AuthRepository", "Puntos agregados: +$pointsToAdd ($reason). Total: $newPoints")
                Result.success(newPoints)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al agregar puntos: ${e.message}", e)
            Result.failure(e)
        }
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
                .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
            
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
                .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
            
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
                .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
            existingUser != null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al verificar alias: ${e.message}", e)
            false
        }
    }
    
    /**
     * Inicia la suscripción a actualizaciones en tiempo real del usuario actual.
     * 
     * Este método:
     * - Configura un filtro en RealtimeService para el userId actual
     * - Inicia la escucha de eventos de usuarios
     * - Actualiza automáticamente _currentUser cuando hay cambios en puntos o nivel
     * 
     * @param userId ID del usuario actual para filtrar eventos
     */
    suspend fun startRealtimeSubscription(userId: String) {
        try {
            if (userId.isBlank()) {
                Log.w("AuthRepository", "No se puede iniciar suscripción Realtime: userId vacío")
                return
            }
            
            Log.d("AuthRepository", "Iniciando suscripción Realtime para usuario: $userId")
            
            // Configurar filtro en RealtimeService
            RealtimeService.addUserFilterByUserId(userId)
            
            // Iniciar escucha de eventos si aún no se ha iniciado
            if (!isListeningToRealtime) {
                startListeningToRealtimeEvents()
                isListeningToRealtime = true
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al iniciar suscripción Realtime: ${e.message}", e)
        }
    }
    
    /**
     * Detiene la suscripción a actualizaciones en tiempo real.
     * Limpia los filtros activos.
     */
    fun stopRealtimeSubscription() {
        try {
            Log.d("AuthRepository", "Deteniendo suscripción Realtime")
            RealtimeService.clearUserFilters()
            // No detenemos la escucha completa, solo limpiamos filtros
            // La conexión base se mantiene activa durante toda la sesión
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al detener suscripción Realtime: ${e.message}", e)
        }
    }
    
    /**
     * Inicia la escucha de eventos de RealtimeService.
     * Este método se llama una sola vez y mantiene la escucha activa.
     * 
     * Cómo funciona:
     * - Escucha eventos de usuarios: cuando hay UPDATE en puntos o gamification_level
     * - Actualiza _currentUser con los nuevos valores
     * - Los eventos se procesan en el scope de Realtime para no bloquear el hilo principal
     */
    private fun startListeningToRealtimeEvents() {
        // Escuchar eventos de usuarios
        RealtimeService.userEvents
            .onEach { event ->
                if (event != null) {
                    handleUserRealtimeEvent(event)
                }
            }
            .launchIn(realtimeScope)
        
        Log.d("AuthRepository", "Escucha de eventos Realtime iniciada")
    }
    
    /**
     * Maneja eventos de usuarios en tiempo real.
     * 
     * Eventos:
     * - UPDATE: Actualiza el usuario actual con nuevos puntos o nivel
     * 
     * Cuando el usuario gana puntos o sube de nivel:
     * - Los triggers de Supabase actualizan points y gamification_level en la BD
     * - RealtimeService detecta el cambio y emite un evento
     * - Este método actualiza _currentUser con los nuevos valores
     * - Las pantallas que muestran puntos/nivel se actualizan automáticamente
     */
    private fun handleUserRealtimeEvent(event: com.nexusbiz.nexusbiz.service.UserRealtimeEvent) {
        try {
            val currentUser = _currentUser.value
            if (currentUser == null || currentUser.id != event.userId) {
                // Si no es el usuario actual, ignorar
                return
            }
            
            event.user?.let { remoteUser ->
                // Convertir RemoteUser a User local
                val newTier = when (remoteUser.gamificationLevel) {
                    com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO -> UserTier.GOLD
                    com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA -> UserTier.SILVER
                    else -> UserTier.BRONZE
                }
                
                val newGamificationLevel = when (remoteUser.gamificationLevel) {
                    com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO -> GamificationLevel.ORO
                    com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA -> GamificationLevel.PLATA
                    else -> GamificationLevel.BRONCE
                }
                
                // Actualizar usuario en memoria
                val updatedUser = currentUser.copy(
                    points = remoteUser.points,
                    tier = newTier,
                    gamificationLevel = newGamificationLevel,
                    badges = remoteUser.badges,
                    streak = remoteUser.streak,
                    completedGroups = remoteUser.completedGroups,
                    totalSavings = remoteUser.totalSavings
                )
                
                _currentUser.value = updatedUser
                
                Log.d("AuthRepository", "Usuario actualizado en tiempo real: puntos=${remoteUser.points}, nivel=${remoteUser.gamificationLevel}")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error al manejar evento de usuario: ${e.message}", e)
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
                .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
            
            if (existingUser != null) {
                return Result.failure(Exception("El alias ya está en uso"))
            }
            
            // Crear usuario
            val userId = UUID.randomUUID().toString()
            
            // Insertar usuario usando mapa explícito
            // IMPORTANTE: Asegúrate de que la columna fecha_nacimiento existe en Supabase
            // Ejecuta el archivo migration_add_fecha_nacimiento.sql si recibes un error
            // Nota: phone no se incluye porque no existe en el nuevo esquema de la base de datos
            supabase.from("usuarios").insert(
                mapOf(
                    "id" to userId,
                    "alias" to alias,
                    "password_hash" to passwordHash,
                    "fecha_nacimiento" to fechaNacimiento,
                    "district" to district,
                    "user_type" to "STORE_OWNER"
                )
            )
            
            // Crear objeto User para el estado local (sin fecha_nacimiento por ahora)
            val user = User(
                id = userId,
                alias = alias,
                passwordHash = passwordHash,
                fechaNacimiento = fechaNacimiento, // Se mantiene en el objeto local para validación
                district = district,
                userType = com.nexusbiz.nexusbiz.data.model.UserType.STORE_OWNER
            )
            
            // Crear bodega usando objeto Store serializable
            // Nota: owner_alias se establece como null porque no existe en el nuevo esquema de la base de datos
            // plan_type se establece como "FREE" por defecto según el esquema
            val storeId = UUID.randomUUID().toString()
            val remoteStore = com.nexusbiz.nexusbiz.data.remote.model.Store(
                id = storeId,
                name = nombreComercial,
                address = address,
                district = district,
                phone = "",
                ownerId = userId,
                hasStock = true,
                rating = 0.0,
                totalSales = 0,
                ruc = ruc,
                commercialName = nombreComercial,
                ownerAlias = null, // No existe en BD, se omite en serialización
                plan = "FREE" // Mapea a plan_type en BD
            )
            supabase.from("bodegas").insert(remoteStore)
            
            // Crear objeto Store local para el estado de la aplicación (con ownerAlias para compatibilidad con vistas)
            val store = Store(
                id = storeId,
                name = nombreComercial,
                address = address,
                district = district,
                phone = "",
                ownerId = userId,
                hasStock = true,
                rating = 0.0,
                totalSales = 0,
                ruc = ruc,
                commercialName = nombreComercial,
                ownerAlias = alias // Solo para el objeto local, no se inserta en BD
            )
            
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
