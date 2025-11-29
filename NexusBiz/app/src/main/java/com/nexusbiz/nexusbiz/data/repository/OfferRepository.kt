package com.nexusbiz.nexusbiz.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nexusbiz.nexusbiz.data.model.Offer
import com.nexusbiz.nexusbiz.data.model.OfferStatus
import com.nexusbiz.nexusbiz.data.model.Reservation
import com.nexusbiz.nexusbiz.data.model.ReservationStatus
import com.nexusbiz.nexusbiz.data.model.GamificationLevel
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import com.nexusbiz.nexusbiz.data.remote.SupabaseStorage
import com.nexusbiz.nexusbiz.data.remote.model.Offer as RemoteOffer
import com.nexusbiz.nexusbiz.data.remote.model.Reservation as RemoteReservation
import com.nexusbiz.nexusbiz.data.remote.model.OfferStatus as RemoteOfferStatus
import com.nexusbiz.nexusbiz.data.remote.model.ReservationStatusRemote
import com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel as RemoteGamificationLevel
import com.nexusbiz.nexusbiz.service.RealtimeService
import com.nexusbiz.nexusbiz.service.RealtimeEventType
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Repositorio que maneja ofertas y reservas, integrado con Supabase Realtime.
 * 
 * Este repositorio:
 * - Mantiene StateFlows actualizados automáticamente cuando hay cambios en la BD
 * - Escucha eventos de RealtimeService para ofertas y reservas
 * - Actualiza las listas localmente cuando llegan eventos (INSERT, UPDATE, DELETE)
 * - Mantiene compatibilidad con métodos existentes de fetch manual
 * 
 * Cómo funciona la sincronización en tiempo real:
 * 1. RealtimeService escucha cambios en tablas "ofertas" y "reservas"
 * 2. Cuando hay un cambio, RealtimeService emite un evento
 * 3. Este repositorio recibe el evento y actualiza el StateFlow correspondiente
 * 4. Los ViewModels que observan estos StateFlows se actualizan automáticamente
 * 5. Las pantallas se recomponen y las cards se mueven entre secciones según el estado
 */
class OfferRepository {
    private val supabase: io.github.jan.supabase.SupabaseClient
        get() = SupabaseManager.client
    private val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val offers: StateFlow<List<Offer>> = _offers.asStateFlow()
    
    // StateFlow para reservas (nuevo)
    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()
    
    // Scope para corrutinas de Realtime
    private val realtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Flag para saber si ya se inició la escucha de eventos
    private var isListeningToRealtime = false
    
    private fun longToTimestamp(millis: Long): String {
        return java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toOffsetDateTime()
            .toString()
    }
    
    private fun offerFromRemote(remote: RemoteOffer): Offer {
        return Offer(
            id = remote.id,
            productName = remote.productName,
            productKey = remote.productKey,
            description = remote.description,
            imageUrl = remote.imageUrl,
            normalPrice = remote.normalPrice,
            groupPrice = remote.groupPrice,
            targetUnits = remote.targetUnits,
            reservedUnits = remote.reservedUnits,
            validatedUnits = remote.validatedUnits,
            storeId = remote.storeId,
            storeName = remote.storeName,
            district = remote.district,
            latitude = remote.latitude,
            longitude = remote.longitude,
            pickupAddress = remote.pickupAddress,
            status = when (remote.status) {
                RemoteOfferStatus.ACTIVE -> OfferStatus.ACTIVE
                RemoteOfferStatus.PICKUP -> OfferStatus.PICKUP
                RemoteOfferStatus.COMPLETED -> OfferStatus.COMPLETED
                RemoteOfferStatus.EXPIRED -> OfferStatus.EXPIRED
            },
            durationHours = remote.durationHours,
            createdAt = remote.createdAt,
            expiresAt = remote.expiresAt,
            updatedAt = remote.updatedAt
        )
    }

    /**
     * Sube la imagen de una oferta a Supabase Storage y devuelve la URL pública.
     *
     * @param context Contexto necesario para leer el archivo desde la URI local
     * @param imageUri URI local de la imagen (content:// o file://)
     * @param offerId ID de la oferta para nombrar el archivo de forma determinista
     */
    suspend fun uploadOfferImage(
        context: Context,
        imageUri: Uri,
        offerId: String
    ): String? {
        return SupabaseStorage.uploadPublicImage(
            context = context,
            imageUri = imageUri,
            pathBuilder = { extension ->
                // Guardar imágenes de ofertas en una carpeta dedicada
                "offers/$offerId.$extension"
            }
        )
    }
    
