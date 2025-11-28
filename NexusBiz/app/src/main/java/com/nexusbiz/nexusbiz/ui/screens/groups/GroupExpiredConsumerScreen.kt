package com.nexusbiz.nexusbiz.ui.screens.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.data.model.Group
import com.nexusbiz.nexusbiz.data.model.Offer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun GroupExpiredConsumerScreen(
    group: Group? = null, // @Deprecated - mantener temporalmente
    offer: com.nexusbiz.nexusbiz.data.model.Offer? = null,
    onBack: () -> Unit,
    onQuickBuy: (String) -> Unit,
    onExplore: () -> Unit,
) {
    val activeOffer = offer
    val activeGroup = group
    
    if (activeOffer == null && activeGroup == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No se pudo cargar la información de la oferta")
        }
        return
    }

    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val groupPrice = activeOffer?.groupPrice ?: (if (activeGroup?.groupPrice ?: 0.0 > 0) activeGroup!!.groupPrice else activeGroup?.normalPrice ?: 0.0)
    val normalPrice = activeOffer?.normalPrice ?: (if (activeGroup?.normalPrice ?: 0.0 > 0) activeGroup!!.normalPrice else groupPrice)
    val targetUnits = max(1, activeOffer?.targetUnits ?: activeGroup?.targetSize ?: 1)
    val currentUnits = (activeOffer?.reservedUnits ?: activeGroup?.reservedUnits ?: 0).coerceAtLeast(0)
    val progress = activeOffer?.progress ?: (currentUnits.toFloat() / targetUnits.toFloat()).coerceIn(0f, 1f)
    val unitsMissing = activeOffer?.unitsNeeded ?: max(0, targetUnits - currentUnits)
    val expiredTime = remember(activeOffer?.expiresAt, activeGroup?.expiresAt) {
        val expiresAt = activeOffer?.expiresAt?.let { 
            try {
                java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
            } catch (e: Exception) {
                null
            }
        } ?: activeGroup?.expiresAt ?: System.currentTimeMillis()
        SimpleDateFormat("HH:mm", Locale("es", "PE")).format(Date(expiresAt))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F7))
    ) {
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
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ExpiredIcon()

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "El grupo no llegó a la meta",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No se alcanzaron las unidades necesarias antes del tiempo límite",
                    fontSize = 16.sp,
                    color = Color(0xFF606060),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tu reserva fue cancelada automáticamente",
                    fontSize = 14.sp,
                    color = Color(0xFF606060),
                    textAlign = TextAlign.Center
                )
            }

            // Progress card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.White,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Progreso final del grupo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$currentUnits / $targetUnits",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF606060),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "unidades reservadas",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp,
                            color = Color(0xFF606060),
                            textAlign = TextAlign.Center
                        )
                    }

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = Color(0xFFFF914D),
                        trackColor = Color(0xFFF4F4F7)
                    )

                    Text(
                        text = "Faltaron $unitsMissing unidades",
                        fontSize = 14.sp,
                        color = Color(0xFFFF914D),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .border(
                                BorderStroke(1.dp, Color(0xFFF4F4F7)),
                                RoundedCornerShape(0.dp)
                            )
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFF606060),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Expirado a las $expiredTime",
                            fontSize = 13.sp,
                            color = Color(0xFF606060)
                        )
                    }
                }
            }

            // Product card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.White,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = (activeOffer?.imageUrl ?: activeGroup?.productImage ?: "").ifEmpty { "https://via.placeholder.com/150" },
                        contentDescription = activeOffer?.productName ?: activeGroup?.productName ?: "Producto",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = activeOffer?.productName ?: activeGroup?.productName ?: "Producto",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = (activeOffer?.storeName ?: activeGroup?.storeName ?: "").ifBlank { "Bodega" },
                            fontSize = 13.sp,
                            color = Color(0xFF606060)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Precio que nos iba a salir grupal:",
                                    fontSize = 13.sp,
                                    color = Color(0xFF606060)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currency.format(groupPrice),
                                    fontSize = 16.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "por unidad",
                                    fontSize = 12.sp,
                                    color = Color(0xFF606060)
                                )
                            }
                            if (normalPrice > groupPrice) {
                                Text(
                                    text = "Precio normal: ${currency.format(normalPrice)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF606060),
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color(0xFFF0F7FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(text = "No se generó ningún QR")
                    InfoRow(text = "No se realizó ningún cobro")
                    InfoRow(text = "No tienes ningún compromiso de pago")
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onQuickBuy(activeOffer?.id ?: activeGroup?.productId?.ifBlank { activeGroup?.id } ?: "") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver bodegas con stock ahora",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                OutlinedButton(
                    onClick = onExplore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, Color(0xFFF4F4F7)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF606060)
                    )
                ) {
                    Text(
                        text = "Explorar otras ofertas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF606060)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpiredIcon() {
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF914D).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = Color(0xFFFF914D),
                modifier = Modifier.size(64.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF914D)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = Color(0xFF1E3A8A),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF1E3A8A)
        )
    }
}


