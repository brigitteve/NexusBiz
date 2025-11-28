package com.nexusbiz.nexusbiz.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.data.model.Group
import com.nexusbiz.nexusbiz.data.model.GroupStatus
import com.nexusbiz.nexusbiz.data.model.Participant
import com.nexusbiz.nexusbiz.ui.components.StoreBottomNavBar
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProductsScreen(
    groups: List<Group> = emptyList(),
    onGroupClick: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
            var selectedTab by rememberSaveable { mutableStateOf("active") }

            val groupsToUse = remember(groups) {
                groups
            }
    
    // Categorizar ofertas según la lógica del React
    val now = System.currentTimeMillis()
    
    // ACTIVOS: Están activos Y no han expirado Y no alcanzaron la meta
            val activeOffers = groupsToUse.filter { group ->
        val isNotExpired = group.expiresAt > now
        val isNotComplete = group.reservedUnits < group.targetSize
        val isActive = group.status == GroupStatus.ACTIVE
        isActive && isNotExpired && isNotComplete
    }
    
    // EXPIRADOS: Expiró el tiempo O ya no están activos PERO no alcanzaron la meta
            val expiredOffers = groupsToUse.filter { group ->
        val isExpired = group.expiresAt <= now || group.status != GroupStatus.ACTIVE
        val isNotComplete = group.reservedUnits < group.targetSize
        isExpired && isNotComplete
    }
    
    // FINALIZADOS: Ya no están activos (isActive: false) Y alcanzaron la meta
            val completedOffers = groupsToUse.filter { group ->
        val isComplete = group.reservedUnits >= group.targetSize
        val isInactive = group.status != GroupStatus.ACTIVE
        isInactive && isComplete
    }
    
    val offersForTab = when (selectedTab) {
        "active" -> activeOffers
        "expired" -> expiredOffers
        "completed" -> completedOffers
        else -> emptyList()
    }
    
    Scaffold(
        containerColor = Color(0xFFF4F4F7),
        bottomBar = {
            StoreBottomNavBar(
                currentRoute = "store_offers",
                onItemClick = { route ->
                    when (route) {
                        "store_dashboard" -> onNavigateToDashboard()
                        "store_offers" -> Unit
                        "store_profile" -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
                modifier = Modifier
                    .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Atrás",
                                tint = Color(0xFF606060),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Mis Productos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                }
            }
            
            // Tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TabButtonWithBadge(
                        text = "Activos",
                        badgeCount = activeOffers.size,
                        badgeColor = Color.White,
                        badgeBackground = Color(0xFF10B981),
                        isSelected = selectedTab == "active",
                        onClick = { selectedTab = "active" },
                        modifier = Modifier.weight(1f)
                    )
                    TabButtonWithBadge(
                        text = "Expirados",
                        badgeCount = expiredOffers.size,
                        badgeColor = Color.White,
                        badgeBackground = Color(0xFF606060),
                        isSelected = selectedTab == "expired",
                        onClick = { selectedTab = "expired" },
                        modifier = Modifier.weight(1f)
                    )
                    TabButtonWithBadge(
                        text = "Finalizados",
                        badgeCount = completedOffers.size,
                        badgeColor = Color(0xFF10B981),
                        badgeBackground = Color(0xFF10B981).copy(alpha = 0.2f),
                        isSelected = selectedTab == "completed",
                        onClick = { selectedTab = "completed" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (offersForTab.isEmpty()) {
                    item {
                        when (selectedTab) {
                            "active" -> EmptyState(
                                icon = Icons.Default.ShoppingBag,
                                title = "No tienes ofertas activas",
                                subtitle = "Crea una nueva oferta para comenzar"
                            )
                            "expired" -> EmptyState(
                                icon = Icons.Default.AccessTime,
                                title = "No hay ofertas expiradas"
                            )
                            "completed" -> EmptyState(
                                icon = Icons.Default.People,
                                title = "No hay grupos completados",
                                subtitle = "Los grupos finalizados aparecerán aquí"
                            )
                            else -> EmptyState(
                                icon = Icons.Default.ShoppingBag,
                                title = "No hay ofertas"
                            )
                        }
                    }
                } else {
                    items(offersForTab, key = { it.id }) { offer ->
                        when (selectedTab) {
                            "active" -> OfferCard(
                                group = offer,
                                status = "Activo",
                                onClick = { onGroupClick(offer.id) }
                            )
                            "expired" -> OfferCard(
                                group = offer,
                                status = "Expirado",
                                onClick = { onGroupClick(offer.id) }
                            )
                            "completed" -> OfferCard(
                                group = offer,
                                status = "Finalizado",
                                onClick = { onGroupClick(offer.id) }
                            )
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButtonWithBadge(
    text: String,
    badgeCount: Int,
    badgeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeBackground: Color = badgeColor
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFFF4F4F7) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF606060)
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = badgeBackground
                ) {
                    Text(
                        text = badgeCount.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    group: Group,
    status: String,
    onClick: () -> Unit
) {
    val progress = group.progress.coerceIn(0f, 1f)
    val timeRemaining = getTimeRemaining(group.expiresAt)
    
    val statusConfig = when (status) {
        "Activo" -> OfferStatusConfig(
            backgroundColor = Color(0xFF10B981),
            textColor = Color.White,
            label = "Activo"
        )
        "Expirado" -> OfferStatusConfig(
            backgroundColor = Color(0xFF606060),
            textColor = Color.White,
            label = "Expirado"
        )
        "Finalizado" -> OfferStatusConfig(
            backgroundColor = Color(0xFF10B981).copy(alpha = 0.1f),
            textColor = Color(0xFF10B981),
            label = "Finalizado"
        )
        else -> OfferStatusConfig(
            backgroundColor = Color(0xFF606060),
            textColor = Color.White,
            label = status
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image
            AsyncImage(
                model = group.productImage.ifEmpty { "https://via.placeholder.com/150" },
                contentDescription = group.productName,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F4F7)),
                contentScale = ContentScale.Crop
            )
            
            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = group.productName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusConfig.backgroundColor
                    ) {
                        Text(
                            text = statusConfig.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusConfig.textColor
                        )
                    }
                }
                
                // Progress (solo para no expirados)
                if (status != "Expirado") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFFF4F4F7)
                        )
                        Text(
                        text = "${group.reservedUnits}/${group.targetSize} unidades · Quedan $timeRemaining",
                            fontSize = 14.sp,
                            color = Color(0xFF606060)
                        )
                    }
                } else {
                        Text(
                        text = "Solo ${group.reservedUnits} de ${group.targetSize} unidades · $timeRemaining",
                        fontSize = 14.sp,
                        color = Color(0xFF606060)
                    )
                }
                
                // Info adicional para finalizados
                if (status == "Finalizado") {
                    Text(
                        text = "${group.targetSize} unidades vendidas",
                        fontSize = 14.sp,
                        color = Color(0xFF10B981)
                    )
                }
                
                // Action
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                        text = "Ver grupo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private data class OfferStatusConfig(
    val backgroundColor: Color,
    val textColor: Color,
    val label: String
)

private fun getTimeRemaining(expiresAt: Long): String {
    val now = System.currentTimeMillis()
    val diff = expiresAt - now
    
    if (diff <= 0) return "Expirado"
    
    val hours = floor((diff / (1000 * 60 * 60)).toDouble()).toInt()
    val minutes = floor(((diff % (1000 * 60 * 60)) / (1000 * 60)).toDouble()).toInt()
    
    return when {
        hours > 24 -> {
            val days = hours / 24
            "${days}d ${hours % 24}h"
        }
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFF4F4F7),
                modifier = Modifier.size(64.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF606060)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF606060).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

