package com.nexusbiz.nexusbiz.ui.screens.store

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ScanQRScreen(
    grupoData: GrupoScanData = GrupoScanData(),
    onQRScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    var isScanning by rememberSaveable { mutableStateOf(true) }
    var flashlightOn by rememberSaveable { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<ScanResultFeedback?>(null) }

    var unidadesValidadas by rememberSaveable { mutableStateOf(grupoData.unidadesValidadas) }
    var clientesAtendidos by rememberSaveable { mutableStateOf(grupoData.clientesAtendidos) }
    var clientesValidados by remember { mutableStateOf(grupoData.clientesValidados.toMutableSet()) }

    LaunchedEffect(isScanning, scanResult) {
        if (isScanning && scanResult == null) {
            delay(3000)
            val result = simulateScan(clientesValidados)
            handleScanResult(
                result = result,
                onSuccess = { cliente, unidades, qrId ->
                    clientesValidados.add(cliente)
                    unidadesValidadas += unidades
                    clientesAtendidos += 1
                    onQRScanned(qrId)
                },
                onFeedback = { scanResult = it }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CameraBackground()

        // Top overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.85f),
                        1f to Color.Transparent
                    )
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }
                Text(
                    text = "Escanear QR",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = { flashlightOn = !flashlightOn },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (flashlightOn) Color(0xFFFACC15) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashlightOn,
                        contentDescription = "Linterna",
                        tint = if (flashlightOn) Color.Black else Color.White
                    )
                }
            }
        }

        // Scan frame
        ScanFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            isScanning = isScanning && scanResult == null
        )

        // Manual scan trigger
        Button(
            onClick = {
                if (scanResult == null) {
                    val result = simulateScan(clientesValidados)
                    handleScanResult(
                        result = result,
                        onSuccess = { cliente, unidades, qrId ->
                            clientesValidados.add(cliente)
                            unidadesValidadas += unidades
                            clientesAtendidos += 1
                            onQRScanned(qrId)
                        },
                        onFeedback = { scanResult = it }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
                .clip(RoundedCornerShape(999.dp))
                .shadow(8.dp, RoundedCornerShape(999.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
            Text(text = "Simular escaneo", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        // Bottom status panel
        BottomStatusPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            producto = grupoData.producto,
            unidadesValidadas = unidadesValidadas,
            metaUnidades = grupoData.metaUnidades,
            clientesAtendidos = clientesAtendidos,
            onClose = onBack
        )

        // Feedback overlay
        scanResult?.let { feedback ->
            FeedbackDialog(
                feedback = feedback,
                onDismiss = {
                    scanResult = null
                    isScanning = true
                }
            )
        }
    }
}

private fun handleScanResult(
    result: SimulatedScanResult,
    onSuccess: (String, Int, String) -> Unit,
    onFeedback: (ScanResultFeedback) -> Unit
) {
    when (result.type) {
        ScanResultType.SUCCESS -> {
            onSuccess(result.cliente!!, result.unidades!!, result.qrId!!)
            onFeedback(
                ScanResultFeedback(
                    type = ScanResultType.SUCCESS,
                    title = "Reserva validada",
                    subtitle = "Cliente: ${result.cliente}",
                    unidades = result.unidades
                )
            )
        }

        ScanResultType.WARNING -> {
            onFeedback(
                ScanResultFeedback(
                    type = ScanResultType.WARNING,
                    title = "QR ya validado",
                    subtitle = "Este QR ya fue utilizado"
                )
            )
        }

        ScanResultType.ERROR -> {
            onFeedback(
                ScanResultFeedback(
                    type = ScanResultType.ERROR,
                    title = "QR incorrecto",
                    subtitle = "Este QR no pertenece a este grupo"
                )
            )
        }
    }
}

@Composable
private fun CameraBackground() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
            ),
            size = size,
            style = Fill
        )
        repeat(60) {
            val x = Random.nextFloat() * size.width
            val y = Random.nextFloat() * size.height
            val radius = Random.nextDouble(1.0, 3.0).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = Random.nextFloat()),
                radius = radius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}

@Composable
private fun ScanFrame(modifier: Modifier = Modifier, isScanning: Boolean) {
    val frameSize = 260.dp
    val infiniteTransition = rememberInfiniteTransition(label = "scan-line")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan-line-anim"
    )

    Box(
        modifier = modifier
            .size(frameSize)
            .border(4.dp, Color(0xFF10B981), RoundedCornerShape(40.dp))
            .padding(4.dp)
    ) {
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp)
                    .offset(y = ((frameSize - 12.dp) * scanOffset) - frameSize / 2)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF34D399),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Apunta la cámara al código QR",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BottomStatusPanel(
    modifier: Modifier,
    producto: String,
    unidadesValidadas: Int,
    metaUnidades: Int,
    clientesAtendidos: Int,
    onClose: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.85f)
                )
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(28.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = producto, color = Color.White, fontWeight = FontWeight.SemiBold)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Unidades validadas", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text(
                        "$unidadesValidadas / $metaUnidades",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((unidadesValidadas / metaUnidades.toFloat()).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color(0xFF4ADE80), RoundedCornerShape(999.dp))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Clientes atendidos", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text(
                        clientesAtendidos.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Text(text = "Cerrar escáner", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FeedbackDialog(
    feedback: ScanResultFeedback,
    onDismiss: () -> Unit
) {
    val (borderColor, icon, iconTint, backgroundTint) = when (feedback.type) {
        ScanResultType.SUCCESS -> Quad(Color(0xFF10B981), Icons.Default.CheckCircle, Color(0xFF059669), Color(0xFFD1FAE5))
        ScanResultType.WARNING -> Quad(Color(0xFFF59E0B), Icons.Default.Warning, Color(0xFFB45309), Color(0xFFFFF7ED))
        ScanResultType.ERROR -> Quad(Color(0xFFEF4444), Icons.Default.Warning, Color(0xFFB91C1C), Color(0xFFFEE2E2))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(3.dp, borderColor, RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(backgroundTint, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(36.dp))
            }

            Text(text = feedback.title, color = Color(0xFF111827), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(
                text = feedback.subtitle,
                color = Color(0xFF4B5563),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            feedback.unidades?.let {
                Text(
                    text = "Unidades: $it",
                    color = Color(0xFF1F2937),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = borderColor)
            ) {
                Text("OK", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatColumn(label = "Reservadas", value = reservedText)
        StatColumn(label = "Vendidas", value = soldText)
        StatColumn(label = "Escaneadas", value = scannedText)
    }
}

@Composable
private fun RowScope.InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Column {
                Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
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
            Text(text = title, fontSize = 13.sp, color = Color(0xFF9CA3AF))
        }
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

private fun simulateScan(validatedClients: Set<String>): SimulatedScanResult {
    val randomQR = QR_DATABASE.random()
    return when {
        randomQR.grupoId != "GRUPO_ACTUAL" -> SimulatedScanResult(type = ScanResultType.ERROR)
        validatedClients.contains(randomQR.cliente) -> SimulatedScanResult(type = ScanResultType.WARNING)
        else -> SimulatedScanResult(
            type = ScanResultType.SUCCESS,
            cliente = randomQR.cliente,
            unidades = randomQR.unidades,
            qrId = randomQR.id
        )
    }
}

private data class QRRecord(
    val id: String,
    val cliente: String,
    val unidades: Int,
    val grupoId: String
)

private enum class ScanResultType { SUCCESS, WARNING, ERROR }

private data class SimulatedScanResult(
    val type: ScanResultType,
    val cliente: String? = null,
    val unidades: Int? = null,
    val qrId: String? = null
)

private data class ScanResultFeedback(
    val type: ScanResultType,
    val title: String,
    val subtitle: String,
    val unidades: Int? = null
)

data class GrupoScanData(
    val producto: String = "Leche Gloria 1L",
    val unidadesValidadas: Int = 12,
    val metaUnidades: Int = 20,
    val clientesAtendidos: Int = 8,
    val clientesValidados: Set<String> = setOf("Usuario123")
)

private val QR_DATABASE = listOf(
    QRRecord("QR001", "Usuario123", 2, "GRUPO_ACTUAL"),
    QRRecord("QR002", "MariaP45", 3, "GRUPO_ACTUAL"),
    QRRecord("QR003", "CarlosM88", 1, "GRUPO_ACTUAL"),
    QRRecord("QR004", "AnaLucia22", 2, "GRUPO_ACTUAL"),
    QRRecord("QR005", "PedroJR", 4, "GRUPO_ACTUAL"),
    QRRecord("QR006", "LucyVega", 2, "GRUPO_ACTUAL"),
    QRRecord("QR007", "JorgeL99", 3, "GRUPO_ACTUAL"),
    QRRecord("QR008", "SofiaRios", 1, "GRUPO_ACTUAL"),
    QRRecord("QR999", "TestUser", 2, "OTRO_GRUPO")
)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)