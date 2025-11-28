package com.nexusbiz.nexusbiz.ui.screens.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.nexusbiz.nexusbiz.ui.components.BottomNavBar
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsScreen(
    activeGroups: List<Group> = emptyList(),
    pickupGroups: List<Group> = emptyList(),
    completedGroups: List<Group> = emptyList(),
    expiredGroups: List<Group> = emptyList(),
    onActiveGroupClick: (String) -> Unit,
    onPickupGroupClick: (String) -> Unit,
    onCompletedGroupClick: (String) -> Unit,
    onExpiredGroupClick: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val backgroundColor = Color(0xFFF4F4F7)
    val accentColor = Color(0xFF10B981)
    val mutedTextColor = Color(0xFF606060)

    var selectedTabName by rememberSaveable { mutableStateOf(MyGroupTab.Active.name) }
    val selectedTab = MyGroupTab.valueOf(selectedTabName)

    val groupsForTab = when (selectedTab) {
        MyGroupTab.Active -> activeGroups.filterNot { it.isExpired }
        MyGroupTab.Pickup -> pickupGroups
        MyGroupTab.Completed -> completedGroups
        MyGroupTab.Expired -> (expiredGroups + activeGroups.filter { it.isExpired }).distinctBy { it.id }
    }
    val emptyMessage = when (selectedTab) {
        MyGroupTab.Active -> "No tienes grupos activos"
        MyGroupTab.Pickup -> "No tienes productos listos para retiro"
        MyGroupTab.Completed -> "Aún no has completado ninguna compra"
        MyGroupTab.Expired -> "No tienes grupos expirados"
    }

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            BottomNavBar(
                currentRoute = "my_groups",
                onItemClick = { route ->
                    when (route) {
                        "my_groups" -> Unit
                        "home" -> onNavigateToHome()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            Header(mutedTextColor = mutedTextColor)
            TabSwitcher(
                selectedTab = selectedTab,
                onTabSelected = { selectedTabName = it.name },
                accentColor = accentColor,
                mutedTextColor = mutedTextColor
            )
            if (groupsForTab.isEmpty()) {
                EmptyState(message = emptyMessage, mutedTextColor = mutedTextColor)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 6.dp)
                ) {
                    items(groupsForTab, key = { it.id }) { group ->
                        when (selectedTab) {
                            MyGroupTab.Active -> {
                                GroupReservationCard(
                                    group = group,
                                    accentColor = accentColor,
                                    mutedTextColor = mutedTextColor,
                                    onClick = { onActiveGroupClick(group.id) }
                                )
                            }
                            MyGroupTab.Pickup -> {
                                GroupRetiroCard(
                                    group = group,
                                    accentColor = accentColor,
                                    mutedTextColor = mutedTextColor,
                                    onClick = { onPickupGroupClick(group.id) }
                                )
                            }
                            MyGroupTab.Completed -> {
                                GroupCompletedCard(
                                    group = group,
                                    accentColor = accentColor,
                                    mutedTextColor = mutedTextColor,
                                    onClick = { onCompletedGroupClick(group.id) }
                                )
                            }
                            MyGroupTab.Expired -> {
                                GroupExpiredCard(
                                    group = group,
                                    accentColor = accentColor,
                                    mutedTextColor = mutedTextColor,
                                    onClick = { onExpiredGroupClick(group.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class MyGroupTab(val label: String) {
    Active("Activos"),
    Pickup("En retiro"),
    Completed("Completados"),
    Expired("Expirados")
}

private data class StatusBadgeStyle(
    val label: String,
    val background: Color,
    val content: Color
)

private fun statusBadgeFor(status: GroupStatus): StatusBadgeStyle {
    return when (status) {
        GroupStatus.ACTIVE -> StatusBadgeStyle(
            label = "En reserva",
            background = Color(0xFFFACC15),
            content = Color(0xFF1A1A1A)
        )
        GroupStatus.PICKUP -> StatusBadgeStyle(
            label = "Listo para retirar",
            background = Color(0xFF10B981),
            content = Color.White
        )
        GroupStatus.VALIDATED, GroupStatus.COMPLETED -> StatusBadgeStyle(
            label = "Completado",
            background = Color(0xFF10B981).copy(alpha = 0.1f),
            content = Color(0xFF10B981)
        )
        GroupStatus.EXPIRED -> StatusBadgeStyle(
            label = "Expirado",
            background = Color(0xFF606060).copy(alpha = 0.1f),
            content = Color(0xFF606060)
        )
    }
}

@Composable
private fun Header(mutedTextColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Mis Grupos",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Gestiona tus compras grupales",
            fontSize = 14.sp,
            color = mutedTextColor
        )
    }
    Divider(color = Color(0xFFF4F4F7))
}

@Composable
private fun TabSwitcher(
    selectedTab: MyGroupTab,
    onTabSelected: (MyGroupTab) -> Unit,
    accentColor: Color,
    mutedTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF4F4F7), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MyGroupTab.values().forEach { tab ->
                val selected = tab == selectedTab
                Text(
                    text = tab.label,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 10.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = if (selected) accentColor else mutedTextColor
                )
            }
        }
    }
}

@Composable
private fun GroupCompletedCard(
    group: Group,
    accentColor: Color,
    mutedTextColor: Color,
    onClick: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val price = currencyFormat.format(if (group.groupPrice > 0) group.groupPrice else group.normalPrice)
    val reservedUnits = group.reservedUnits.coerceAtLeast(0)
    val completedBadge = StatusBadgeStyle(
        label = "Completado",
        background = accentColor.copy(alpha = 0.1f),
        content = accentColor
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = group.productImage.ifEmpty { "https://via.placeholder.com/140" },
                    contentDescription = group.productName,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = group.productName.ifBlank { "Producto" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (group.storeName.isNotBlank()) group.storeName else "Bodega",
                            fontSize = 14.sp,
                            color = mutedTextColor
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = price,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor
                        )
                        if (reservedUnits > 0) {
                            QuantityBadge(quantity = reservedUnits, accentColor = accentColor)
                        }
                        StatusPill(badge = completedBadge)
                    }
                }
            }

            Divider(color = Color(0xFFF4F4F7))

            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1A1A1A)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ver detalles",
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupRetiroCard(
    group: Group,
    accentColor: Color,
    mutedTextColor: Color,
    onClick: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val reservedUnits = group.reservedUnits.coerceAtLeast(0)
    val price = currencyFormat.format(if (group.groupPrice > 0) group.groupPrice else group.normalPrice)
    val totalPrice = currencyFormat.format((if (group.groupPrice > 0) group.groupPrice else group.normalPrice) * reservedUnits)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = Color.White,
        border = BorderStroke(2.dp, accentColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = group.productImage.ifEmpty { "https://via.placeholder.com/140" },
                    contentDescription = group.productName,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = group.productName.ifBlank { "Producto" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (group.storeName.isNotBlank()) group.storeName else "Bodega",
                            fontSize = 13.sp,
                            color = mutedTextColor
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "$price × $reservedUnits = $totalPrice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }

            // Action Button
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accentColor
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Ver mi QR de retiro",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupExpiredCard(
    group: Group,
    accentColor: Color,
    mutedTextColor: Color,
    onClick: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val reservedUnits = group.reservedUnits.coerceAtLeast(0)
    val price = currencyFormat.format(if (group.groupPrice > 0) group.groupPrice else group.normalPrice)
    val progress = group.progress.coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = group.productImage.ifEmpty { "https://via.placeholder.com/140" },
                    contentDescription = group.productName,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = group.productName.ifBlank { "Producto" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (group.storeName.isNotBlank()) group.storeName else "Bodega",
                            fontSize = 12.sp,
                            color = mutedTextColor
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "$price · ${reservedUnits} ${if (reservedUnits == 1) "unidad" else "unidades"}",
                        fontSize = 13.sp,
                        color = Color(0xFF1D7D41)
                    )
                }
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = mutedTextColor.copy(alpha = 0.6f),
                trackColor = Color(0xFFF4F4F4)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = mutedTextColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${group.reservedUnits}/${group.targetSize} unidades alcanzadas",
                    fontSize = 13.sp,
                    color = mutedTextColor
                )
            }

            ExpiredMessage(group = group)

            Divider(color = Color(0xFFF4F4F7))

            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF606060)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ver detalles",
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF606060),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupReservationCard(
    group: Group,
    accentColor: Color,
    mutedTextColor: Color,
    onClick: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val effectiveStatus = if (group.isExpired) GroupStatus.EXPIRED else group.status
    val badge = remember(effectiveStatus) { statusBadgeFor(effectiveStatus) }
    val reservedUnits = group.reservedUnits.coerceAtLeast(0)
    val price = currencyFormat.format(if (group.groupPrice > 0) group.groupPrice else group.normalPrice)
    val hoursRemaining = remember(group.expiresAt, effectiveStatus) {
        max(0L, group.expiresAt - System.currentTimeMillis()) / (1000 * 60 * 60)
    }
    val progress = group.progress.coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column {
            // Main content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image
                AsyncImage(
                    model = group.productImage.ifEmpty { "https://via.placeholder.com/140" },
                    contentDescription = group.productName,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentScale = ContentScale.Crop
                )

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = group.productName.ifBlank { "Producto" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF1A1A1A)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (group.storeName.isNotBlank()) group.storeName else "Bodega",
                                fontSize = 14.sp,
                                color = mutedTextColor
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Price, quantity badge, status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = price,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF1D7D41)
                        )
                        if (reservedUnits > 0) {
                            QuantityBadge(quantity = reservedUnits, accentColor = accentColor)
                        }
                        StatusPill(badge = badge)
                    }
                }
            }

            // Progress Bar (only for ACTIVE/reserved)
            if (effectiveStatus == GroupStatus.ACTIVE) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        color = accentColor,
                        trackColor = Color(0xFFF4F4F4)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = mutedTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${group.reservedUnits}/${group.targetSize} unidades",
                                fontSize = 14.sp,
                                color = mutedTextColor
                            )
                        }
                        if (hoursRemaining > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = mutedTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${hoursRemaining}h restantes",
                                    fontSize = 14.sp,
                                    color = mutedTextColor
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = Color(0xFFF4F4F7))

            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accentColor
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ver detalles",
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

        }
    }
}

