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
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.data.repository.StoreRepository
import com.nexusbiz.nexusbiz.ui.screens.quickbuy.QuickBuyScreen
import com.nexusbiz.nexusbiz.ui.screens.quickbuy.StoreDetailScreen
import com.nexusbiz.nexusbiz.ui.viewmodel.AuthViewModel
import com.nexusbiz.nexusbiz.util.LocationHelper
import com.nexusbiz.nexusbiz.util.Screen
import com.nexusbiz.nexusbiz.util.onSuccess
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

fun androidx.navigation.NavGraphBuilder.sharedNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    productRepository: ProductRepository,
    storeRepository: StoreRepository,
    authRepository: AuthRepository
) {
    navigation(
        route = SHARED_GRAPH_ROUTE,
        startDestination = Screen.QuickBuy.route
    ) {
        composable(
            Screen.QuickBuy.route,
            arguments = listOf(navArgument(Screen.QuickBuy.PRODUCT_ID_ARG) { 
                type = NavType.StringType
                defaultValue = "" // Permitir que sea opcional para búsqueda
            })
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
            
            val productIdArg = backStackEntry.arguments?.getString(Screen.QuickBuy.PRODUCT_ID_ARG) ?: ""
            
            // Función para abrir Google Maps con una bodega
            val openGoogleMaps: (com.nexusbiz.nexusbiz.data.model.Store) -> Unit = { store ->
                try {
                    // Intentar usar coordenadas primero para navegación directa
                    val lat = store.latitude
                    val lon = store.longitude
                    if (lat != null && lon != null) {
                        // Abrir Google Maps con navegación directa usando coordenadas
                        val uri = android.net.Uri.parse("google.navigation:q=$lat,$lon")
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.google.android.apps.maps")
                        context.startActivity(intent)
                    } else {
                        // Si no hay coordenadas, usar la dirección
                        val address = store.address.takeIf { it.isNotBlank() } ?: store.name
                        val district = store.district.takeIf { it.isNotBlank() } ?: ""
                        val query = android.net.Uri.encode("$address, $district")
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                        )
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    // Si Google Maps no está disponible, intentar con navegador web
                    try {
                        val address = store.address.takeIf { it.isNotBlank() } ?: store.name
                        val district = store.district.takeIf { it.isNotBlank() } ?: ""
                        val query = android.net.Uri.encode("$address, $district")
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                        )
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Si falla, no hacer nada
                    }
                }
            }
            val productId = if (productIdArg.isBlank()) null else productIdArg
            
            var product by remember { mutableStateOf<com.nexusbiz.nexusbiz.data.model.Product?>(null) }
            var stores by remember { mutableStateOf(emptyList<com.nexusbiz.nexusbiz.data.model.Store>()) }
            var isLoadingStores by remember { mutableStateOf(false) }
            var allProducts by remember { mutableStateOf(emptyList<com.nexusbiz.nexusbiz.data.model.Product>()) }
            val currentClient = authViewModel.currentClient
            
            // Cargar todos los productos para la búsqueda
            LaunchedEffect(Unit) {
                val userDistrict = currentClient?.district?.takeIf { it.isNotBlank() } ?: "Trujillo"
                allProducts = productRepository.getProducts(userDistrict, null, null)
            }
            
            // Si hay productId, cargar producto y bodegas
            LaunchedEffect(productId, currentClient) {
                if (productId != null) {
                    isLoadingStores = true
                    product = productRepository.getProductById(productId)
                    
                    // Siempre obtener ubicación para calcular distancias
                    // Primero intentar usar ubicación guardada, si no está, obtenerla del dispositivo
                    val userLat: Double?
                    val userLon: Double?
                    
                    if (currentClient?.latitude != null && currentClient.longitude != null) {
                        // Usar ubicación guardada
                        userLat = currentClient.latitude
                        userLon = currentClient.longitude
                        android.util.Log.d("SharedNavGraph", "Usando ubicación guardada: lat=$userLat, lon=$userLon")
                    } else {
                        // Obtener ubicación actual del dispositivo y guardarla
                        val locationHelper = LocationHelper(context)
                        val location = locationHelper.getCurrentLocation()
                        userLat = location?.first
                        userLon = location?.second
                        
                        android.util.Log.d("SharedNavGraph", "Ubicación obtenida del dispositivo: lat=$userLat, lon=$userLon")
                        
                        // Guardar ubicación si se obtuvo
                        if (location != null && currentClient != null) {
                            scope.launch {
                                authRepository.updateUserLocation(
                                    currentClient.id,
                                    location.first,
                                    location.second
                                ).onSuccess {
                                    android.util.Log.d("SharedNavGraph", "Ubicación guardada correctamente en BD")
                                }
                            }
                        }
                    }
                    
                    // Obtener el distrito del producto o del usuario para filtrar ofertas
                    val productDistrict = product?.district
                    val userDistrict = currentClient?.district?.takeIf { it.isNotBlank() } ?: productDistrict
                    
                    android.util.Log.d("SharedNavGraph", "Buscando bodegas - distrito: $userDistrict, producto: ${product?.name}")
                    
                    // Buscar bodegas con stock del producto, siempre usando bodegas cercanas
                    // Pero filtrando ofertas por distrito para mostrar solo las relevantes
                    stores = storeRepository.getStoresWithStock(
                        productId = productId,
                        userDistrict = userDistrict, // Pasar el distrito para filtrar ofertas
                        userLat = userLat,
                        userLon = userLon,
                        useNearbyStores = true, // Siempre usar bodegas cercanas (5km)
                        userId = currentClient?.id
                    )
                    
                    android.util.Log.d("SharedNavGraph", "Bodegas encontradas: ${stores.size}")
                    isLoadingStores = false
                }
            }
            
            QuickBuyScreen(
                productId = productId,
                productName = product?.name,
                productImageUrl = product?.imageUrl,
                stores = stores,
                products = allProducts,
                onProductSelected = { selectedProductId ->
                    // Navegar a la misma pantalla pero con el productId seleccionado
                    navController.navigate(Screen.QuickBuy.createRoute(selectedProductId))
                },
                onStoreClick = openGoogleMaps, // Usar la función definida arriba
                onBack = { navController.popBackStack() },
                isLoadingStores = isLoadingStores
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
            val context = LocalContext.current
            StoreDetailScreen(
                store = store,
                productName = productName,
                onNavigate = {
                    store?.let { s ->
                        try {
                            // Intentar usar coordenadas primero para navegación directa
                            val lat = s.latitude
                            val lon = s.longitude
                            if (lat != null && lon != null) {
                                // Abrir Google Maps con navegación directa usando coordenadas
                                val uri = android.net.Uri.parse("google.navigation:q=$lat,$lon")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                intent.setPackage("com.google.android.apps.maps")
                                context.startActivity(intent)
                            } else {
                                // Si no hay coordenadas, usar la dirección
                                val address = s.address.takeIf { it.isNotBlank() } ?: s.name
                                val district = s.district.takeIf { it.isNotBlank() } ?: ""
                                val query = android.net.Uri.encode("$address, $district")
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                                )
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            // Si Google Maps no está disponible, intentar con navegador web
                            try {
                                val address = s.address.takeIf { it.isNotBlank() } ?: s.name
                                val district = s.district.takeIf { it.isNotBlank() } ?: ""
                                val query = android.net.Uri.encode("$address, $district")
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Si falla, no hacer nada
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
