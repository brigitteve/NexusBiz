package com.nexusbiz.nexusbiz.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.nexusbiz.nexusbiz.data.model.GroupStatus
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.data.repository.OfferRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.util.onSuccess
import com.nexusbiz.nexusbiz.util.onFailure
import com.nexusbiz.nexusbiz.ui.screens.groups.GroupCompletedConsumerScreen
import com.nexusbiz.nexusbiz.ui.screens.groups.GroupDetailScreen
import com.nexusbiz.nexusbiz.ui.screens.groups.GroupExpiredConsumerScreen
import com.nexusbiz.nexusbiz.ui.screens.groups.GroupReadyForPickupScreen
import com.nexusbiz.nexusbiz.ui.screens.groups.GroupReservedScreen
import com.nexusbiz.nexusbiz.ui.screens.groups.MyGroupsScreen
import com.nexusbiz.nexusbiz.ui.screens.groups.PickupQRScreen
import com.nexusbiz.nexusbiz.ui.screens.home.HomeScreen
import com.nexusbiz.nexusbiz.ui.screens.product.ProductDetailScreen
import com.nexusbiz.nexusbiz.ui.screens.product.ReservationSuccessScreen
import com.nexusbiz.nexusbiz.ui.screens.profile.EditProfileScreen
import com.nexusbiz.nexusbiz.ui.screens.profile.ProfileScreen
import com.nexusbiz.nexusbiz.ui.screens.profile.SettingsScreen
import com.nexusbiz.nexusbiz.ui.screens.profile.TermsAndPrivacyScreen
import com.nexusbiz.nexusbiz.ui.screens.store.ModeSwitchTarget
import com.nexusbiz.nexusbiz.ui.screens.store.ModeSwitchingScreen
import com.nexusbiz.nexusbiz.ui.viewmodel.AppViewModel
import com.nexusbiz.nexusbiz.ui.viewmodel.AuthViewModel
import com.nexusbiz.nexusbiz.util.Screen
import kotlinx.coroutines.launch

