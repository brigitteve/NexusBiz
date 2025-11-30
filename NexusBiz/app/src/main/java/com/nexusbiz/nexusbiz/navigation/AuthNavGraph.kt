package com.nexusbiz.nexusbiz.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.nexusbiz.nexusbiz.data.model.Store
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.ui.screens.auth.ChangePasswordScreen
import com.nexusbiz.nexusbiz.ui.screens.auth.SelectDistrictScreen
import com.nexusbiz.nexusbiz.ui.screens.auth.ForgotPasswordScreen
import com.nexusbiz.nexusbiz.ui.screens.auth.LoginScreen
import com.nexusbiz.nexusbiz.ui.screens.auth.RegisterScreen
import com.nexusbiz.nexusbiz.ui.screens.onboarding.OnboardingScreen1
import com.nexusbiz.nexusbiz.ui.screens.onboarding.OnboardingScreen2
import com.nexusbiz.nexusbiz.ui.screens.store.BodegaCommercialDataScreen
import com.nexusbiz.nexusbiz.ui.screens.store.BodegaCredentialsScreen
import com.nexusbiz.nexusbiz.ui.screens.store.BodegaRegistrationModalScreen
import com.nexusbiz.nexusbiz.ui.screens.store.BodegaRegistrationSuccessScreen
import com.nexusbiz.nexusbiz.ui.screens.store.BodegaValidateRucScreen
import com.nexusbiz.nexusbiz.ui.viewmodel.AuthViewModel
import com.nexusbiz.nexusbiz.util.Screen
import kotlinx.coroutines.launch

