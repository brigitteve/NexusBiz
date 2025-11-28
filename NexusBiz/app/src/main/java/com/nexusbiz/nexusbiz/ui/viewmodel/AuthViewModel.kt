package com.nexusbiz.nexusbiz.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusbiz.nexusbiz.data.model.Store
import com.nexusbiz.nexusbiz.data.model.User
import com.nexusbiz.nexusbiz.data.model.UserType
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.util.onSuccess
import com.nexusbiz.nexusbiz.util.onFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UserRole {
    CLIENTE, BODEGUERO
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOnboardingComplete: Boolean = false,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // Estado global de sesión por rol
    var currentRole: UserRole? by mutableStateOf(null)
        private set
    
    var currentClient: User? by mutableStateOf(null)
        private set
    
    var currentStore: Store? by mutableStateOf(null)
        private set
    
    init {
        viewModelScope.launch {
            authRepository.isOnboardingComplete.collect { complete ->
                _uiState.value = _uiState.value.copy(isOnboardingComplete = complete)
            }
        }
        
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(isLoggedIn = user != null)
            }
        }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            authRepository.completeOnboarding()
        }
    }
    
    fun login(alias: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.login(alias, password)
                .onSuccess { user ->
                    if (user.userType == UserType.STORE_OWNER) {
                        authRepository.loginStore(alias, password)
                            .onSuccess { store ->
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                setStoreSession(store)
                                onSuccess()
                            }
                            .onFailure { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = error.message
                                )
                            }
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        setClientSession(user)
                        onSuccess()
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    fun loginStore(alias: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.loginStore(alias, password)
                .onSuccess { store ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Establecer sesión de bodeguero
                    setStoreSession(store)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    fun register(
        alias: String,
        password: String,
        fechaNacimiento: String,
        distrito: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.register(alias, password, fechaNacimiento, distrito)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    setClientSession(user)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    fun changePassword(
        userId: String,
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.changePassword(userId, oldPassword, newPassword)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // Limpiar estado global de sesión
            currentRole = null
            currentClient = null
            currentStore = null
        }
    }
    
    // Funciones para establecer sesión según rol
    fun setClientSession(user: User) {
        currentRole = UserRole.CLIENTE
        currentClient = user
        currentStore = null
    }
    
    fun setStoreSession(store: Store) {
        currentRole = UserRole.BODEGUERO
        currentStore = store
        currentClient = null
    }
    
    fun registerStoreOwner(
        alias: String,
        password: String,
        fechaNacimiento: String,
        ruc: String,
        razonSocial: String,
        nombreComercial: String,
        district: String,
        address: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.registerStoreOwner(
                alias = alias,
                password = password,
                fechaNacimiento = fechaNacimiento,
                ruc = ruc,
                razonSocial = razonSocial,
                nombreComercial = nombreComercial,
                district = district,
                address = address
            )
                .onSuccess { store ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    setStoreSession(store)
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                    onError(error.message ?: "Error desconocido")
                }
        }
    }
}
