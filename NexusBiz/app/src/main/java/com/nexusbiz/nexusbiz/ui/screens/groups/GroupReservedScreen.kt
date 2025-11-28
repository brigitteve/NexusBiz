package com.nexusbiz.nexusbiz.ui.screens.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.nexusbiz.nexusbiz.data.model.User
import java.text.NumberFormat
import java.util.UUID
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupReservedScreen(
    group: Group?,
    currentUser: User?,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    var showShareModal by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }

    if (group == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Reserva no encontrada")
        }
        return
    }

    val progress = (group.progress * 100).coerceIn(0f, 100f)
    val unitsNeeded = (group.targetSize - group.reservedUnits).coerceAtLeast(0)
    val participantCount = max(1, group.participantCount)
    val userReservationQuantity = group.activeParticipants
        .firstOrNull { it.userId == currentUser?.id }
        ?.reservedUnits
        ?.coerceAtLeast(0)
        ?: 0
    val userLevel = currentUser?.badges?.firstOrNull()?.ifBlank { null } ?: "Bronce"

    val fallbackParticipants = listOf(
        ParticipantData(id = "user-0", name = "Tú", units = userReservationQuantity, isYou = true),
        ParticipantData(id = "user-1", name = "María R.", units = 3, isYou = false),
        ParticipantData(id = "user-2", name = "Ana L.", units = 2, isYou = false),
        ParticipantData(id = "user-3", name = "Carlos M.", units = 2, isYou = false),
        ParticipantData(id = "user-4", name = "Luis P.", units = 3, isYou = false)
    )

    val participants = remember(group.participants, currentUser?.id, userReservationQuantity) {
        val activeParticipants = group.activeParticipants
        if (activeParticipants.isNotEmpty()) {
            activeParticipants.mapIndexed { index, participant ->
                val alias = participant.alias.ifBlank { "Participante ${index + 1}" }
                val isCurrentUser = participant.userId == currentUser?.id
                ParticipantData(
                    id = participant.userId.ifBlank { UUID.randomUUID().toString() },
                    name = if (isCurrentUser) "Tú" else alias,
                    units = participant.reservedUnits.coerceAtLeast(0),
                    isYou = isCurrentUser
                )
            }
        } else {
            fallbackParticipants
        }
    }

    val shareLink = remember(group.id) { "https://nexusbiz.app/grupo/${group.id}" }

    val handleCopyLink = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Enlace del grupo", shareLink)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F7))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp,
                tonalElevation = 0.dp
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
                        text = "Grupo – ${group.productName}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    IconButton(onClick = {
                        showShareModal = true
                        onShare()
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = Color(0xFF10B981)
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = group.productImage.ifEmpty { "https://via.placeholder.com/400" },
                            contentDescription = group.productName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(192.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = group.productName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.storeName.ifBlank { "Bodega" },
                                        fontSize = 14.sp,
                                        color = Color(0xFF606060)
                                    )
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "Precio grupal:",
                                    fontSize = 14.sp,
                                    color = Color(0xFF606060)
                                )
                                Text(
                                    text = currencyFormat.format(group.groupPrice.takeIf { it > 0 } ?: group.normalPrice),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "por unidad",
                                    fontSize = 14.sp,
                                    color = Color(0xFF606060)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "En reserva",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                // Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progreso del grupo",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFFFF914D),
                                    modifier = Modifier.size(20.dp)
                                )
                                var hoursRemaining by remember(group.expiresAt) {
                                    mutableStateOf(max(0L, (group.expiresAt - System.currentTimeMillis()) / (1000 * 60 * 60)))
                                }
                                LaunchedEffect(group.expiresAt) {
                                    while (hoursRemaining > 0) {
                                        kotlinx.coroutines.delay(60 * 60 * 1000) // Actualizar cada hora
                                        hoursRemaining = max(0L, (group.expiresAt - System.currentTimeMillis()) / (1000 * 60 * 60))
                                    }
                                }
                                Text(
                                    text = "Quedan ${hoursRemaining}h",
                                    fontSize = 14.sp,
                                    color = Color(0xFFFF914D),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${group.reservedUnits} / ${group.targetSize}",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    text = "unidades reservadas",
                                    fontSize = 14.sp,
                                    color = Color(0xFF606060)
                                )
                            }

                            LinearProgressIndicator(
                                progress = progress / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFFF4F4F7)
                            )

                            Text(
                                text = "Faltan $unitsNeeded unidades para completar la meta",
                                fontSize = 14.sp,
                                color = Color(0xFF10B981),
                                textAlign = TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "💡 Cuando lleguen a ${group.targetSize} unidades, se activará el precio grupal y recibirás tu QR de retiro",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = Color(0xFF1A1A1A),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Tu Reserva Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tu reserva",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Nivel $userLevel",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Has reservado:",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "$userReservationQuantity ${if (userReservationQuantity == 1) "unidad" else "unidades"}",
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Participants List
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Participantes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                text = "${participants.size} personas",
                                fontSize = 14.sp,
                                color = Color(0xFF606060)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            participants.forEach { participant ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (participant.isYou) {
                                                Color(0xFF10B981).copy(alpha = 0.05f)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .then(
                                            if (participant.isYou) {
                                                Modifier.border(
                                                    BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f)),
                                                    RoundedCornerShape(12.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (participant.isYou) {
                                                    Color(0xFF10B981)
                                                } else {
                                                    Color(0xFFF4F4F7)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = participant.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (participant.isYou) Color.White else Color(0xFF1A1A1A)
                                        )
                                    }
                                    Text(
                                        text = participant.name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 15.sp,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = "${participant.units} ${if (participant.units == 1) "unidad" else "unidades"}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "🔒 Mostramos solo alias por privacidad",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .border(
                                    BorderStroke(1.dp, Color(0xFFF4F4F7)),
                                    RoundedCornerShape(0.dp)
                                )
                                .padding(top = 8.dp),
                            fontSize = 12.sp,
                            color = Color(0xFF606060),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            showShareModal = true
                            onShare()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compartir grupo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = handleCopyLink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(2.dp, Color(0xFFF4F4F7)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF606060)
                        )
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color(0xFF606060),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copiar enlace del grupo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF606060)
                        )
                    }
                }
            }
        }
    }

    // Share Modal (simplificado por ahora)
    if (showShareModal) {
        AlertDialog(
            onDismissRequest = { showShareModal = false },
            title = { Text("Compartir grupo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Comparte este grupo con tus amigos")
                    OutlinedTextField(
                        value = shareLink,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Enlace del grupo") },
                        trailingIcon = {
                            IconButton(onClick = handleCopyLink) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareModal = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

private data class ParticipantData(
    val id: String,
    val name: String,
    val units: Int,
    val isYou: Boolean
)
