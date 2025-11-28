package com.nexusbiz.nexusbiz.ui.screens.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.data.model.Group
import com.nexusbiz.nexusbiz.data.model.GroupStatus
import com.nexusbiz.nexusbiz.data.model.Participant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val StoreBackground = Color(0xFFF4F4F7)
private val StoreSurface = Color.White
private val StoreBorder = Color(0xFFE3E8EF)
private val StorePrimary = Color(0xFF10B981)
private val StorePrimaryDark = Color(0xFF059669)
private val StoreAccentBlue = Color(0xFF3B82F6)
private val StoreWarning = Color(0xFFFF914D)
private val StoreDanger = Color(0xFFDC2626)
private val StoreTitleText = Color(0xFF1A1A1A)
private val StoreBodyText = Color(0xFF111827)
private val StoreMutedText = Color(0xFF6B7280)
private val StoreSubtleText = Color(0xFF9CA3AF)

@Composable
fun StoreGroupDetailScreen(
    group: Group?,
    participants: List<StoreParticipantDisplay>,
    onBack: () -> Unit,
    onShare: (Group) -> Unit,
    onPublishSimilar: (() -> Unit)? = null,
    onViewHistory: (() -> Unit)? = null,
    onScanQR: (() -> Unit)? = null
) {
    if (group == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StoreBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No se pudo cargar la oferta", color = StoreMutedText)
        }
        return
    }

    // Usar los participantes proporcionados, o mapear desde el grupo si están vacíos
    // Asegurar que siempre tengamos una lista válida
    val participantList = remember(participants, group.id, group.participants) { 
        if (participants.isNotEmpty()) {
            participants
        } else {
            mapParticipantsForStore(group)
        }
    }

    when (group.status) {
        GroupStatus.EXPIRED -> StoreExpiredContent(
            group = group,
            onBack = onBack,
            onPublishSimilar = onPublishSimilar,
            onClose = onBack
        )

        GroupStatus.COMPLETED, GroupStatus.VALIDATED -> StoreCompletedContent(
            group = group,
            participants = participantList,
            onBack = onBack,
            onViewHistory = onViewHistory
        )

        GroupStatus.ACTIVE, GroupStatus.PICKUP -> StoreActiveContent(
            group = group,
            participants = participantList,
            onBack = onBack,
            onShare = onShare,
            onScanQR = onScanQR
        )
    }
}

