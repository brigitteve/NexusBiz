package com.nexusbiz.nexusbiz.ui.screens.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.data.repository.RucRepository
import com.nexusbiz.nexusbiz.util.Validators
import com.nexusbiz.nexusbiz.util.onSuccess
import com.nexusbiz.nexusbiz.util.onFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// -----------------------------------------------------------
// 1. Modal de entrada al flujo de registro de bodega
// -----------------------------------------------------------
@Composable
fun BodegaRegistrationModalScreen(
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val accent = Color(0xFF10B981)
    val accentDeep = Color(0xFF059669)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F7).copy(alpha = 0.5f))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(accent, accentDeep))
                        )
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .shadow(12.dp, RoundedCornerShape(24.dp))
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Registra tu bodega en NexusBiz",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Únete a la red de bodegas que venden más con compras colectivas",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val benefits = listOf(
                        "Vende más con reservas colectivas" to "Vecinos reservan unidades hasta completar la meta.",
                        "Sin pagos, sin complicaciones" to "Los clientes pagan directo en tu bodega.",
                        "Solo escanea el QR y entrega las unidades" to "Cada cliente llega con su QR. Tú validas y entregas."
                    )
                    benefits.forEach { (title, desc) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    title,
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(desc, color = Color(0xFF606060), fontSize = 13.sp)
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            "Comenzar registro",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(20.dp)
                        )
                    }
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF606060))
                    ) {
                        Text("Cancelar", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------
// 2. Paso 1 de 4: Validar RUC (API Decolecta)
// -----------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodegaValidateRucScreen(
    onBack: () -> Unit,
    onValidated: (ruc: String, razonSocial: String, nombreComercial: String) -> Unit
) {
    var ruc by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var isValidated by remember { mutableStateOf(false) }
    var razonSocial by remember { mutableStateOf("") }
    var nombreComercial by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val accent = Color(0xFF10B981)
    val accentDeep = Color(0xFF059669)
    val scope = rememberCoroutineScope()
    val rucRepository = remember { RucRepository() }

    fun validateRuc() {
        if (ruc.length == 11 && !isValidating) {
            scope.launch {
                isValidating = true
                isValidated = false
                errorMessage = null
                val result = try {
                    rucRepository.consultarRuc(ruc)
                } finally {
                    isValidating = false
                }
                result.onSuccess { response ->
                    isValidated = true
                    razonSocial = response.razonSocial ?: ""
                    nombreComercial = response.nombreComercial ?: ""
                }.onFailure { exception ->
                    errorMessage = exception.message ?: "Error al validar el RUC"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F7).copy(alpha = 0.5f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(accent, accentDeep)))
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .shadow(12.dp, RoundedCornerShape(24.dp))
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                androidx.compose.material.icons.Icons.Outlined.Description,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Validar RUC",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Paso 1 de 4",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Número de RUC",
                                color = Color(0xFF1A1A1A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(" *", color = Color(0xFFEF4444), fontSize = 14.sp)
                        }

                        OutlinedTextField(
                            value = ruc,
                            onValueChange = { input: String ->
                                ruc = input.filter { it.isDigit() }.take(11)
                                isValidated = false
                                errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            placeholder = {
                                Text("43435435454", color = Color(0xFF9CA3AF))
                            },
                            textStyle = TextStyle(
                                color = Color(0xFF1A1A1A),
                                fontSize = 16.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    validateRuc()
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = Color(0xFFF4F4F7),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = accent,
                                focusedTextColor = Color(0xFF1A1A1A),
                                unfocusedTextColor = Color(0xFF1A1A1A),
                                focusedPlaceholderColor = Color(0xFF9CA3AF),
                                unfocusedPlaceholderColor = Color(0xFF9CA3AF)
                            )
                        )

                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isValidated,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = accent.copy(alpha = 0.1f),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(accent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        "RUC validado correctamente",
                                        color = accent,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isValidated,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Razón social", color = Color(0xFF606060), fontSize = 13.sp)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF4F4F7),
                                    tonalElevation = 0.dp
                                ) {
                                    Text(
                                        text = razonSocial.ifEmpty { "-" },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        color = Color(0xFF1A1A1A)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isValidated,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Nombre comercial",
                                        color = Color(0xFF1A1A1A),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(" *", color = Color(0xFFEF4444), fontSize = 14.sp)
                                }
                                OutlinedTextField(
                                    value = nombreComercial,
                                    onValueChange = {
                                        val sanitized = Validators.sanitizeName(it)
                                        nombreComercial = sanitized
                                    },
                                    placeholder = {
                                        Text(
                                            "Bodega Don José",
                                            color = Color(0xFF9CA3AF)
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accent,
                                        unfocusedBorderColor = Color(0xFFF4F4F7),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        cursorColor = accent,
                                        focusedTextColor = Color(0xFF1A1A1A),
                                        unfocusedTextColor = Color(0xFF1A1A1A)
                                    )
                                )
                            }
                        }

                        if (!isValidated) {
                            Button(
                                onClick = {
                                    validateRuc()
                                },
                                enabled = ruc.length == 11 && !isValidating,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (ruc.length == 11) accent else Color(
                                        0xFFF4F4F7
                                    ),
                                    contentColor = if (ruc.length == 11) Color.White else Color(
                                        0xFF606060
                                    )
                                )
                            ) {
                                if (isValidating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(end = 8.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Text("Validando...", fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text(
                                        "Validar RUC",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (nombreComercial.trim().isNotEmpty()) {
                                        onValidated(ruc, razonSocial, nombreComercial)
                                    }
                                },
                                enabled = nombreComercial.trim().isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (nombreComercial.trim()
                                            .isNotEmpty()
                                    ) accent else Color(0xFFF4F4F7),
                                    contentColor = if (nombreComercial.trim()
                                            .isNotEmpty()
                                    ) Color.White else Color(0xFF606060)
                                )
                            ) {
                                Text(
                                    "Continuar",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = if (nombreComercial.trim()
                                            .isNotEmpty()
                                    ) Color.White else Color(0xFF606060),
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------
// 3. Paso 2 de 4: Datos comerciales (distrito + dirección)
// -----------------------------------------------------------
@Composable
fun BodegaCommercialDataScreen(
    onBack: () -> Unit,
    onContinue: (district: String, address: String) -> Unit,
    razonSocial: String,
    nombreComercial: String
) {
    val accent = Color(0xFF10B981)
    val accentDeep = Color(0xFF059669)
    var district by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var showDistricts by remember { mutableStateOf(false) }
    val districts = listOf(
        "Trujillo",
        "El Porvenir",
        "Florencia de Mora",
        "Huanchaco",
        "La Esperanza",
        "Laredo",
        "Moche",
        "Poroto",
        "Salaverry",
        "Simbal",
        "Víctor Larco Herrera"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F7).copy(alpha = 0.5f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(accent, accentDeep)))
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .shadow(12.dp, RoundedCornerShape(24.dp))
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Datos Comerciales",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Paso 2 de 4",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Razón social", color = Color(0xFF606060), fontSize = 13.sp)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF4F4F7),
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = razonSocial.ifBlank { "-" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Nombre comercial",
                                color = Color(0xFF606060),
                                fontSize = 13.sp
                            )
                            Text(" *", color = Color(0xFFEF4444), fontSize = 13.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF4F4F7),
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = nombreComercial.ifBlank { "-" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Distrito",
                                color = Color(0xFF1A1A1A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(" *", color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                        Box {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showDistricts = !showDistricts },
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                color = Color.White,
                                border = BorderStroke(
                                    2.dp,
                                    if (district.isNotEmpty()) accent else Color(0xFFF4F4F7)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (district.isNotEmpty()) district else "Selecciona tu distrito",
                                        color = if (district.isNotEmpty()) Color(0xFF1A1A1A) else Color(
                                            0xFF606060
                                        ),
                                        fontSize = 15.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF606060),
                                        modifier = Modifier.rotate(if (showDistricts) 180f else 0f)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showDistricts,
                                onDismissRequest = { showDistricts = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                            ) {
                                districts.forEach { dist ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = dist,
                                                color = if (dist == district) accent else Color(
                                                    0xFF1A1A1A
                                                )
                                            )
                                        },
                                        onClick = {
                                            district = dist
                                            showDistricts = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Dirección aproximada",
                                color = Color(0xFF1A1A1A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(" *", color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                val sanitized = Validators.sanitizeAddress(it)
                                address = sanitized
                            },
                            placeholder = {
                                Text(
                                    "Av. España 123, cerca al mercado central",
                                    color = Color(0xFF9CA3AF)
                                )
                            },
                            minLines = 3,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 96.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = Color(0xFFF4F4F7),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = accent,
                                focusedTextColor = Color(0xFF1A1A1A),
                                unfocusedTextColor = Color(0xFF1A1A1A)
                            )
                        )
                        Text(
                            "No incluyas tu dirección exacta por seguridad",
                            color = Color(0xFF606060),
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (district.isNotEmpty() && Validators.isValidAddress(address)) {
                                onContinue(district, address)
                            }
                        },
                        enabled = district.isNotEmpty() && Validators.isValidAddress(address),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (district.isNotEmpty() && address.isNotBlank()) accent else Color(
                                0xFFF4F4F7
                            ),
                            contentColor = if (district.isNotEmpty() && address.isNotBlank()) Color.White else Color(
                                0xFF606060
                            )
                        )
                    ) {
                        Text("Continuar", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (district.isNotEmpty() && address.isNotBlank()) Color.White else Color(
                                0xFF606060
                            ),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------
// Utilitario de campo solo lectura
// -----------------------------------------------------------
@Composable
fun ReadonlyField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color(0xFF606060), fontSize = 13.sp)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF4F4F7),
            tonalElevation = 0.dp
        ) {
            Text(
                text = value,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                color = Color(0xFF1A1A1A)
            )
        }
    }
}

// -----------------------------------------------------------
// 4. Paso 3 de 4: Credenciales (alias + contraseña)
// -----------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodegaCredentialsScreen(
    onBack: () -> Unit,
    onContinue: (alias: String, password: String, fechaNacimiento: String) -> Unit,
    authRepository: AuthRepository
) {
    var alias by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var aliasError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    val defaultBirthDate = remember {
        LocalDate.now().minusYears(25).format(DateTimeFormatter.ISO_DATE)
    }
    var isCheckingAlias by remember { mutableStateOf(false) }
    val accent = Color(0xFF10B981)
    val accentDeep = Color(0xFF059669)
    val scope = rememberCoroutineScope()

    fun validateForm(): Boolean {
        var isValid = true

        if (alias.trim().isEmpty()) {
            aliasError = "El alias es obligatorio"
            isValid = false
        } else if (!Validators.isValidAlias(alias.trim())) {
            aliasError = Validators.ErrorMessages.INVALID_ALIAS
            isValid = false
        } else {
            aliasError = null
        }

        if (password.length < 6) {
            passwordError = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        } else {
            passwordError = null
        }

        if (confirmPassword != password) {
            confirmPasswordError = "Las contraseñas no coinciden"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        return isValid
    }

    fun checkAliasExists() {
        if (alias.trim().isNotEmpty()) {
            scope.launch {
                isCheckingAlias = true
                aliasError = null
                val existingUser = authRepository.checkAliasExists(alias.trim())
                isCheckingAlias = false
                if (existingUser) {
                    aliasError = "Este alias ya está en uso"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F7).copy(alpha = 0.5f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(accent, accentDeep)))
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .shadow(12.dp, RoundedCornerShape(24.dp))
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Crear credenciales",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Paso 3 de 4",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Alias",
                                color = Color(0xFF1A1A1A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(" *", color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { 
                                val sanitized = Validators.sanitizeAlias(it)
                                alias = sanitized
                                aliasError = if (sanitized.isNotBlank() && !Validators.isValidAlias(sanitized)) {
                                    Validators.ErrorMessages.INVALID_ALIAS
                                } else null
                                if (sanitized.trim().isNotEmpty()) {
                                    checkAliasExists()
                                }
                            },
                            placeholder = {
                                Text(
                                    "mi_bodega_123",
                                    color = Color(0xFF9CA3AF)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (aliasError != null) Color(0xFFEF4444) else accent,
                                unfocusedBorderColor = if (aliasError != null) Color(0xFFEF4444) else Color(
                                    0xFFF4F4F7
                                ),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = accent,
                                focusedTextColor = Color(0xFF1A1A1A),
                                unfocusedTextColor = Color(0xFF1A1A1A),
                                focusedPlaceholderColor = Color(0xFF9CA3AF),
                                unfocusedPlaceholderColor = Color(0xFF9CA3AF)
                            ),
                            trailingIcon = {
                                if (isCheckingAlias) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = accent,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        )
                        AnimatedVisibility(
                            visible = aliasError != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = aliasError ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Contraseña",
                                color = Color(0xFF1A1A1A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(" *", color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                                if (confirmPassword.isNotEmpty() && confirmPassword != it) {
                                    confirmPasswordError = "Las contraseñas no coinciden"
                                } else {
                                    confirmPasswordError = null
                                }
                            },
                            placeholder = {
                                Text(
                                    "Mínimo 6 caracteres",
                                    color = Color(0xFF9CA3AF)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (passwordError != null) Color(0xFFEF4444) else accent,
                                unfocusedBorderColor = if (passwordError != null) Color(0xFFEF4444) else Color(
                                    0xFFF4F4F7
                                ),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = accent,
                                focusedTextColor = Color(0xFF1A1A1A),
                                unfocusedTextColor = Color(0xFF1A1A1A),
                                focusedPlaceholderColor = Color(0xFF9CA3AF),
                                unfocusedPlaceholderColor = Color(0xFF9CA3AF)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            )
                        )
                        AnimatedVisibility(
                            visible = passwordError != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = passwordError ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Confirmar contraseña",
                                color = Color(0xFF1A1A1A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(" *", color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                confirmPasswordError = null
                                if (it.isNotEmpty() && it != password) {
                                    confirmPasswordError = "Las contraseñas no coinciden"
                                }
                            },
                            placeholder = {
                                Text(
                                    "Repite tu contraseña",
                                    color = Color(0xFF9CA3AF)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (confirmPasswordError != null) Color(
                                    0xFFEF4444
                                ) else accent,
                                unfocusedBorderColor = if (confirmPasswordError != null) Color(
                                    0xFFEF4444
                                ) else Color(0xFFF4F4F7),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = accent,
                                focusedTextColor = Color(0xFF1A1A1A),
                                unfocusedTextColor = Color(0xFF1A1A1A),
                                focusedPlaceholderColor = Color(0xFF9CA3AF),
                                unfocusedPlaceholderColor = Color(0xFF9CA3AF)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )
                        AnimatedVisibility(
                            visible = confirmPasswordError != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = confirmPasswordError ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (validateForm() &&
                                aliasError == null
                            ) {
                                onContinue(alias.trim(), password, defaultBirthDate)
                            }
                        },
                        enabled = alias.trim().isNotEmpty() &&
                                password.length >= 6 &&
                                confirmPassword == password &&
                                aliasError == null &&
                                !isCheckingAlias,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (alias.trim().isNotEmpty() &&
                                password.length >= 6 &&
                                confirmPassword == password &&
                                aliasError == null &&
                                !isCheckingAlias
                            ) accent else Color(0xFFF4F4F7),
                            contentColor = if (alias.trim().isNotEmpty() &&
                                password.length >= 6 &&
                                confirmPassword == password &&
                                aliasError == null &&
                                !isCheckingAlias
                            ) Color.White else Color(0xFF606060)
                        )
                    ) {
                        Text("Continuar", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (alias.trim().isNotEmpty() &&
                                    password.length >= 6 &&
                                    confirmPassword == password &&
                                    aliasError == null &&
                                    !isCheckingAlias
                                ) Color.White else Color(0xFF606060),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

    }
}

// -----------------------------------------------------------
// 5. Pantalla de éxito / final del flujo
// -----------------------------------------------------------
@Composable
fun BodegaRegistrationSuccessScreen(
    onFinish: () -> Unit,
    storeName: String = "Bodega"
) {
    val accent = Color(0xFF10B981)
    val accentDeep = Color(0xFF059669)
    var showContent by remember { mutableStateOf(false) }
    var showTransition by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    if (showTransition) {
        BodegaRegistrationTransitionScreen(
            storeName = storeName,
            onComplete = onFinish
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(accent, accentDeep, accent)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(500)
            ),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val iconAlpha by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0f,
                        animationSpec = tween(500, delayMillis = 200),
                        label = "iconAlpha"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0.8f,
                        animationSpec = tween(500, delayMillis = 200),
                        label = "iconScale"
                    )
                    if (showContent || iconAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .shadow(20.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E7EB))
                                .alpha(iconAlpha)
                                .scale(iconScale),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    val confetti1Alpha by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0f,
                        animationSpec = tween(300, delayMillis = 400),
                        label = "confetti1Alpha"
                    )
                    val confetti1Scale by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0.5f,
                        animationSpec = tween(300, delayMillis = 400),
                        label = "confetti1Scale"
                    )
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        if (showContent || confetti1Alpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFACC15))
                                    .alpha(confetti1Alpha)
                                    .scale(confetti1Scale)
                            )
                        }
                    }

                    val confetti2Alpha by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0f,
                        animationSpec = tween(300, delayMillis = 500),
                        label = "confetti2Alpha"
                    )
                    val confetti2Scale by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0.5f,
                        animationSpec = tween(300, delayMillis = 500),
                        label = "confetti2Scale"
                    )
                    Box(modifier = Modifier.align(Alignment.BottomStart)) {
                        if (showContent || confetti2Alpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF914D))
                                    .alpha(confetti2Alpha)
                                    .scale(confetti2Scale)
                            )
                        }
                    }

                    val confetti3Alpha by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0f,
                        animationSpec = tween(300, delayMillis = 600),
                        label = "confetti3Alpha"
                    )
                    val confetti3Scale by animateFloatAsState(
                        targetValue = if (showContent) 1f else 0.5f,
                        animationSpec = tween(300, delayMillis = 600),
                        label = "confetti3Scale"
                    )
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        if (showContent || confetti3Alpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.4f))
                                    .alpha(confetti3Alpha)
                                    .scale(confetti3Scale)
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "¡Bienvenido, ${storeName.ifBlank { "Bodega" }}!",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Todo listo",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 800)) + slideInVertically(
                        initialOffsetY = { 20 },
                        animationSpec = tween(500, delayMillis = 800)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                        Text(
                            "¡Completado!",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 1000))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Próximos pasos",
                            color = Color(0xFF1A1A1A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val steps = listOf(
                            Icons.Default.Inventory to ("Crea tu primera oferta" to "Publica productos con precios grupales"),
                            Icons.Default.FlashOn to ("Recibe reservas" to "Los clientes reservarán unidades"),
                            Icons.Default.Store to ("Gestiona las ventas" to "Valida QR y entrega productos")
                        )
                        steps.forEachIndexed { index, (icon, info) ->
                            AnimatedVisibility(
                                visible = showContent,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        300,
                                        delayMillis = 700 + (index * 150)
                                    )
                                ) + slideInVertically(
                                    initialOffsetY = { 20 },
                                    animationSpec = tween(
                                        300,
                                        delayMillis = 700 + (index * 150)
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = accent.copy(alpha = 0.1f),
                                        tonalElevation = 0.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.size(44.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            info.first,
                                            color = Color(0xFF1A1A1A),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            info.second,
                                            color = Color(0xFF606060),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            if (index != steps.lastIndex) {
                                AnimatedVisibility(
                                    visible = showContent,
                                    enter = fadeIn(
                                        animationSpec = tween(
                                            200,
                                            delayMillis = 1000 + (index * 150)
                                        )
                                    )
                                ) {
                                    HorizontalDivider(
                                        color = Color(0xFFF0F0F0)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showTransition = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
                ) {
                    Text(
                        "Comenzar",
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------
// 6. Pantalla de transición mientras carga dashboard bodeguero
// -----------------------------------------------------------
@Composable
fun BodegaRegistrationTransitionScreen(
    storeName: String,
    onComplete: () -> Unit
) {
    val accent = Color(0xFF10B981)
    val accentDeep = Color(0xFF059669)
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
        delay(2000)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(accent, accentDeep, accent))),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(500)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = Color.White,
                        strokeWidth = 4.dp
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Preparando tu dashboard...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        storeName.ifBlank { "Bodega" },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
