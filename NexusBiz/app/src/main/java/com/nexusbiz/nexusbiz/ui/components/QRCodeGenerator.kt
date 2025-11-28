package com.nexusbiz.nexusbiz.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun QRCodeDisplay(
    data: String,
    modifier: Modifier = Modifier,
    size: Int = 300
) {
    val bitmap = remember(data, size) { generateQRCode(data, size) }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(size.dp)
            )
        } ?: Text(
            text = "Error generando QR",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

fun generateQRCode(data: String, size: Int): Bitmap? {
    return try {
        // Usar ZXing para generar QR
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val hints = java.util.Hashtable<com.google.zxing.EncodeHintType, Any>()
        hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
        hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[com.google.zxing.EncodeHintType.MARGIN] = 1
        
        val bitMatrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints)
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

