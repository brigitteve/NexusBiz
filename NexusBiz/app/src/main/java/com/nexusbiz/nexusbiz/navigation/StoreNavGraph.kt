package com.nexusbiz.nexusbiz.navigation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.data.repository.OfferRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.data.repository.StoreRepository
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import com.nexusbiz.nexusbiz.util.ImageUriHelper
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import com.nexusbiz.nexusbiz.ui.screens.auth.ChangePasswordScreen
import com.nexusbiz.nexusbiz.ui.screens.store.MyProductsScreen
import com.nexusbiz.nexusbiz.ui.screens.store.OfferPublishedScreen
import com.nexusbiz.nexusbiz.ui.screens.store.PublishProductScreen
import com.nexusbiz.nexusbiz.ui.screens.store.ScanQRScreen
import com.nexusbiz.nexusbiz.ui.screens.store.StoreDashboardScreen
import com.nexusbiz.nexusbiz.ui.screens.store.StoreGroupDetailScreen
import com.nexusbiz.nexusbiz.ui.screens.store.StoreProfileScreen
import com.nexusbiz.nexusbiz.ui.screens.store.StoreSubscriptionProScreen
import com.nexusbiz.nexusbiz.ui.screens.store.mapParticipantsForStore
import com.nexusbiz.nexusbiz.ui.viewmodel.AppViewModel
import com.nexusbiz.nexusbiz.ui.viewmodel.AuthViewModel
import com.nexusbiz.nexusbiz.util.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun androidx.navigation.NavGraphBuilder.storeNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    appViewModel: AppViewModel,
    authRepository: AuthRepository,
    productRepository: ProductRepository,
    offerRepository: OfferRepository,
    storeRepository: StoreRepository
) {
    navigation(
        route = STORE_GRAPH_ROUTE,
        startDestination = Screen.StoreDashboard.route
    ) {
        composable(Screen.StoreDashboard.route) {
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                    navController.navigate(Screen.LoginBodega.route) {
                        popUpTo(Screen.StoreDashboard.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val storeOwner = authViewModel.currentStore
            val storeId = storeOwner?.id
            val ownerUserId = storeOwner?.ownerId
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            // Refrescar datos al entrar al dashboard
            LaunchedEffect(ownerUserId, storeId) {
                storeId?.let { id ->
                    appViewModel.fetchProducts("")
                    appViewModel.fetchOffersByStore(id) // Obtener ofertas de la bodega, no del usuario
                }
            }
            
            // INTEGRACIÓN REALTIME: Iniciar suscripción en tiempo real para StoreDashboard (bodeguero)
            // Escucha cambios en ofertas de la bodega (filtrado por storeId)
            // Cuando hay cambios, las ofertas se actualizan automáticamente y las cards se mueven entre secciones
            // (Activos → En Retiro → Finalizados)
            LaunchedEffect(storeId) {
                if (storeId != null && storeId.isNotBlank()) {
                    appViewModel.startRealtimeUpdates(
                        com.nexusbiz.nexusbiz.ui.viewmodel.RealtimeContext(
                            storeId = storeId
                        )
                    )
                }
            }
            
            // Detener suscripción cuando se sale de la pantalla
            androidx.compose.runtime.DisposableEffect(storeId) {
                onDispose {
                    appViewModel.stopRealtimeUpdates()
                }
            }
            val myProducts = storeId?.let { id ->
                appUiState.products.filter { it.storeId == id }
            } ?: emptyList()
            // @Deprecated - grupos ya no se usan
            val storeGroups = emptyList<com.nexusbiz.nexusbiz.data.model.Group>()
            val activeGroups = storeGroups.filter { it.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE && !it.isExpired }
            val storeOffers = storeId?.let { id ->
                appUiState.offers.filter { it.storeId == id }
            } ?: emptyList()
            val ownerAlias = storeOwner?.ownerAlias ?: storeOwner?.commercialName ?: storeOwner?.name ?: "Bodega"
            val storePlan = storeOwner?.plan ?: com.nexusbiz.nexusbiz.data.model.StorePlan.FREE
            StoreDashboardScreen(
                products = myProducts,
                activeGroups = activeGroups, // @Deprecated
                offers = storeOffers,
                ownerAlias = ownerAlias,
                storePlan = storePlan,
                onPublishProduct = { navController.navigate(Screen.PublishProduct.route) },
                onViewProducts = {
                    navController.navigate(Screen.MyProducts.route) { launchSingleTop = true }
                },
                onGroupClick = { groupId ->
                    navController.navigate(Screen.StoreGroupDetail.createRoute(groupId))
                },
                onOfferClick = { offerId ->
                    navController.navigate(Screen.StoreGroupDetail.createRoute(offerId)) // Usar misma ruta temporalmente
                },
                onScanQR = { navController.navigate(Screen.ScanQR.route) },
                onBack = { navController.popBackStack() },
                onNavigateToOffers = {
                    navController.navigate(Screen.MyProducts.route) { launchSingleTop = true }
                },
                onNavigateToProfile = { navController.navigate(Screen.StoreProfile.route) },
                onSwitchToConsumer = {
                    if (authViewModel.currentRole == com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.StoreDashboard.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            Screen.OfferPublished.route,
            arguments = listOf(navArgument(Screen.OfferPublished.OFFER_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val offerId = backStackEntry.arguments?.getString(Screen.OfferPublished.OFFER_ID_ARG) ?: ""
            OfferPublishedScreen(
                offerId = offerId,
                onViewOffer = {
                    navController.navigate(Screen.StoreGroupDetail.createRoute(offerId)) {
                        popUpTo(Screen.StoreDashboard.route) { inclusive = false }
                    }
                },
                onBackToDashboard = {
                    navController.navigate(Screen.StoreDashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.PublishProduct.route) {
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                    navController.navigate(Screen.LoginBodega.route) {
                        popUpTo(Screen.PublishProduct.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            val storeSnapshot = authViewModel.currentStore
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var isLoading by remember { mutableStateOf(false) }
            
            // Mostrar error si existe
            errorMessage?.let { error ->
                LaunchedEffect(error) {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    errorMessage = null
                }
            }
            
            PublishProductScreen(
                storeAddress = storeSnapshot?.address ?: "",
                onPublish = { name, imageUrl, cat, normalPrice, groupPrice, min, max, productImage, durationHours ->
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            // Obtener usuario: primero intentar desde currentUser, si no está disponible, obtenerlo desde ownerId de la bodega
                            var userSnapshot = currentUser
                            if (userSnapshot == null && storeSnapshot?.ownerId != null) {
                                // Intentar obtener el usuario desde el ownerId de la bodega
                                try {
                                    val remoteUser = SupabaseManager.client.from("usuarios")
                                        .select {
                                            filter { eq("id", storeSnapshot.ownerId) }
                                        }
                                        .decodeSingleOrNull<com.nexusbiz.nexusbiz.data.remote.model.User>()
                                    
                                    if (remoteUser != null) {
                                        // Convertir RemoteUser a User local
                                        userSnapshot = com.nexusbiz.nexusbiz.data.model.User(
                                            id = remoteUser.id,
                                            alias = remoteUser.alias,
                                            passwordHash = remoteUser.passwordHash ?: "",
                                            fechaNacimiento = remoteUser.fechaNacimiento ?: "",
                                            district = remoteUser.district,
                                            email = remoteUser.email,
                                            avatar = remoteUser.avatar,
                                            latitude = remoteUser.latitude,
                                            longitude = remoteUser.longitude,
                                            points = remoteUser.points,
                                            tier = when (remoteUser.gamificationLevel) {
                                                com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO -> com.nexusbiz.nexusbiz.data.model.UserTier.GOLD
                                                com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA -> com.nexusbiz.nexusbiz.data.model.UserTier.SILVER
                                                else -> com.nexusbiz.nexusbiz.data.model.UserTier.BRONZE
                                            },
                                            gamificationLevel = when (remoteUser.gamificationLevel) {
                                                com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.ORO -> com.nexusbiz.nexusbiz.data.model.GamificationLevel.ORO
                                                com.nexusbiz.nexusbiz.data.remote.model.GamificationLevel.PLATA -> com.nexusbiz.nexusbiz.data.model.GamificationLevel.PLATA
                                                else -> com.nexusbiz.nexusbiz.data.model.GamificationLevel.BRONCE
                                            },
                                            badges = remoteUser.badges,
                                            streak = remoteUser.streak,
                                            completedGroups = remoteUser.completedGroups,
                                            totalSavings = remoteUser.totalSavings,
                                            userType = when (remoteUser.userType) {
                                                com.nexusbiz.nexusbiz.data.remote.model.UserType.STORE_OWNER -> com.nexusbiz.nexusbiz.data.model.UserType.STORE_OWNER
                                                else -> com.nexusbiz.nexusbiz.data.model.UserType.CONSUMER
                                            },
                                            createdAt = remoteUser.createdAt
                                        )
                                        android.util.Log.d("StoreNavGraph", "Usuario obtenido desde ownerId: ${userSnapshot.id}")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("StoreNavGraph", "Error al obtener usuario desde ownerId: ${e.message}", e)
                                }
                            }
                            
                            if (userSnapshot == null) {
                                errorMessage = "Usuario no encontrado. Por favor, inicia sesión nuevamente."
                                isLoading = false
                                return@launch
                            }
                            
                            val ownerStore = storeSnapshot ?: storeRepository.getStoresByOwner(userSnapshot.id).firstOrNull()
                            if (ownerStore == null) {
                                errorMessage = "No se encontró la bodega asociada. Por favor, verifica tu cuenta."
                                isLoading = false
                                android.util.Log.e("StoreNavGraph", "Bodega no encontrada para usuario: ${userSnapshot.id}")
                                return@launch
                            }
                            
                            val storeId = ownerStore.id
                            val storeName = ownerStore.name.ifEmpty { ownerStore.commercialName ?: userSnapshot.alias.ifEmpty { "Mi Bodega" } }
                            // Usar el distrito de la bodega, si no tiene usar el del usuario, si no tiene usar "Trujillo"
                            val storeDistrict = when {
                                ownerStore.district.isNotBlank() -> ownerStore.district
                                userSnapshot.district.isNotBlank() -> userSnapshot.district
                                else -> "Trujillo"
                            }
                            val storeAddressValue = storeSnapshot?.address?.takeIf { it.isNotBlank() } ?: ""
                            android.util.Log.d("StoreNavGraph", "Publicando producto - storeId: $storeId, storeName: $storeName, district: $storeDistrict")
                            
                            // Validar límite de ofertas activas según el plan
                            val storePlan = ownerStore.plan ?: com.nexusbiz.nexusbiz.data.model.StorePlan.FREE
                            if (storePlan == com.nexusbiz.nexusbiz.data.model.StorePlan.FREE) {
                                // Consultar ofertas activas directamente desde la base de datos
                                // Esto asegura que solo se cuenten ofertas realmente activas (no expiradas)
                                val activeOffersCount = offerRepository.getActiveOffersCountByStore(storeId)
                                
                                android.util.Log.d("StoreNavGraph", "Ofertas activas (no expiradas) encontradas: $activeOffersCount para storeId: $storeId")
                                if (activeOffersCount >= 2) {
                                    isLoading = false
                                    errorMessage = "Plan Gratuito: Solo puedes tener 2 ofertas activas. Actualiza a Plan PRO para ofertas ilimitadas."
                                    return@launch
                                }
                            }
                            
                            // REFACTORIZADO: Subir imagen a Supabase Storage antes de crear la oferta
                            // Generar ID de oferta primero para nombrar el archivo
                            val offerId = java.util.UUID.randomUUID().toString()
                            var finalImageUrl: String? = null

                            if (productImage.isNotBlank()) {
                                // Validar si es URL remota o URI local
                                if (com.nexusbiz.nexusbiz.util.ImageUriHelper.isRemoteUrl(productImage)) {
                                    // Ya es una URL remota válida, usarla directamente
                                    finalImageUrl = productImage
                                    android.util.Log.d("StoreNavGraph", "Imagen es URL remota, usando directamente: $finalImageUrl")
                                } else {
                                    // Es una URI local, convertir a Uri y subir
                                    try {
                                        val localUri = Uri.parse(productImage)
                                        
                                        // Validar que sea una URI local válida
                                        if (com.nexusbiz.nexusbiz.util.ImageUriHelper.isLocalUri(localUri)) {
                                            android.util.Log.d("StoreNavGraph", "Subiendo imagen local para oferta: $offerId")
                                            
                                            // Subir imagen usando el nuevo método que retorna Result
                                            val uploadResult = offerRepository.uploadOfferImage(context, localUri, offerId)
                                            uploadResult.fold(
                                                onSuccess = { url ->
                                                    finalImageUrl = url
                                                    android.util.Log.d("StoreNavGraph", "Imagen subida exitosamente: $url")
                                                },
                                                onFailure = { error ->
                                                    android.util.Log.e("StoreNavGraph", "Error al subir imagen: ${error.message}", error)
                                                    // NO usar placeholder, dejar null para que se cree la oferta sin imagen
                                                    // o mostrar error al usuario
                                                    errorMessage = "Error al subir imagen: ${error.message}"
                                                    isLoading = false
                                                    return@launch
                                                }
                                            )
                                        } else {
                                            android.util.Log.w("StoreNavGraph", "URI no es local ni remota válida: $productImage")
                                            // No usar placeholder, continuar sin imagen
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("StoreNavGraph", "Error al parsear URI de imagen: ${e.message}", e)
                                        // No usar placeholder, continuar sin imagen
                                    }
                                }
                            }
                            
                            // Si no hay imagen, usar null (no placeholder)
                            // El campo image_url en la BD acepta null
                            
                            // Crear oferta directamente (no crear grupo)
                            // Usar finalImageUrl si existe, sino null (no placeholder)
                            val offerResult = offerRepository.createOffer(
                                productName = name,
                                description = "",
                                imageUrl = finalImageUrl ?: "",
                                normalPrice = normalPrice,
                                groupPrice = groupPrice,
                                targetUnits = min,
                                storeId = storeId,
                                storeName = storeName,
                                district = storeDistrict,
                                pickupAddress = storeAddressValue,
                                durationHours = durationHours,
                                latitude = null,
                                longitude = null
                            )
                            
                            offerResult.fold(
                                onSuccess = { offer ->
                                    isLoading = false
                                    // Refrescar productos y ofertas de la bodega para mostrar la nueva oferta
                                    storeId?.let { id ->
                                        appViewModel.fetchProducts("")
                                        appViewModel.fetchOffersByStore(id) // Refrescar ofertas de la bodega
                                    }
                                    // También refrescar ofertas activas para el modo cliente
                                    appViewModel.fetchAllActiveOffers(storeDistrict)
                                    // Navegar a la pantalla de oferta publicada
                                    navController.navigate(Screen.OfferPublished.createRoute(offer.id)) {
                                        popUpTo(Screen.PublishProduct.route) { inclusive = true }
                                    }
                                },
                                onFailure = { error ->
                                    isLoading = false
                                    errorMessage = error.message ?: "Error al crear la oferta"
                                    android.util.Log.e("StoreNavGraph", "Error al crear oferta: ${error.message}", error)
                                }
                            )
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Error inesperado: ${e.message ?: "Error desconocido"}"
                            android.util.Log.e("StoreNavGraph", "Error inesperado al publicar producto", e)
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                isLoading = isLoading
            )
        }
        composable(Screen.MyProducts.route) {
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                    navController.navigate(Screen.LoginBodega.route) {
                        popUpTo(Screen.MyProducts.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val storeOwner = authViewModel.currentStore
            val storeId = storeOwner?.id
            val ownerUserId = storeOwner?.ownerId
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(ownerUserId, storeId) {
                // CORRECCIÓN: Para bodeguero, obtener todas las ofertas de su bodega, no solo donde tiene reservas
                storeId?.let { id ->
                    appViewModel.fetchProducts("")
                    appViewModel.fetchOffersByStore(id)
                }
            }
            
            // INTEGRACIÓN REALTIME: Iniciar suscripción en tiempo real para MyProductsScreen (bodeguero)
            // Escucha cambios en ofertas de la bodega (filtrado por storeId)
            // Cuando una oferta cambia de estado (ACTIVE → PICKUP → COMPLETED → EXPIRED),
            // la card se mueve automáticamente entre las secciones (Activos → Expirados → Completados)
            LaunchedEffect(storeId) {
                if (storeId != null && storeId.isNotBlank()) {
                    appViewModel.startRealtimeUpdates(
                        com.nexusbiz.nexusbiz.ui.viewmodel.RealtimeContext(
                            storeId = storeId
                        )
                    )
                }
            }
            
            // Detener suscripción cuando se sale de la pantalla
            androidx.compose.runtime.DisposableEffect(storeId) {
                onDispose {
                    appViewModel.stopRealtimeUpdates()
                }
            }
            val myProducts = storeId?.let { id ->
                appUiState.products.filter { it.storeId == id }
            } ?: emptyList()
            // @Deprecated - grupos ya no se usan
            val storeGroups = emptyList<com.nexusbiz.nexusbiz.data.model.Group>()
            val storeOffers = storeId?.let { id ->
                appUiState.offers.filter { it.storeId == id }
            } ?: emptyList()
            MyProductsScreen(
                groups = storeGroups, // @Deprecated
                offers = storeOffers,
                onGroupClick = { groupId ->
                    navController.navigate(Screen.StoreGroupDetail.createRoute(groupId))
                },
                onOfferClick = { offerId ->
                    navController.navigate(Screen.StoreGroupDetail.createRoute(offerId))
                },
                onBack = { navController.popBackStack() },
                onNavigateToDashboard = { navController.navigate(Screen.StoreDashboard.route) },
                onNavigateToProfile = { navController.navigate(Screen.StoreProfile.route) }
            )
        }
        composable(
            Screen.StoreGroupDetail.route,
            arguments = listOf(navArgument(Screen.StoreGroupDetail.GROUP_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                    navController.navigate(Screen.LoginBodega.route) {
                        popUpTo(Screen.StoreGroupDetail.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val groupId = backStackEntry.arguments?.getString(Screen.StoreGroupDetail.GROUP_ID_ARG) ?: ""
            val context = LocalContext.current
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            val currentStore = authViewModel.currentStore
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            var isLoadingGroup by remember { mutableStateOf(true) }
            var groupLoadError by remember { mutableStateOf<String?>(null) }
            
            // Refrescar el grupo periódicamente para ver cambios en tiempo real
            LaunchedEffect(groupId) {
                if (groupId.isBlank()) {
                    groupLoadError = "ID de grupo no válido"
                    isLoadingGroup = false
                    return@LaunchedEffect
                }
                
                try {
                    isLoadingGroup = true
                    groupLoadError = null
                    
                    // Intentar obtener la oferta
                    appViewModel.fetchOfferById(groupId)
                    
                    // Esperar un momento para que se actualice el estado
                    kotlinx.coroutines.delay(1000)
                    isLoadingGroup = false
                    
                    // Refrescar cada 5 segundos para ver actualizaciones de clientes
                    // Usar ensureActive() para cancelar correctamente cuando el composable se descompone
                    while (isActive) {
                        kotlinx.coroutines.delay(5000)
                        ensureActive() // Verificar si la corrutina sigue activa
                        appViewModel.fetchOfferById(groupId)
                    }
                } catch (e: CancellationException) {
                    // Cancelación normal cuando el composable se descompone - no es un error
                    android.util.Log.d("StoreNavGraph", "Refresco periódico cancelado (normal)")
                    isLoadingGroup = false
                } catch (e: Exception) {
                    android.util.Log.e("StoreNavGraph", "Error al cargar grupo: ${e.message}", e)
                    groupLoadError = "Error al cargar el grupo: ${e.message}"
                    isLoadingGroup = false
                }
            }
            val offer = appUiState.offers.firstOrNull { it.id == groupId }
            
            // Si después de cargar no se encuentra la oferta, mostrar error
            LaunchedEffect(offer, isLoadingGroup) {
                if (!isLoadingGroup && offer == null && groupId.isNotBlank() && groupLoadError == null) {
                    // Intentar cargar una vez más
                    kotlinx.coroutines.delay(500)
                    appViewModel.fetchOfferById(groupId)
                }
            }
            
            // Mostrar estado de carga o error
            if (isLoadingGroup) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF4F4F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF10B981))
                        Text(
                            text = "Cargando oferta...",
                            color = Color(0xFF606060),
                            fontSize = 14.sp
                        )
                    }
                }
                return@composable
            }
            
            if (groupLoadError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF4F4F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = groupLoadError ?: "Error desconocido",
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Volver")
                        }
                    }
                }
                return@composable
            }
            
            // Obtener reservas de la oferta
            var reservations by remember { mutableStateOf<List<com.nexusbiz.nexusbiz.data.model.Reservation>>(emptyList()) }
            LaunchedEffect(offer?.id) {
                offer?.let {
                    reservations = offerRepository.getReservationsByOffer(it.id)
                }
            }
            
            StoreGroupDetailScreen(
                group = null, // @Deprecated
                offer = offer,
                reservations = reservations,
                onBack = { navController.popBackStack() },
                onShare = { targetOffer ->
                    val shareLink = "https://nexusbiz.app/oferta/${targetOffer.id}"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Únete a la oferta \"${targetOffer.productName}\" y consigue el precio grupal de S/ ${
                                (if (targetOffer.groupPrice > 0) targetOffer.groupPrice else targetOffer.normalPrice)
                            }. $shareLink"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir oferta"))
                },
                onScanQR = {
                    navController.navigate(Screen.ScanQR.createRoute(groupId))
                },
                onPublishSimilar = {
                    Toast.makeText(context, "Publicar oferta similar próximamente", Toast.LENGTH_SHORT).show()
                },
                onViewHistory = {
                    Toast.makeText(context, "Historial disponible próximamente", Toast.LENGTH_SHORT).show()
                }
            )
        }
        composable(
            Screen.ScanQR.route,
            arguments = listOf(navArgument(Screen.ScanQR.GROUP_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                    navController.navigate(Screen.LoginBodega.route) {
                        popUpTo(Screen.ScanQR.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val groupId = backStackEntry.arguments?.getString(Screen.ScanQR.GROUP_ID_ARG)
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            val storeOwner = authViewModel.currentStore
            val storeId = storeOwner?.id
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            ScanQRScreen(
                groupId = groupId,
                onQRScanned = { qrCode ->
                    scope.launch {
                        // CORRECCIÓN: El QR contiene el ID de la reserva (reservation.id)
                        // El bodeguero valida la reserva del cliente, no la suya propia
                        // Por lo tanto, usamos validateReservationByStore que busca por storeId
                        if (storeId != null && storeId.isNotBlank()) {
                            val result = offerRepository.validateReservationByStore(qrCode, storeId)
                            result.onSuccess { userId ->
                                Toast.makeText(context, "QR escaneado correctamente. Reserva validada exitosamente", Toast.LENGTH_SHORT).show()
                                
                                // Actualizar gamificación: ahorro y grupos completados
                                groupId?.let { offerId ->
                                    val offer = offerRepository.getOfferById(offerId)
                                    val reservation = offerRepository.getReservationsByOffer(offerId)
                                        .firstOrNull { it.id == qrCode }
                                    
                                    if (offer != null && reservation != null) {
                                        // Calcular ahorro: (precio_normal - precio_grupal) * unidades
                                        val savingsPerUnit = offer.normalPrice - offer.groupPrice
                                        val totalSavings = savingsPerUnit * reservation.units
                                        
                                        // Actualizar ahorro total del usuario
                                        if (totalSavings > 0) {
                                            authRepository.updateTotalSavings(userId, totalSavings)
                                                .onFailure { e ->
                                                    android.util.Log.e("StoreNavGraph", "Error al actualizar ahorro: ${e.message}", e)
                                                }
                                        }
                                        
                                        // Verificar si la oferta se completó después de validar esta reserva
                                        // Esperar un momento para que el trigger actualice validated_units
                                        kotlinx.coroutines.delay(600)
                                        val updatedOffer = offerRepository.getOfferById(offerId)
                                        
                                        // Si la oferta se completó (validated_units >= target_units), incrementar completed_groups
                                        // para todos los usuarios que participaron
                                        if (updatedOffer != null && 
                                            updatedOffer.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.COMPLETED &&
                                            updatedOffer.validatedUnits >= updatedOffer.targetUnits) {
                                            // Obtener todas las reservas validadas de esta oferta
                                            val validatedReservations = offerRepository.getReservationsByOffer(offerId)
                                                .filter { it.status == com.nexusbiz.nexusbiz.data.model.ReservationStatus.VALIDATED }
                                            
                                            // Incrementar completed_groups para cada usuario que participó
                                            validatedReservations.forEach { validatedReservation ->
                                                authRepository.incrementCompletedGroups(validatedReservation.userId)
                                                    .onFailure { e ->
                                                        android.util.Log.e("StoreNavGraph", "Error al incrementar grupos completados: ${e.message}", e)
                                                    }
                                            }
                                        }
                                    }
                                }
                                
                                // Refrescar la oferta para ver los cambios
                                groupId?.let { 
                                    appViewModel.fetchOfferById(it)
                                }
                                navController.popBackStack()
                            }.onFailure { error ->
                                Toast.makeText(context, "Error al validar QR: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Error: No se pudo identificar la bodega", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.StoreProfile.route) {
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                    navController.navigate(Screen.LoginBodega.route) {
                        popUpTo(Screen.StoreProfile.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            val store = authViewModel.currentStore
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            StoreProfileScreen(
                store = store,
                user = currentUser,
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onChangePassword = { navController.navigate(Screen.ChangePassword.route) },
                onTermsAndPrivacy = { navController.navigate(Screen.TermsAndPrivacy.route) },
                onPlanPro = { navController.navigate(Screen.StoreSubscriptionPro.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToDashboard = { navController.navigate(Screen.StoreDashboard.route) },
                onNavigateToOffers = { navController.navigate(Screen.MyProducts.route) }
            )
        }
        composable(Screen.ChangePassword.route) {
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            ChangePasswordScreen(
                onBack = { navController.popBackStack() },
                onChangePassword = { oldPassword, newPassword ->
                    currentUser?.let { user ->
                        authViewModel.changePassword(user.id, oldPassword, newPassword) {
                            navController.popBackStack()
                        }
                    }
                },
                isLoading = authViewModel.uiState.value.isLoading,
                errorMessage = authViewModel.uiState.value.errorMessage
            )
        }
        composable(Screen.StoreSubscriptionPro.route) {
            StoreSubscriptionProScreen(
                onBack = { navController.popBackStack() },
                onSubscribe = { }
            )
        }
    }
}
