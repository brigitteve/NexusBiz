package com.nexusbiz.nexusbiz.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen1(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE9F7EF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingBag,
                contentDescription = "Icono ahorro",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(48.dp)
    )
}

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Ahorra comprando en grupo en tu distrito",
                color = Color(0xFF111111),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Únete a compras colectivas y paga menos en productos básicos.",
                color = Color(0xFF6B7280),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
    )
}

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IndicatorDot(isActive = true)
                Spacer(modifier = Modifier.width(8.dp))
                IndicatorDot(isActive = false)
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(
                    text = "Siguiente",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Saltar",
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen2(
    onNext: () -> Unit,
    onSkip: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFFFF8E7)),
            contentAlignment = Alignment.Center
        ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Icono escudo",
                    tint = Color(0xFFF4C015),
                    modifier = Modifier.size(52.dp)
                )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Sin pagos en la app",
                color = Color(0xFF111111),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Solo reservas, QR de retiro y pagas directamente en la bodega.",
                color = Color(0xFF6B7280),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IndicatorDot(isActive = false)
                Spacer(modifier = Modifier.width(8.dp))
                IndicatorRectActive()
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(
                    text = "Continuar",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Saltar",
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.SemiBold
                )
                }
            }
    }
}

@Composable
private fun IndicatorRectActive() {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF10B981))
    )
}

@Composable
private fun IndicatorDot(isActive: Boolean) {
    val color = if (isActive) Color(0xFF10B981) else Color(0xFFD1D5DB)
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color = color, shape = RoundedCornerShape(6.dp))
    )
}

