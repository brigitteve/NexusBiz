package com.nexusbiz.nexusbiz.navigation

import android.content.Intent
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
import com.nexusbiz.nexusbiz.data.repository.GroupRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.data.repository.StoreRepository
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
import kotlinx.coroutines.launch

fun androidx.navigation.NavGraphBuilder.storeNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    appViewModel: AppViewModel,
    authRepository: AuthRepository,
    productRepository: ProductRepository,
    groupRepository: GroupRepository,
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
            // Refrescar datos al entrar al dashboard y periódicamente para mantener sincronización
            LaunchedEffect(ownerUserId, storeId) {
                ownerUserId?.let { userId ->
                    appViewModel.fetchProducts("")
                    appViewModel.fetchGroups(userId)
                }
                // Refrescar cada 10 segundos para mantener sincronización con la BD
                while (true) {
                    kotlinx.coroutines.delay(10000)
                    ownerUserId?.let { userId ->
                        appViewModel.fetchGroups(userId)
                    }
                }
            }
            val myProducts = storeId?.let { id ->
                appUiState.products.filter { it.storeId == id }
            } ?: emptyList()
            val storeGroups = storeId?.let { id ->
                appUiState.groups.filter {
                    it.storeId == id || myProducts.any { prod -> prod.id == it.productId }
                }
            } ?: emptyList()
            val activeGroups = storeGroups.filter { it.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE && !it.isExpired }
            val ownerAlias = storeOwner?.ownerAlias ?: storeOwner?.commercialName ?: storeOwner?.name ?: "Bodega"
            val storePlan = storeOwner?.plan ?: com.nexusbiz.nexusbiz.data.model.StorePlan.FREE
            StoreDashboardScreen(
                products = myProducts,
                activeGroups = activeGroups,
                ownerAlias = ownerAlias,
                storePlan = storePlan,
                onPublishProduct = { navController.navigate(Screen.PublishProduct.route) },
                onViewProducts = {
                    navController.navigate(Screen.MyProducts.route) { launchSingleTop = true }
                },
                onGroupClick = { groupId ->
                    navController.navigate(Screen.StoreGroupDetail.createRoute(groupId))
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
                    val userSnapshot = currentUser
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
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
                            val storeDistrict = ownerStore.district.ifEmpty { userSnapshot.district.ifEmpty { "Trujillo" } }
                            
                            // Validar límite de ofertas activas según el plan
                            val storePlan = ownerStore.plan ?: com.nexusbiz.nexusbiz.data.model.StorePlan.FREE
                            if (storePlan == com.nexusbiz.nexusbiz.data.model.StorePlan.FREE) {
                                // Consultar ofertas activas directamente desde la BD
                                // Si no hay datos o hay error, la función devuelve lista vacía, permitiendo crear
                                val activeGroupsForStore = groupRepository.getActiveGroupsByStore(storeId)
                                
                                android.util.Log.d("StoreNavGraph", "Ofertas activas encontradas: ${activeGroupsForStore.size} para storeId: $storeId")
                                
                                // Verificar si ya tiene 2 ofertas activas
                                if (activeGroupsForStore.size >= 2) {
                                    isLoading = false
                                    errorMessage = "Plan Gratuito: Solo puedes tener 2 ofertas activas. Actualiza a Plan PRO para ofertas ilimitadas."
                                    return@launch
                                }
                            }
                                
                                val product = com.nexusbiz.nexusbiz.data.model.Product(
                                    name = name,
                                    description = "",
                                    category = cat,
                                    normalPrice = normalPrice,
                                    groupPrice = groupPrice,
                                    minGroupSize = min,
                                    maxGroupSize = max,
                                    storeId = storeId,
                                    storeName = storeName,
                                    district = storeDistrict,
                                    durationHours = durationHours,
                                    imageUrl = imageUrl,
                                    storePlan = storePlan // Incluir el plan de la bodega
                                )
                                
                                val productResult = productRepository.createProduct(product)
                                productResult.fold(
                                    onSuccess = { createdProduct ->
                                        val groupResult = groupRepository.createGroup(
                                            productId = createdProduct.id,
                                            productName = name,
                                            productImage = productImage.ifEmpty { "https://via.placeholder.com/150" },
                                            creatorId = userSnapshot.id,
                                            creatorAlias = userSnapshot.alias.ifEmpty { "Bodega" },
                                            targetSize = min,
                                            storeId = storeId,
                                            storeName = storeName,
                                            normalPrice = normalPrice,
                                            groupPrice = groupPrice,
                                            durationHours = durationHours,
                                            initialReservedUnits = 0 // Las bodegas nunca crean participantes automáticos
                                        )
                                        
                                        groupResult.fold(
                                            onSuccess = { group ->
                                                isLoading = false
                                                // Refrescar productos y grupos para mostrar la nueva oferta
                                                userSnapshot?.let { 
                                                    appViewModel.fetchProducts("")
                                                    appViewModel.fetchGroups(it.id)
                                                }
                                                // Navegar a la pantalla de oferta publicada
                                                navController.navigate(Screen.OfferPublished.createRoute(group.id)) {
                                                    popUpTo(Screen.PublishProduct.route) { inclusive = true }
                                                }
                                            },
                                            onFailure = { error ->
                                                isLoading = false
                                                errorMessage = error.message ?: "Error al crear el grupo"
                                                android.util.Log.e("StoreNavGraph", "Error al crear grupo: ${error.message}", error)
                                            }
                                        )
                                    },
                                    onFailure = { error ->
                                        isLoading = false
                                        errorMessage = error.message ?: "Error al crear el producto"
                                        android.util.Log.e("StoreNavGraph", "Error al crear producto: ${error.message}", error)
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
            LaunchedEffect(ownerUserId) {
                ownerUserId?.let { userId ->
                    appViewModel.fetchProducts("")
                    appViewModel.fetchGroups(userId)
                }
            }
            val myProducts = storeId?.let { id ->
                appUiState.products.filter { it.storeId == id }
            } ?: emptyList()
            val storeGroups = storeId?.let { id ->
                appUiState.groups.filter {
                    it.storeId == id || myProducts.any { prod -> prod.id == it.productId }
                }
            } ?: emptyList()
            MyProductsScreen(
                groups = storeGroups,
                onGroupClick = { groupId ->
                    navController.navigate(Screen.StoreGroupDetail.createRoute(groupId))
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
                    appViewModel.fetchGroupById(groupId)
                    // Esperar un momento para que se actualice el estado
                    kotlinx.coroutines.delay(500)
                    isLoadingGroup = false
                    
                    // Refrescar cada 5 segundos para ver actualizaciones de clientes
                    while (true) {
                        kotlinx.coroutines.delay(5000)
                        appViewModel.fetchGroupById(groupId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("StoreNavGraph", "Error al cargar grupo: ${e.message}", e)
                    groupLoadError = "Error al cargar el grupo: ${e.message}"
                    isLoadingGroup = false
                }
            }
            val group = appUiState.groups.firstOrNull { it.id == groupId }
            
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
                            text = "Cargando grupo...",
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
            // Filtrar participantes excluyendo a la bodega (por storeId y ownerId)
            // Usar ownerId del store, o currentUser?.id como fallback
            val storeOwnerId = currentStore?.ownerId ?: currentUser?.id
            val participantDisplay = remember(group, currentStore?.id, storeOwnerId) { 
                mapParticipantsForStore(
                    group, 
                    storeId = currentStore?.id,
                    storeOwnerId = storeOwnerId
                ) 
            }
            StoreGroupDetailScreen(
                group = group,
                participants = participantDisplay,
                onBack = { navController.popBackStack() },
                onShare = { targetGroup ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Únete al grupo \"${targetGroup.productName}\" y consigue el precio grupal de S/ ${
                                (if (targetGroup.groupPrice > 0) targetGroup.groupPrice else targetGroup.normalPrice)
                            }."
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir grupo"))
                },
                onFinalizeEarly = {
                    Toast.makeText(context, "Función disponible próximamente", Toast.LENGTH_SHORT).show()
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
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            ScanQRScreen(
                groupId = groupId,
                onQRScanned = { qrCode ->
                    scope.launch {
                        // El QR contiene el código del grupo
                        // Validar que el QR pertenezca al grupo esperado
                        val result = groupRepository.validateQRByCode(qrCode)
                        result.onSuccess { group ->
                            // Verificar que el grupo escaneado coincida con el grupo actual
                            if (groupId != null && group.id != groupId) {
                                Toast.makeText(context, "Este QR no pertenece a este grupo", Toast.LENGTH_SHORT).show()
                            } else {
                                // El QR es válido, ahora necesitamos validar al participante
                                // Por ahora, mostramos un mensaje de éxito
                                Toast.makeText(context, "QR válido. Buscando participante...", Toast.LENGTH_SHORT).show()
                                // TODO: Implementar selección de participante o actualizar QR para incluir userId
                                // Refrescar el grupo para ver los cambios
                                groupId?.let { appViewModel.fetchGroupById(it) }
                                navController.popBackStack()
                            }
                        }.onFailure { error ->
                            Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
        composable(Screen.StoreSubscriptionPro.route) {
            StoreSubscriptionProScreen(
                onBack = { navController.popBackStack() },
                onSubscribe = { }
            )
        }
    }
}
