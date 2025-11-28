package com.nexusbiz.nexusbiz.ui.screens.store

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StoreBoostVisibilityScreen(
    onBack: () -> Unit,
    onRequestBoost: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedOption by rememberSaveable { mutableStateOf<String?>(null) }

    val options = listOf(
        BoostOption(
            id = "highlight",
            icon = Icons.Default.FlashOn,
            title = "Destacar mi oferta por 24h",
            description = "Aparece en la sección destacada del Home",
            price = "S/ 15",
            color = Color(0xFFFACC15),
            benefits = listOf("Fondo amarillo destacado", "+200% visibilidad", "Primero en resultados")
        ),
        BoostOption(
            id = "position",
            icon = Icons.Default.Visibility,
            title = "Ganar posición en el Home",
            description = "Sube hasta el top 3 en tu distrito",
            price = "S/ 10",
            color = Color(0xFFFF914D),
            benefits = listOf("Top 3 en tu distrito", "+100% clicks", "Por 12 horas")
        ),
        BoostOption(
            id = "notify",
            icon = Icons.Default.Notifications,
            title = "Notificar a mi distrito",
            description = "Envía una notificación push a usuarios cercanos",
            price = "S/ 20",
            color = Color(0xFF10B981),
            benefits = listOf("Push notification", "Usuarios a 2km", "Hasta 500 personas")
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A).copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .align(Alignment.Center)
                .background(Color.White, RoundedCornerShape(28.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFFFACC15), Color(0xFFFF914D))),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButtonBubble(
                            icon = Icons.Default.ArrowBack,
                            onClick = onBack
                        )
                        IconButtonBubble(
                            icon = null,
                            onClick = onBack,
                            label = "✕"
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        BoostIconPlaceholder(icon = Icons.Default.FlashOn, color = Color.White)
                        Column {
                            Text("Aumenta tu visibilidad", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text("Llega a más clientes", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { option ->
                    BoostOptionCard(
                        option = option,
                        selected = option.id == selectedOption,
                        onSelect = { selectedOption = option.id }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureHighlight(
                        icon = Icons.Default.QueryStats,
                        text = "Estadísticas avanzadas",
                        color = Color(0xFF10B981)
                    )
                    FeatureHighlight(
                        icon = Icons.Default.TrendingUp,
                        text = "Más visibilidad",
                        color = Color(0xFFFACC15)
                    )
                    FeatureHighlight(
                        icon = Icons.Default.Star,
                        text = "Insignia PRO",
                        color = Color(0xFFFF914D)
                    )
                }

                Button(
                    onClick = {
                        selectedOption?.let { id ->
                            val option = options.firstOrNull { it.id == id }
                            option?.let {
                                val message = "Hola, quiero solicitar: ${it.title} - ${it.price}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/51999999999?text=${Uri.encode(message)}"))
                                context.startActivity(intent)
                                onRequestBoost(id)
                                onBack()
                            }
                        }
                    },
                    enabled = selectedOption != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedOption != null) Color(0xFF10B981) else Color(0xFFF1F5F9),
                        contentColor = if (selectedOption != null) Color.White else Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Solicitar Boost", fontWeight = FontWeight.SemiBold)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Text(
                        text = "💬 El pago se coordina por WhatsApp",
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IconButtonBubble(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    label: String? = null
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White)
        } else if (label != null) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun RowScope.FeatureHighlight(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(110.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Text(text = text, color = Color(0xFF606060), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BoostOptionCard(
    option: BoostOption,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (selected) Color(0xFF10B981) else Color(0xFFF4F4F7)
    val background = if (selected) Color(0xFF10B981).copy(alpha = 0.06f) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(option.color.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = option.color,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(option.title, color = Color(0xFF1A1A1A), fontWeight = FontWeight.SemiBold)
                    Text(option.description, color = Color(0xFF606060), fontSize = 13.sp)
                }
                Text(option.price, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                option.benefits.forEach {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(option.color, CircleShape)
                        )
                        Text(it, color = Color(0xFF606060), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoostIconPlaceholder(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
    }
}

private data class BoostOption(
    val id: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val price: String,
    val color: Color,
    val benefits: List<String>
)

