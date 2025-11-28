package com.nexusbiz.nexusbiz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.data.model.Product
import com.nexusbiz.nexusbiz.data.model.StorePlan
import com.nexusbiz.nexusbiz.data.model.Group
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeGroup: Group? = null // Grupo activo para mostrar datos reales
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    val discountPercent = if (product.normalPrice > 0.0) {
        (((1.0 - (product.groupPrice / product.normalPrice)).coerceAtLeast(0.0)) * 100).roundToInt()
    } else {
        0
    }
    
    // Usar datos reales del grupo si existe, sino usar valores por defecto
    val targetUnits = activeGroup?.targetSize ?: product.minGroupSize
    val currentUnits = activeGroup?.currentSize ?: 0
    val progressValue = if (targetUnits > 0) (currentUnits.toFloat() / targetUnits).coerceIn(0f, 1f) else 0f
    val remainingUnits = (targetUnits - currentUnits).coerceAtLeast(0)
    
    // Calcular tiempo restante usando el tiempo real del grupo
    val timeRemaining = activeGroup?.let { group ->
        // Usar timeRemaining del grupo que ya calcula correctamente expiresAt - ahora
        group.timeRemaining
    } ?: 0L
    
    val hoursRemaining = (timeRemaining / (1000 * 60 * 60)).toInt()
    val minutesRemaining = ((timeRemaining % (1000 * 60 * 60)) / (1000 * 60)).toInt()
    val timeText = if (timeRemaining > 0 && activeGroup != null) {
        if (hoursRemaining > 0) "${hoursRemaining}h ${minutesRemaining}m" else "${minutesRemaining}m"
    } else if (activeGroup != null) {
        "Expirado"
    } else {
        "Sin oferta activa"
    }

    val isPro = product.storePlan == StorePlan.PRO
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPro) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) {
                Color(0xFFFFF9E6) // Fondo dorado claro para plan PRO
            } else {
                Color.White
            }
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                AsyncImage(
                    model = product.imageUrl?.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/300x200",
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                // Badge PRO dorado
                if (isPro) {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFACC15)
                    ) {
                        Text(
                            text = "PRO",
                            color = Color(0xFF7C2D12),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                }
                if (discountPercent > 0) {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopEnd),
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF10B981)
                    ) {
                        Text(
                            text = "-$discountPercent%",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.storeName.ifBlank { "Bodega sin nombre" },
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currencyFormat.format(product.normalPrice),
                        color = Color(0xFF9CA3AF),
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currencyFormat.format(product.groupPrice),
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "x unidad",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE5E7EB))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressValue)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF10B981))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Participantes",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$currentUnits/$targetUnits unidades",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = Color(0xFFFEF9C3),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Faltan ${remainingUnits.coerceAtLeast(1)}",
                            color = Color(0xFFCA8A04),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Tiempo",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = timeText,
                        color = if (timeRemaining > 0) Color(0xFF6B7280) else Color(0xFFDC2626),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

