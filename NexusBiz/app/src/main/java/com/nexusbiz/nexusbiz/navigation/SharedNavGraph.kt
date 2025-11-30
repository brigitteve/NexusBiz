package com.nexusbiz.nexusbiz.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.data.repository.StoreRepository
import com.nexusbiz.nexusbiz.ui.screens.quickbuy.QuickBuyScreen
import com.nexusbiz.nexusbiz.ui.screens.quickbuy.StoreDetailScreen
import com.nexusbiz.nexusbiz.ui.viewmodel.AuthViewModel
import com.nexusbiz.nexusbiz.util.LocationHelper
import com.nexusbiz.nexusbiz.util.Screen
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

fun androidx.navigation.NavGraphBuilder.sharedNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    productRepository: ProductRepository,
    storeRepository: StoreRepository
) {
    navigation(
        route = SHARED_GRAPH_ROUTE,
        startDestination = Screen.QuickBuy.route
    ) {
        composable(
            Screen.QuickBuy.route,
            arguments = listOf(navArgument(Screen.QuickBuy.PRODUCT_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.QuickBuy.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            
            val productId = backStackEntry.arguments?.getString(Screen.QuickBuy.PRODUCT_ID_ARG) ?: ""
            var product by remember { mutableStateOf<com.nexusbiz.nexusbiz.data.model.Product?>(null) }
            var stores by remember { mutableStateOf(emptyList<com.nexusbiz.nexusbiz.data.model.Store>()) }
            val currentClient = authViewModel.currentClient
            
            // Obtener flag de bodegas cercanas del estado guardado
            val useNearbyStores = remember {
                navController.previousBackStackEntry?.savedStateHandle?.get<Boolean>("useNearbyStores") ?: false
            }
            
            // Limpiar el flag después de leerlo
            navController.previousBackStackEntry?.savedStateHandle?.remove<Boolean>("useNearbyStores")
            
            LaunchedEffect(productId, currentClient, useNearbyStores) {
                product = productRepository.getProductById(productId)
                
                val userLat: Double?
                val userLon: Double?
                
                if (useNearbyStores) {
                    // Si se solicitan bodegas cercanas, obtener ubicación actual
                    val locationHelper = LocationHelper(context)
                    val location = locationHelper.getCurrentLocation()
                    userLat = location?.first
                    userLon = location?.second
                } else {
                    // Usar ubicación del perfil del usuario
                    userLat = currentClient?.latitude
                    userLon = currentClient?.longitude
                }
                
                stores = storeRepository.getStoresWithStock(
                    productId = productId,
                    userDistrict = if (useNearbyStores) null else currentClient?.district,
                    userLat = userLat,
                    userLon = userLon,
                    useNearbyStores = useNearbyStores,
                    userId = currentClient?.id
                )
            }
            QuickBuyScreen(
                productName = product?.name ?: "Producto",
                productImageUrl = product?.imageUrl ?: "",
                stores = stores,
                onStoreClick = { storeId ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("productName", product?.name ?: "Producto")
                    navController.navigate(Screen.StoreDetail.createRoute(storeId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.StoreDetail.route,
            arguments = listOf(navArgument(Screen.StoreDetail.STORE_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString(Screen.StoreDetail.STORE_ID_ARG) ?: ""
            var store by remember { mutableStateOf<com.nexusbiz.nexusbiz.data.model.Store?>(null) }
            LaunchedEffect(storeId) {
                store = storeRepository.getStoreById(storeId)
            }
            val productName = remember {
                backStackEntry.savedStateHandle.get<String>("productName") ?: "Producto"
            }
            StoreDetailScreen(
                store = store,
                productName = productName,
                onNavigate = {
                    store?.let { s ->
                        val lat = s.latitude
                        val lon = s.longitude
                        if (lat != null && lon != null) {
                            val uri = android.net.Uri.parse("google.navigation:q=$lat,$lon")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            try {
                                // context.startActivity(intent)
                            } catch (_: Exception) {
                                // Ignorar si no está disponible Maps
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
