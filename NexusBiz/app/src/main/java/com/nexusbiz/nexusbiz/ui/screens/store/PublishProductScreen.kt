package com.nexusbiz.nexusbiz.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.ui.components.CustomTextField
import com.nexusbiz.nexusbiz.util.Validators
import kotlin.random.Random

@Composable
fun PublishProductScreen(
    onPublish: (String, String, String, Double, Double, Int, Int, String, Int) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var normalPrice by remember { mutableStateOf("") }
    var groupPrice by remember { mutableStateOf("") }
    var targetUnits by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("12") }
    var imageUrl by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var normalPriceError by remember { mutableStateOf<String?>(null) }
    var groupPriceError by remember { mutableStateOf<String?>(null) }
    var targetUnitsError by remember { mutableStateOf<String?>(null) }

    val randomImages = listOf(
        "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400",
        "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=400",
        "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400"
    )

    fun validateForm(): Boolean {
        var isValid = true
        
        // Validar nombre
        if (!Validators.isValidName(name)) {
            nameError = Validators.ErrorMessages.INVALID_NAME
            isValid = false
        } else {
            nameError = null
        }
        
        // Validar precio normal
        if (!Validators.isValidPrice(normalPrice)) {
            normalPriceError = Validators.ErrorMessages.INVALID_PRICE
            isValid = false
        } else {
            normalPriceError = null
        }
        
        // Validar precio grupal
        val normal = normalPrice.toDoubleOrNull()
        val group = groupPrice.toDoubleOrNull()
        if (!Validators.isValidPrice(groupPrice)) {
            groupPriceError = Validators.ErrorMessages.INVALID_PRICE
            isValid = false
        } else if (normal != null && group != null && !Validators.isValidGroupPrice(group, normal)) {
            groupPriceError = Validators.ErrorMessages.INVALID_GROUP_PRICE
            isValid = false
        } else {
            groupPriceError = null
        }
        
        // Validar target units
        if (!Validators.isValidQuantity(targetUnits)) {
            targetUnitsError = Validators.ErrorMessages.INVALID_QUANTITY
            isValid = false
        } else {
            targetUnitsError = null
        }
        
        // Validar duración
        val durationHours = duration.toIntOrNull() ?: 0
        if (durationHours <= 0) {
            isValid = false
        }
        
        return isValid
    }

    fun canPublish(): Boolean {
        val normal = normalPrice.toDoubleOrNull()
        val group = groupPrice.toDoubleOrNull()
        val target = targetUnits.toIntOrNull()
        val durationHours = duration.toIntOrNull() ?: 0
        return Validators.isValidName(name) &&
            normal != null && normal > 0 &&
            group != null && group > 0 &&
            Validators.isValidGroupPrice(group, normal) &&
            target != null && target >= 1 &&
            durationHours > 0
    }

    fun publish() {
        if (validateForm()) {
            val normal = normalPrice.toDoubleOrNull() ?: 0.0
            val group = groupPrice.toDoubleOrNull() ?: 0.0
            val target = targetUnits.toIntOrNull() ?: 0
            val durationHours = duration.toIntOrNull() ?: 12
            onPublish(name, imageUrl, "General", normal, group, target, target, imageUrl, durationHours)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F7))
    ) {
        Column {
            // Header - sticky con shadow-sm
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color(0xFF606060),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Publicar oferta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Image uploader - aspect-square con borde discontinuo
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Foto del producto",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable {
                                imageUrl = randomImages[Random.nextInt(randomImages.size)]
                            }
                            .border(
                                BorderStroke(
                                    width = 2.dp,
                                    color = Color(if (imageUrl.isBlank()) 0xFFF4F4F7 else 0xFF10B981)
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isBlank()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    Text(
                                        text = "Subir foto",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Text(
                                        text = "Toca para seleccionar",
                                        fontSize = 12.sp,
                                        color = Color(0xFF606060)
                                    )
                                }
                            }
                        } else {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Foto del producto",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Product Name
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nombre del producto",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    StyledInputField(
                        value = name,
                        onValueChange = { 
                            val sanitized = Validators.sanitizeName(it)
                            name = sanitized
                            nameError = if (sanitized.isNotBlank() && !Validators.isValidName(sanitized)) {
                                Validators.ErrorMessages.INVALID_NAME
                            } else null
                        },
                        placeholder = "Ej: Leche Gloria 1L",
                        modifier = Modifier.fillMaxWidth(),
                        errorMessage = nameError
                    )
                }

                // Prices - Grid de 2 columnas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PriceField(
                        label = "Precio normal",
                        value = normalPrice,
                        onValueChange = { input ->
                            if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                normalPrice = input
                                normalPriceError = if (input.isNotBlank() && !Validators.isValidPrice(input)) {
                                    Validators.ErrorMessages.INVALID_PRICE
                                } else null
                                // Revalidar precio grupal si ambos están llenos
                                if (groupPrice.isNotBlank()) {
                                    val normal = normalPrice.toDoubleOrNull()
                                    val group = groupPrice.toDoubleOrNull()
                                    if (normal != null && group != null && !Validators.isValidGroupPrice(group, normal)) {
                                        groupPriceError = Validators.ErrorMessages.INVALID_GROUP_PRICE
                                    } else {
                                        groupPriceError = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        errorMessage = normalPriceError
                    )
                    PriceField(
                        label = "Precio grupal",
                        value = groupPrice,
                        onValueChange = { input ->
                            if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                groupPrice = input
                                val normal = normalPrice.toDoubleOrNull()
                                val group = input.toDoubleOrNull()
                                if (input.isNotBlank() && !Validators.isValidPrice(input)) {
                                    groupPriceError = Validators.ErrorMessages.INVALID_PRICE
                                } else if (normal != null && group != null && !Validators.isValidGroupPrice(group, normal)) {
                                    groupPriceError = Validators.ErrorMessages.INVALID_GROUP_PRICE
                                } else {
                                    groupPriceError = null
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        errorMessage = groupPriceError
                    )
                }

                // Savings Preview
                if (normalPrice.isNotBlank() && groupPrice.isNotBlank()) {
                    val normal = normalPrice.toDoubleOrNull()
                    val group = groupPrice.toDoubleOrNull()
                    if (normal != null && group != null && normal > group) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Ahorro por unidad: S/ ${(normal - group).coerceAtLeast(0.0).format(2)}",
                                fontSize = 14.sp,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // Target Units
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Meta de unidades a vender",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    StyledInputField(
                        value = targetUnits,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                targetUnits = input
                                targetUnitsError = if (input.isNotBlank() && !Validators.isValidQuantity(input)) {
                                    Validators.ErrorMessages.INVALID_QUANTITY
                                } else null
                            }
                        },
                        placeholder = "Ej: 20",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth(),
                        errorMessage = targetUnitsError
                    )
                    Text(
                        text = "Cantidad de unidades que necesitas vender para activar el precio grupal",
                        fontSize = 12.sp,
                        color = Color(0xFF606060)
                    )
                }

                // Duration Selector - Grid de 4 columnas
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Duración de la oferta",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    val durations = listOf("4", "8", "12", "24")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        durations.forEach { value ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { duration = value },
                                color = if (duration == value) Color(0xFFFACC15) else Color.White,
                                border = BorderStroke(
                                    2.dp,
                                    if (duration == value) Color(0xFFFACC15) else Color(0xFFF4F4F7)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${value}h",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (duration == value) Color.White else Color(0xFF606060),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Address (readonly) - Fondo gris con borde
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dirección de retiro",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        color = Color(0xFFF4F4F7),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, Color(0xFFF4F4F7))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF606060),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Av. Principal 123, San Isidro",
                                fontSize = 14.sp,
                                color = Color(0xFF606060)
                            )
                        }
                    }
                    Text(
                        text = "Esta dirección se llenó desde tu perfil",
                        fontSize = 12.sp,
                        color = Color(0xFF606060)
                    )
                }

                Spacer(modifier = Modifier.height(128.dp))
            }
        }

        // Fixed Bottom Button - con borde superior
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, Color(0xFFF4F4F7))
        ) {
            Button(
                onClick = { publish() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.6f)
                ),
                enabled = !isLoading && canPublish()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Publicar oferta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StyledInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                BorderStroke(2.dp, if (isFocused) Color(0xFF10B981) else Color(0xFFF4F4F7)),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color(0xFF9CA3AF)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            interactionSource = interactionSource,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color(0xFF10B981),
                errorContainerColor = Color.Transparent
            ),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFF1A1A1A)
            ),
            isError = errorMessage != null,
            supportingText = errorMessage?.let { 
                { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        )
    }
}

@Composable
private fun PriceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A1A)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(
                    BorderStroke(2.dp, if (isFocused) Color(0xFF10B981) else Color(0xFFF4F4F7)),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "S/", fontSize = 14.sp, color = Color(0xFF606060))
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    placeholder = { Text("0.00", fontSize = 14.sp, color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    interactionSource = interactionSource,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Color(0xFF10B981),
                        errorContainerColor = Color.Transparent
                    ),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF1A1A1A)
                    ),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { 
                        { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    }
                )
            }
        }
    }
}

private fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}