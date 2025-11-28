package com.nexusbiz.nexusbiz.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusbiz.nexusbiz.util.Validators

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToRegisterBodega: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLogin: (String, String) -> Unit
) {
    var alias by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var aliasError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
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
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "NexusBiz",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111111),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        StyledField(
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
            keyboardType = KeyboardType.Text,
            errorMessage = aliasError,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Alias",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        StyledField(
            label = "Contraseña",
            value = password,
            onValueChange = { password = it },
            placeholder = "••••••••",
            isPassword = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Ver contraseña",
                        tint = Color(0xFF9CA3AF)
                    )
                }
            }
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { 
                if (Validators.isValidAlias(alias)) {
                    aliasError = null
                    onLogin(alias, password)
                } else {
                    aliasError = Validators.ErrorMessages.INVALID_ALIAS
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.6f)
            ),
            enabled = !isLoading && alias.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Iniciar sesión",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { 
                android.util.Log.d("LoginScreen", "Botón 'Crear cuenta' presionado")
                onNavigateToRegister()
            }, 
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Crear cuenta",
                color = Color(0xFF10B981),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        TextButton(onClick = onNavigateToForgotPassword, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
                text = "¿Olvidaste tu contraseña?",
                color = Color(0xFF6B7280),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(color = Color(0xFFE5E7EB), modifier = Modifier.weight(1f))
            Text(
                text = "¿Eres bodeguero?",
                color = Color(0xFF606060),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Divider(color = Color(0xFFE5E7EB), modifier = Modifier.weight(1f))
        }

        OutlinedButton(
            onClick = { 
                android.util.Log.d("LoginScreen", "Botón 'Registrar bodega' presionado")
                onNavigateToRegisterBodega()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp, brush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF10B981)))),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF10B981)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Registra tu bodega", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = buildAnnotatedString {
                append("Al continuar, aceptas los ")
                pushStyle(SpanStyle(color = Color(0xFF10B981)))
                append("Términos")
                pop()
                append(" y la ")
                pushStyle(SpanStyle(color = Color(0xFF10B981)))
                append("Política de privacidad")
                pop()
            },
            fontSize = 12.sp,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StyledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    errorMessage: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF111111),
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color(0xFF9CA3AF)) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = Color(0xFF10B981),
                cursorColor = Color(0xFF10B981),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                errorContainerColor = Color.White,
                focusedTextColor = Color(0xFF1A1A1A),
                unfocusedTextColor = Color(0xFF1A1A1A)
            ),
            isError = errorMessage != null,
            supportingText = errorMessage?.let { 
                { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        )
    }
}
