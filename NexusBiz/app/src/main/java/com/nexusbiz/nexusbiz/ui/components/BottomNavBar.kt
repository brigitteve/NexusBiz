package com.nexusbiz.nexusbiz.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Inicio", Icons.Default.Home)
    object MyGroups : BottomNavItem("my_groups", "Mis grupos", Icons.Default.Group)
    object Profile : BottomNavItem("profile", "Perfil", Icons.Default.Person)
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.MyGroups,
        BottomNavItem.Profile
    )
    val accent = Color(0xFF10B981)
    val muted = Color(0xFF6B7280)

    Surface(
        modifier = modifier,
        color = Color.White,
        tonalElevation = 6.dp
    ) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemClick(item.route) },
                    icon = {
                        Icon(
                            modifier = Modifier.offset(y = 2.dp),
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) accent else muted
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 12.sp,
                            color = if (selected) accent else muted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        unselectedIconColor = muted,
                        unselectedTextColor = muted,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