@Composable
private fun QuantityBadge(quantity: Int, accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = 0.1f)
    ) {
        Text(
            text = "$quantity ${if (quantity == 1) "unidad" else "unidades"}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = accentColor
        )
    }
}

@Composable
private fun StatusPill(badge: StatusBadgeStyle) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = badge.background
    ) {
        Text(
            text = badge.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = badge.content
        )
    }
}

@Composable
private fun ProgressSection(
    current: Int,
    target: Int,
    hoursRemaining: Long,
    progress: Float,
    accentColor: Color,
    mutedTextColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = accentColor,
            trackColor = Color(0xFFF4F4F4)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = mutedTextColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$current/$target unidades",
                    fontSize = 13.sp,
                    color = mutedTextColor
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = mutedTextColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${hoursRemaining}h restantes",
                    fontSize = 13.sp,
                    color = mutedTextColor
                )
            }
        }
    }
}

@Composable
private fun ExpiredMessage(group: Group) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF606060).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Color(0xFF606060).copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⏰ Se agotó el tiempo de compra para este grupo",
            fontSize = 13.sp,
            color = Color(0xFF606060)
        )
        Text(
            text = "Solo se alcanzaron ${group.reservedUnits} de ${group.targetSize} unidades",
            fontSize = 12.sp,
            color = Color(0xFF606060).copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActionButton(
    status: GroupStatus,
    onClick: () -> Unit,
    accentColor: Color
) {
    val isExpired = status == GroupStatus.EXPIRED
    val label = when (status) {
        GroupStatus.ACTIVE -> "Ver grupo"
        GroupStatus.PICKUP, GroupStatus.COMPLETED, GroupStatus.VALIDATED -> "Ver QR de retiro"
        GroupStatus.EXPIRED -> "Grupo expirado"
    }

    OutlinedButton(
        onClick = onClick,
        enabled = !isExpired,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isExpired) Color(0x33606060) else Color(0xFFE5E7EB)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isExpired) Color(0xFF606060).copy(alpha = 0.6f) else accentColor,
            disabledContentColor = Color(0xFF606060).copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = FontWeight.SemiBold)
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isExpired) Color(0xFF606060).copy(alpha = 0.6f) else accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    mutedTextColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = mutedTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
