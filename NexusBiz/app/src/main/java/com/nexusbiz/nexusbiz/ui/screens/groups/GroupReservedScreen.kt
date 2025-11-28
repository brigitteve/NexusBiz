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
import com.nexusbiz.nexusbiz.data.model.Offer
import com.nexusbiz.nexusbiz.data.model.Reservation
import com.nexusbiz.nexusbiz.data.model.User
import com.nexusbiz.nexusbiz.data.repository.OfferRepository
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.UUID
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupReservedScreen(
    group: Group? = null, // @Deprecated - mantener temporalmente
    offer: Offer? = null,
    reservations: List<Reservation> = emptyList(),
    currentUser: User?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onCreateReservation: ((Int) -> Unit)? = null
) {
    var showShareModal by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }

    // Usar offer si está disponible, sino group (deprecated)
    val activeOffer = offer
    val activeGroup = group
    
    if (activeOffer == null && activeGroup == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Reserva no encontrada")
        }
        return
    }

    // Usar datos de la oferta si está disponible, sino del grupo
    val progress = ((activeOffer?.progress ?: activeGroup?.progress ?: 0f) * 100).coerceIn(0f, 100f)
    val unitsNeeded = activeOffer?.unitsNeeded ?: (activeGroup?.let { (it.targetSize - it.reservedUnits).coerceAtLeast(0) } ?: 0)
    val reservedUnits = activeOffer?.reservedUnits ?: activeGroup?.reservedUnits ?: 0
    val targetUnits = activeOffer?.targetUnits ?: activeGroup?.targetSize ?: 0
    
    // Obtener reserva del usuario actual
    val userReservation = reservations.firstOrNull { it.userId == currentUser?.id }
    val userReservationQuantity = userReservation?.units ?: activeGroup?.activeParticipants
        ?.firstOrNull { it.userId == currentUser?.id }
        ?.reservedUnits
        ?.coerceAtLeast(0)
        ?: 0
    
    // Calcular unidades reservadas por otros (excluyendo al usuario actual)
    val unitsReservedByOthers = reservedUnits - userReservationQuantity
    val participantCount = max(1, reservations.size.takeIf { it > 0 } ?: activeGroup?.participantCount ?: 1)
    
    // Obtener nivel del usuario basado en puntos
    val userLevel = remember(currentUser?.points) {
        when {
            (currentUser?.points ?: 0) >= 200 -> "Oro"
            (currentUser?.points ?: 0) >= 100 -> "Plata"
            else -> "Bronce"
        }
    }

    // IMPORTANTE: Solo mostrar participantes reales de la base de datos
    // Usar reservas si hay oferta, sino participantes del grupo (deprecated)
    val participants = remember(reservations, activeGroup?.participants, currentUser?.id, userReservationQuantity) {
        if (reservations.isNotEmpty()) {
            // Mapear reservas reales de la base de datos
            reservations.map { reservation ->
                val alias = reservation.userAlias?.ifBlank { "Participante" } ?: "Participante"
                val isCurrentUser = reservation.userId == currentUser?.id
                ParticipantData(
                    id = reservation.userId.ifBlank { UUID.randomUUID().toString() },
                    name = if (isCurrentUser) "Tú" else alias,
                    units = reservation.units.coerceAtLeast(0),
                    isYou = isCurrentUser
                )
            }
        } else if (activeGroup != null) {
            // Usar participantes del grupo (deprecated)
            val activeParticipants = activeGroup.activeParticipants
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
                // Si no hay participantes activos, mostrar solo el usuario actual si tiene reserva
                if (userReservationQuantity > 0) {
                    listOf(
                        ParticipantData(
                            id = currentUser?.id ?: "user-0",
                            name = "Tú",
                            units = userReservationQuantity,
                            isYou = true
                        )
                    )
                } else {
                    emptyList()
                }
            }
        } else {
            emptyList()
        }
    }

    val shareLink = remember(activeOffer?.id, activeGroup?.id) { 
        "https://nexusbiz.app/oferta/${activeOffer?.id ?: activeGroup?.id}" 
    }

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
                        text = "Oferta – ${activeOffer?.productName ?: activeGroup?.productName ?: "Producto"}",
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
                            model = (activeOffer?.imageUrl ?: activeGroup?.productImage)?.takeIf { it.isNotBlank() } 
                                ?: "https://via.placeholder.com/400",
                            contentDescription = activeOffer?.productName ?: activeGroup?.productName,
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
                                    text = activeOffer?.productName ?: activeGroup?.productName ?: "Producto",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = (activeOffer?.storeName ?: activeGroup?.storeName)?.takeIf { it.isNotBlank() } 
                                            ?: "Bodega",
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
                                    text = currencyFormat.format(
                                        activeOffer?.groupPrice?.takeIf { it > 0 } 
                                            ?: activeGroup?.groupPrice?.takeIf { it > 0 } 
                                            ?: activeOffer?.normalPrice 
                                            ?: activeGroup?.normalPrice 
                                            ?: 0.0
                                    ),
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
                                // CORRECCIÓN: Actualizar tiempo restante en tiempo real cada minuto
                                var hoursRemaining by remember(activeOffer?.expiresAt, activeGroup?.expiresAt) {
                                    val expiresAt = activeOffer?.expiresAt?.let { 
                                        try {
                                            java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                                        } catch (e: Exception) {
                                            null
                                        }
                                    } ?: activeGroup?.expiresAt ?: 0L
                                    mutableStateOf(max(0L, (expiresAt - System.currentTimeMillis()) / (1000 * 60 * 60)))
                                }
                                // Actualizar cada minuto para mostrar tiempo restante en tiempo real
                                LaunchedEffect(activeOffer?.expiresAt, activeGroup?.expiresAt) {
                                    while (true) {
                                        val expiresAt = activeOffer?.expiresAt?.let { 
                                            try {
                                                java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                                            } catch (e: Exception) {
                                                null
                                            }
                                        } ?: activeGroup?.expiresAt ?: 0L
                                        hoursRemaining = max(0L, (expiresAt - System.currentTimeMillis()) / (1000 * 60 * 60))
                                        kotlinx.coroutines.delay(60 * 1000) // Actualizar cada minuto
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
                                    text = "$reservedUnits / $targetUnits",
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
                        
                        // Mostrar unidades reservadas por otros
                        if (unitsReservedByOthers > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Otros han reservado: ",
                                    fontSize = 13.sp,
                                    color = Color(0xFF606060)
                                )
                                Text(
                                    text = "$unitsReservedByOthers ${if (unitsReservedByOthers == 1) "unidad" else "unidades"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "💡 Para activar el código QR de retiro, la oferta debe alcanzar $targetUnits unidades reservadas",
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
                            // Detalle de mi reserva
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Detalle de mi reserva",
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
                    // Mostrar botón de reserva solo si el usuario no tiene reserva y la oferta está activa
                    if (userReservation == null && activeOffer != null && activeOffer.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.ACTIVE && !activeOffer.isExpired && onCreateReservation != null) {
                        var quantity by remember { mutableStateOf(1) }
                        val maxQuantity = remember(currentUser?.points) {
                            when {
                                (currentUser?.points ?: 0) >= 300 -> 6
                                (currentUser?.points ?: 0) >= 100 -> 4
                                else -> 2
                            }
                        }
                        
                        LaunchedEffect(maxQuantity) {
                            if (quantity > maxQuantity) quantity = maxQuantity
                        }
                        
                        // Selector de cantidad
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cantidad:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1A1A1A)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    enabled = quantity > 1
                                ) {
                                    Text(
                                        text = "-",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (quantity > 1) Color(0xFF10B981) else Color(0xFFCCCCCC)
                                    )
                                }
                                Text(
                                    text = "$quantity",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                IconButton(
                                    onClick = { if (quantity < maxQuantity && quantity < (activeOffer?.unitsNeeded ?: 0)) quantity++ },
                                    enabled = quantity < maxQuantity && quantity < (activeOffer?.unitsNeeded ?: 0)
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (quantity < maxQuantity && quantity < (activeOffer?.unitsNeeded ?: 0)) Color(0xFF10B981) else Color(0xFFCCCCCC)
                                    )
                                }
                            }
                        }
                        
                        Button(
                            onClick = { onCreateReservation(quantity) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Reservar $quantity ${if (quantity == 1) "unidad" else "unidades"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                    
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