@Composable
private fun StoreGroupScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StoreBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StoreSurface)
                    .border(BorderStroke(1.dp, StoreBorder)),
                color = StoreSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .background(StoreBackground, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = StoreTitleText)
                    }
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StoreTitleText
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun GroupHeaderCard(
    productName: String,
    imageUrl: String,
    badgeText: String,
    badgeColor: Color,
    badgeContent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StoreSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, StoreBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = imageUrl.ifEmpty { "https://via.placeholder.com/160" },
                    contentDescription = productName,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(StoreBackground),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = productName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = StoreTitleText)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = badgeColor
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeContent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun StoreActiveContent(
    group: Group,
    participants: List<StoreParticipantDisplay>,
    onBack: () -> Unit,
    onShare: (Group) -> Unit,
    onScanQR: (() -> Unit)? = null
) {
    val progress = (group.currentSize.toFloat() / max(1, group.targetSize).toFloat()).coerceIn(0f, 1f)
    val progressPercent = (progress * 100).toInt()
    val remaining = group.targetSize - group.currentSize
    var showParticipants by rememberSaveable(group.id) { mutableStateOf(true) } // Mostrar participantes por defecto

    StoreGroupScaffold(
        title = "Detalles del grupo",
        onBack = onBack
    ) {
        GroupHeaderCard(
            productName = group.productName,
            imageUrl = group.productImage,
            badgeText = when (group.status) {
                com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE -> "Completándose"
                com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP -> "Listo para retirar"
                else -> "Activo"
            },
            badgeColor = when (group.status) {
                com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE -> StorePrimary.copy(alpha = 0.15f)
                com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP -> StoreAccentBlue.copy(alpha = 0.15f)
                else -> StorePrimary.copy(alpha = 0.15f)
            },
            badgeContent = when (group.status) {
                com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE -> StorePrimaryDark
                com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP -> StoreAccentBlue
                else -> StorePrimaryDark
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // IMPORTANTE: Usar currentSize directamente de Supabase (mantenido por trigger)
                    // Faltan X unidades = target_size - current_size
                    // Progreso = current_size / target_size
                    val actualReserved = group.currentSize
                    val actualProgress = if (group.targetSize > 0) actualReserved.toFloat() / group.targetSize.toFloat() else 0f
                    val actualProgressPercent = (actualProgress * 100).toInt()
                    val actualRemaining = (group.targetSize - actualReserved).coerceAtLeast(0)
                    
                    ProgressSection(
                        reserved = actualReserved,
                        total = group.targetSize,
                        progressPercent = actualProgressPercent,
                        remaining = actualRemaining
                    )
                    val timeElapsed = formatTimeElapsed(group.createdAt)
                    val timeRemaining = formatTimeRemaining(group.expiresAt)
                    InfoSummaryCard(
                        modifier = Modifier.padding(top = 12.dp),
                        items = listOf(
                            InfoItem(Icons.Default.AccessTime, "Tiempo transcurrido", timeElapsed),
                            InfoItem(Icons.Default.AccessTime, "Tiempo restante", timeRemaining),
                            InfoItem(Icons.Default.People, "Participantes", "${participants.size} personas")
                        )
                    )
                }
            }
        )

        // IMPORTANTE: Validados = participantes con is_validated = true (aumentan según escaneo QR)
        // Pendientes = participantes reservados pero aún no validados
        val validatedCount = participants.count { it.state == ParticipantState.VALIDATED || it.state == ParticipantState.RETIRED }
        val validatedUnits = participants.filter { it.state == ParticipantState.VALIDATED || it.state == ParticipantState.RETIRED }
            .sumOf { it.units }
        val totalReservedUnits = participants.sumOf { it.units }
        val pendingUnits = if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) {
            // En PICKUP: pendientes = unidades reservadas - unidades validadas
            totalReservedUnits - validatedUnits
        } else {
            // En ACTIVE: faltan unidades para alcanzar la meta
            (group.targetSize - totalReservedUnits).coerceAtLeast(0)
        }
        
        // Mostrar estadísticas de reservas/validaciones
        ValidationStatsCard(
            validated = if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) {
                validatedUnits // En PICKUP: mostrar unidades validadas
            } else {
                totalReservedUnits // En ACTIVE: mostrar unidades reservadas
            },
            pending = pendingUnits,
            validatedTitle = if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) "Validados (unidades)" else "Reservadas (unidades)",
            pendingTitle = if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) "Por validar (unidades)" else "Faltan (unidades)"
        )

        ParticipantsExpandableList(
            title = "Ver participantes (${participants.size} personas · ${totalReservedUnits} unidades)",
            participants = participants,
            expanded = showParticipants,
            onToggle = { showParticipants = !showParticipants }
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Botón "Escanear QR" solo habilitado si estado = PICKUP (cuando se alcanzó la meta)
            if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) {
                GradientButton(
                    text = "Escanear QR",
                    onClick = onScanQR ?: {}
                )
            } else {
                DisabledButton(
                    text = "Escanear QR", 
                    subtitle = if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE) 
                        "Solo disponible cuando se alcance la meta de ${group.targetSize} unidades" 
                    else 
                        "No disponible para este estado"
                )
            }
            // Botón "Compartir grupo" al final
            GradientButton(
                text = "Compartir grupo",
                onClick = { onShare(group) }
            )
        }
    }
}

