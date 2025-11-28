package com.nexusbiz.nexusbiz.ui.screens.store

import android.widget.Toast
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun StoreGroupDetailScreen(
    group: Group?,
    participants: List<StoreParticipantDisplay>,
    onBack: () -> Unit,
    onShare: (Group) -> Unit,
    onFinalizeEarly: (Group) -> Unit,
    onPublishSimilar: (() -> Unit)? = null,
    onViewHistory: (() -> Unit)? = null,
    onScanQR: (() -> Unit)? = null
) {
    if (group == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F4F7)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No se pudo cargar la oferta", color = Color(0xFF606060))
        }
        return
    }

    val context = LocalContext.current
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
            onFinalizeEarly = onFinalizeEarly,
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
        color = Color(0xFFF4F4F7)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFF0F0F2)))
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
                            .background(Color(0xFFF4F4F7), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color(0xFF1A1A1A))
                    }
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        .background(Color(0xFFF4F4F7)),
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = productName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
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
    onFinalizeEarly: (Group) -> Unit,
    onScanQR: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val progress = (group.currentSize.toFloat() / max(1, group.targetSize).toFloat()).coerceIn(0f, 1f)
    val progressPercent = (progress * 100).toInt()
    val remaining = group.targetSize - group.currentSize
    var showParticipants by rememberSaveable(group.id) { mutableStateOf(false) }
    var showFinalizeConfirmation by remember { mutableStateOf(false) }

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
                com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE -> Color(0xFF059669).copy(alpha = 0.15f)
                com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                else -> Color(0xFF059669).copy(alpha = 0.15f)
            },
            badgeContent = when (group.status) {
                com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE -> Color(0xFF059669)
                com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP -> Color(0xFF3B82F6)
                else -> Color(0xFF059669)
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .border(BorderStroke(1.dp, Color(0xFFF0F0F2)), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Solo mostrar tiempo restante si estado es ACTIVE
                        if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE) {
                            InfoPill(
                                icon = Icons.Default.AccessTime,
                                label = "Tiempo restante",
                                value = formatTimeRemaining(group.expiresAt)
                            )
                        }
                        InfoPill(
                            icon = Icons.Default.People,
                            label = "Participantes",
                            value = "${participants.size} personas"
                        )
                    }
                }
            }
        )

        // IMPORTANTE: Validados = participantes con is_validated = true
        // Por reservar = current_size - validados (unidades, no personas)
        val validatedCount = participants.count { it.state == ParticipantState.VALIDATED || it.state == ParticipantState.RETIRED }
        val validatedUnits = participants.filter { it.state == ParticipantState.VALIDATED || it.state == ParticipantState.RETIRED }
            .sumOf { it.units }
        val pendingUnits = group.currentSize - validatedUnits
        
        ValidationStatsCard(
            validated = validatedCount,
            pending = if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) pendingUnits else participants.count { it.state == ParticipantState.PENDING },
            pendingTitle = if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) "Por validar (unidades)" else "Por reservar"
        )

        ParticipantsExpandableList(
            title = "Ver participantes (${participants.size})",
            participants = participants,
            expanded = showParticipants,
            onToggle = { showParticipants = !showParticipants }
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientButton(
                text = "Compartir grupo",
                onClick = { onShare(group) }
            )
            // Botón "Escanear QR" solo habilitado si estado = PICKUP
            if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP) {
                GradientButton(
                    text = "Escanear QR",
                    onClick = onScanQR ?: {}
                )
            } else {
                DisabledButton(
                    text = "Escanear QR", 
                    subtitle = if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE) 
                        "Solo disponible cuando se llegue a la meta" 
                    else 
                        "No disponible para este estado"
                )
            }
            // Botón "Finalizar anticipadamente" solo si estado = ACTIVE
            if (group.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE) {
                OutlinedDangerButton(
                    text = "Finalizar grupo anticipadamente",
                    onClick = { showFinalizeConfirmation = true }
                )
            }
        }
    }

    if (showFinalizeConfirmation) {
        ConfirmFinalizeDialog(
            onDismiss = { showFinalizeConfirmation = false },
            onConfirm = {
                showFinalizeConfirmation = false
                onFinalizeEarly(group)
                Toast.makeText(context, "Grupo finalizado", Toast.LENGTH_SHORT).show()
            }
        )
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
                        iconTint = Color(0xFF059669)
                    )
                } else {
                    InfoBanner(
                        background = Color(0xFFFFF7ED),
                        content = "Pendientes: $pendingUnits unidades",
                        iconTint = Color(0xFFEA580C)
                    )
                }
            }
        )

        ValidationStatsCard(
            validated = scannedCount,
            pending = pendingUnits
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
            badgeColor = Color(0xFFF5F5F5),
            badgeContent = Color(0xFF6B7280),
            content = {
                ExpiredProgressSection(
                    reserved = group.currentSize,
                    total = group.targetSize
                )
                Text(
                    text = "Meta no alcanzada antes del tiempo límite",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Text(
                text = "Este grupo expiró y no generó QR de retiro.",
                modifier = Modifier.padding(24.dp),
                color = Color(0xFF475569),
                textAlign = TextAlign.Center
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onPublishSimilar != null) {
                GradientButton(
                    text = "Publicar nueva oferta similar",
                    icon = Icons.Default.Share,
                    onClick = onPublishSimilar
                )
            }
            SecondaryButton(
                text = "Cerrar grupo",
                onClick = onClose
            )
        }
    }
}

