package com.nexusbiz.nexusbiz.ui.screens.store

import android.Manifest
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanQRScreen(
    groupId: String? = null,
    onQRScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var isScanning by rememberSaveable { mutableStateOf(false) }
    var flashlightOn by rememberSaveable { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<ScanResultFeedback?>(null) }
    var barcodeView by remember { mutableStateOf<DecoratedBarcodeView?>(null) }

    // Solicitar permiso de cámara al iniciar
    LaunchedEffect(Unit) {
        if (cameraPermissionState.status !is PermissionStatus.Granted) {
            cameraPermissionState.launchPermissionRequest()
        } else {
            isScanning = true
        }
    }

    // Observar cuando se otorga el permiso
    LaunchedEffect(cameraPermissionState.status) {
        if (cameraPermissionState.status is PermissionStatus.Granted) {
            isScanning = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Mostrar cámara solo si tiene permiso
        if (cameraPermissionState.status is PermissionStatus.Granted) {
            AndroidView(
                factory = { ctx ->
                    val view = DecoratedBarcodeView(ctx).apply {
                        val formats = listOf(BarcodeFormat.QR_CODE)
                        barcodeView = this
                        decoderFactory = DefaultDecoderFactory(formats)
                        resume()
                    }
                    view
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    if (isScanning && scanResult == null) {
                        view.decodeContinuous(object : BarcodeCallback {
                            override fun barcodeResult(result: BarcodeResult?) {
                                result?.text?.let { qrCode ->
                                    // Detener el escaneo temporalmente
                                    isScanning = false
                                    view.pause()
                                    
                                    // Procesar el resultado
                                    handleQRResult(
                                        qrCode = qrCode,
                                        groupId = groupId,
                                        onSuccess = { message ->
                                            scanResult = ScanResultFeedback(
                                                type = ScanResultType.SUCCESS,
                                                title = "QR escaneado",
                                                subtitle = message
                                            )
                                            onQRScanned(qrCode)
                                        },
                                        onError = { errorMessage ->
                                            scanResult = ScanResultFeedback(
                                                type = ScanResultType.ERROR,
                                                title = "Error",
                                                subtitle = errorMessage
                                            )
                                            // Reanudar escaneo después de un momento
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                isScanning = true
                                                view.resume()
                                            }, 2000)
                                        },
                                        onWarning = { warningMessage ->
                                            scanResult = ScanResultFeedback(
                                                type = ScanResultType.WARNING,
                                                title = "Advertencia",
                                                subtitle = warningMessage
                                            )
                                            // Reanudar escaneo después de un momento
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                isScanning = true
                                                view.resume()
                                            }, 2000)
                                        }
                                    )
                                }
                            }

                            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
                                // No necesitamos hacer nada aquí
                            }
                        })
                    } else if (!isScanning) {
                        view.pause()
                    }
                }
            )

            // Control de linterna
            LaunchedEffect(flashlightOn) {
                barcodeView?.let { view ->
                    if (flashlightOn) {
                        view.setTorchOn()
                    } else {
                        view.setTorchOff()
                    }
                }
            }

            // Limpiar cuando se desmonte
            DisposableEffect(Unit) {
                onDispose {
                    barcodeView?.pause()
                    barcodeView?.setTorchOff()
                }
            }
        } else {
            // Mostrar mensaje si no tiene permiso
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Se necesita permiso de cámara para escanear QR",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Solicitar permiso", color = Color.White)
                }
            }
        }

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
                    enabled = cameraPermissionState.status is PermissionStatus.Granted,
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

        // Scan frame overlay
        if (cameraPermissionState.status is PermissionStatus.Granted) {
            ScanFrame(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                isScanning = isScanning && scanResult == null
            )
        }

        // Feedback overlay
        scanResult?.let { feedback ->
            FeedbackDialog(
                feedback = feedback,
                onDismiss = {
                    scanResult = null
                    if (cameraPermissionState.status is PermissionStatus.Granted) {
                        isScanning = true
                        barcodeView?.resume()
                    }
                }
            )
        }
    }
}

private fun handleQRResult(
    qrCode: String,
    groupId: String?,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
    onWarning: (String) -> Unit
) {
    // Validar que el QR tenga un formato válido
    if (qrCode.isBlank()) {
        onError("Código QR vacío")
        return
    }

    // El QR puede contener solo el código del grupo o un formato codificado
    // Por ahora, asumimos que el QR contiene el código del grupo
    // La validación real se hará en el repositorio
    // Si hay un groupId esperado, podemos validar que coincida
    onSuccess("QR escaneado correctamente")
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

private enum class ScanResultType { SUCCESS, WARNING, ERROR }

private data class ScanResultFeedback(
    val type: ScanResultType,
    val title: String,
    val subtitle: String
)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