@Composable
private fun StoreCompletedContent(
    group: Group,
    participants: List<StoreParticipantDisplay>,
    onBack: () -> Unit,
    onViewHistory: (() -> Unit)?
) {
    // Calcular usando la lista filtrada de participantes (solo clientes, sin bodega)
    val totalReservedByClients = participants.sumOf { it.units }
    val scannedCount = participants.count { it.state == ParticipantState.VALIDATED || it.state == ParticipantState.RETIRED }
    val pendingCount = participants.count { it.state == ParticipantState.PENDING }
    val pendingUnits = participants.filter { it.state == ParticipantState.PENDING }.sumOf { it.units }
    var showParticipants by rememberSaveable(group.id) { mutableStateOf(false) }

    StoreGroupScaffold(
        title = "Detalles del grupo",
        onBack = onBack
    ) {
        GroupHeaderCard(
            productName = group.productName,
            imageUrl = group.productImage,
            badgeText = "Finalizado",
            badgeColor = Color(0xFFDBEAFE),
            badgeContent = Color(0xFF1D4ED8),
            content = {
                StatsGrid(
                    reservedText = "${totalReservedByClients}/${group.targetSize}",
                    soldText = totalReservedByClients.toString(),
                    scannedText = scannedCount.toString()
                )

                if (pendingUnits == 0) {
                    InfoBanner(
                        background = Color(0xFFD1FAE5),
                        icon = Icons.Default.CheckCircle,
                        content = "Todos los retiros fueron validados correctamente.",
                        iconTint = StorePrimaryDark
                    )
                } else {
                    InfoBanner(
                        background = Color(0xFFFFF7ED),
                        content = "Pendientes: $pendingUnits unidades",
                        iconTint = StoreWarning
                    )
                }
            }
        )

        ValidationStatsCard(
            validated = scannedCount,
            pending = pendingUnits,
            validatedTitle = "Validados (unidades)",
            pendingTitle = "Pendientes (unidades)"
        )

        ParticipantsExpandableList(
            title = "Ver participantes (${participants.size})",
            participants = participants,
            expanded = showParticipants,
            onToggle = { showParticipants = !showParticipants }
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(text = "Volver al dashboard", onClick = onBack)
            if (onViewHistory != null) {
                SecondaryButton(
                    text = "Ver historial del grupo",
                    icon = Icons.Default.Share,
                    onClick = onViewHistory
                )
            }
        }
    }
}

