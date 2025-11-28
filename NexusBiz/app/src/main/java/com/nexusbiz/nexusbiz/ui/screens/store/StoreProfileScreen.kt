package com.nexusbiz.nexusbiz.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusbiz.nexusbiz.data.model.Store
import com.nexusbiz.nexusbiz.data.model.User
import com.nexusbiz.nexusbiz.ui.components.StoreBottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreProfileScreen(
    store: Store?,
    user: User?,
    onBack: () -> Unit,
    onEditProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onTermsAndPrivacy: () -> Unit = {},
    onPlanPro: () -> Unit = {},
    onBoostVisibility: () -> Unit = {},
    onLogout: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToOffers: () -> Unit = {}
) {
    if (store == null && user == null) return
    val storeData = store
    val ownerAlias = user?.alias.orEmpty()

    Scaffold(
        containerColor = Color(0xFFF4F4F7),
        bottomBar = {
            StoreBottomNavBar(
                currentRoute = "store_profile",
                onItemClick = { route ->
                    when (route) {
                        "store_dashboard" -> onNavigateToDashboard()
                        "store_offers" -> onNavigateToOffers()
                        "store_profile" -> Unit
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color(0xFF606060)
                        )
                    }
                    Text(
                        text = "Mi Perfil",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card Principal de Información
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Información de la bodega",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        val storeName = storeData?.commercialName ?: storeData?.name ?: "Bodega"
                        val storeRuc = storeData?.ruc.orEmpty()
                        val razonSocial = storeData?.commercialName.orEmpty()
                        val ownerName = storeData?.ownerAlias ?: user?.alias ?: storeName
                        val alias = user?.alias ?: storeData?.ownerAlias ?: storeName
                        val district = storeData?.district ?: user?.district.orEmpty()
                        val address = storeData?.address.orEmpty()

                        InfoRow(
                            icon = Icons.Default.Store,
                            label = "Nombre de bodega",
                            value = storeName.ifEmpty { "Sin nombre" }
                        )

                        InfoRow(
                            icon = Icons.Default.Description,
                            label = "RUC",
                            value = if (storeRuc.isNotEmpty()) storeRuc else "No disponible"
                        )

                        InfoRow(
                            icon = Icons.Default.Description,
                            label = "Razón social",
                            value = if (razonSocial.isNotEmpty()) razonSocial else "No disponible"
                        )

                        InfoRow(
                            icon = Icons.Default.Person,
                            label = "Propietario",
                            value = if (ownerName.isNotEmpty()) ownerName else "No disponible"
                        )

                        InfoRow(
                            icon = Icons.Default.Person,
                            label = "Alias",
                            value = alias.ifEmpty { "No definido" }
                        )

                        InfoRow(
                            icon = Icons.Default.LocationOn,
                            label = "Distrito",
                            value = district.ifEmpty { "No definido" }
                        )

                        InfoRow(
                            icon = Icons.Default.LocationOn,
                            label = "Dirección",
                            value = if (address.isNotEmpty()) address else "No disponible"
                        )
                    }
                }

                // Card: Plan PRO
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onPlanPro),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFACC15), Color(0xFFFF914D))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Plan PRO",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Nuevo",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Desbloquea más funciones",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Card: Boost de visibilidad
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBoostVisibility),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFF914D).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.OfflineBolt,
                                    contentDescription = null,
                                    tint = Color(0xFFFF914D),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Boost de visibilidad",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    text = "Destaca tus ofertas por 24h",
                                    fontSize = 14.sp,
                                    color = Color(0xFF606060)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF606060),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Opciones de configuración
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton(
                        icon = Icons.Default.Person,
                        text = "Editar información",
                        onClick = onEditProfile
                    )
                    OptionButton(
                        icon = Icons.Default.Lock,
                        text = "Cambiar contraseña",
                        onClick = onChangePassword
                    )
                    OptionButton(
                        icon = Icons.Default.Description,
                        text = "Términos y privacidad",
                        onClick = onTermsAndPrivacy
                    )
                }

                // Botón de cierre de sesión
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEF2F2),
                        contentColor = Color(0xFFDC2626)
                    ),
                    border = BorderStroke(2.dp, Color(0xFFFEE2E2))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Cerrar sesión",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF606060),
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = Color(0xFF606060)
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF1A1A1A)
                )
            }
        }
        if (showDivider) {
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF4F4F7))
        }
    }
}

@Composable
private fun OptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF606060),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF1A1A1A)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF606060),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