fun androidx.navigation.NavGraphBuilder.consumerNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    appViewModel: AppViewModel,
    authRepository: AuthRepository,
    productRepository: ProductRepository,
    offerRepository: OfferRepository
) {
    navigation(
        route = CONSUMER_GRAPH_ROUTE,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            var selectedCategory by remember { mutableStateOf<String?>(null) }
            var searchQuery by remember { mutableStateOf<String?>(null) }
            var categories by remember { mutableStateOf<List<com.nexusbiz.nexusbiz.data.model.Category>>(emptyList()) }
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            // Obtener el distrito del usuario actual
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            var userDistrict by remember { mutableStateOf(currentUser?.district?.takeIf { it.isNotBlank() } ?: "Trujillo") }
            
            // Actualizar distrito cuando cambie el usuario
            LaunchedEffect(currentUser?.district) {
                currentUser?.district?.takeIf { it.isNotBlank() }?.let {
                    userDistrict = it
                }
            }
            
            // Obtener productos del distrito del usuario Y todas las ofertas activas
            LaunchedEffect(selectedCategory, searchQuery, userDistrict) {
                appViewModel.fetchProducts(userDistrict, selectedCategory, searchQuery)
                // IMPORTANTE: Cargar TODAS las ofertas activas de la BD para mostrar ofertas reales
                appViewModel.fetchAllActiveOffers(userDistrict)
            }
            
            // INTEGRACIÓN REALTIME: Iniciar suscripción en tiempo real para HomeScreen (cliente)
            // Escucha cambios en ofertas del distrito y reservas del usuario
            // Cuando hay cambios, las ofertas se actualizan automáticamente y las cards se mueven entre secciones
            val currentUserId = currentUser?.id
            LaunchedEffect(userDistrict, currentUserId) {
                if (userDistrict.isNotBlank() && currentUserId != null) {
                    appViewModel.startRealtimeUpdates(
                        com.nexusbiz.nexusbiz.ui.viewmodel.RealtimeContext(
                            district = userDistrict,
                            userId = currentUserId
                        )
                    )
                }
            }
            
            // Detener suscripción cuando se sale de la pantalla
            androidx.compose.runtime.DisposableEffect(userDistrict, currentUser?.id) {
                onDispose {
                    appViewModel.stopRealtimeUpdates()
                }
            }
            LaunchedEffect(Unit) {
                categories = productRepository.getCategories()
            }
            // Otorgar puntos diarios si corresponde
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                currentUser?.let { user ->
                    val prefs = context.getSharedPreferences("nexusbiz_prefs", android.content.Context.MODE_PRIVATE)
                    val lastDailyPointsDate = prefs.getString("last_daily_points_${user.id}", null)
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    
                    if (lastDailyPointsDate != today) {
                        authRepository.addPoints(user.id, 5, "Abrir app diario")
                        prefs.edit().putString("last_daily_points_${user.id}", today).apply()
                    }
                }
            }
            HomeScreen(
                district = userDistrict,
                products = appUiState.products,
                offers = appUiState.offers,
                categories = categories,
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                },
                onCategoryClick = { categoryId ->
                    selectedCategory = if (categoryId == "Todos") null else categoryId
                },
                selectedCategory = selectedCategory,
                onSearchQueryChange = { query ->
                    searchQuery = if (query.isBlank()) null else query
                },
                onDistrictChange = { newDistrict ->
                    userDistrict = newDistrict
                },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToMyGroups = { navController.navigate(Screen.MyGroups.route) },
                onSwitchToStore = {
                    if (authViewModel.currentRole == com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                        authViewModel.logout()
                        navController.navigate(Screen.LoginBodega.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                authRepository = authRepository
            )
        }
        composable(
            Screen.ModeSwitching.route,
            arguments = listOf(navArgument(Screen.ModeSwitching.MODE_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val modeArg = backStackEntry.arguments?.getString(Screen.ModeSwitching.MODE_ARG)
            val targetMode = ModeSwitchTarget.fromRoute(modeArg)
            ModeSwitchingScreen(
                targetMode = targetMode,
                onFinish = {
                    authViewModel.logout()
                    if (targetMode == ModeSwitchTarget.BODEGUERO) {
                        navController.navigate(Screen.LoginBodega.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            Screen.ProductDetail.route,
            arguments = listOf(navArgument(Screen.ProductDetail.PRODUCT_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ProductDetail.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val productId = backStackEntry.arguments?.getString(Screen.ProductDetail.PRODUCT_ID_ARG) ?: ""
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            val userDistrict = currentUser?.district?.takeIf { it.isNotBlank() } ?: "Trujillo"
            // Refrescar productos y TODAS las ofertas activas cuando cambia el productId o el usuario
            LaunchedEffect(productId, currentUser?.id, userDistrict) {
                appViewModel.fetchProducts(userDistrict, null, null)
                // IMPORTANTE: Cargar TODAS las ofertas activas de la BD, no solo del usuario
                appViewModel.fetchAllActiveOffers(userDistrict)
                // También cargar ofertas del usuario para sus reservas
                currentUser?.let { appViewModel.fetchOffers(it.id) }
            }
            // RealtimeService actualizará automáticamente las ofertas cuando haya cambios
            // No es necesario hacer polling periódico
            val product = appUiState.products.firstOrNull { it.id == productId }
            // Buscar oferta activa para este producto (por product_key o nombre)
            val activeOffer = appUiState.offers.firstOrNull { 
                (it.productKey.equals(product?.name?.lowercase()?.trim(), ignoreCase = true) ||
                 it.productName.equals(product?.name, ignoreCase = true)) &&
                it.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.ACTIVE && 
                !it.isExpired
            }
            // Buscar solo ofertas activas (grupos deprecated)
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            ProductDetailScreen(
                product = product,
                offer = activeOffer,
                user = currentUser,
                timeRemaining = activeOffer?.timeRemaining ?: 0,
                onJoinGroup = { quantity ->
                    // @Deprecated - Esta función ya no está disponible
                    // Usar onCreateReservation en su lugar
                    Toast.makeText(context, "Por favor usa el botón de reserva", Toast.LENGTH_SHORT).show()
                },
                onCreateReservation = { quantity ->
                    // Validar que solo CLIENTES pueden hacer reservas
                    if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                        Toast.makeText(context, "Solo los clientes pueden hacer reservas", Toast.LENGTH_SHORT).show()
                        return@ProductDetailScreen
                    }
                    
                    activeOffer?.let { offer ->
                        currentUser?.let { user ->
                            scope.launch {
                                // Calcular nivel de gamificación del usuario
                                val userLevel = user.gamificationLevel ?: when {
                                    user.points >= 300 -> com.nexusbiz.nexusbiz.data.model.GamificationLevel.ORO
                                    user.points >= 100 -> com.nexusbiz.nexusbiz.data.model.GamificationLevel.PLATA
                                    else -> com.nexusbiz.nexusbiz.data.model.GamificationLevel.BRONCE
                                }
                                
                                // Validar límite por nivel antes de intentar reservar
                                val maxUnits = user.maxReservationUnits()
                                if (quantity > maxUnits) {
                                    val levelName = when (userLevel) {
                                        com.nexusbiz.nexusbiz.data.model.GamificationLevel.BRONCE -> "BRONCE"
                                        com.nexusbiz.nexusbiz.data.model.GamificationLevel.PLATA -> "PLATA"
                                        com.nexusbiz.nexusbiz.data.model.GamificationLevel.ORO -> "ORO"
                                    }
                                    Toast.makeText(
                                        context,
                                        "Tu nivel $levelName permite máximo $maxUnits unidades por reserva",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }
                                
                                // Validar que la oferta esté activa antes de intentar reservar
                                if (offer.status != com.nexusbiz.nexusbiz.data.model.OfferStatus.ACTIVE) {
                                    val errorMsg = when (offer.status) {
                                        com.nexusbiz.nexusbiz.data.model.OfferStatus.PICKUP -> "La oferta ya alcanzó la meta y está en retiro"
                                        com.nexusbiz.nexusbiz.data.model.OfferStatus.COMPLETED -> "La oferta ya fue completada"
                                        com.nexusbiz.nexusbiz.data.model.OfferStatus.EXPIRED -> "La oferta ha expirado"
                                        else -> "La oferta no está activa"
                                    }
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                
                                if (offer.isExpired) {
                                    Toast.makeText(context, "La oferta ha expirado", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                
                                // Validar unidades disponibles
                                val availableUnits = (offer.targetUnits - offer.reservedUnits).coerceAtLeast(0)
                                if (quantity > availableUnits) {
                                    val errorMsg = if (availableUnits == 0) {
                                        "La oferta ya alcanzó la meta"
                                    } else {
                                        "Solo quedan $availableUnits unidades disponibles"
                                    }
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                
                                // Crear la reserva
                                val result = appViewModel.createReservation(
                                    offerId = offer.id,
                                    userId = user.id,
                                    units = quantity,
                                    userLevel = userLevel,
                                    currentRole = authViewModel.currentRole
                                )
                                
                                // Verificar el resultado y mostrar mensajes apropiados
                                result.onSuccess { reservation ->
                                    // CORRECCIÓN: Refrescar las ofertas del usuario después de crear la reserva
                                    // para que aparezca inmediatamente en "Mis Grupos"
                                    appViewModel.fetchOffers(user.id)
                                    
                                    // Los puntos se otorgan automáticamente por triggers de BD (JOIN_GROUP +5)
                                    // No es necesario agregar puntos manualmente aquí
                                    Toast.makeText(context, "Reserva creada exitosamente", Toast.LENGTH_SHORT).show()
                                    navController.navigate(Screen.ReservationSuccess.createRoute(quantity))
                                }.onFailure { error ->
                                    // Mostrar el mensaje de error al usuario
                                    val errorMessage = error.message ?: "Error al crear la reserva"
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    android.util.Log.e("ConsumerNavGraph", "Error al crear reserva: $errorMessage", error)
                                }
                            }
                        } ?: Toast.makeText(context, "Debes iniciar sesión para hacer reservas", Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(context, "No hay oferta activa para este producto", Toast.LENGTH_SHORT).show()
                },
                onCreateGroup = { quantity ->
                    // @Deprecated - Los clientes ya no crean grupos
                    // Las ofertas son creadas por los bodegueros
                    Toast.makeText(context, "Las ofertas son creadas por los bodegueros. Busca una oferta activa.", Toast.LENGTH_LONG).show()
                },
                onShareGroup = {
                    currentUser?.let { user ->
                        scope.launch {
                            // Otorgar +5 puntos por compartir grupo
                            authRepository.addPoints(user.id, 5, "Compartir grupo")
                        }
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Únete al grupo \"${product?.name}\" y consigue el precio grupal de S/ ${product?.groupPrice ?: 0.0}."
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir grupo"))
                },
                onViewStores = { navController.navigate(Screen.QuickBuy.createRoute(productId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.ReservationSuccess.route,
            arguments = listOf(navArgument(Screen.ReservationSuccess.QUANTITY_ARG) {
                type = NavType.IntType
                defaultValue = 1
            })
        ) { backStackEntry ->
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ReservationSuccess.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val quantity = backStackEntry.arguments?.getInt(Screen.ReservationSuccess.QUANTITY_ARG) ?: 1
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            ReservationSuccessScreen(
                quantity = quantity,
                onGoHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onViewReservations = {
                    // CORRECCIÓN: Refrescar ofertas del usuario antes de navegar a "Mis Grupos"
                    // para asegurar que la reserva recién creada aparezca
                    currentUser?.let { user ->
                        scope.launch {
                            appViewModel.fetchOffers(user.id)
                        }
                    }
                    navController.navigate(Screen.MyGroups.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }
        composable(Screen.MyGroups.route) {
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.MyGroups.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            val userId = currentUser?.id ?: ""
            val userDistrict = currentUser?.district?.takeIf { it.isNotBlank() } ?: "Trujillo"
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(userId) {
                if (userId.isNotBlank()) {
                    appViewModel.fetchOffers(userId)
                }
            }
            
            // INTEGRACIÓN REALTIME: Iniciar suscripción en tiempo real para MyGroupsScreen (cliente)
            // Escucha cambios en ofertas del distrito y reservas del usuario
            // Cuando una oferta cambia de estado (ACTIVE → PICKUP → COMPLETED → EXPIRED),
            // la card se mueve automáticamente entre las secciones (Activos → En Retiro → Completados → Expirados)
            LaunchedEffect(userDistrict, userId) {
                if (userDistrict.isNotBlank() && userId.isNotBlank()) {
                    appViewModel.startRealtimeUpdates(
                        com.nexusbiz.nexusbiz.ui.viewmodel.RealtimeContext(
                            district = userDistrict,
                            userId = userId
                        )
                    )
                    
                    // También iniciar suscripción de usuario para puntos/nivel
                    authRepository.startRealtimeSubscription(userId)
                }
            }
            
            // Detener suscripciones cuando se sale de la pantalla
            androidx.compose.runtime.DisposableEffect(userDistrict, userId) {
                onDispose {
                    appViewModel.stopRealtimeUpdates()
                    authRepository.stopRealtimeSubscription()
                }
            }
            // CORRECCIÓN: Refrescar ofertas del usuario cada vez que se entra a la pantalla
            // para asegurar que se muestren todas las ofertas donde tiene reservas
            LaunchedEffect(Unit) {
                if (userId.isNotBlank()) {
                    Log.d("ConsumerNavGraph", "Refrescando ofertas del usuario en MyGroupsScreen: $userId")
                    appViewModel.fetchOffers(userId)
                }
            }
            
            // También refrescar cuando cambie el userId
            LaunchedEffect(userId) {
                if (userId.isNotBlank()) {
                    Log.d("ConsumerNavGraph", "Refrescando ofertas del usuario (userId cambió): $userId")
                    appViewModel.fetchOffers(userId)
                }
            }
            
            // CORRECCIÓN: Filtrar ofertas según el estado de la RESERVA del usuario, no solo el estado de la oferta
            // Obtener las reservas del usuario desde el StateFlow del repositorio para que se actualicen automáticamente
            val allReservations by offerRepository.reservations.collectAsState(initial = emptyList())
            val userReservations = remember(allReservations, userId) {
                allReservations.filter { it.userId == userId }
            }
            
            // También refrescar las reservas cuando cambie el userId
            LaunchedEffect(userId) {
                if (userId.isNotBlank()) {
                    offerRepository.fetchReservationsByUser(userId)
                    Log.d("ConsumerNavGraph", "Reservas del usuario obtenidas: ${userReservations.size}")
                    userReservations.forEach { reservation ->
                        Log.d("ConsumerNavGraph", "Reserva: id=${reservation.id}, offer_id=${reservation.offerId}, status=${reservation.status}")
                    }
                }
            }
            
            // Usar las ofertas del estado actual
            val myOffers = appUiState.offers
            Log.d("ConsumerNavGraph", "Total ofertas en appUiState: ${myOffers.size}")
            myOffers.forEach { offer ->
                val reservation = userReservations.firstOrNull { it.offerId == offer.id }
                Log.d("ConsumerNavGraph", "Oferta: id=${offer.id}, producto=${offer.productName}, offer_status=${offer.status}, reservation_status=${reservation?.status}")
            }
            
            // CORRECCIÓN: Filtrar según el estado de la RESERVA del usuario y el estado de la oferta
            // Lógica de negocio:
            // - Activos: Reserva RESERVED y oferta ACTIVE (aún no alcanzó la meta)
            // - En Retiro: Oferta PICKUP (alcanzó la meta) y reserva RESERVED (puede retirar con QR)
            // - Completados: Reserva VALIDATED (ya retiró) y oferta COMPLETED
            // - Expirados: Reserva EXPIRED o oferta EXPIRED
            
            // Activos: Reserva RESERVED y oferta ACTIVE (aún no alcanzó la meta para retiro)
            val activeOffers = myOffers.filter { offer ->
                val reservation = userReservations.firstOrNull { it.offerId == offer.id }
                reservation?.status == com.nexusbiz.nexusbiz.data.model.ReservationStatus.RESERVED &&
                offer.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.ACTIVE &&
                !offer.isExpired
            }
            
            // En Retiro: Oferta PICKUP (alcanzó la meta) y reserva RESERVED (puede retirar con QR)
            // También incluir reservas VALIDATED que aún están en PICKUP (en proceso de retiro)
            val pickupOffers = myOffers.filter { offer ->
                val reservation = userReservations.firstOrNull { it.offerId == offer.id }
                val matches = offer.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.PICKUP &&
                reservation != null &&
                (reservation.status == com.nexusbiz.nexusbiz.data.model.ReservationStatus.RESERVED ||
                 reservation.status == com.nexusbiz.nexusbiz.data.model.ReservationStatus.VALIDATED)
                if (offer.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.PICKUP) {
                    Log.d("ConsumerNavGraph", "Oferta PICKUP encontrada: ${offer.id}, reservation=${reservation?.id}, reservation_status=${reservation?.status}, matches=$matches")
                }
                matches
            }
            
            // Completados: Reserva VALIDATED (ya retiró) y oferta COMPLETED
            val completedOffers = myOffers.filter { offer ->
                val reservation = userReservations.firstOrNull { it.offerId == offer.id }
                reservation?.status == com.nexusbiz.nexusbiz.data.model.ReservationStatus.VALIDATED &&
                offer.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.COMPLETED
            }
            
            // Expirados: Reserva EXPIRED o oferta EXPIRED
            val expiredOffers = myOffers.filter { offer ->
                val reservation = userReservations.firstOrNull { it.offerId == offer.id }
                reservation?.status == com.nexusbiz.nexusbiz.data.model.ReservationStatus.EXPIRED ||
                offer.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.EXPIRED ||
                (offer.isExpired && reservation?.status != com.nexusbiz.nexusbiz.data.model.ReservationStatus.VALIDATED && offer.status != com.nexusbiz.nexusbiz.data.model.OfferStatus.COMPLETED)
            }
            
            Log.d("ConsumerNavGraph", "Ofertas filtradas por estado de reserva - Activos: ${activeOffers.size}, En Retiro: ${pickupOffers.size}, Completados: ${completedOffers.size}, Expirados: ${expiredOffers.size}")
            
            // CORRECCIÓN: Convertir ofertas a grupos para compatibilidad con MyGroupsScreen
            // MyGroupsScreen solo acepta List<Group>, así que convertimos las ofertas
            fun offerToGroup(offer: com.nexusbiz.nexusbiz.data.model.Offer): com.nexusbiz.nexusbiz.data.model.Group {
                val expiresAtLong = offer.expiresAt?.let {
                    try {
                        java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                    } catch (e: Exception) {
                        System.currentTimeMillis() + (offer.durationHours * 60 * 60 * 1000L)
                    }
                } ?: (System.currentTimeMillis() + (offer.durationHours * 60 * 60 * 1000L))
                
                val createdAtLong = offer.createdAt?.let {
                    try {
                        java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                } ?: System.currentTimeMillis()
                
                val groupStatus = when (offer.status) {
                    com.nexusbiz.nexusbiz.data.model.OfferStatus.ACTIVE -> com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE
                    com.nexusbiz.nexusbiz.data.model.OfferStatus.PICKUP -> com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP
                    com.nexusbiz.nexusbiz.data.model.OfferStatus.COMPLETED -> com.nexusbiz.nexusbiz.data.model.GroupStatus.COMPLETED
                    com.nexusbiz.nexusbiz.data.model.OfferStatus.EXPIRED -> com.nexusbiz.nexusbiz.data.model.GroupStatus.EXPIRED
                }
                
                return com.nexusbiz.nexusbiz.data.model.Group(
                    id = offer.id,
                    productId = offer.id, // Usar el mismo ID temporalmente
                    productName = offer.productName,
                    productImage = offer.imageUrl ?: "",
                    creatorId = offer.storeId,
                    creatorAlias = offer.storeName,
                    participants = emptyList(), // Se cargarán desde reservas si es necesario
                    currentSize = offer.reservedUnits,
                    targetSize = offer.targetUnits,
                    status = groupStatus,
                    expiresAt = expiresAtLong,
                    createdAt = createdAtLong,
                    storeId = offer.storeId,
                    storeName = offer.storeName,
                    qrCode = offer.id, // Usar el ID como QR temporalmente
                    validatedAt = null,
                    normalPrice = offer.normalPrice,
                    groupPrice = offer.groupPrice
                )
            }
            
            val activeGroups = activeOffers.map { offerToGroup(it) }
            val pickupGroups = pickupOffers.map { offerToGroup(it) }
            val completedGroups = completedOffers.map { offerToGroup(it) }
            val expiredGroups = expiredOffers.map { offerToGroup(it) }
            
            Log.d("ConsumerNavGraph", "Grupos convertidos - Activos: ${activeGroups.size}, En Retiro: ${pickupGroups.size}, Completados: ${completedGroups.size}, Expirados: ${expiredGroups.size}")
            MyGroupsScreen(
                activeGroups = activeGroups, // @Deprecated - usar offers
                pickupGroups = pickupGroups, // @Deprecated
                completedGroups = completedGroups, // @Deprecated
                expiredGroups = expiredGroups, // @Deprecated
                onActiveGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onPickupGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onCompletedGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onExpiredGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onNavigateToHome = { navController.navigate(Screen.Home.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(
            Screen.GroupDetail.route,
            arguments = listOf(navArgument(Screen.GroupDetail.GROUP_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.GroupDetail.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val groupId = backStackEntry.arguments?.getString(Screen.GroupDetail.GROUP_ID_ARG) ?: ""
            val currentUserState by authRepository.currentUser.collectAsState(initial = null)
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            var reservations by remember { mutableStateOf<List<com.nexusbiz.nexusbiz.data.model.Reservation>>(emptyList()) }
            
            LaunchedEffect(groupId) { 
                appViewModel.fetchOfferById(groupId)
                // Cargar reservas si hay oferta
                offerRepository.getOfferById(groupId)?.let { offer ->
                    reservations = offerRepository.getReservationsByOffer(offer.id)
                }
            }
            val offer = appUiState.offers.firstOrNull { it.id == groupId }
            when {
                offer?.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.COMPLETED -> {
                    GroupCompletedConsumerScreen(
                        offer = offer,
                        reservations = reservations,
                        currentUser = currentUserState,
                        onBack = { navController.popBackStack() },
                        onViewOffers = { navController.navigate(Screen.Home.route) },
                        onViewGroups = {
                            navController.popBackStack()
                            navController.navigate(Screen.MyGroups.route)
                        }
                    )
                }
                offer?.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.EXPIRED -> {
                    GroupExpiredConsumerScreen(
                        offer = offer,
                        onBack = { navController.popBackStack() },
                        onQuickBuy = { productId ->
                            navController.navigate(Screen.QuickBuy.createRoute(productId))
                        },
                        onExplore = { navController.navigate(Screen.Home.route) }
                    )
                }
                (offer?.isExpired == true && offer.status != com.nexusbiz.nexusbiz.data.model.OfferStatus.COMPLETED) -> {
                    GroupExpiredConsumerScreen(
                        offer = offer,
                        onBack = { navController.popBackStack() },
                        onQuickBuy = { productId ->
                            navController.navigate(Screen.QuickBuy.createRoute(productId))
                        },
                        onExplore = { navController.navigate(Screen.Home.route) }
                    )
                }
                offer?.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.PICKUP -> {
                    val userReservation = reservations.firstOrNull { it.userId == currentUserState?.id }
                    GroupReadyForPickupScreen(
                        offer = offer,
                        reservations = reservations,
                        currentUser = currentUserState,
                        onBack = { navController.popBackStack() },
                        onViewQR = { 
                            val targetId = offer?.id ?: ""
                            navController.navigate(Screen.PickupQR.createRoute(targetId)) 
                        }
                    )
                }
                else -> {
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current
                    GroupReservedScreen(
                        offer = offer,
                        reservations = reservations,
                        currentUser = currentUserState,
                        onBack = { navController.popBackStack() },
                        onShare = { },
                        onCreateReservation = { quantity ->
                            // Validar que solo CLIENTES pueden hacer reservas
                            if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                                Toast.makeText(context, "Solo los clientes pueden hacer reservas", Toast.LENGTH_SHORT).show()
                                return@GroupReservedScreen
                            }
                            
                            offer?.let { activeOffer ->
                                currentUserState?.let { user ->
                                    scope.launch {
                                        // Calcular nivel de gamificación del usuario
                                        val userLevel = user.gamificationLevel ?: when {
                                            user.points >= 300 -> com.nexusbiz.nexusbiz.data.model.GamificationLevel.ORO
                                            user.points >= 100 -> com.nexusbiz.nexusbiz.data.model.GamificationLevel.PLATA
                                            else -> com.nexusbiz.nexusbiz.data.model.GamificationLevel.BRONCE
                                        }
                                        
                                        // Validar límite por nivel antes de intentar reservar
                                        val maxUnits = user.maxReservationUnits()
                                        if (quantity > maxUnits) {
                                            val levelName = when (userLevel) {
                                                com.nexusbiz.nexusbiz.data.model.GamificationLevel.BRONCE -> "BRONCE"
                                                com.nexusbiz.nexusbiz.data.model.GamificationLevel.PLATA -> "PLATA"
                                                com.nexusbiz.nexusbiz.data.model.GamificationLevel.ORO -> "ORO"
                                            }
                                            Toast.makeText(
                                                context,
                                                "Tu nivel $levelName permite máximo $maxUnits unidades por reserva",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }
                                        
                                        // Validar que la oferta esté activa antes de intentar reservar
                                        if (activeOffer.status != com.nexusbiz.nexusbiz.data.model.OfferStatus.ACTIVE) {
                                            val errorMsg = when (activeOffer.status) {
                                                com.nexusbiz.nexusbiz.data.model.OfferStatus.PICKUP -> "La oferta ya alcanzó la meta y está en retiro"
                                                com.nexusbiz.nexusbiz.data.model.OfferStatus.COMPLETED -> "La oferta ya fue completada"
                                                com.nexusbiz.nexusbiz.data.model.OfferStatus.EXPIRED -> "La oferta ha expirado"
                                                else -> "La oferta no está activa"
                                            }
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                            return@launch
                                        }
                                        
                                        if (activeOffer.isExpired) {
                                            Toast.makeText(context, "La oferta ha expirado", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        
                                        // Validar unidades disponibles
                                        val availableUnits = (activeOffer.targetUnits - activeOffer.reservedUnits).coerceAtLeast(0)
                                        if (quantity > availableUnits) {
                                            val errorMsg = if (availableUnits == 0) {
                                                "La oferta ya alcanzó la meta"
                                            } else {
                                                "Solo quedan $availableUnits unidades disponibles"
                                            }
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                            return@launch
                                        }
                                        
                                        // Crear la reserva
                                        val result = appViewModel.createReservation(
                                            offerId = activeOffer.id,
                                            userId = user.id,
                                            units = quantity,
                                            userLevel = userLevel,
                                            currentRole = authViewModel.currentRole
                                        )
                                        
                                        // Verificar el resultado y mostrar mensajes apropiados
                                        result.onSuccess { reservation ->
                                            // CORRECCIÓN: Refrescar las ofertas del usuario después de crear la reserva
                                            // para que aparezca inmediatamente en "Mis Grupos"
                                            appViewModel.fetchOffers(user.id)
                                            
                                            Toast.makeText(context, "Reserva creada exitosamente", Toast.LENGTH_SHORT).show()
                                            navController.navigate(Screen.ReservationSuccess.createRoute(quantity))
                                        }.onFailure { error ->
                                            val errorMessage = error.message ?: "Error al crear la reserva"
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                            android.util.Log.e("ConsumerNavGraph", "Error al crear reserva: $errorMessage", error)
                                        }
                                    }
                                } ?: Toast.makeText(context, "Debes iniciar sesión para hacer reservas", Toast.LENGTH_SHORT).show()
                            } ?: Toast.makeText(context, "No hay oferta activa para este producto", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        composable(
            Screen.PickupQR.route,
            arguments = listOf(navArgument(Screen.PickupQR.GROUP_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.PickupQR.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val groupId = backStackEntry.arguments?.getString(Screen.PickupQR.GROUP_ID_ARG) ?: ""
            val currentUserState by authRepository.currentUser.collectAsState(initial = null)
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(groupId, currentUserState?.id) {
                currentUserState?.let { 
                    appViewModel.fetchOffers(it.id) 
                }
                appViewModel.fetchOfferById(groupId)
            }
            val offer = appUiState.offers.firstOrNull { it.id == groupId }
            var reservations by remember { mutableStateOf<List<com.nexusbiz.nexusbiz.data.model.Reservation>>(emptyList()) }
            
            LaunchedEffect(offer?.id, currentUserState?.id) {
                offer?.let { 
                    reservations = offerRepository.getReservationsByOffer(it.id)
                }
            }
            
            val userReservation = reservations.firstOrNull { it.userId == currentUserState?.id }
            
            PickupQRScreen(
                offer = offer,
                reservation = userReservation,
                currentUser = currentUserState,
                onBack = { navController.popBackStack() },
                onShare = { }
            )
        }
        composable(Screen.Profile.route) {
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            ProfileScreen(
                user = currentUser,
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onChangePassword = { navController.navigate(Screen.ChangePassword.route) },
                onChangeToStoreMode = {
                    navController.navigate(Screen.ModeSwitching.createRoute(ModeSwitchTarget.BODEGUERO))
                },
                onTermsAndPrivacy = { navController.navigate(Screen.TermsAndPrivacy.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToHome = { navController.navigate(Screen.Home.route) },
                onNavigateToMyGroups = { navController.navigate(Screen.MyGroups.route) }
            )
        }
        composable(Screen.EditProfile.route) {
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            EditProfileScreen(
                user = currentUser,
                onSave = { name ->
                    currentUser?.let { user ->
                        scope.launch {
                            authRepository.updateProfile(user.copy(alias = name))
                        }
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                isLoading = false,
                errorMessage = null
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onChangePassword = { navController.navigate(Screen.ChangePassword.route) },
                onNotificationSettings = { },
                onLanguageSettings = { },
                onAbout = { },
                onPrivacyPolicy = { },
                onTerms = { }
            )
        }
        composable(Screen.TermsAndPrivacy.route) {
            TermsAndPrivacyScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
