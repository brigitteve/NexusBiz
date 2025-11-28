package com.nexusbiz.nexusbiz.ui.screens.store

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StoreSubscriptionProScreen(
    onBack: () -> Unit,
    onSubscribe: () -> Unit = {}
) {
    val context = LocalContext.current
    val freeFeatures = listOf(
        FeatureItem("1 oferta activa", true),
        FeatureItem("1 grupo por oferta", true),
        FeatureItem("Sin estadísticas", false),
        FeatureItem("Posición estándar en Home", true)
    )
    val proFeatures = listOf(
        FeatureItem("4 ofertas activas", true),
        FeatureItem("3 grupos simultáneos", true),
        FeatureItem("Estadísticas del distrito", true),
        FeatureItem("Mejor posición en Home", true),
        FeatureItem("Insignia \"Bodega PRO\"", true),
        FeatureItem("Soporte prioritario", true)
    )

    Column(
        modifier = Modifier
            .background(Color(0xFFF4F4F7))
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFACC15), Color(0xFFFF914D))
                    )
                )
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                    Text(
                        text = "Planes",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Mejora a PRO",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    Text(
                        text = "Desbloquea todo el potencial de tu bodega",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PlanCard(
                title = "Plan Básico",
                subtitle = "Para empezar",
                price = "Gratis",
                gradient = false,
                features = freeFeatures
            )
            PlanCard(
                title = "Plan PRO",
                subtitle = "Para crecer tu negocio",
                price = "S/ 29 / mes",
                gradient = true,
                features = proFeatures,
                onAction = {
                    onSubscribe()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/51999999999?text=Hola,%20quiero%20actualizar%20a%20PRO"))
                    context.startActivity(intent)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Highlight(
                    icon = Icons.Default.BarChart,
                    text = "Estadísticas avanzadas",
                    color = Color(0xFF10B981)
                )
                Highlight(
                    icon = Icons.Default.TrendingUp,
                    text = "Más visibilidad",
                    color = Color(0xFFFACC15)
                )
                Highlight(
                    icon = Icons.Default.Star,
                    text = "Insignia PRO",
                    color = Color(0xFFFF914D)
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    price: String,
    gradient: Boolean,
    features: List<FeatureItem>,
    onAction: (() -> Unit)? = null
) {
    val background = if (gradient) {
        Brush.linearGradient(listOf(Color(0xFFFACC15), Color(0xFFFF914D)))
    } else {
        Brush.linearGradient(listOf(Color.White, Color.White))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (gradient) 12.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .background(background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        color = if (gradient) Color.White else Color(0xFF1A1A1A),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = if (gradient) Color.White.copy(alpha = 0.9f) else Color(0xFF606060),
                        fontSize = 14.sp
                    )
                }
                if (!gradient) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF4F4F7)
                    ) {
                        Text(
                            text = price,
                            color = Color(0xFF606060),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "Recomendado",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (gradient) {
                Text(
                    text = price,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (feature.available) Color.White.copy(alpha = if (gradient) 0.3f else 0.1f) else Color.Transparent,
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (!feature.available) Color(0xFF606060).copy(alpha = 0.3f) else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (feature.available) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (feature.available) Color.White else Color(0xFF606060),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = feature.text,
                            color = if (feature.available) {
                                if (gradient) Color.White else Color(0xFF1A1A1A)
                            } else {
                                Color(0xFF606060).copy(alpha = 0.5f)
                            },
                            fontSize = 14.sp
                        )
                    }
                }
            }

            onAction?.let {
                Button(
                    onClick = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gradient) Color.White else Color(0xFF10B981),
                        contentColor = if (gradient) Color(0xFFFF914D) else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = if (gradient) "Hacerme PRO" else "Quedarme en Básico", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RowScope.Highlight(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(110.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Text(text = text, color = Color(0xFF606060), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

private data class FeatureItem(
    val text: String,
    val available: Boolean
)

