package com.nexusbiz.nexusbiz.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val districts = listOf(
    "Trujillo",
    "La Esperanza",
    "El Porvenir",
    "Florencia de Mora",
    "Víctor Larco Herrera",
    "Huanchaco",
    "Moche",
    "Salaverry",
    "Laredo",
    "Simbal",
    "Poroto"
)

@Composable
fun SelectDistrictScreen(
    initialDistrict: String? = null,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val selectedDistrict = remember { mutableStateOf(initialDistrict) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FAFB)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(0.dp),
                color = Color.White,
                tonalElevation = 0.dp,
                border = BorderStroke(0.dp, Color.Transparent)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Selecciona tu distrito",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(districts) { district ->
                    val isSelected = district == selectedDistrict.value
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = if (isSelected) 4.dp else 2.dp,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(if (isSelected) Color(0xFFF0FDF4) else Color.White)
                            .clickable {
                                selectedDistrict.value = district
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFF0FDF4) else Color.White,
                        border = if (isSelected) BorderStroke(2.dp, Color(0xFF10B981)) else BorderStroke(0.dp, Color.Transparent),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = district,
                                fontSize = 16.sp,
                                color = if (isSelected) Color(0xFF065F46) else Color(0xFF1A1A1A),
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Seleccionado",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Button(
                        onClick = {
                            selectedDistrict.value?.let { onConfirm(it) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = selectedDistrict.value != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.5f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Confirmar distrito",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