@Composable
private fun StoreExpiredContent(
    group: Group,
    onBack: () -> Unit,
    onPublishSimilar: (() -> Unit)?,
    onClose: () -> Unit
) {
    StoreGroupScaffold(
        title = "Detalles del grupo",
        onBack = onBack
    ) {
        GroupHeaderCard(
            productName = group.productName,
            imageUrl = group.productImage,
            badgeText = "Expirado",
            badgeColor = StoreBackground,
            badgeContent = StoreMutedText,
            content = {
                ExpiredProgressSection(
                    reserved = group.currentSize,
                    total = group.targetSize
                )
                Text(
                    text = "Meta no alcanzada",
                    color = StoreDanger,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        )
    }
}


@Composable
private fun ProgressSection(
    reserved: Int,
    total: Int,
    progressPercent: Int,
    remaining: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Progreso de reservas", color = StoreMutedText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = "Las reservas van avanzando conforme los clientes se unen", color = StoreSubtleText, fontSize = 12.sp)
            }
            Text(text = "$reserved/$total", color = StoreBodyText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = progressPercent / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = StorePrimary,
            trackColor = StoreBackground
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$progressPercent% completado",
                color = StorePrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Faltan $remaining unidades",
                color = StoreWarning,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ExpiredProgressSection(
    reserved: Int,
    total: Int
) {
    val progress = (reserved.toFloat() / max(1, total).toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Progreso del grupo", color = StoreMutedText, fontSize = 14.sp)
            Text(text = "$reserved/$total unidades", color = StoreBodyText, fontSize = 14.sp)
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = StoreBorder,
            trackColor = StoreBackground
        )
        Text(
            text = "Solo $reserved de $total unidades reservadas",
            color = StoreMutedText,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun InfoSummaryCard(
    items: List<InfoItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StoreSurface),
        border = BorderStroke(1.dp, StoreBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoGrid(items = items)
        }
    }
}

@Composable
private fun InfoGrid(items: List<InfoItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { info ->
                    InfoPill(
                        modifier = Modifier.weight(1f),
                        icon = info.icon,
                        label = info.label,
                        value = info.value
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class InfoItem(
    val icon: ImageVector,
    val label: String,
    val value: String
)

@Composable
private fun ValidationStatsCard(
    validated: Int,
    pending: Int,
    validatedTitle: String = "Validados",
    pendingTitle: String = "Pendientes"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StoreSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, StoreBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Validaciones", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = StoreTitleText)
            Text(
                text = "Los validados aumentan según el escaneo QR. Los pendientes son reservas aún no validadas.",
                fontSize = 12.sp,
                color = StoreSubtleText,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            val total = (validated + pending).coerceAtLeast(1)
            LinearProgressIndicator(
                progress = validated.toFloat() / total,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = StorePrimary,
                trackColor = StoreBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricPill(
                    title = validatedTitle,
                    value = validated,
                    accent = StorePrimary
                )
                MetricPill(
                    title = pendingTitle,
                    value = pending,
                    accent = StoreWarning
                )
            }
        }
    }
}

@Composable
private fun RowScope.MetricPill(
    title: String,
    value: Int,
    accent: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, fontSize = 12.sp, color = accent.copy(alpha = 0.9f))
        Text(text = value.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StoreBodyText)
    }
}

@Composable
private fun InfoPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = StoreSurface,
        border = BorderStroke(1.dp, StoreBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(StorePrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = StorePrimaryDark, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = label, fontSize = 12.sp, color = StoreSubtleText)
                Text(text = value, fontSize = 15.sp, color = StoreBodyText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ParticipantsExpandableList(
    title: String,
    participants: List<StoreParticipantDisplay>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StoreSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, StoreBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = StoreMutedText)
                    Text(text = title, color = StoreTitleText, fontWeight = FontWeight.Medium)
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = StoreMutedText,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 90f else 0f)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Divider(color = StoreBackground)
                    participants.forEachIndexed { index, participant ->
                        ParticipantRow(participant = participant, showDivider = index != participants.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: StoreParticipantDisplay, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = when (participant.state) {
                                ParticipantState.RETIRED -> StoreAccentBlue
                                ParticipantState.VALIDATED -> StorePrimary
                                ParticipantState.PENDING -> StoreWarning
                            }.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.initials,
                        color = when (participant.state) {
                            ParticipantState.RETIRED -> Color(0xFF1E3A8A)
                            ParticipantState.VALIDATED -> Color(0xFF065F46)
                            ParticipantState.PENDING -> Color(0xFF7C2D12)
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(text = participant.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = StoreTitleText)
                    Text(
                        text = "${participant.units} ${if (participant.units == 1) "unidad" else "unidades"}",
                        fontSize = 13.sp,
                        color = StoreMutedText,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = participant.reservationDate,
                        fontSize = 12.sp,
                        color = StoreSubtleText
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = participant.stateConfig.background
            ) {
                Text(
                    text = participant.stateConfig.label,
                    color = participant.stateConfig.content,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        if (showDivider) {
            Divider(color = StoreBackground, thickness = 1.dp, modifier = Modifier.padding(start = 20.dp))
        }
    }
}

@Composable
private fun GradientButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(listOf(StorePrimary, StorePrimaryDark)),
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = Color.White)
                }
                Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DisabledButton(text: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StoreBorder,
                disabledContainerColor = StoreBorder,
                disabledContentColor = StoreSubtleText
            )
        ) {
            Text(text = text, fontWeight = FontWeight.SemiBold)
        }
        Text(text = subtitle, color = StoreSubtleText, fontSize = 12.sp)
    }
}

@Composable
private fun OutlinedDangerButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, StoreDanger.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StoreDanger)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = StorePrimary)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StoreBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StoreBodyText)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, contentDescription = null, tint = StoreBodyText) }
            Text(text = text, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun InfoBanner(
    background: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: String,
    iconTint: Color = Color(0xFF2563EB)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Text(text = content, color = iconTint, fontSize = 13.sp)
    }
}

