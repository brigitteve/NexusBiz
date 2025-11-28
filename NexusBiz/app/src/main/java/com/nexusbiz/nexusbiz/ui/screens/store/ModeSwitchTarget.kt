package com.nexusbiz.nexusbiz.ui.screens.store

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ModeSwitchTarget(
    val modeId: String,
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
    val iconShadowColor: Color
) {
    CLIENTE(
        modeId = "cliente",
        title = "Cambiando a modo cliente...",
        subtitle = "Un momento por favor",
        icon = Icons.Default.ShoppingCart,
        iconColor = Color(0xFF009E62),
        backgroundColor = Color(0xFF009E62),
        iconShadowColor = Color(0xFF009E62).copy(alpha = 0.2f)
    ),
    BODEGUERO(
        modeId = "bodeguero",
        title = "Cambiando a modo Bodeguero...",
        subtitle = "Un momento por favor",
        icon = Icons.Default.Store,
        iconColor = Color(0xFFF9C642),
        backgroundColor = Color(0xFFF9C642),
        iconShadowColor = Color(0xFFF9C642).copy(alpha = 0.2f)
    );

    companion object {
        fun fromRoute(route: String?): ModeSwitchTarget =
            values().firstOrNull { it.modeId == route } ?: CLIENTE
    }
}