@Composable
private fun ConfirmFinalizeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Finalizar grupo", fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(text = "¿Seguro que deseas finalizar este grupo antes de tiempo?")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Sí, finalizar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ProgressSection(
    reserved: Int,
    total: Int,
    progressPercent: Int,
    remaining: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Progreso del grupo", color = Color(0xFF6B7280), fontSize = 14.sp)
            Text(text = "$reserved/$total unidades", color = Color(0xFF1F2937), fontSize = 14.sp)
        }
        LinearProgressIndicator(
            progress = progressPercent / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = Color(0xFF10B981),
            trackColor = Color(0xFFE5E7EB)
        )
        Text(
            text = "$progressPercent% completado · Faltan $remaining unidades",
            color = Color(0xFF6B7280),
            fontSize = 13.sp
        )
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
            Text(text = "Progreso del grupo", color = Color(0xFF6B7280), fontSize = 14.sp)
            Text(text = "$reserved/$total unidades", color = Color(0xFF1F2937), fontSize = 14.sp)
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = Color(0xFF9CA3AF),
            trackColor = Color(0xFFE5E7EB)
        )
        Text(
            text = "Solo $reserved de $total unidades reservadas",
            color = Color(0xFF6B7280),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun RowScope.StatCard(
    title: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(96.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 13.sp, color = Color(0xFF606060))
        }
    }
}

@Composable
private fun ValidationStatsCard(
    validated: Int,
    pending: Int,
    pendingTitle: String = "Pendientes"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Validaciones", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    title = "Validados",
                    value = validated.toString(),
                    color = Color(0xFF10B981)
                )
                StatCard(
                    title = pendingTitle,
                    value = pending.toString(),
                    color = Color(0xFFFF914D)
                )
            }
        }
    }
}

@Composable
private fun RowScope.InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8F8FA),
        border = BorderStroke(1.dp, Color(0xFFF0F0F2))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF606060), modifier = Modifier.size(20.dp))
            Column {
                Text(text = label, fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Text(text = value, fontSize = 14.sp, color = Color(0xFF111827), fontWeight = FontWeight.Medium)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF606060))
                    Text(text = title, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Medium)
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF606060),
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 90f else 0f)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Divider(color = Color(0xFFF4F4F7))
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
                                ParticipantState.RETIRED -> Color(0xFF3B82F6)
                                ParticipantState.VALIDATED -> Color(0xFF10B981)
                                ParticipantState.PENDING -> Color(0xFFFF914D)
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
                    Text(text = participant.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                    Text(
                        text = "${participant.units} ${if (participant.units == 1) "unidad" else "unidades"} · ${participant.reservationDate}",
                        fontSize = 12.sp,
                        color = Color(0xFF606060)
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
            Divider(color = Color(0xFFF4F4F7), thickness = 1.dp, modifier = Modifier.padding(start = 20.dp))
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
                    Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
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
                containerColor = Color(0xFFE5E7EB),
                disabledContainerColor = Color(0xFFE5E7EB),
                disabledContentColor = Color(0xFF9CA3AF)
            )
        ) {
            Text(text = text, fontWeight = FontWeight.SemiBold)
        }
        Text(text = subtitle, color = Color(0xFF9CA3AF), fontSize = 12.sp)
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
        border = BorderStroke(2.dp, Color(0xFFFECACA)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
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
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
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
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1F2937))
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, contentDescription = null, tint = Color(0xFF1F2937)) }
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
        Text(text = label, color = Color(0xFF6B7280), fontSize = 12.sp)
        Text(text = value, color = Color(0xFF111827), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
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
            content = Color(0xFF2563EB)
        )
        ParticipantState.VALIDATED -> ParticipantStateConfig(
            label = "Validado",
            background = Color(0xFFD1FAE5),
            content = Color(0xFF059669)
        )
        ParticipantState.PENDING -> ParticipantStateConfig(
            label = "Pendiente",
            background = Color(0xFFFFF7ED),
            content = Color(0xFFEA580C)
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