    suspend fun fetchAllActiveOffers(district: String? = null): List<Offer> {
        return try {
            Log.d("OfferRepository", "Obteniendo ofertas activas para distrito: $district")
            val remoteOffers = supabase.from("ofertas")
                .select {
                    filter {
                        eq("status", "ACTIVE")
                        if (district != null && district.isNotBlank()) {
                            eq("district", district)
                        }
                    }
                }
                .decodeList<RemoteOffer>()
            
            val offers = remoteOffers
                .map { offerFromRemote(it) }
                .filter { offer ->
                    val isActive = !offer.isExpired && offer.status == OfferStatus.ACTIVE
                    if (isActive) {
                        Log.d("OfferRepository", "Oferta activa: ${offer.id}, producto: ${offer.productName}, distrito: ${offer.district}, reservadas: ${offer.reservedUnits}/${offer.targetUnits}, expira: ${offer.expiresAt}")
                    } else {
                        Log.d("OfferRepository", "Oferta filtrada: ${offer.id}, producto: ${offer.productName}, status: ${offer.status}, expirada: ${offer.isExpired}, distrito: ${offer.district}")
                    }
                    isActive
                }
            Log.d("OfferRepository", "Total ofertas activas obtenidas: ${offers.size} para distrito: $district")
            _offers.value = offers
            offers
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al obtener ofertas activas: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Obtiene una oferta por su ID, independientemente de su estado.
     * Usado para obtener ofertas donde el usuario tiene reservas.
     * 
     * IMPORTANTE: No filtra por estado, obtiene ofertas en cualquier estado
     * (ACTIVE, PICKUP, COMPLETED, EXPIRED) para que aparezcan en "Mis Grupos".
     */
    suspend fun getOfferById(offerId: String): Offer? {
        return try {
            Log.d("OfferRepository", "Obteniendo oferta por ID: $offerId")
            val remoteOffer = supabase.from("ofertas")
                .select {
                    filter { eq("id", offerId) }
                }
                .decodeSingleOrNull<RemoteOffer>()
            
            if (remoteOffer != null) {
                val offer = offerFromRemote(remoteOffer)
                Log.d("OfferRepository", "Oferta obtenida: id=${offer.id}, producto=${offer.productName}, status=${offer.status}, reserved_units=${offer.reservedUnits}/${offer.targetUnits}")
                offer
            } else {
                Log.w("OfferRepository", "Oferta no encontrada en BD: $offerId")
                null
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Cancelación normal cuando el scope de Compose (rememberCoroutineScope, etc.) sale de composición.
            // No es un error real de Supabase; re-lanzamos la excepción para que la corrutina se cancele
            // sin marcar la oferta como "no encontrada".
            Log.d("OfferRepository", "getOfferById cancelado por salida de composición: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al obtener oferta $offerId: ${e.message}", e)
            Log.e("OfferRepository", "Stack trace: ${e.stackTraceToString()}")
            null
        }
    }
    
    suspend fun createOffer(
        productName: String,
        description: String,
        imageUrl: String,
        normalPrice: Double,
        groupPrice: Double,
        targetUnits: Int,
        storeId: String,
        storeName: String,
        district: String,
        pickupAddress: String,
        durationHours: Int = 24,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<Offer> {
        @Serializable
        data class OfferInsert(
            @SerialName("id") val id: String,
            @SerialName("product_name") val productName: String,
            @SerialName("product_key") val productKey: String,
            @SerialName("description") val description: String? = null,
            @SerialName("image_url") val imageUrl: String? = null,
            @SerialName("normal_price") val normalPrice: Double,
            @SerialName("group_price") val groupPrice: Double,
            @SerialName("target_units") val targetUnits: Int,
            @SerialName("reserved_units") val reservedUnits: Int = 0,
            @SerialName("validated_units") val validatedUnits: Int = 0,
            @SerialName("store_id") val storeId: String,
            @SerialName("store_name") val storeName: String,
            @SerialName("district") val district: String,
            @SerialName("latitude") val latitude: Double? = null,
            @SerialName("longitude") val longitude: Double? = null,
            @SerialName("pickup_address") val pickupAddress: String,
            @SerialName("status") val status: RemoteOfferStatus,
            @SerialName("duration_hours") val durationHours: Int,
            @SerialName("expires_at") val expiresAt: String
        )
        
        return try {
            if (targetUnits < 1) {
                return Result.failure(Exception("La meta debe ser al menos 1 unidad"))
            }
            if (durationHours !in listOf(4, 8, 12, 24)) {
                return Result.failure(Exception("Duración debe ser 4, 8, 12 o 24 horas"))
            }
            if (normalPrice <= 0 || groupPrice <= 0) {
                return Result.failure(Exception("Los precios deben ser mayores a 0"))
            }
            if (groupPrice >= normalPrice) {
                return Result.failure(Exception("El precio grupal debe ser menor al precio normal"))
            }
            
            val offerId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val expiresAt = now + (durationHours * 60 * 60 * 1000L)
            
            // Crear objeto OfferInsert serializable para la inserción
            // Usamos una clase específica para garantizar que "target_units" SIEMPRE se envíe a Supabase
            val offerInsert = OfferInsert(
                id = offerId,
                productName = productName,
                productKey = productName.lowercase().trim(),
                description = description.takeIf { it.isNotBlank() },
                imageUrl = imageUrl.takeIf { it.isNotBlank() },
                normalPrice = normalPrice,
                groupPrice = groupPrice,
                targetUnits = targetUnits,
                reservedUnits = 0,
                validatedUnits = 0,
                storeId = storeId,
                storeName = storeName,
                district = district,
                latitude = latitude,
                longitude = longitude,
                pickupAddress = pickupAddress,
                status = RemoteOfferStatus.ACTIVE,
                durationHours = durationHours,
                expiresAt = longToTimestamp(expiresAt)
            )
            
            Log.d("OfferRepository", "Creando oferta: $offerId con target_units=$targetUnits")
            supabase.from("ofertas").insert(offerInsert)
            
            val offer = getOfferById(offerId)
            if (offer != null) {
                fetchAllActiveOffers(district)
                Result.success(offer)
            } else {
                Result.failure(Exception("Error al crear la oferta"))
            }
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al crear oferta: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Crea una reserva para una oferta.
     * 
     * Validaciones realizadas:
     * 1. Límites por nivel de gamificación (BRONCE: 2, PLATA: 4, ORO: 6)
     * 2. Oferta existe y está activa
     * 3. Oferta no ha expirado
     * 4. Unidades disponibles suficientes
     * 5. Usuario no tiene ya una reserva para esta oferta (UNIQUE constraint)
     * 
     * Después de crear la reserva:
     * - El trigger de Supabase actualiza reserved_units automáticamente
     * - Si reserved_units >= target_units, el trigger cambia el estado a PICKUP
     * - RealtimeService detectará el cambio y actualizará la UI automáticamente
     */
    suspend fun createReservation(
        offerId: String,
        userId: String,
        units: Int,
        userLevel: GamificationLevel
    ): Result<Reservation> {
        return try {
            // Validar límites por nivel de gamificación según el esquema SQL
            val maxUnitsByLevel = when (userLevel) {
                GamificationLevel.BRONCE -> 2
                GamificationLevel.PLATA -> 4
                GamificationLevel.ORO -> 6
            }
            
            if (units < 1) {
                return Result.failure(Exception("Debes reservar al menos 1 unidad"))
            }
            
            if (units > maxUnitsByLevel) {
                val levelName = when (userLevel) {
                    GamificationLevel.BRONCE -> "BRONCE"
                    GamificationLevel.PLATA -> "PLATA"
                    GamificationLevel.ORO -> "ORO"
                }
                return Result.failure(
                    Exception("Tu nivel $levelName permite máximo $maxUnitsByLevel unidades por reserva")
                )
            }
            
            // Obtener la oferta más reciente de la BD para tener datos actualizados
            // Esto es importante porque reserved_units puede haber cambiado desde la última carga
            val offer = getOfferById(offerId) ?: return Result.failure(Exception("Oferta no encontrada"))
            
            // Validar estado de la oferta
            if (offer.status != OfferStatus.ACTIVE) {
                val statusMessage = when (offer.status) {
                    OfferStatus.PICKUP -> "La oferta ya alcanzó la meta y está en retiro"
                    OfferStatus.COMPLETED -> "La oferta ya fue completada"
                    OfferStatus.EXPIRED -> "La oferta ha expirado"
                    else -> "La oferta no está activa"
                }
                return Result.failure(Exception(statusMessage))
            }
            
            // Validar expiración
            if (offer.isExpired) {
                return Result.failure(Exception("La oferta ha expirado"))
            }
            
            // Validar unidades disponibles
            val availableUnits = (offer.targetUnits - offer.reservedUnits).coerceAtLeast(0)
            if (units > availableUnits) {
                return Result.failure(
                    Exception(
                        if (availableUnits == 0) "La oferta ya alcanzó la meta"
                        else "Solo quedan $availableUnits unidades disponibles"
                    )
                )
            }
            
            // Verificar si el usuario ya tiene una reserva para esta oferta (UNIQUE constraint)
            val existingReservation = supabase.from("reservas")
                .select {
                    filter {
                        eq("offer_id", offerId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<RemoteReservation>()
            
            if (existingReservation != null && existingReservation.status != ReservationStatusRemote.CANCELLED) {
                return Result.failure(Exception("Ya tienes una reserva activa para esta oferta"))
            }
            
            val totalPrice = offer.groupPrice * units
            val reservationId = UUID.randomUUID().toString()
            
            // Mapear GamificationLevel local a RemoteGamificationLevel
            val remoteLevel = when (userLevel) {
                GamificationLevel.BRONCE -> RemoteGamificationLevel.BRONCE
                GamificationLevel.PLATA -> RemoteGamificationLevel.PLATA
                GamificationLevel.ORO -> RemoteGamificationLevel.ORO
            }
            
            // Crear objeto Reservation serializable para la inserción
            // Solo incluir campos que existen en la tabla reservas (no campos de vista)
            @kotlinx.serialization.Serializable
            data class ReservationInsert(
                @SerialName("id") val id: String,
                @SerialName("offer_id") val offerId: String,
                @SerialName("user_id") val userId: String,
                @SerialName("units") val units: Int,
                @SerialName("total_price") val totalPrice: Double,
                @SerialName("level_snapshot") val levelSnapshot: RemoteGamificationLevel,
                @SerialName("status") val status: ReservationStatusRemote
            )
            
            val reservationInsert = ReservationInsert(
                id = reservationId,
                offerId = offerId,
                userId = userId,
                units = units,
                totalPrice = totalPrice,
                levelSnapshot = remoteLevel,
                status = ReservationStatusRemote.RESERVED
            )
            
            supabase.from("reservas").insert(reservationInsert)
            
            // El trigger actualizará reserved_units automáticamente
            // Esperar un momento para que el trigger se ejecute
            kotlinx.coroutines.delay(500)
            
            // Actualizar la oferta en el StateFlow para reflejar el cambio en reserved_units
            // Esto permite que la UI se actualice inmediatamente sin esperar a Realtime
            val updatedOffer = getOfferById(offerId)
            updatedOffer?.let {
                val currentOffers = _offers.value.toMutableList()
                val index = currentOffers.indexOfFirst { o -> o.id == offerId }
                if (index >= 0) {
                    currentOffers[index] = it
                    _offers.value = currentOffers
                    Log.d("OfferRepository", "Oferta actualizada después de reserva: ${it.id}, reserved_units: ${it.reservedUnits}/${it.targetUnits}")
                } else {
                    // CORRECCIÓN: Si la oferta no está en la lista, agregarla
                    // Esto es importante para que aparezca en "Mis Grupos" del cliente
                    currentOffers.add(it)
                    _offers.value = currentOffers
                    Log.d("OfferRepository", "Oferta agregada después de reserva: ${it.id}, reserved_units: ${it.reservedUnits}/${it.targetUnits}")
                }
            }
            
            val reservation = Reservation(
                id = reservationId,
                offerId = offerId,
                userId = userId,
                units = units,
                totalPrice = totalPrice,
                levelSnapshot = userLevel,
                status = ReservationStatus.RESERVED
            )
            
            // CORRECCIÓN: Actualizar el StateFlow de reservas para que aparezca inmediatamente en "Mis Grupos"
            val currentReservations = _reservations.value.toMutableList()
            val existingIndex = currentReservations.indexOfFirst { it.id == reservationId }
            if (existingIndex >= 0) {
                currentReservations[existingIndex] = reservation
            } else {
                currentReservations.add(reservation)
            }
            _reservations.value = currentReservations
            Log.d("OfferRepository", "Reserva agregada al StateFlow: ${reservation.id}, status: ${reservation.status}")
            
            Result.success(reservation)
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al crear reserva: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getReservationsByOffer(offerId: String): List<Reservation> {
        return try {
            val remoteReservations = supabase.from("reservas_completas")
                .select {
                    filter { eq("offer_id", offerId) }
                }
                .decodeList<RemoteReservation>()
            
            remoteReservations.map { remote ->
                reservationFromRemote(remote)
            }
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al obtener reservas: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Valida una reserva (cambia de RESERVED a VALIDATED).
     * Usado cuando el cliente valida su propia reserva.
     * 
     * Este método:
     * - Actualiza el status de la reserva a VALIDATED
     * - El trigger de Supabase actualiza validated_units automáticamente
     * - Si validated_units >= target_units, el trigger cambia el estado de la oferta a COMPLETED
     * - RealtimeService detectará el cambio y actualizará la UI automáticamente
     * - Actualiza el StateFlow de ofertas para reflejar los cambios inmediatamente
     */
    suspend fun validateReservation(reservationId: String, userId: String): Result<String> {
        return try {
            // Obtener la reserva para saber qué oferta actualizar
            val reservation = supabase.from("reservas_completas")
                .select {
                    filter {
                        eq("id", reservationId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<RemoteReservation>()
            
            if (reservation == null) {
                return Result.failure(Exception("Reserva no encontrada"))
            }
            
            val offerId = reservation.offerId
            
            // Actualizar el status de la reserva
            supabase.from("reservas")
                .update(mapOf(
                    "status" to "VALIDATED",
                    "validated_at" to longToTimestamp(System.currentTimeMillis())
                )) {
                    filter {
                        eq("id", reservationId)
                        eq("user_id", userId)
                    }
                }
            
            // El trigger actualizará validated_units y puede cambiar el estado de la oferta
            // Esperar un momento para que el trigger se ejecute
            kotlinx.coroutines.delay(500)
            
            // Actualizar la oferta en el StateFlow para reflejar los cambios
            val updatedOffer = getOfferById(offerId)
            updatedOffer?.let {
                val currentOffers = _offers.value.toMutableList()
                val index = currentOffers.indexOfFirst { o -> o.id == offerId }
                if (index >= 0) {
                    currentOffers[index] = it
                    _offers.value = currentOffers
                    Log.d("OfferRepository", "Oferta actualizada después de validar reserva: ${it.id}, status: ${it.status}, validated_units: ${it.validatedUnits}")
                }
            }
            
            Result.success(userId)
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al validar reserva: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Valida una reserva desde el lado del bodeguero (escaneando QR del cliente).
     * El QR contiene el ID de la reserva, y se valida que pertenezca a una oferta de la bodega.
     * 
     * Este método:
     * - Busca la reserva solo por su ID (sin filtrar por user_id del cliente)
     * - Verifica que la reserva pertenezca a una oferta de la bodega del bodeguero
     * - Actualiza el status de la reserva a VALIDATED
     * - El trigger de Supabase actualiza validated_units automáticamente
     * - Si validated_units >= target_units, el trigger cambia el estado de la oferta a COMPLETED
     * - RealtimeService detectará el cambio y actualizará la UI automáticamente
     * - Actualiza el StateFlow de ofertas para reflejar los cambios inmediatamente
     */
    suspend fun validateReservationByStore(reservationId: String, storeId: String): Result<String> {
        return try {
            Log.d("OfferRepository", "Validando reserva desde bodega: reservationId=$reservationId, storeId=$storeId")
            
            // Obtener la reserva solo por su ID (sin filtrar por user_id)
            val reservation = supabase.from("reservas_completas")
                .select {
                    filter {
                        eq("id", reservationId)
                    }
                }
                .decodeSingleOrNull<RemoteReservation>()
            
            if (reservation == null) {
                Log.e("OfferRepository", "Reserva no encontrada: $reservationId")
                return Result.failure(Exception("Reserva no encontrada"))
            }
            
            Log.d("OfferRepository", "Reserva encontrada: id=${reservation.id}, offer_id=${reservation.offerId}, user_id=${reservation.userId}, status=${reservation.status}")
            
            // Verificar que la reserva pertenezca a una oferta de esta bodega
            val offer = getOfferById(reservation.offerId)
            if (offer == null) {
                Log.e("OfferRepository", "Oferta no encontrada para reserva: ${reservation.offerId}")
                return Result.failure(Exception("Oferta no encontrada"))
            }
            
            if (offer.storeId != storeId) {
                Log.e("OfferRepository", "La reserva no pertenece a una oferta de esta bodega. storeId de oferta: ${offer.storeId}, storeId del bodeguero: $storeId")
                return Result.failure(Exception("Esta reserva no pertenece a una oferta de tu bodega"))
            }
            
            // Verificar que la reserva esté en estado RESERVED
            if (reservation.status != ReservationStatusRemote.RESERVED) {
                val statusMsg = when (reservation.status) {
                    ReservationStatusRemote.VALIDATED -> "Esta reserva ya fue validada"
                    ReservationStatusRemote.EXPIRED -> "Esta reserva expiró"
                    ReservationStatusRemote.CANCELLED -> "Esta reserva fue cancelada"
                    else -> "La reserva no está en estado válido para validar"
                }
                Log.e("OfferRepository", "Reserva no está en estado RESERVED: ${reservation.status}")
                return Result.failure(Exception(statusMsg))
            }
            
            // Actualizar el status de la reserva (sin filtrar por user_id, porque el bodeguero valida)
            supabase.from("reservas")
                .update(mapOf(
                    "status" to "VALIDATED",
                    "validated_at" to longToTimestamp(System.currentTimeMillis())
                )) {
                    filter {
                        eq("id", reservationId)
                    }
                }
            
            Log.d("OfferRepository", "Reserva actualizada a VALIDATED: $reservationId")
            
            // El trigger actualizará validated_units y puede cambiar el estado de la oferta
            // Esperar un momento para que el trigger se ejecute
            kotlinx.coroutines.delay(500)
            
            // Actualizar la oferta en el StateFlow para reflejar los cambios
            val updatedOffer = getOfferById(reservation.offerId)
            updatedOffer?.let {
                val currentOffers = _offers.value.toMutableList()
                val index = currentOffers.indexOfFirst { o -> o.id == reservation.offerId }
                if (index >= 0) {
                    currentOffers[index] = it
                    _offers.value = currentOffers
                    Log.d("OfferRepository", "Oferta actualizada después de validar reserva: ${it.id}, status: ${it.status}, validated_units: ${it.validatedUnits}")
                }
            }
            
            // Actualizar también el StateFlow de reservas
            val currentReservations = _reservations.value.toMutableList()
            val reservationIndex = currentReservations.indexOfFirst { it.id == reservationId }
            if (reservationIndex >= 0) {
                currentReservations[reservationIndex] = currentReservations[reservationIndex].copy(
                    status = ReservationStatus.VALIDATED,
                    validatedAt = longToTimestamp(System.currentTimeMillis())
                )
                _reservations.value = currentReservations
                Log.d("OfferRepository", "Reserva actualizada en StateFlow: $reservationId")
            }
            
            Result.success(reservation.userId)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Esta excepción ocurre cuando el scope de Compose (rememberCoroutineScope) sale de composición.
            // No es un error real de Supabase, así que la propagamos para que se cancele la corrutina
            // sin mostrar un mensaje de error al usuario.
            Log.d("OfferRepository", "Validación de reserva cancelada (scope de Compose destruido): ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al validar reserva desde bodega: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun fetchOffersByStore(storeId: String): List<Offer> {
        return try {
            Log.d("OfferRepository", "Obteniendo ofertas para bodega: $storeId")
            val remoteOffers = supabase.from("ofertas")
                .select {
                    filter { eq("store_id", storeId) }
                }
                .decodeList<RemoteOffer>()
            
            val offers = remoteOffers.map { offerFromRemote(it) }
            Log.d("OfferRepository", "Ofertas encontradas para bodega $storeId: ${offers.size}")
            offers.forEach { offer ->
                Log.d("OfferRepository", "Oferta: ${offer.id}, producto: ${offer.productName}, status: ${offer.status}, expirada: ${offer.isExpired}")
            }
            offers
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al obtener ofertas de la bodega: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Obtiene todas las reservas del usuario, independientemente de su estado.
     * Usado en "Mis Grupos" para mostrar todas las ofertas donde el usuario tiene reservas.
     * 
     * IMPORTANTE: No filtra por estado, obtiene todas las reservas (RESERVED, VALIDATED, EXPIRED, CANCELLED)
     * para que el usuario pueda ver todas sus ofertas en "Mis Grupos".
     */
    suspend fun fetchReservationsByUser(userId: String): List<Reservation> {
        return try {
            Log.d("OfferRepository", "Obteniendo reservas para usuario: $userId")
            val remoteReservations = supabase.from("reservas_completas")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<RemoteReservation>()
            
            Log.d("OfferRepository", "Reservas encontradas en BD: ${remoteReservations.size}")
            remoteReservations.forEach { remote ->
                Log.d("OfferRepository", "Reserva: id=${remote.id}, offer_id=${remote.offerId}, status=${remote.status}, units=${remote.units}")
            }
            
            val reservations = remoteReservations.map { remote ->
                reservationFromRemote(remote)
            }
            
            // Actualizar StateFlow
            _reservations.value = reservations
            
            Log.d("OfferRepository", "Total reservas mapeadas: ${reservations.size}")
            reservations
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al obtener reservas del usuario: ${e.message}", e)
            Log.e("OfferRepository", "Stack trace: ${e.stackTraceToString()}")
            emptyList()
        }
    }
    
    /**
     * Convierte un RemoteReservation a Reservation local.
     * Usado tanto para fetch manual como para eventos Realtime.
     */
    private fun reservationFromRemote(remote: RemoteReservation): Reservation {
        return Reservation(
            id = remote.id,
            offerId = remote.offerId,
            userId = remote.userId,
            units = remote.units,
            totalPrice = remote.totalPrice,
            levelSnapshot = when (remote.levelSnapshot) {
                RemoteGamificationLevel.BRONCE -> GamificationLevel.BRONCE
                RemoteGamificationLevel.PLATA -> GamificationLevel.PLATA
                RemoteGamificationLevel.ORO -> GamificationLevel.ORO
            },
            status = when (remote.status) {
                ReservationStatusRemote.RESERVED -> ReservationStatus.RESERVED
                ReservationStatusRemote.VALIDATED -> ReservationStatus.VALIDATED
                ReservationStatusRemote.EXPIRED -> ReservationStatus.EXPIRED
                ReservationStatusRemote.CANCELLED -> ReservationStatus.CANCELLED
            },
            reservedAt = remote.reservedAt,
            validatedAt = remote.validatedAt,
            userAlias = remote.userAlias,
            userAvatar = remote.userAvatar,
            productName = remote.productName,
            productImage = remote.productImage,
            storeName = remote.storeName,
            pickupAddress = remote.pickupAddress
        )
    }
    
    /**
     * Inicia la suscripción a actualizaciones en tiempo real.
     * 
     * Este método:
     * - Configura filtros en RealtimeService según el contexto (district, storeId, userId)
     * - Inicia la escucha de eventos de ofertas y reservas
     * - Actualiza automáticamente los StateFlows cuando hay cambios
     * 
     * @param district Filtro para ofertas por distrito (modo cliente)
     * @param storeId Filtro para ofertas por bodega (modo bodeguero)
     * @param userId Filtro para reservas del usuario actual
     */
    suspend fun startRealtimeSubscription(
        district: String? = null,
        storeId: String? = null,
        userId: String? = null
    ) {
        try {
            Log.d("OfferRepository", "Iniciando suscripción Realtime - district: $district, storeId: $storeId, userId: $userId")
            
            // Configurar filtros en RealtimeService
            if (district != null && district.isNotBlank()) {
                RealtimeService.addOfferFilterByDistrict(district)
            }
            if (storeId != null && storeId.isNotBlank()) {
                RealtimeService.addOfferFilterByStoreId(storeId)
            }
            if (userId != null && userId.isNotBlank()) {
                RealtimeService.addReservationFilterByUserId(userId)
            }
            
            // Iniciar escucha de eventos si aún no se ha iniciado
            if (!isListeningToRealtime) {
                startListeningToRealtimeEvents()
                isListeningToRealtime = true
            }
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al iniciar suscripción Realtime: ${e.message}", e)
        }
    }
    
    /**
     * Detiene la suscripción a actualizaciones en tiempo real.
     * Limpia los filtros activos.
     */
    fun stopRealtimeSubscription() {
        try {
            Log.d("OfferRepository", "Deteniendo suscripción Realtime")
            RealtimeService.clearOfferFilters()
            RealtimeService.clearReservationFilters()
            // No detenemos la escucha completa, solo limpiamos filtros
            // La conexión base se mantiene activa durante toda la sesión
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al detener suscripción Realtime: ${e.message}", e)
        }
    }
    
    /**
     * Inicia la escucha de eventos de RealtimeService.
     * Este método se llama una sola vez y mantiene la escucha activa.
     * 
     * Cómo funciona:
     * - Escucha eventos de ofertas: cuando hay INSERT/UPDATE/DELETE, actualiza _offers
     * - Escucha eventos de reservas: cuando hay INSERT/UPDATE/DELETE, actualiza _reservations
     * - Los eventos se procesan en el scope de Realtime para no bloquear el hilo principal
     */
    private fun startListeningToRealtimeEvents() {
        // Escuchar eventos de ofertas
        RealtimeService.offerEvents
            .onEach { event ->
                if (event != null) {
                    handleOfferRealtimeEvent(event)
                }
            }
            .launchIn(realtimeScope)
        
        // Escuchar eventos de reservas
        RealtimeService.reservationEvents
            .onEach { event ->
                if (event != null) {
                    handleReservationRealtimeEvent(event)
                }
            }
            .launchIn(realtimeScope)
        
        Log.d("OfferRepository", "Escucha de eventos Realtime iniciada")
    }
    
    /**
     * Maneja eventos de ofertas en tiempo real.
     * 
     * Eventos:
     * - INSERT: Agrega la nueva oferta a la lista
     * - UPDATE: Actualiza la oferta existente en la lista
     * - DELETE: Remueve la oferta de la lista
     * 
     * Después de actualizar, el StateFlow se actualiza automáticamente y las pantallas
     * que observan este StateFlow se recomponen, moviendo las cards entre secciones según el estado.
     */
    private fun handleOfferRealtimeEvent(event: com.nexusbiz.nexusbiz.service.OfferRealtimeEvent) {
        try {
            val currentOffers = _offers.value.toMutableList()
            
            when (event.eventType) {
                RealtimeEventType.INSERT -> {
                    // INSERT: Agregar nueva oferta
                    event.offer?.let { remoteOffer ->
                        val offer = offerFromRemote(remoteOffer)
                        if (!currentOffers.any { it.id == offer.id }) {
                            currentOffers.add(offer)
                            _offers.value = currentOffers
                            Log.d("OfferRepository", "Oferta agregada en tiempo real: ${offer.id}, status: ${offer.status}")
                        }
                    }
                }
                RealtimeEventType.UPDATE -> {
                    // UPDATE: Actualizar oferta existente o agregarla si no existe
                    event.offer?.let { remoteOffer ->
                        val offer = offerFromRemote(remoteOffer)
                        val index = currentOffers.indexOfFirst { it.id == offer.id }
                        if (index >= 0) {
                            currentOffers[index] = offer
                            _offers.value = currentOffers
                            Log.d("OfferRepository", "Oferta actualizada en tiempo real: ${offer.id}, status: ${offer.status}")
                            
                            // Si el estado cambió, la card se moverá automáticamente entre secciones
                            // porque las pantallas filtran por estado (ACTIVE, PICKUP, COMPLETED, EXPIRED)
                        } else {
                            // Si no existe, agregarla
                            currentOffers.add(offer)
                            _offers.value = currentOffers
                            Log.d("OfferRepository", "Oferta agregada en tiempo real (UPDATE): ${offer.id}")
                        }
                    }
                }
                RealtimeEventType.DELETE -> {
                    // DELETE: Remover oferta
                    val index = currentOffers.indexOfFirst { it.id == event.offerId }
                    if (index >= 0) {
                        currentOffers.removeAt(index)
                        _offers.value = currentOffers
                        Log.d("OfferRepository", "Oferta eliminada en tiempo real: ${event.offerId}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al manejar evento de oferta: ${e.message}", e)
        }
    }
    
    /**
     * Maneja eventos de reservas en tiempo real.
     * 
     * Eventos:
     * - INSERT: Agrega la nueva reserva a la lista
     * - UPDATE: Actualiza la reserva existente (ej: cuando cambia de RESERVED a VALIDATED)
     * - DELETE: Remueve la reserva de la lista
     * 
     * Cuando una reserva cambia de RESERVED a VALIDATED:
     * - El cliente verá el QR habilitado automáticamente
     * - El bodeguero verá el contador actualizado
     * - Los triggers de Supabase actualizarán validated_units y pueden cambiar el estado de la oferta
     */
    private fun handleReservationRealtimeEvent(event: com.nexusbiz.nexusbiz.service.ReservationRealtimeEvent) {
        try {
            // Para actualizar reservas, necesitamos obtener los datos completos desde la vista
            // porque el evento solo trae campos básicos de la tabla reservas
            realtimeScope.launch {
                when (event.eventType) {
                    RealtimeEventType.INSERT, RealtimeEventType.UPDATE -> {
                        // Obtener la reserva completa desde la vista reservas_completas
                        val reservation = try {
                            val remoteReservation = supabase.from("reservas_completas")
                                .select {
                                    filter { eq("id", event.reservationId) }
                                }
                                .decodeSingleOrNull<RemoteReservation>()
                            
                            remoteReservation?.let { reservationFromRemote(it) }
                        } catch (e: Exception) {
                            Log.e("OfferRepository", "Error al obtener reserva completa: ${e.message}", e)
                            null
                        }
                        
                        reservation?.let {
                            val currentReservations = _reservations.value.toMutableList()
                            val index = currentReservations.indexOfFirst { r -> r.id == it.id }
                            if (index >= 0) {
                                currentReservations[index] = it
                            } else {
                                currentReservations.add(it)
                            }
                            _reservations.value = currentReservations
                            
                            // Si la reserva cambió de estado, también actualizar la oferta relacionada
                            if (event.eventType == RealtimeEventType.UPDATE && event.offerId.isNotBlank()) {
                                // Forzar actualización de la oferta para reflejar cambios en reserved_units/validated_units
                                getOfferById(event.offerId)?.let { updatedOffer ->
                                    val currentOffers = _offers.value.toMutableList()
                                    val offerIndex = currentOffers.indexOfFirst { o -> o.id == updatedOffer.id }
                                    if (offerIndex >= 0) {
                                        currentOffers[offerIndex] = updatedOffer
                                        _offers.value = currentOffers
                                    }
                                }
                            }
                            
                            Log.d("OfferRepository", "Reserva actualizada en tiempo real: ${it.id}, status: ${it.status}")
                        }
                    }
                    RealtimeEventType.DELETE -> {
                        val currentReservations = _reservations.value.toMutableList()
                        val index = currentReservations.indexOfFirst { it.id == event.reservationId }
                        if (index >= 0) {
                            currentReservations.removeAt(index)
                            _reservations.value = currentReservations
                            Log.d("OfferRepository", "Reserva eliminada en tiempo real: ${event.reservationId}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OfferRepository", "Error al manejar evento de reserva: ${e.message}", e)
        }
    }
}

