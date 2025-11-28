package com.nexusbiz.nexusbiz.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimerDisplay(
    timeRemaining: Long,
    modifier: Modifier = Modifier,
    onExpired: () -> Unit = {}
) {
    var currentTime by remember { mutableStateOf(timeRemaining) }
    var isExpired by remember { mutableStateOf(false) }
    
    LaunchedEffect(timeRemaining) {
        currentTime = timeRemaining
        isExpired = false
    }
    
    LaunchedEffect(currentTime) {
        if (currentTime > 0 && !isExpired) {
            delay(1000)
            currentTime -= 1000
            if (currentTime <= 0) {
                isExpired = true
                onExpired()
            }
        }
    }
    
    val hours = (currentTime / (1000 * 60 * 60)).toInt()
    val minutes = ((currentTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
    val seconds = ((currentTime % (1000 * 60)) / 1000).toInt()
    
    Text(
        text = if (isExpired) "Expirado" else String.format("%02dh %02dm %02ds", hours, minutes, seconds),
        style = MaterialTheme.typography.titleMedium,
        color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