@Composable
private fun StatsGrid(
    reservedText: String,
    soldText: String,
    scannedText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatColumn(label = "Reservadas", value = reservedText)
        StatColumn(label = "Vendidas", value = soldText)
        StatColumn(label = "Escaneadas", value = scannedText)
    }
}

@Composable
private fun RowScope.StatColumn(label: String, value: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = StoreMutedText, fontSize = 12.sp)
        Text(text = value, color = StoreBodyText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatTimeRemaining(expiresAt: Long): String {
    val now = System.currentTimeMillis()
    val diff = expiresAt - now
    if (diff <= 0) return "Tiempo vencido"
    val hours = diff / (1000 * 60 * 60)
    val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatTimeElapsed(createdAt: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - createdAt
    if (diff <= 0) return "Recién creado"
    val hours = diff / (1000 * 60 * 60)
    val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun Double.formatPrice(): String = String.format(Locale("es", "PE"), "%.2f", this)

enum class ParticipantState { PENDING, VALIDATED, RETIRED }

data class ParticipantStateConfig(
    val label: String,
    val background: Color,
    val content: Color
)

private fun participantStateConfig(state: ParticipantState): ParticipantStateConfig {
    return when (state) {
        ParticipantState.RETIRED -> ParticipantStateConfig(
            label = "Retirado",
            background = Color(0xFFDBEAFE),
            content = StoreAccentBlue
        )
        ParticipantState.VALIDATED -> ParticipantStateConfig(
            label = "Validado",
            background = Color(0xFFD1FAE5),
            content = StorePrimaryDark
        )
        ParticipantState.PENDING -> ParticipantStateConfig(
            label = "Pendiente",
            background = Color(0xFFFFF7ED),
            content = StoreWarning
        )
    }
}

data class StoreParticipantDisplay(
    val name: String,
    val initials: String,
    val units: Int,
    val reservationDate: String,
    val state: ParticipantState,
    val stateConfig: ParticipantStateConfig
)

/**
 * Mapea participantes para la vista de bodega, EXCLUYENDO a la bodega misma.
 * Las bodegas nunca deben aparecer como participantes de sus propios grupos.
 */
fun mapParticipantsForStore(
    group: Group?, 
    storeId: String? = null,
    storeOwnerId: String? = null
): List<StoreParticipantDisplay> {
    if (group == null) return emptyList()
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))
    
    // Filtrar participantes: excluir a la bodega
    // Las bodegas nunca deben aparecer como participantes de sus propios grupos
    val participants = group.participants.filter { participant ->
        // Excluir si el userId del participante coincide con el ownerId de la bodega
        if (storeOwnerId != null && participant.userId == storeOwnerId) {
            return@filter false
        }
        // También excluir si el participante es el creador del grupo Y el grupo pertenece a esta bodega
        // (esto cubre el caso donde el creatorId es el owner de la bodega)
        if (storeId != null && group.storeId == storeId && participant.userId == group.creatorId) {
            // Verificar si el creatorId es el owner de la bodega
            if (storeOwnerId != null && group.creatorId == storeOwnerId) {
                return@filter false
            }
        }
        true
    }

    return participants.mapIndexed { index, participant ->
        // IMPORTANTE: Usar is_validated y status correctamente según el esquema SQL
        // is_validated = true significa que el participante ya retiró
        // status = 'VALIDATED' también indica que está validado
        val state = when {
            participant.isValidated || participant.status == com.nexusbiz.nexusbiz.data.model.ReservationStatus.VALIDATED -> ParticipantState.VALIDATED
            participant.status == com.nexusbiz.nexusbiz.data.model.ReservationStatus.CANCELLED -> ParticipantState.PENDING // Los cancelados no se muestran (ya filtrados)
            else -> ParticipantState.PENDING
        }
        StoreParticipantDisplay(
            name = participant.alias.ifBlank { "Participante ${index + 1}" },
            initials = participant.alias.takeIf { it.isNotBlank() }?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("")
                ?: "PN",
            units = participant.reservedUnits.coerceAtLeast(1),
            reservationDate = formatter.format(Date(participant.joinedAt)),
            state = state,
            stateConfig = participantStateConfig(state)
        )
    }
}
