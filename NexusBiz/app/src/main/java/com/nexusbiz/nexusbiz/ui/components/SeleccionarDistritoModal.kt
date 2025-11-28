package com.nexusbiz.nexusbiz.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val distritos = listOf(
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
fun SeleccionarDistritoModal(
    distritoActual: String = "",
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var distritoSeleccionado by remember { mutableStateOf(distritoActual) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color(0xFFF8F9FA)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ENCABEZADO
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp),
                    color = Color.White,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Selecciona tu distrito",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF111827)
                        )
                    }
                }

                // LISTA DE DISTRITOS
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(distritos) { distrito ->
                        val isSelected = distritoSeleccionado == distrito
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { distritoSeleccionado = distrito },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFFE7FBEA) else Color.White,
                            border = if (isSelected) {
                                BorderStroke(2.dp, Color(0xFF00C853))
                            } else {
                                BorderStroke(2.dp, Color.Transparent)
                            },
                            tonalElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = distrito,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF00C853),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // BOTÓN FIJO BOTTOM
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (distritoSeleccionado.isNotEmpty()) {
                                    onConfirmar(distritoSeleccionado)
                                }
                            },
                            enabled = distritoSeleccionado.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (distritoSeleccionado.isNotEmpty()) {
                                    Color(0xFF00C853)
                                } else {
                                    Color(0xFFD1D5DB)
                                },
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFD1D5DB),
                                disabledContentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Guardar distrito",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

