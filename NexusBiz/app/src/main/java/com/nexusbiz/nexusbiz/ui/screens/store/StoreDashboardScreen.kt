package com.nexusbiz.nexusbiz.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.data.model.Group
import com.nexusbiz.nexusbiz.data.model.GroupStatus
import com.nexusbiz.nexusbiz.data.model.Product
import com.nexusbiz.nexusbiz.ui.components.StoreBottomNavBar
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDashboardScreen(
    products: List<Product> = emptyList(),
    activeGroups: List<Group> = emptyList(),
    ownerAlias: String = "Bodega",
    storePlan: com.nexusbiz.nexusbiz.data.model.StorePlan = com.nexusbiz.nexusbiz.data.model.StorePlan.FREE,
    onPublishProduct: () -> Unit,
    onViewProducts: () -> Unit,
    onGroupClick: (String) -> Unit,
    onScanQR: () -> Unit,
    onBack: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onSwitchToConsumer: () -> Unit
) {
    val accent = Color(0xFF10B981)
    val warning = Color(0xFFFF914D)
    val muted = Color(0xFF606060)
    val background = Color(0xFFF4F4F7)
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    // Activos: ofertas con status ACTIVE que no han expirado
    val activeOffers = remember(activeGroups) {
        activeGroups.filter { it.status == GroupStatus.ACTIVE && !it.isExpired }
    }
    // En retiro: ofertas con status PICKUP (listas para escanear QR)
    val pickupOffers = remember(activeGroups) {
        activeGroups.filter { it.status == GroupStatus.PICKUP }
    }
    // Finalizados: ofertas completadas (VALIDATED o COMPLETED)
    val finishedOffers = remember(activeGroups) {
        activeGroups.filter { it.status == GroupStatus.VALIDATED || it.status == GroupStatus.COMPLETED }
    }

    Scaffold(
        containerColor = background,
        bottomBar = {
            StoreBottomNavBar(
                currentRoute = "store_dashboard",
                onItemClick = { route ->
                    when (route) {
                        "store_dashboard" -> Unit
                        "store_offers" -> onNavigateToOffers()
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
            HeaderSection(
                userName = ownerAlias,
                activeCount = activeOffers.size,
                pickupCount = pickupOffers.size,
                completedCount = finishedOffers.size,
                onSwitchMode = onSwitchToConsumer,
                onBack = onBack
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Button(
                        onClick = onScanQR,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Escanear QR de Cliente",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                item {
                    // Verificar si puede publicar más ofertas según el plan
                    val canPublishMore = if (storePlan == com.nexusbiz.nexusbiz.data.model.StorePlan.FREE) {
                        activeOffers.size < 2
                    } else {
                        true // Plan PRO: ofertas ilimitadas
                    }
                    
                    Button(
                        onClick = onPublishProduct,
                        enabled = canPublishMore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canPublishMore) accent else Color(0xFF9CA3AF),
                            disabledContainerColor = Color(0xFF9CA3AF)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (canPublishMore) {
                                "Publicar nueva oferta"
                            } else {
                                "Límite alcanzado (2/2)"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!canPublishMore) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Plan Gratuito: Solo 2 ofertas activas. Actualiza a PRO para más.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ofertas de hoy",
                            color = Color(0xFF1A1A1A),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = onViewProducts) {
                            Text("Ver todas", color = accent)
                        }
                    }
                }

                if (activeOffers.isEmpty()) {
                    item {
                        EmptyOfferState()
                    }
                } else {
                    items(activeOffers, key = { it.id }) { group ->
                        OfferCard(
                            group = group,
                            accent = accent,
                            warning = warning,
                            muted = muted,
                            currencyFormat = currencyFormat,
                            onClick = { onGroupClick(group.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    userName: String,
    activeCount: Int,
    pickupCount: Int,
    completedCount: Int,
    onSwitchMode: () -> Unit,
    onBack: () -> Unit
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFFFACC15), Color(0xFFFF914D)))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(gradient)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    IconButton(onClick = onSwitchMode) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar modo", tint = Color.White)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Hola,",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = userName.ifEmpty { "Bodega" },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill(label = "Activos", value = activeCount.toString())
                    StatPill(label = "En retiro", value = pickupCount.toString())
                    StatPill(label = "Finalizados", value = completedCount.toString())
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 4.dp,
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Text(label, fontSize = 12.sp, color = Color(0xFF606060))
        }
    }
}

@Composable
private fun OfferCard(
    group: Group,
    accent: Color,
    warning: Color,
    muted: Color,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val progress = group.progress.coerceIn(0f, 1f)
    val hoursRemaining = ((group.expiresAt - System.currentTimeMillis()) / (1000 * 60 * 60)).coerceAtLeast(0)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product Image
                AsyncImage(
                    model = group.productImage.ifEmpty { "https://via.placeholder.com/150" },
                    contentDescription = group.productName,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Product Name and Price
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = group.productName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "S/ ${String.format("%.2f", group.groupPrice.takeIf { it > 0 } ?: group.normalPrice)}",
                            fontSize = 14.sp,
                            color = Color(0xFF606060)
                        )
                    }
                    
                    // Progress and Stats
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accent,
                            trackColor = Color(0xFFF4F4F7)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = muted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${group.reservedUnits}/${group.targetSize} unidades",
                                    fontSize = 14.sp,
                                    color = muted
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = warning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Quedan ${hoursRemaining}h",
                                    fontSize = 14.sp,
                                    color = warning
                                )
                            }
                        }
                    }
                }
            }
            
            // Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, Color(0xFFF4F4F7)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accent
                    )
                ) {
                    Text(
                        text = "Ver grupo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOfferState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(vertical = 24.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(36.dp)
        )
        Text("Aún no tienes ofertas activas", color = Color(0xFF606060), textAlign = TextAlign.Center)
    }
}
