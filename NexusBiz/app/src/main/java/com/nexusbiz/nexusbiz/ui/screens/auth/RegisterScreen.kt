package com.nexusbiz.nexusbiz.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusbiz.nexusbiz.ui.components.SeleccionarDistritoModal
import com.nexusbiz.nexusbiz.util.Validators
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onCreateAccount: (String, String, String, String) -> Unit,
    onBack: () -> Unit,
) {
    var alias by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf<String?>(null) }
    var distrito by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var aliasError by remember { mutableStateOf<String?>(null) }
    var fechaError by remember { mutableStateOf<String?>(null) }
    var distritoError by remember { mutableStateOf<String?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showDistritoModal by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    
    // DatePicker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = null
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 0.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF111111))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Crear cuenta",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF111111),
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Únete a NexusBiz y empieza a ahorrar",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )

            Spacer(Modifier.height(32.dp))

            // Campo Alias
            StyledRegisterField(
                label = "Alias",
                value = alias,
                onValueChange = { 
                    val sanitized = Validators.sanitizeAlias(it)
                    alias = sanitized
                    aliasError = if (sanitized.isNotBlank() && !Validators.isValidAlias(sanitized)) {
                        Validators.ErrorMessages.INVALID_ALIAS
                    } else null
                },
                placeholder = "Ej: Carlos",
                errorMessage = aliasError,
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

            // Campo Fecha de Nacimiento
            Text(
                text = "Fecha de Nacimiento",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111111)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF3F4F6)),
                color = Color(0xFFF3F4F6),
                onClick = { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = fechaNacimiento?.let { 
                            try {
                                val date = LocalDate.parse(it, DateTimeFormatter.ISO_DATE)
                                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            } catch (e: Exception) {
                                it
                            }
                        } ?: "DD/MM/YYYY",
                        color = if (fechaNacimiento != null) Color(0xFF1A1A1A) else Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            }
            fechaError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Campo Distrito
            Text(
                text = "Distrito",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111111)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF3F4F6)),
                color = Color(0xFFF3F4F6),
                onClick = { showDistritoModal = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = distrito.ifEmpty { "Selecciona tu distrito" },
                        color = if (distrito.isNotEmpty()) Color(0xFF1A1A1A) else Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            }
            distritoError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Campo Contraseña
            StyledRegisterField(
                label = "Contraseña",
                value = password,
                onValueChange = { password = it },
                placeholder = "Mínimo 6 caracteres",
                isPassword = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Ver contraseña",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            TermsBox()

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { 
                    var hasError = false
                    
                    // Validar alias
                    if (!Validators.isValidAlias(alias)) {
                        aliasError = Validators.ErrorMessages.INVALID_ALIAS
                        hasError = true
                    }
                    
                    // Validar fecha de nacimiento
                    if (fechaNacimiento == null) {
                        fechaError = "Debes seleccionar tu fecha de nacimiento"
                        hasError = true
                    } else {
                        // Validar edad mínima 18 años
                        val formatter = DateTimeFormatter.ISO_DATE
                        val birthDate = LocalDate.parse(fechaNacimiento, formatter)
                        val age = java.time.Period.between(birthDate, LocalDate.now()).years
                        if (age < 18) {
                            fechaError = "Debes ser mayor de 18 años"
                            hasError = true
                        } else {
                            fechaError = null
                        }
                    }
                    
                    // Validar distrito
                    if (distrito.isEmpty()) {
                        distritoError = "Debes seleccionar un distrito"
                        hasError = true
                    }
                    
                    // Validar contraseña
                    if (password.length < 6) {
                        hasError = true
                    }
                    
                    if (!hasError && fechaNacimiento != null) {
                        onCreateAccount(alias, password, fechaNacimiento!!, distrito)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                enabled = alias.isNotBlank() && fechaNacimiento != null && distrito.isNotEmpty() && password.length >= 6
            ) {
                Text("Crear cuenta")
            }
        }
    }
    
    // DatePicker Dialog
    if (showDatePicker) {
        CustomDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { dateMillis ->
                dateMillis?.let {
                    val date = java.time.Instant.ofEpochMilli(it)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    fechaNacimiento = date.format(DateTimeFormatter.ISO_DATE)
                    fechaError = null
                    showDatePicker = false
                }
            },
            datePickerState = datePickerState
        )
    }
    
    
    // Distrito Modal
    if (showDistritoModal) {
        SeleccionarDistritoModal(
            distritoActual = distrito,
            onDismiss = { showDistritoModal = false },
            onConfirmar = { selectedDistrito ->
                distrito = selectedDistrito
                distritoError = null
                showDistritoModal = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Long?) -> Unit,
    datePickerState: DatePickerState
) {
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
            }) {
                Text("Confirmar", color = Color(0xFF10B981))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", color = Color(0xFF6B7280))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun TermsBox() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Al continuar, aceptas los ",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
            Row {
                Text(
                    text = "Términos de servicio",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "y",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Política de privacidad",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
private fun StyledRegisterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111111)
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            placeholder = { Text(placeholder, color = Color(0xFF9CA3AF)) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
            ),
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedContainerColor = Color(0xFFF3F4F6),
                unfocusedContainerColor = Color(0xFFF3F4F6),
                disabledContainerColor = Color(0xFFF3F4F6),
                errorContainerColor = Color(0xFFF3F4F6)
            ),
            isError = errorMessage != null,
            supportingText = errorMessage?.let { 
                { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        )
    }
}
