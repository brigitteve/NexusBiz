package com.nexusbiz.nexusbiz.ui.screens.groups

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusbiz.nexusbiz.data.model.Group
import com.nexusbiz.nexusbiz.data.model.Offer
import com.nexusbiz.nexusbiz.data.model.Reservation
import com.nexusbiz.nexusbiz.data.model.User
import com.nexusbiz.nexusbiz.ui.components.QRCodeDisplay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupQRScreen(
    group: Group? = null, // @Deprecated - mantener temporalmente
    offer: Offer? = null,
    reservation: Reservation? = null,
    currentUser: User?,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val activeOffer = offer
    val activeGroup = group
    val userReservation = reservation

    if (activeOffer == null && activeGroup == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Reserva no encontrada")
        }
        return
    }

    // IMPORTANTE: QR solo disponible si estado es PICKUP
    val isPickup = activeOffer?.status == com.nexusbiz.nexusbiz.data.model.OfferStatus.PICKUP ||
                   activeGroup?.status == com.nexusbiz.nexusbiz.data.model.GroupStatus.PICKUP
    
    if (!isPickup) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when {
                        activeOffer != null -> when (activeOffer.status) {
                            com.nexusbiz.nexusbiz.data.model.OfferStatus.ACTIVE -> "El QR aún no está disponible.\nLa meta no se ha completado."
                            com.nexusbiz.nexusbiz.data.model.OfferStatus.COMPLETED -> "La oferta ya fue completada."
                            com.nexusbiz.nexusbiz.data.model.OfferStatus.EXPIRED -> "La oferta expiró sin completar la meta."
                            else -> "El QR no está disponible para este estado."
                        }
                        else -> when (activeGroup?.status) {
                            com.nexusbiz.nexusbiz.data.model.GroupStatus.ACTIVE -> "El QR aún no está disponible.\nLa meta no se ha completado."
                            com.nexusbiz.nexusbiz.data.model.GroupStatus.VALIDATED -> "El grupo ya fue finalizado.\nTodos los participantes retiraron."
                            com.nexusbiz.nexusbiz.data.model.GroupStatus.EXPIRED -> "El grupo expiró sin completar la meta."
                            com.nexusbiz.nexusbiz.data.model.GroupStatus.COMPLETED -> "El grupo ya fue completado."
                            else -> "El QR no está disponible para este estado."
                        }
                    },
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color(0xFF606060)
                )
                Button(onClick = onBack) {
                    Text("Volver")
                }
            }
        }
        return
    }

    // Si no hay reserva del usuario, no podemos generar un QR válido
    if (userReservation == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "No se encontró una reserva activa para generar el QR.",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color(0xFF606060)
                )
                Button(onClick = onBack) {
                    Text("Volver")
                }
            }
        }
        return
    }

    // Obtener datos de la reserva del usuario
    val userReservationQuantity = userReservation.units.coerceAtLeast(0)
    
    // Calcular precios y totales
    val groupPrice = activeOffer?.groupPrice ?: (activeGroup?.groupPrice?.takeIf { it > 0 } ?: activeGroup?.normalPrice ?: 0.0)
    val totalReservationPrice = groupPrice * userReservationQuantity
    
    // Obtener estado de la reserva
    val reservationStatus = when {
        userReservation != null -> if (userReservation.isValidated) "Completado" else "Pendiente en retiro"
        else -> {
            val userParticipant = activeGroup?.activeParticipants?.firstOrNull { it.userId == currentUser?.id }
            if (userParticipant?.isValidated == true) "Completado" else "Pendiente en retiro"
        }
    }
    
    // Dirección de la bodega
    val storeName = activeOffer?.storeName ?: activeGroup?.storeName ?: "Bodega"
    val pickupAddress = activeOffer?.pickupAddress ?: ""
    val storeAddress = if (pickupAddress.isNotBlank()) {
        pickupAddress
    } else if (currentUser?.district?.isNotBlank() == true) {
        "$storeName, ${currentUser.district}"
    } else {
        storeName
    }
    
    // Generar código QR basado en la reserva
    // IMPORTANTE: El QR debe contener SIEMPRE el ID de la reserva,
    // que es lo que escaneará el bodeguero para validarla.
    val reservationCode = userReservation.id

    val handleNavigate = {
        val query = Uri.encode(storeAddress)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
        context.startActivity(intent)
    }

    val productName = activeOffer?.productName ?: activeGroup?.productName ?: "Producto"
    val handleShare = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Mi código QR de retiro: $reservationCode\nProducto: $productName\nBodega: $storeName")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir QR"))
        onShare()
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
                        text = "Mi QR de retiro",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.width(48.dp))
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
                // QR Card - Centrado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // QR Grande
                        QRCodeDisplay(
                            data = reservationCode,
                            modifier = Modifier.size(280.dp),
                            size = 280
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = productName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A),
                                textAlign = TextAlign.Center
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = storeName.ifBlank { "Bodega" },
                                    fontSize = 16.sp,
                                    color = Color(0xFF606060)
                                )
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF606060),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = storeAddress,
                                    fontSize = 14.sp,
                                    color = Color(0xFF606060)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (reservationStatus == "Completado") {
                                    Color(0xFF10B981)
                                } else {
                                    Color(0xFFFFD447)
                                }
                            ) {
                                Text(
                                    text = reservationStatus,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (reservationStatus == "Completado") Color.White else Color(0xFF1A1A1A)
                                )
                            }
                        }
                    }
                }

                // Info Card
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
                                text = "Unidades reservadas:",
                                fontSize = 14.sp,
                                color = Color(0xFF606060)
                            )
                            Text(
                                text = "$userReservationQuantity ${if (userReservationQuantity == 1) "unidad" else "unidades"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unidades reservadas:",
                                fontSize = 14.sp,
                                color = Color(0xFF606060)
                            )
                            Text(
                                text = "$userReservationQuantity ${if (userReservationQuantity == 1) "unidad" else "unidades"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Precio por unidad:",
                                fontSize = 14.sp,
                                color = Color(0xFF606060)
                            )
                            Text(
                                text = currencyFormat.format(groupPrice),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total de la reserva:",
                                fontSize = 14.sp,
                                color = Color(0xFF606060)
                            )
                            Text(
                                text = currencyFormat.format(totalReservationPrice),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }
                }

                // Info Card Gris
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F7)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "Muestra este QR en la bodega para validar tu retiro.",
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            color = Color(0xFF606060),
                            lineHeight = 20.sp
                        )
                    }
                }

                // Botón Cómo llegar
                Button(
                    onClick = handleNavigate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cómo llegar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Botón Compartir QR
                OutlinedButton(
                    onClick = handleShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, Color(0xFFF4F4F7)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1A1A1A)
                    )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Compartir QR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        }
    }
}

