package com.nexusbiz.nexusbiz.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cambiar Contraseña",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111111)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 0.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF111111))
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Divider(color = Color(0xFFE5E7EB))
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Campo Contraseña Actual
            Text(
                text = "Contraseña Actual",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111111)
            )
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { 
                    oldPassword = it
                    passwordError = null
                },
                placeholder = {
                    Text("Ingresa tu contraseña actual", color = Color(0xFF9CA3AF))
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = Color(0xFF111111),
                    fontSize = 16.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp)),
                visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showOldPassword = !showOldPassword }) {
                        Icon(
                            imageVector = if (showOldPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF3F4F6),
                    unfocusedContainerColor = Color(0xFFF3F4F6),
                    cursorColor = Color(0xFF111111)
                )
            )

            Spacer(Modifier.height(24.dp))

            // Campo Nueva Contraseña
            Text(
                text = "Nueva Contraseña",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111111)
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    passwordError = null
                },
                placeholder = {
                    Text("Mínimo 8 caracteres", color = Color(0xFF9CA3AF))
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = Color(0xFF111111),
                    fontSize = 16.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp)),
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            imageVector = if (showNewPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF3F4F6),
                    unfocusedContainerColor = Color(0xFFF3F4F6),
                    cursorColor = Color(0xFF111111)
                )
            )

            Spacer(Modifier.height(24.dp))

            // Campo Confirmar Nueva Contraseña
            Text(
                text = "Confirmar Nueva Contraseña",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111111)
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    passwordError = null
                },
                placeholder = {
                    Text("Confirma tu nueva contraseña", color = Color(0xFF9CA3AF))
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = Color(0xFF111111),
                    fontSize = 16.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp)),
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF3F4F6),
                    unfocusedContainerColor = Color(0xFFF3F4F6),
                    cursorColor = Color(0xFF111111)
                ),
                isError = passwordError != null,
                supportingText = passwordError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            )

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    // Validaciones
                    when {
                        oldPassword.isEmpty() -> {
                            passwordError = "Ingresa tu contraseña actual"
                        }
                        newPassword.length < 8 -> {
                            passwordError = "La nueva contraseña debe tener al menos 8 caracteres"
                        }
                        newPassword != confirmPassword -> {
                            passwordError = "Las contraseñas no coinciden"
                        }
                        oldPassword == newPassword -> {
                            passwordError = "La nueva contraseña debe ser diferente a la actual"
                        }
                        else -> {
                            passwordError = null
                            onChangePassword(oldPassword, newPassword)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                enabled = !isLoading && oldPassword.isNotEmpty() && newPassword.length >= 8 && confirmPassword.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Cambiar contraseña",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
