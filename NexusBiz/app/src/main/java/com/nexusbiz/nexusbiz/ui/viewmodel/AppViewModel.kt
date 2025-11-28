package com.nexusbiz.nexusbiz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusbiz.nexusbiz.data.model.Group
import com.nexusbiz.nexusbiz.data.model.Product
import com.nexusbiz.nexusbiz.data.repository.GroupRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.util.onSuccess
import com.nexusbiz.nexusbiz.util.onFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(
    val products: List<Product> = emptyList(),
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class GroupCreateArgs(
    val productId: String,
    val productName: String,
    val productImage: String,
    val creatorId: String,
    val creatorAlias: String,
    val targetSize: Int,
    val storeId: String,
    val storeName: String,
    val normalPrice: Double,
    val groupPrice: Double,
    val durationHours: Int = 24
)

class AppViewModel(
    private val productRepository: ProductRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun fetchProducts(district: String? = null, category: String? = null, search: String? = null) {
        viewModelScope.launch {
            productRepository.fetchProducts(district.orEmpty(), category, search)
            _uiState.value = _uiState.value.copy(products = productRepository.products.value)
        }
    }

    fun fetchGroups(userId: String? = null) {
        viewModelScope.launch {
            groupRepository.fetchGroups(userId)
            _uiState.value = _uiState.value.copy(groups = groupRepository.groups.value)
        }
    }

    fun fetchGroupById(id: String) {
        viewModelScope.launch {
            groupRepository.getGroupById(id)?.let { group ->
                val current = _uiState.value.groups.toMutableList()
                val idx = current.indexOfFirst { it.id == id }
                if (idx >= 0) current[idx] = group else current.add(group)
                _uiState.value = _uiState.value.copy(groups = current)
            }
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

    fun createGroup(args: GroupCreateArgs, currentRole: com.nexusbiz.nexusbiz.ui.viewmodel.UserRole?) {
        viewModelScope.launch {
            // Validar que solo CLIENTES pueden crear grupos desde la app de consumidor
            // Las bodegas crean grupos automáticamente al publicar ofertas, pero no como participantes
            if (currentRole == com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.BODEGUERO) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Las bodegas no pueden crear grupos como participantes"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            groupRepository.createGroup(
                productId = args.productId,
                productName = args.productName,
                productImage = args.productImage,
                creatorId = args.creatorId,
                creatorAlias = args.creatorAlias,
                targetSize = args.targetSize,
                storeId = args.storeId,
                storeName = args.storeName,
                normalPrice = args.normalPrice,
                groupPrice = args.groupPrice,
                durationHours = args.durationHours,
                initialReservedUnits = 0 // No crear participante automático
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groups = groupRepository.groups.value
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun joinGroup(
        groupId: String, 
        userId: String, 
        alias: String, 
        avatar: String, 
        district: String?, 
        quantity: Int,
        userPoints: Int,
        currentRole: com.nexusbiz.nexusbiz.ui.viewmodel.UserRole?
    ) {
        viewModelScope.launch {
            // Validar que solo CLIENTES pueden unirse a grupos
            if (currentRole != com.nexusbiz.nexusbiz.ui.viewmodel.UserRole.CLIENTE) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Solo los clientes pueden hacer reservas"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            groupRepository.joinGroup(groupId, userId, alias, avatar, district, quantity, userPoints)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groups = groupRepository.groups.value
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }
}
