package com.nexusbiz.nexusbiz.data.repository

import android.util.Log
import com.nexusbiz.nexusbiz.data.model.Group
import com.nexusbiz.nexusbiz.data.model.GroupStatus
import com.nexusbiz.nexusbiz.data.model.Participant
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class GroupRepository {
    private val supabase: io.github.jan.supabase.SupabaseClient
        get() = SupabaseManager.client
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()
    
    private fun timestampToLong(timestamp: String?): Long {
        if (timestamp == null) return System.currentTimeMillis()
        return try {
            java.time.OffsetDateTime.parse(timestamp).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    private fun longToTimestamp(millis: Long): String {
        return java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toOffsetDateTime()
            .toString()
    }

    @Serializable
    private data class GroupInsert(
        @SerialName("id") val id: String,
        @SerialName("product_id") val productId: String,
        @SerialName("product_name") val productName: String,
        @SerialName("product_image") val productImage: String,
        @SerialName("creator_id") val creatorId: String,
        @SerialName("creator_alias") val creatorAlias: String,
        @SerialName("current_size") val currentSize: Int,
        @SerialName("target_size") val targetSize: Int,
        @SerialName("status") val status: String,
        @SerialName("expires_at") val expiresAt: String,
        @SerialName("store_id") val storeId: String,
        @SerialName("store_name") val storeName: String,
        @SerialName("qr_code") val qrCode: String? = null,
        @SerialName("normal_price") val normalPrice: Double,
        @SerialName("group_price") val groupPrice: Double
    )

    @Serializable
    private data class ParticipantInsert(
        @SerialName("id") val id: String,
        @SerialName("group_id") val groupId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("alias") val alias: String,
        @SerialName("avatar") val avatar: String,
        @SerialName("reserved_units") val reservedUnits: Int,
        @SerialName("joined_at") val joinedAt: String,
        @SerialName("is_validated") val isValidated: Boolean = false,
        @SerialName("validated_at") val validatedAt: String? = null
    )

    private fun jsonString(obj: JsonObject, key: String): String =
        obj[key]?.jsonPrimitive?.contentOrNull ?: ""
    private fun jsonDouble(obj: JsonObject, key: String): Double =
        obj[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
    private fun jsonInt(obj: JsonObject, key: String): Int =
        obj[key]?.jsonPrimitive?.intOrNull ?: 0
    private fun jsonBool(obj: JsonObject, key: String): Boolean =
        obj[key]?.jsonPrimitive?.booleanOrNull ?: false

    private fun participantFromJson(obj: JsonObject): Participant = Participant(
        id = jsonString(obj, "id"),
        groupId = jsonString(obj, "group_id"),
        userId = jsonString(obj, "user_id"),
        alias = jsonString(obj, "alias"),
        avatar = jsonString(obj, "avatar"),
        reservedUnits = jsonInt(obj, "reserved_units").takeIf { it > 0 } ?: 1,
        joinedAt = timestampToLong(jsonString(obj, "joined_at")),
        isValidated = jsonBool(obj, "is_validated"),
        validatedAt = jsonString(obj, "validated_at").ifEmpty { null }?.let { timestampToLong(it) }
    )

    private suspend fun loadParticipantsForGroup(groupId: String): List<Participant> {
        return try {
            supabase.from("participantes")
                .select {
                    filter { eq("group_id", groupId) }
                }
                .decodeList<JsonObject>()
                .map { participantFromJson(it) }
                .sortedBy { it.joinedAt }
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al cargar participantes: ${e.message}", e)
            emptyList()
        }
    }
    private suspend fun groupFromDB(groupData: JsonObject): Group {
        val groupId = jsonString(groupData, "id")
        val participants = loadParticipantsForGroup(groupId)
        
        // IMPORTANTE: Usar current_size directamente de Supabase
        // El trigger update_group_current_size() lo mantiene actualizado automáticamente
        // No calcular desde participantes porque puede haber desfase
        val currentSizeFromDB = jsonInt(groupData, "current_size")
        
        return Group(
            id = groupId,
            productId = jsonString(groupData, "product_id"),
            productName = jsonString(groupData, "product_name"),
            productImage = jsonString(groupData, "product_image"),
            creatorId = jsonString(groupData, "creator_id"),
            creatorAlias = jsonString(groupData, "creator_alias"),
            participants = participants,
            // Usar el valor de Supabase que el trigger mantiene actualizado
            currentSize = currentSizeFromDB.coerceAtLeast(0),
            targetSize = jsonInt(groupData, "target_size").takeIf { it > 0 } ?: 3,
            status = GroupStatus.valueOf(jsonString(groupData, "status").ifEmpty { "ACTIVE" }),
            expiresAt = timestampToLong(jsonString(groupData, "expires_at")),
            createdAt = timestampToLong(jsonString(groupData, "created_at")),
            storeId = jsonString(groupData, "store_id"),
            storeName = jsonString(groupData, "store_name"),
            qrCode = jsonString(groupData, "qr_code"),
            validatedAt = jsonString(groupData, "validated_at").ifEmpty { null }?.let { timestampToLong(it) },
            normalPrice = jsonDouble(groupData, "normal_price"),
            groupPrice = jsonDouble(groupData, "group_price")
        )
    }
    
    suspend fun createGroup(
        productId: String,
        productName: String,
        productImage: String,
        creatorId: String,
        creatorAlias: String,
        targetSize: Int,
        storeId: String,
        storeName: String,
        normalPrice: Double = 0.0,
        groupPrice: Double = 0.0,
        durationHours: Int = 24,
        initialReservedUnits: Int = 1
    ): Result<Group> {
        return try {
            // Validaciones
            if (targetSize < 1) {
                return Result.failure(Exception("La meta debe ser al menos 1 unidad"))
            }
            if (durationHours <= 0) {
                return Result.failure(Exception("La duración debe ser mayor a 0"))
            }
            if (normalPrice <= 0 || groupPrice <= 0) {
                return Result.failure(Exception("Los precios deben ser mayores a 0"))
            }
            if (groupPrice >= normalPrice) {
                return Result.failure(Exception("El precio grupal debe ser menor al precio normal"))
            }
            val initialUnits = initialReservedUnits.coerceAtLeast(1)
            
            // Verificar si ya existe un grupo activo para este producto
            val existingGroups = supabase.from("grupos")
                .select {
                    filter {
                        eq("product_id", productId)
                        eq("status", "ACTIVE")
                    }
                }
                .decodeList<JsonObject>()
            
            val now = System.currentTimeMillis()
            val activeGroup = existingGroups.firstOrNull { groupData ->
                val expiresAt = timestampToLong(jsonString(groupData, "expires_at"))
                expiresAt > now
            }
            
            if (activeGroup != null) {
                return Result.failure(Exception("Ya existe una oferta activa para este producto"))
            }
            
            // Crear grupo
            val groupId = UUID.randomUUID().toString()
            val expiresAt = now + (durationHours * 60 * 60 * 1000L)
            
            val groupData = GroupInsert(
                id = groupId,
                productId = productId,
                productName = productName,
                productImage = productImage,
                creatorId = creatorId,
                creatorAlias = creatorAlias,
                currentSize = 0, // Los grupos se crean vacíos, sin participantes iniciales
                targetSize = targetSize,
                status = "ACTIVE",
                expiresAt = longToTimestamp(expiresAt),
                storeId = storeId,
                storeName = storeName,
                qrCode = null,
                normalPrice = normalPrice,
                groupPrice = groupPrice
            )
            
            Log.d("GroupRepository", "Creando grupo: $groupId para producto: $productId")
            supabase.from("grupos").insert(groupData)
            Log.d("GroupRepository", "Grupo insertado exitosamente")
            
            // NOTA: NO crear participante automáticamente al crear el grupo.
            // Los grupos se crean vacíos y solo se llenan cuando los CLIENTES hacen reservas.
            // Las bodegas nunca deben ser participantes de sus propios grupos.
            
            // Cargar grupo completo
            val group = supabase.from("grupos")
                .select {
                    filter { eq("id", groupId) }
                }
                .decodeSingle<JsonObject>()
                .let { groupFromDB(it) }
            fetchGroups(creatorId)
            Result.success(group)
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al crear grupo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun joinGroup(
        groupId: String, 
        userId: String, 
        alias: String, 
        avatar: String,
        userDistrict: String? = null,
        quantity: Int = 1,
        userPoints: Int = 0 // Puntos del usuario para validar membresía
    ): Result<Group> {
        return try {
            if (quantity < 1) {
                return Result.failure(Exception("Debes reservar al menos 1 unidad"))
            }
            
            // Validar límite por tier (nuevo sistema: Bronce: 2, Plata: 4, Oro: 6+)
            val maxUnits = when {
                userPoints >= 200 -> 6  // Oro
                userPoints >= 100 -> 4  // Plata
                else -> 2               // Bronce
            }
            
            if (quantity > maxUnits) {
                val levelName = when {
                    userPoints >= 200 -> "Oro"
                    userPoints >= 100 -> "Plata"
                    else -> "Bronce"
                }
                return Result.failure(Exception("Tu nivel $levelName permite máximo $maxUnits unidades por oferta"))
            }
            // Obtener grupo
            val groupData = supabase.from("grupos")
                .select {
                    filter { eq("id", groupId) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            if (groupData == null) {
                return Result.failure(Exception("Grupo no encontrado"))
            }
        
            val group = groupFromDB(groupData)
        
            // Validar estado: solo ACTIVE permite nuevas reservas
            // PICKUP significa que ya se alcanzó la meta, no se permiten más reservas
            if (group.status != GroupStatus.ACTIVE) {
                val errorMsg = when (group.status) {
                    GroupStatus.PICKUP -> "El grupo ya alcanzó la meta y está listo para retiro"
                    GroupStatus.VALIDATED -> "El grupo ya fue finalizado"
                    GroupStatus.EXPIRED -> "El grupo ha expirado"
                    GroupStatus.COMPLETED -> "El grupo ya fue completado"
                    else -> "El grupo ya no está activo"
                }
                return Result.failure(Exception(errorMsg))
            }
        
            // Validar expiración
            if (group.isExpired) {
                return Result.failure(Exception("El grupo ha expirado"))
            }
            
            // Calcular unidades disponibles (target_size - current_size)
            // El trigger actualiza current_size automáticamente, así que usamos el valor de Supabase
            val availableUnits = (group.targetSize - group.currentSize).coerceAtLeast(0)
            if (quantity > availableUnits) {
                return Result.failure(
                    Exception(
                        if (availableUnits == 0) "El grupo ya alcanzó la meta"
                        else "Solo quedan $availableUnits unidades disponibles"
                    )
                )
            }
            
            val existingParticipant = supabase.from("participantes")
                .select {
                    filter {
                        eq("group_id", groupId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<JsonObject>()
            
            val nowIso = longToTimestamp(System.currentTimeMillis())
            val existingUnits = existingParticipant?.let { jsonInt(it, "reserved_units") }?.takeIf { it > 0 } ?: 0
            val isFirstTime = existingParticipant == null // Es primera vez si no existe participante previo
            
            if (existingParticipant != null) {
                val participantId = jsonString(existingParticipant, "id")
                val participantStatus = jsonString(existingParticipant, "status")
                
                // Si el participante está cancelado, no permitir agregar más unidades
                if (participantStatus == "CANCELLED") {
                    return Result.failure(Exception("Tu reserva fue cancelada. No puedes agregar más unidades."))
                }
                
                val updatedUnits = existingUnits + quantity
                val updates = mutableMapOf<String, Any>(
                    "reserved_units" to updatedUnits,
                    "joined_at" to nowIso
                )
                supabase.from("participantes")
                    .update(updates) {
                        filter { eq("id", participantId) }
                    }
            } else {
                // Agregar nuevo participante
                val participant = ParticipantInsert(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId,
                    userId = userId,
                    alias = alias,
                    avatar = avatar,
                    reservedUnits = quantity,
                    joinedAt = nowIso
                )
                supabase.from("participantes").insert(participant)
            }
            
            // IMPORTANTE: NO actualizar current_size ni status manualmente
            // El trigger update_group_current_size() lo hace automáticamente:
            // 1. Actualiza current_size sumando reserved_units de participantes (status != 'CANCELLED')
            // 2. Si current_size >= target_size Y status = 'ACTIVE', cambia a PICKUP y genera qr_code
            
            // Esperar un momento para que el trigger se ejecute, luego recargar el grupo
            kotlinx.coroutines.delay(500) // Pequeño delay para asegurar que el trigger se ejecutó
            val updatedGroup = getGroupById(groupId) ?: group
            fetchGroups(userId)
            Result.success(updatedGroup)
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al unirse al grupo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getGroupById(groupId: String): Group? {
        return try {
            val groupData = supabase.from("grupos")
                .select {
                    filter { eq("id", groupId) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            groupData?.let { groupFromDB(it) }
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            null
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al obtener grupo: ${e.message}", e)
            null
        }
    }
    
    suspend fun fetchGroups(userId: String? = null): List<Group> {
        return try {
            val list: List<Group> = if (userId.isNullOrBlank()) {
                supabase.from("grupos").select().decodeList<JsonObject>()
                    .mapNotNull { groupFromDB(it) }
            } else {
                val created = supabase.from("grupos")
                    .select {
                        filter { eq("creator_id", userId) }
                    }
                    .decodeList<JsonObject>()
                    .mapNotNull { groupFromDB(it) }
                
                val participantIds = supabase.from("participantes")
                    .select(columns = Columns.ALL) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<JsonObject>()
                    .map { jsonString(it, "group_id") }
                    .distinct()
                
                val participantGroups = if (participantIds.isEmpty()) emptyList() else {
                    participantIds.mapNotNull { id -> getGroupById(id) }
                }
                
                (created + participantGroups).distinctBy { it.id }
            }
            
            _groups.value = list
            list
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            _groups.value = emptyList()
            emptyList()
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al obtener grupos: ${e.message}", e)
            _groups.value = emptyList()
            emptyList()
        }
    }
    
    suspend fun getMyGroups(userId: String): List<Group> = fetchGroups(userId)
    
    suspend fun getGroupsByStatus(userId: String, status: GroupStatus): List<Group> {
        return fetchGroups(userId).filter { it.status == status }
    }
    
    /**
     * Obtiene TODOS los grupos activos (ACTIVE y no expirados) de la base de datos
     * Usado para mostrar ofertas de todas las bodegas a los clientes
     * IMPORTANTE: Esta función obtiene grupos de TODAS las bodegas, no solo del usuario
     */
    suspend fun fetchAllActiveGroups(): List<Group> {
        return try {
            val now = System.currentTimeMillis()
            Log.d("GroupRepository", "Obteniendo todos los grupos activos de la BD")
            val groups = supabase.from("grupos")
                .select {
                    filter {
                        eq("status", "ACTIVE")
                    }
                }
                .decodeList<JsonObject>()
                .mapNotNull { groupFromDB(it) }
                .filter { group ->
                    // Filtrar grupos que no han expirado
                    val isActive = group.expiresAt > now && !group.isExpired
                    if (isActive) {
                        Log.d("GroupRepository", "Grupo activo encontrado: ${group.id}, producto: ${group.productId}, current_size: ${group.currentSize}, target_size: ${group.targetSize}")
                    }
                    isActive
                }
            Log.d("GroupRepository", "Total grupos activos obtenidos: ${groups.size}")
            // Actualizar el estado con todos los grupos activos
            val currentGroups = _groups.value.toMutableList()
            // Actualizar o agregar grupos activos
            groups.forEach { activeGroup ->
                val idx = currentGroups.indexOfFirst { it.id == activeGroup.id }
                if (idx >= 0) {
                    currentGroups[idx] = activeGroup
                } else {
                    currentGroups.add(activeGroup)
                }
            }
            _groups.value = currentGroups
            groups
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al obtener todos los grupos activos: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Obtiene grupos activos (ACTIVE y no expirados) de una bodega específica
     * Usado para validar límites de ofertas según el plan
     */
    suspend fun getActiveGroupsByStore(storeId: String): List<Group> {
        return try {
            val now = System.currentTimeMillis()
            supabase.from("grupos")
                .select {
                    filter {
                        eq("store_id", storeId)
                        eq("status", "ACTIVE")
                    }
                }
                .decodeList<JsonObject>()
                .mapNotNull { groupFromDB(it) }
                .filter { group ->
                    // Filtrar grupos que no han expirado
                    group.expiresAt > now && !group.isExpired
                }
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al obtener grupos activos de bodega: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Valida un QR escaneado por el bodeguero
     * Busca el grupo por QR y luego busca el participante del cliente que escaneó
     */
    suspend fun validateQRByCode(qrCode: String): Result<Group> {
        return try {
            // Buscar grupo por QR
            val group = getGroupByQr(qrCode) ?: return Result.failure(Exception("QR no encontrado. Verifica que el código sea correcto."))
            
            // Validar que el grupo esté en estado PICKUP
            if (group.status != GroupStatus.PICKUP) {
                val errorMsg = when (group.status) {
                    GroupStatus.ACTIVE -> "El grupo aún no alcanzó la meta. El QR no está disponible."
                    GroupStatus.VALIDATED -> "El grupo ya fue finalizado. Todos los participantes retiraron."
                    GroupStatus.EXPIRED -> "El grupo expiró sin completar la meta."
                    GroupStatus.COMPLETED -> "El grupo ya fue completado."
                    else -> "El grupo no está listo para validar QR."
                }
                return Result.failure(Exception(errorMsg))
            }
            
            Result.success(group)
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al validar QR: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Valida un participante específico (usado cuando el bodeguero escanea el QR del cliente)
     * El qrCode debe ser el código del grupo, y luego se busca el participante por userId
     * Retorna el userId del participante validado para otorgar puntos
     */
    suspend fun validateParticipantByQR(qrCode: String, userId: String): Result<String> {
        return try {
            // Buscar grupo por QR
            val group = getGroupByQr(qrCode) ?: return Result.failure(Exception("QR no encontrado"))
            
            // Validar que el grupo esté en PICKUP
            if (group.status != GroupStatus.PICKUP) {
                return Result.failure(Exception("El grupo no está listo para validar. Estado: ${group.status}"))
            }
            
            // Buscar participante del usuario en este grupo
            val participantData = supabase.from("participantes")
                .select {
                    filter {
                        eq("group_id", group.id)
                        eq("user_id", userId)
                        neq("status", "CANCELLED")
                    }
                }
                .decodeSingleOrNull<JsonObject>()
            
            if (participantData == null) {
                return Result.failure(Exception("No se encontró una reserva válida para este usuario en este grupo"))
            }
            
            val participantId = jsonString(participantData, "id")
            
            // Validar que no esté ya validado
            if (jsonBool(participantData, "is_validated")) {
                return Result.failure(Exception("Este participante ya fue validado anteriormente"))
            }
            
            // Validar el participante (actualiza is_validated y status)
            // Retornar userId para otorgar puntos
            validateParticipant(participantId)
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al validar participante por QR: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun getGroupByQr(qrCode: String): Group? {
        return try {
            val groupData = supabase.from("grupos")
                .select {
                    filter { eq("qr_code", qrCode) }
                }
                .decodeSingleOrNull<JsonObject>()
            groupData?.let { groupFromDB(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun validateGroup(groupIdOrQr: String): Result<Group> {
        return try {
            val group = getGroupById(groupIdOrQr) ?: getGroupByQr(groupIdOrQr)
                ?: return Result.failure(Exception("Grupo no encontrado"))
            
            // Solo validar grupos que están en estado PICKUP
            if (group.status != GroupStatus.PICKUP) {
                return Result.failure(Exception("El grupo no está listo para validar"))
            }
            
            // Marcar participante como validado (esto se hace por participante, no por grupo)
            // El trigger de la BD cambiará el grupo a VALIDATED cuando todos estén validados
            
            Result.success(group)
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al validar grupo: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun validateParticipant(participantId: String): Result<String> {
        return try {
            // Obtener el participante primero para validar
            val participantData = supabase.from("participantes")
                .select {
                    filter { eq("id", participantId) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            if (participantData == null) {
                return Result.failure(Exception("Participante no encontrado"))
            }
            
            val userId = jsonString(participantData, "user_id")
            val groupId = jsonString(participantData, "group_id")
            val group = getGroupById(groupId)
            
            // Validar que el grupo esté en PICKUP
            if (group?.status != GroupStatus.PICKUP) {
                return Result.failure(Exception("El grupo no está listo para validar. Debe estar en estado PICKUP."))
            }
            
            // Validar que el participante no esté ya validado
            if (jsonBool(participantData, "is_validated")) {
                return Result.failure(Exception("Este participante ya fue validado"))
            }
            
            val now = longToTimestamp(System.currentTimeMillis())
            // Actualizar is_validated Y status a VALIDATED
            // El trigger check_group_completion verificará si todos están validados
            // y cambiará el grupo a VALIDATED si corresponde
            supabase.from("participantes")
                .update(mapOf(
                    "is_validated" to true,
                    "validated_at" to now,
                    "status" to "VALIDATED"
                )) {
                    filter { eq("id", participantId) }
                }
            
            // El trigger check_group_completion actualizará el grupo a VALIDATED
            // cuando todos los participantes tengan status = 'VALIDATED'
            // Retornar userId para otorgar puntos
            Result.success(userId)
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al validar participante: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun expireGroups() {
        // Los grupos se expiran automáticamente con el trigger de la BD
        // Esta función puede usarse para forzar expiración si es necesario
        try {
            val now = longToTimestamp(System.currentTimeMillis())
            supabase.from("grupos")
                .update(mapOf("status" to "EXPIRED")) {
                    filter {
                        eq("status", "ACTIVE")
                        lt("expires_at", now)
                    }
                }
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado al expirar grupos", e)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al expirar grupos: ${e.message}", e)
        }
    }
    
    suspend fun getGroupQR(groupId: String, userId: String): Result<String> {
        return try {
            val group = getGroupById(groupId) ?: return Result.failure(Exception("Grupo no encontrado"))
            
            // Validar que el usuario esté en el grupo
            val participant = supabase.from("participantes")
                .select {
                    filter {
                        eq("group_id", groupId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<Participant>()
            
            if (participant == null) {
                return Result.failure(Exception("No eres parte de este grupo"))
            }
            
            // QR solo disponible cuando estado es PICKUP (según requisitos)
            // VALIDATED significa que todos ya retiraron, no se debe mostrar QR
            if (group.status != GroupStatus.PICKUP) {
                val errorMsg = when (group.status) {
                    GroupStatus.ACTIVE -> "El QR aún no está disponible. La meta no se ha completado."
                    GroupStatus.VALIDATED -> "El grupo ya fue finalizado. Todos los participantes retiraron."
                    GroupStatus.EXPIRED -> "El grupo expiró sin completar la meta."
                    GroupStatus.COMPLETED -> "El grupo ya fue completado."
                    else -> "El QR no está disponible para este estado."
                }
                return Result.failure(Exception(errorMsg))
            }
            
            if (group.qrCode.isBlank()) {
                return Result.failure(Exception("QR no generado"))
            }
            
            Result.success(group.qrCode)
        } catch (e: IllegalStateException) {
            Log.e("GroupRepository", "Supabase no inicializado", e)
            Result.failure(Exception("Error de conexión. Intenta nuevamente."))
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error al obtener QR: ${e.message}", e)
            Result.failure(e)
        }
    }
}