fun androidx.navigation.NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    authRepository: AuthRepository,
    startDestination: String
) {
    navigation(
        route = AUTH_GRAPH_ROUTE,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding1.route) {
            val scope = rememberCoroutineScope()
            OnboardingScreen1(
                onNext = { navController.navigate(Screen.Onboarding2.route) },
                onSkip = {
                    scope.launch { authRepository.completeOnboarding() }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding1.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Onboarding2.route) {
            val scope = rememberCoroutineScope()
            OnboardingScreen2(
                onNext = {
                    scope.launch { authRepository.completeOnboarding() }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding1.route) { inclusive = true }
                    }
                },
                onSkip = {
                    scope.launch { authRepository.completeOnboarding() }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding1.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    when (authViewModel.currentRole) {
                        com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO -> {
                            navController.navigate(Screen.StoreDashboard.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                        else -> {
                            navController.navigate(Screen.SelectDistrict.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onNavigateToRegisterBodega = { navController.navigate(Screen.BodegaRegistrationModal.route) },
                isLoading = authViewModel.uiState.value.isLoading,
                errorMessage = authViewModel.uiState.value.errorMessage,
                onLogin = { alias, password ->
                    authViewModel.login(alias, password) {
                        when (authViewModel.currentRole) {
                            com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO -> {
                                navController.navigate(Screen.StoreDashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            else -> {
                                navController.navigate(Screen.SelectDistrict.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            )
        }
        composable(Screen.LoginBodega.route) {
            LaunchedEffect(Unit) {
                if (authViewModel.currentRole == com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.LoginBodega.route) { inclusive = true }
                    }
                    return@LaunchedEffect
                }
            }
            LoginScreen(
                onLoginSuccess = {
                    if (authViewModel.currentRole == com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                        navController.navigate(Screen.StoreDashboard.route) {
                            popUpTo(Screen.LoginBodega.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.BodegaRegistrationModal.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onNavigateToRegisterBodega = { navController.navigate(Screen.BodegaRegistrationModal.route) },
                isLoading = authViewModel.uiState.value.isLoading,
                errorMessage = authViewModel.uiState.value.errorMessage,
                onLogin = { alias, password ->
                    authViewModel.loginStore(alias, password) {
                        navController.navigate(Screen.StoreDashboard.route) {
                            popUpTo(Screen.LoginBodega.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.SelectDistrict.route) {
            val scope = rememberCoroutineScope()
            val currentUser = authViewModel.currentClient
            
            SelectDistrictScreen(
                initialDistrict = currentUser?.district?.takeIf { it.isNotBlank() },
                onBack = {
                    navController.popBackStack()
                },
                onConfirmDistrict = { district ->
                    currentUser?.let { user ->
                        scope.launch {
                            val result = authRepository.updateUserDistrict(user.id, district)
                            result.fold(
                                onSuccess = {
                                    // Distrito actualizado correctamente en la BD, navegar
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onFailure = { error ->
                                    // Error al actualizar, pero navegar de todos modos
                                    // El error se puede mostrar al usuario si es necesario
                                    android.util.Log.e("AuthNavGraph", "Error al guardar distrito: ${error.message}")
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                    } ?: run {
                        // Si no hay usuario actual, solo navegar
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onConfirmNearby = {
                    // Guardar flag de bodegas cercanas en el estado guardado
                    navController.currentBackStackEntry?.savedStateHandle?.set("useNearbyStores", true)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            val scope = rememberCoroutineScope()
            RegisterScreen(
                onCreateAccount = { alias, password, fechaNacimiento, distrito ->
                    authViewModel.register(alias, password, fechaNacimiento, distrito) {
                        navController.navigate(Screen.SelectDistrict.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ChangePassword.route) {
            val currentUser = authViewModel.currentClient
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
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onResetPassword = { alias, newPassword ->
                    authViewModel.resetPasswordByAlias(alias, newPassword) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                        }
                    }
                },
                isLoading = authViewModel.uiState.value.isLoading,
                errorMessage = authViewModel.uiState.value.errorMessage
            )
        }
        composable(Screen.BodegaRegistrationModal.route) {
            BodegaRegistrationModalScreen(
                onStart = { navController.navigate(Screen.BodegaValidateRUC.route) },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Screen.BodegaValidateRUC.route) {
            BodegaValidateRucScreen(
                onBack = { navController.popBackStack() },
                onValidated = { ruc, razonSocial, nombreComercial ->
                    navController.currentBackStackEntry?.savedStateHandle?.apply {
                        set("ruc", ruc)
                        set("razonSocial", razonSocial)
                        set("nombreComercial", nombreComercial)
                    }
                    navController.navigate(Screen.BodegaCommercialData.route)
                }
            )
        }
        composable(Screen.BodegaCommercialData.route) {
            val ruc =
                navController.previousBackStackEntry?.savedStateHandle?.get<String>("ruc")
                    .orEmpty()
            val razonSocial =
                navController.previousBackStackEntry?.savedStateHandle?.get<String>("razonSocial")
                    .orEmpty()
            val nombreComercial =
                navController.previousBackStackEntry?.savedStateHandle?.get<String>("nombreComercial")
                    .orEmpty()
            BodegaCommercialDataScreen(
                onBack = { navController.popBackStack() },
                onContinue = { district, address ->
                    navController.currentBackStackEntry?.savedStateHandle?.apply {
                        set("ruc", ruc)
                        set("razonSocial", razonSocial)
                        set("nombreComercial", nombreComercial)
                        set("district", district)
                        set("address", address)
                    }
                    navController.navigate(Screen.BodegaCredentials.route)
                },
                razonSocial = razonSocial,
                nombreComercial = nombreComercial
            )
        }
        composable(Screen.BodegaCredentials.route) {
            val razonSocial =
                navController.previousBackStackEntry?.savedStateHandle?.get<String>("razonSocial")
                    .orEmpty()
            val nombreComercial =
                navController.previousBackStackEntry?.savedStateHandle?.get<String>("nombreComercial")
                    .orEmpty()
            val district =
                navController.previousBackStackEntry?.savedStateHandle?.get<String>("district")
                    .orEmpty()
            val address =
                navController.previousBackStackEntry?.savedStateHandle?.get<String>("address")
                    .orEmpty()
            val ruc =
                navController.previousBackStackEntry?.savedStateHandle?.get<String>("ruc")
                    .orEmpty()
            BodegaCredentialsScreen(
                onBack = { navController.popBackStack() },
                onContinue = { alias, password, fechaNacimiento ->
                    authViewModel.registerStoreOwner(
                        alias = alias,
                        password = password,
                        fechaNacimiento = fechaNacimiento,
                        ruc = ruc,
                        razonSocial = razonSocial,
                        nombreComercial = nombreComercial,
                        district = district,
                        address = address,
                        onSuccess = {
                            navController.navigate(Screen.BodegaRegistrationSuccess.route) {
                                popUpTo(Screen.Login.route) { inclusive = false }
                            }
                        },
                        onError = { error ->
                            // El error se mostrará en el UI state
                        }
                    )
                },
                authRepository = authRepository
            )
        }
        composable(Screen.BodegaRegistrationSuccess.route) {
            val store = authViewModel.currentStore
            BodegaRegistrationSuccessScreen(
                storeName = store?.commercialName ?: store?.name ?: "",
                onFinish = {
                    navController.navigate(Screen.StoreDashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
