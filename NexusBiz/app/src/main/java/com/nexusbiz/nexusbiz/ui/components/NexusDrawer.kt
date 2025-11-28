package com.nexusbiz.nexusbiz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NexusDrawerContent(
    userName: String = "Usuario",
    userPhone: String = "",
    onNavigateToProfile: () -> Unit,
    onNavigateToMyGroups: () -> Unit,
    onSwitchToStore: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(Color(0xFF10B981)),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = onClose, modifier = Modifier.padding(8.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar drawer", tint = Color.White)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(56.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(userName, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (userPhone.isNotBlank()) "*** *** ${userPhone.takeLast(4)}" else "",
                    color = Color(0xFFE5E7EB),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { onNavigateToProfile() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Mi perfil", tint = Color(0xFF6B7280))
                    Spacer(Modifier.width(16.dp))
                    Text("Mi perfil", color = Color(0xFF1A1A1A), fontSize = 16.sp)
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { onNavigateToMyGroups() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = "Mis grupos", tint = Color(0xFF6B7280))
                    Spacer(Modifier.width(16.dp))
                    Text("Mis grupos", color = Color(0xFF1A1A1A), fontSize = 16.sp)
                }
            }
            Button(
                onClick = onSwitchToStore,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(imageVector = Icons.Default.Store, contentDescription = "Modo bodeguero", tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text("Cambiar a modo bodeguero")
            }
            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Cerrar sesión", tint = Color(0xFFEF4444))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", color = Color(0xFFEF4444))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "NexusBiz v1.0.0",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center
        )
    }
}

