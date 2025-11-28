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
import android.widget.Toast
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.nexusbiz.nexusbiz.data.model.GroupStatus
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.data.repository.GroupRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.util.onSuccess
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
    groupRepository: GroupRepository
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
            LaunchedEffect(selectedCategory, searchQuery) {
                appViewModel.fetchProducts("Trujillo", selectedCategory, searchQuery)
            }
            LaunchedEffect(Unit) {
                categories = productRepository.getCategories()
            }
            // Otorgar puntos diarios si corresponde
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
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
                district = "Trujillo",
                products = appUiState.products,
                groups = appUiState.groups,
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
                }
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
            // Refrescar productos y grupos cuando cambia el productId o el usuario
            LaunchedEffect(productId, currentUser?.id) {
                appViewModel.fetchProducts()
                currentUser?.let { appViewModel.fetchGroups(it.id) }
                // Buscar grupo activo para este producto
                appUiState.groups.firstOrNull { 
                    it.productId == productId && it.status == GroupStatus.ACTIVE && !it.isExpired 
                }?.let { appViewModel.fetchGroupById(it.id) }
            }
            // Refrescar periódicamente para ver nuevas ofertas de bodegueros
            LaunchedEffect(productId) {
                kotlinx.coroutines.delay(10000) // Esperar 10 segundos antes del primer refresh
                while (true) {
                    appViewModel.fetchProducts()
                    // Refrescar grupos del usuario actual
                    currentUser?.let { appViewModel.fetchGroups(it.id) }
                    // Buscar y refrescar grupo activo para este producto
                    // Verificar tiempo real usando expiresAt
                    val now = System.currentTimeMillis()
                    appUiState.groups.firstOrNull { 
                        it.productId == productId && 
                        it.status == GroupStatus.ACTIVE && 
                        it.expiresAt > now // Verificar tiempo real
                    }?.let { appViewModel.fetchGroupById(it.id) }
                    kotlinx.coroutines.delay(10000) // Refrescar cada 10 segundos
                }
            }
            val product = appUiState.products.firstOrNull { it.id == productId }
            // Mostrar grupos activos que no hayan expirado
            // Los grupos creados por bodegueros pueden no tener participantes aún, pero deben ser visibles
            // Verificar tiempo real usando expiresAt
            val now = System.currentTimeMillis()
            val activeGroup = appUiState.groups.firstOrNull {
                it.productId == productId && 
                it.status == GroupStatus.ACTIVE && 
                it.expiresAt > now // Verificar tiempo real, no solo isExpired
            }
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            ProductDetailScreen(
                product = product,
                group = activeGroup,
                user = currentUser,
                timeRemaining = activeGroup?.timeRemaining ?: 0,
                onJoinGroup = { quantity ->
                    // Validar que solo CLIENTES pueden unirse a grupos
                    if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                        Toast.makeText(context, "Solo los clientes pueden hacer reservas", Toast.LENGTH_SHORT).show()
                        return@ProductDetailScreen
                    }
                    
                    currentUser?.let { user ->
                        scope.launch {
                            val targetGroup = activeGroup ?: product?.let { prod ->
                                groupRepository.createGroup(
                                    productId = prod.id,
                                    productName = prod.name,
                                    productImage = prod.imageUrl ?: "",
                                    creatorId = user.id,
                                    creatorAlias = user.alias,
                                    targetSize = prod.minGroupSize,
                                    storeId = prod.storeId,
                                    storeName = prod.storeName,
                                    normalPrice = prod.normalPrice,
                                    groupPrice = prod.groupPrice,
                                    initialReservedUnits = 0 // No crear participante automático
                                ).getOrNull()
                            }
                            targetGroup?.let { group ->
                                if (activeGroup != null) {
                                    val result = groupRepository.joinGroup(
                                        group.id,
                                        user.id,
                                        user.alias,
                                        user.avatar ?: "",
                                        user.district,
                                        quantity,
                                        user.points
                                    )
                                    result.onSuccess { updatedGroup ->
                                        // Verificar si es la primera vez que el usuario se une a este grupo
                                        val isFirstTime = updatedGroup.participants.none { 
                                            it.userId == user.id && it.status != com.nexusbiz.nexusbiz.data.model.ReservationStatus.CANCELLED 
                                        } || updatedGroup.participants.count { it.userId == user.id } == 1
                                        
                                        // Solo otorgar puntos si es la primera vez
                                        if (isFirstTime) {
                                            authRepository.addPoints(user.id, 5, "Unirse al grupo")
                                        }
                                        
                                        // Verificar si el grupo alcanzó la meta (pasó a PICKUP)
                                        if (updatedGroup.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) {
                                            // Otorgar +20 puntos a todos los participantes por completar el grupo
                                            updatedGroup.participants.forEach { participant ->
                                                if (participant.status != com.nexusbiz.nexusbiz.data.model.ReservationStatus.CANCELLED) {
                                                    authRepository.addPoints(participant.userId, 20, "Completar grupo")
                                                }
                                            }
                                        }
                                        
                                        navController.navigate(Screen.ReservationSuccess.createRoute(quantity))
                                    }
                                    result.onFailure { error ->
                                        Toast.makeText(context, error.message ?: "Error al reservar", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Si se creó un grupo nuevo, ahora unirse a él
                                    val result = groupRepository.joinGroup(
                                        group.id,
                                        user.id,
                                        user.alias,
                                        user.avatar ?: "",
                                        user.district,
                                        quantity,
                                        user.points
                                    )
                                    result.onSuccess { updatedGroup ->
                                        // Otorgar +5 puntos por unirse al grupo (siempre es primera vez cuando se crea)
                                        authRepository.addPoints(user.id, 5, "Unirse al grupo")
                                        
                                        // Verificar si el grupo alcanzó la meta (pasó a PICKUP)
                                        if (updatedGroup.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) {
                                            // Otorgar +20 puntos a todos los participantes por completar el grupo
                                            updatedGroup.participants.forEach { participant ->
                                                if (participant.status != com.nexusbiz.nexusbiz.data.model.ReservationStatus.CANCELLED) {
                                                    authRepository.addPoints(participant.userId, 20, "Completar grupo")
                                                }
                                            }
                                        }
                                        
                                        navController.navigate(Screen.ReservationSuccess.createRoute(quantity))
                                    }
                                    result.onFailure { error ->
                                        Toast.makeText(context, error.message ?: "Error al reservar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                },
                onCreateGroup = { quantity ->
                    // Validar que solo CLIENTES pueden crear grupos
                    if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                        Toast.makeText(context, "Solo los clientes pueden crear grupos", Toast.LENGTH_SHORT).show()
                        return@ProductDetailScreen
                    }
                    
                    product?.let { prod ->
                        currentUser?.let { user ->
                            scope.launch {
                                groupRepository.createGroup(
                                    productId = prod.id,
                                    productName = prod.name,
                                    productImage = prod.imageUrl ?: "",
                                    creatorId = user.id,
                                    creatorAlias = user.alias,
                                    targetSize = prod.minGroupSize,
                                    storeId = prod.storeId,
                                    storeName = prod.storeName,
                                    normalPrice = prod.normalPrice,
                                    groupPrice = prod.groupPrice,
                                    initialReservedUnits = 0 // No crear participante automático
                                ).fold(
                                    onSuccess = { group ->
                                        // Después de crear el grupo, unirse a él
                                        groupRepository.joinGroup(
                                            group.id,
                                            user.id,
                                            user.alias,
                                            user.avatar ?: "",
                                            user.district,
                                            quantity,
                                            user.points
                                        ).fold(
                                            onSuccess = { updatedGroup ->
                                                // Otorgar +5 puntos por unirse al grupo (siempre es primera vez cuando se crea)
                                                authRepository.addPoints(user.id, 5, "Unirse al grupo")
                                                navController.navigate(Screen.ReservationSuccess.createRoute(quantity))
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(context, "Error al reservar: ${error.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
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
            ReservationSuccessScreen(
                quantity = quantity,
                onGoHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onViewReservations = {
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
            val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(userId) {
                if (userId.isNotBlank()) {
                    appViewModel.fetchGroups(userId)
                }
            }
            val myGroups = appUiState.groups
            val activeGroups = myGroups.filter { it.status == GroupStatus.ACTIVE && !it.isExpired }
            val pickupGroups = myGroups.filter { it.status == GroupStatus.PICKUP }
            val completedGroups = myGroups.filter { it.status == GroupStatus.COMPLETED || it.status == GroupStatus.VALIDATED }
            val expiredGroups = myGroups.filter {
                (it.status == GroupStatus.EXPIRED || it.isExpired) &&
                    it.status != GroupStatus.COMPLETED &&
                    it.status != GroupStatus.VALIDATED &&
                    it.status != GroupStatus.PICKUP
            }
            MyGroupsScreen(
                activeGroups = activeGroups,
                pickupGroups = pickupGroups,
                completedGroups = completedGroups,
                expiredGroups = expiredGroups,
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
            LaunchedEffect(groupId) { appViewModel.fetchGroupById(groupId) }
            val group = appUiState.groups.firstOrNull { it.id == groupId }
            when {
                group?.status == GroupStatus.COMPLETED || group?.status == GroupStatus.VALIDATED -> {
                    GroupCompletedConsumerScreen(
                        group = group,
                        currentUser = currentUserState,
                        onBack = { navController.popBackStack() },
                        onViewOffers = { navController.navigate(Screen.Home.route) },
                        onViewGroups = {
                            navController.popBackStack()
                            navController.navigate(Screen.MyGroups.route)
                        }
                    )
                }
                group?.status == GroupStatus.EXPIRED -> {
                    GroupExpiredConsumerScreen(
                        group = group,
                        onBack = { navController.popBackStack() },
                        onQuickBuy = { productId ->
                            navController.navigate(Screen.QuickBuy.createRoute(productId))
                        },
                        onExplore = { navController.navigate(Screen.Home.route) }
                    )
                }
                group?.isExpired == true && group.status != GroupStatus.COMPLETED && group.status != GroupStatus.VALIDATED && group.status != GroupStatus.PICKUP -> {
                    GroupExpiredConsumerScreen(
                        group = group,
                        onBack = { navController.popBackStack() },
                        onQuickBuy = { productId ->
                            navController.navigate(Screen.QuickBuy.createRoute(productId))
                        },
                        onExplore = { navController.navigate(Screen.Home.route) }
                    )
                }
                group?.status == GroupStatus.PICKUP -> {
                    GroupReadyForPickupScreen(
                        group = group,
                        currentUser = currentUserState,
                        onBack = { navController.popBackStack() },
                        onViewQR = { navController.navigate(Screen.PickupQR.createRoute(group.id)) }
                    )
                }
                else -> {
                    GroupReservedScreen(
                        group = group,
                        currentUser = currentUserState,
                        onBack = { navController.popBackStack() },
                        onShare = { }
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
                currentUserState?.let { appViewModel.fetchGroups(it.id) }
                appViewModel.fetchGroupById(groupId)
            }
            val group = appUiState.groups.firstOrNull { it.id == groupId }
            PickupQRScreen(
                group = group,
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
