package com.nexusbiz.nexusbiz.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusbiz.nexusbiz.data.model.Offer
import com.nexusbiz.nexusbiz.data.model.Product
import com.nexusbiz.nexusbiz.data.model.GamificationLevel
import com.nexusbiz.nexusbiz.data.repository.OfferRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.util.onSuccess
import com.nexusbiz.nexusbiz.util.onFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class AppUiState(
    val products: List<Product> = emptyList(),
    val offers: List<Offer> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Contexto para suscripciones Realtime.
 * Define qué filtros aplicar según el tipo de usuario y pantalla.
 * 
 * - Cliente: usa district y userId para ver ofertas de su distrito y sus reservas
 * - Bodeguero: usa storeId para ver ofertas de su bodega
 */
data class RealtimeContext(
    val district: String? = null,
    val storeId: String? = null,
    val userId: String? = null
)

data class OfferCreateArgs(
    val productName: String,
    val description: String,
    val imageUrl: String,
    val normalPrice: Double,
    val groupPrice: Double,
    val targetUnits: Int,
    val storeId: String,
    val storeName: String,
    val district: String,
    val pickupAddress: String,
    val durationHours: Int = 24,
    val latitude: Double? = null,
    val longitude: Double? = null
)

class AppViewModel(
    private val productRepository: ProductRepository,
    private val offerRepository: OfferRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun fetchProducts(district: String? = null, category: String? = null, search: String? = null) {
        viewModelScope.launch {
            productRepository.fetchProducts(district.orEmpty(), category, search)
            _uiState.value = _uiState.value.copy(products = productRepository.products.value)
        }
    }

    /**
     * Obtiene las ofertas donde el usuario tiene reservas.
     * Usado en "Mis Grupos" del cliente para mostrar solo las ofertas donde tiene reservas.
     * 
     * IMPORTANTE: Este método reemplaza completamente appUiState.offers con las ofertas del usuario.
     * No se debe llamar fetchAllActiveOffers después de esto si queremos mantener las ofertas del usuario.
     */
    /**
     * Obtiene las ofertas donde el usuario tiene reservas.
     * Usado en "Mis Grupos" del cliente para mostrar solo las ofertas donde tiene reservas.
     * 
     * IMPORTANTE: Este método reemplaza completamente appUiState.offers con las ofertas del usuario.
     * No se debe llamar fetchAllActiveOffers después de esto si queremos mantener las ofertas del usuario.
     */
    fun fetchOffers(userId: String? = null) {
        viewModelScope.launch {
            if (userId != null) {
                // Obtener todas las reservas del usuario
                val reservations = offerRepository.fetchReservationsByUser(userId)
                Log.d("AppViewModel", "Reservas encontradas para usuario $userId: ${reservations.size}")
                
                // Obtener las ofertas correspondientes a cada reserva
                // CORRECCIÓN: Usar coroutine para obtener todas las ofertas en paralelo
                val userOffers = reservations.mapNotNull { reservation ->
                    try {
                        val offer = offerRepository.getOfferById(reservation.offerId)
                        if (offer != null) {
                            Log.d("AppViewModel", "Oferta encontrada para reserva ${reservation.id}: ${offer.id}, producto: ${offer.productName}, status: ${offer.status}, reserved_units: ${offer.reservedUnits}/${offer.targetUnits}")
                        } else {
                            Log.w("AppViewModel", "Oferta no encontrada para reserva ${reservation.id}, offerId: ${reservation.offerId}")
                        }
                        offer
                    } catch (e: Exception) {
                        Log.e("AppViewModel", "Error al obtener oferta ${reservation.offerId} para reserva ${reservation.id}: ${e.message}", e)
                        null
                    }
                }
                
                Log.d("AppViewModel", "Total ofertas del usuario: ${userOffers.size}")
                userOffers.forEach { offer ->
                    Log.d("AppViewModel", "Oferta del usuario: ${offer.id}, producto: ${offer.productName}, status: ${offer.status}")
                }
                
                // Actualizar StateFlow con las ofertas del usuario
                _uiState.value = _uiState.value.copy(offers = userOffers)
            } else {
                offerRepository.fetchAllActiveOffers()
                _uiState.value = _uiState.value.copy(offers = offerRepository.offers.value)
            }
        }
    }

    /**
     * Obtiene TODAS las ofertas activas de la base de datos
     * Usado para mostrar ofertas de todas las bodegas a los clientes
     */
    fun fetchAllActiveOffers(district: String? = null) {
        viewModelScope.launch {
            offerRepository.fetchAllActiveOffers(district)
            _uiState.value = _uiState.value.copy(offers = offerRepository.offers.value)
        }
    }

    fun fetchOfferById(id: String) {
        viewModelScope.launch {
            offerRepository.getOfferById(id)?.let { offer ->
                val current = _uiState.value.offers.toMutableList()
                val idx = current.indexOfFirst { it.id == id }
                if (idx >= 0) current[idx] = offer else current.add(offer)
                _uiState.value = _uiState.value.copy(offers = current)
            }
        }
    }

    /**
     * Obtiene todas las ofertas de una bodega específica
     * Usado para mostrar ofertas del bodeguero en su dashboard y en "Mis Ofertas"
     */
    fun fetchOffersByStore(storeId: String) {
        viewModelScope.launch {
            val storeOffers = offerRepository.fetchOffersByStore(storeId)
            // CORRECCIÓN: Para "Mis Ofertas" del bodeguero, mostrar SOLO las ofertas de su bodega
            // No combinar con otras ofertas para evitar inconsistencias
            _uiState.value = _uiState.value.copy(offers = storeOffers)
        }
    }

    fun publishProduct(product: Product) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            productRepository.publishProduct(product)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        products = productRepository.products.value
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }

    fun createOffer(args: OfferCreateArgs, currentRole: com.nexusbiz.nexusbiz.ui.viewmodel.UserRole?) {
        viewModelScope.launch {
            // Solo bodegueros pueden crear ofertas
            if (currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Solo los bodegueros pueden crear ofertas"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            offerRepository.createOffer(
                productName = args.productName,
                description = args.description,
                imageUrl = args.imageUrl,
                normalPrice = args.normalPrice,
                groupPrice = args.groupPrice,
                targetUnits = args.targetUnits,
                storeId = args.storeId,
                storeName = args.storeName,
                district = args.district,
                pickupAddress = args.pickupAddress,
                durationHours = args.durationHours,
                latitude = args.latitude,
                longitude = args.longitude
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    offers = offerRepository.offers.value
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    /**
     * Crea una reserva para una oferta.
     * 
     * Este método:
     * - Valida que solo clientes puedan hacer reservas
     * - Llama a OfferRepository.createReservation que valida:
     *   * Límites por nivel (BRONCE: 2, PLATA: 4, ORO: 6)
     *   * Oferta activa y no expirada
     *   * Unidades disponibles
     *   * No tener reserva previa para esta oferta
     * - Actualiza el estado con las ofertas actualizadas
     * - Los triggers de Supabase otorgan puntos automáticamente (JOIN_GROUP +5)
     * - RealtimeService detectará el cambio y actualizará la UI automáticamente
     * 
     * @return Result<Reservation> con el resultado de la operación
     */
    suspend fun createReservation(
        offerId: String,
        userId: String,
        units: Int,
        userLevel: GamificationLevel,
        currentRole: com.nexusbiz.nexusbiz.ui.viewmodel.UserRole?
    ): Result<com.nexusbiz.nexusbiz.data.model.Reservation> {
        // Validar que solo CLIENTES pueden hacer reservas
        if (currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
            return Result.failure(Exception("Solo los clientes pueden hacer reservas"))
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        return offerRepository.createReservation(offerId, userId, units, userLevel)
            .also { result ->
                result.onSuccess { reservation ->
                    // CORRECCIÓN: Después de crear una reserva, asegurar que la oferta esté en appUiState.offers
                    // para que aparezca inmediatamente en "Mis Grupos" del cliente
                    val currentOffers = _uiState.value.offers.toMutableList()
                    val updatedOffer = offerRepository.getOfferById(offerId)
                    
                    updatedOffer?.let { offer ->
                        val index = currentOffers.indexOfFirst { it.id == offerId }
                        if (index >= 0) {
                            currentOffers[index] = offer
                            Log.d("AppViewModel", "Oferta actualizada en appUiState después de reserva: ${offer.id}")
                        } else {
                            // Si la oferta no está en la lista, agregarla
                            // Esto es crítico para que aparezca en "Mis Grupos" del cliente
                            currentOffers.add(offer)
                            Log.d("AppViewModel", "Oferta agregada a appUiState después de reserva: ${offer.id}")
                        }
                    }
                    
                    // Actualizar ofertas desde el repositorio (puede haber cambiado el estado a PICKUP)
                    // RealtimeService también actualizará automáticamente cuando detecte el cambio
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        offers = currentOffers,
                        errorMessage = null
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
    }
    
    /**
     * Inicia las suscripciones Realtime según el contexto.
     * 
     * Este método:
     * - Configura filtros en OfferRepository según el contexto (district, storeId, userId)
     * - Inicia la escucha de eventos de ofertas y reservas
     * - Los StateFlows se actualizarán automáticamente cuando haya cambios en la BD
     * 
     * Uso:
     * - Cliente: startRealtimeUpdates(RealtimeContext(district = "Lima", userId = "user123"))
     * - Bodeguero: startRealtimeUpdates(RealtimeContext(storeId = "store456"))
     * 
     * Las pantallas que observan uiState.offers se actualizarán automáticamente
     * y las cards se moverán entre secciones según el estado (ACTIVE → PICKUP → COMPLETED → EXPIRED).
     */
    fun startRealtimeUpdates(context: RealtimeContext) {
        viewModelScope.launch {
            try {
                offerRepository.startRealtimeSubscription(
                    district = context.district,
                    storeId = context.storeId,
                    userId = context.userId
                )
                
                // Observar cambios en el StateFlow de ofertas del repositorio
                // y actualizar el uiState automáticamente
                viewModelScope.launch {
                    offerRepository.offers.collect { offers ->
                        _uiState.value = _uiState.value.copy(offers = offers)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error al iniciar suscripción Realtime: ${e.message}", e)
            }
        }
    }
    
    /**
     * Detiene las suscripciones Realtime.
     * Limpia los filtros activos pero mantiene la conexión base.
     */
    fun stopRealtimeUpdates() {
        viewModelScope.launch {
            try {
                offerRepository.stopRealtimeSubscription()
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error al detener suscripción Realtime: ${e.message}", e)
            }
        }
    }
}
