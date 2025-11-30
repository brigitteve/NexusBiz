package com.nexusbiz.nexusbiz.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.data.repository.OfferRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.data.repository.StoreRepository
import com.nexusbiz.nexusbiz.ui.viewmodel.AppViewModel
import com.nexusbiz.nexusbiz.ui.viewmodel.AuthViewModel
import com.nexusbiz.nexusbiz.ui.viewmodel.UserRole
import com.nexusbiz.nexusbiz.util.Screen

internal const val AUTH_GRAPH_ROUTE = "auth_graph"
internal const val CONSUMER_GRAPH_ROUTE = "consumer_graph"
internal const val STORE_GRAPH_ROUTE = "store_graph"
internal const val SHARED_GRAPH_ROUTE = "shared_graph"

@Composable
fun RootNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    appViewModel: AppViewModel,
    authRepository: AuthRepository,
    productRepository: ProductRepository,
    offerRepository: OfferRepository,
    storeRepository: StoreRepository
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val startGraph = when {
        !authState.isOnboardingComplete -> AUTH_GRAPH_ROUTE
        !authState.isLoggedIn -> AUTH_GRAPH_ROUTE
        authViewModel.currentRole == UserRole.BODEGUERO -> STORE_GRAPH_ROUTE
        authViewModel.currentRole == UserRole.CLIENTE -> CONSUMER_GRAPH_ROUTE
        else -> AUTH_GRAPH_ROUTE
    }

    val authStartDestination = when {
        !authState.isOnboardingComplete -> Screen.Onboarding1.route
        !authState.isLoggedIn -> Screen.Login.route
        else -> Screen.Login.route
    }

    LaunchedEffect(startGraph) {
        navController.navigate(startGraph) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startGraph
    ) {
        authNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            authRepository = authRepository,
            startDestination = authStartDestination
        )
        consumerNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            appViewModel = appViewModel,
            authRepository = authRepository,
            productRepository = productRepository,
            offerRepository = offerRepository
        )
        storeNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            appViewModel = appViewModel,
            authRepository = authRepository,
            productRepository = productRepository,
            offerRepository = offerRepository,
            storeRepository = storeRepository
        )
        sharedNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            productRepository = productRepository,
            storeRepository = storeRepository,
            authRepository = authRepository
        )
    }
}
