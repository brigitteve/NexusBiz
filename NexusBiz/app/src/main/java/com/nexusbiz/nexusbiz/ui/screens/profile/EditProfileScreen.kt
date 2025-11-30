package com.nexusbiz.nexusbiz.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusbiz.nexusbiz.data.model.User
import com.nexusbiz.nexusbiz.util.Validators
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.util.ImageUriHelper
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun EditProfileScreen(
    user: User?,
    onSave: (String, String?) -> Unit, // Cambiar para incluir avatar URL
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onNavigateToLogin: (() -> Unit)? = null
) {
    // Si el usuario es null, navegar a Login automáticamente (solo si se proporciona el callback)
    LaunchedEffect(user, onNavigateToLogin) {
        if (user == null && onNavigateToLogin != null) {
            // Pequeño delay para evitar navegación durante la transición
            kotlinx.coroutines.delay(100)
            try {
                onNavigateToLogin()
            } catch (e: Exception) {
                android.util.Log.e("EditProfileScreen", "Error al navegar a Login: ${e.message}", e)
            }
        }
    }
    
    if (user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF10B981))
        }
        return
    }

    var name by rememberSaveable { mutableStateOf(user.alias) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val accent = Color(0xFF10B981)
    val background = Color(0xFFF4F4F7)
    val muted = Color(0xFF606060)
    
    // Inicializar avatarUri con la URL del usuario si existe
    LaunchedEffect(user?.avatar) {
        user?.avatar?.takeIf { it.isNotBlank() }?.let { url ->
            avatarUri = Uri.parse(url)
        }
    }
    
    // Variable para almacenar el archivo temporal de la cámara
    var cameraPhotoFile by remember { mutableStateOf<File?>(null) }

    // Launcher para galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUri = it
        }
    }

    // Launcher para cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoFile != null) {
            val photoUri = ImageUriHelper.getUriForFile(context, cameraPhotoFile!!)
            photoUri?.let { avatarUri = it }
        }
    }

    fun validateAndSave() {
        localError = null
        
        val sanitizedAlias = Validators.sanitizeAlias(name)
        if (!Validators.isValidAlias(sanitizedAlias)) {
            localError = Validators.ErrorMessages.INVALID_ALIAS
            return
        }
        
        // Si hay una nueva imagen local, subirla primero
        val currentAvatarUri = avatarUri
        if (currentAvatarUri != null && 
            (currentAvatarUri.scheme == "content" || currentAvatarUri.scheme == "file")) {
            // Es una imagen local nueva, necesitamos subirla
            isUploadingAvatar = true
            scope.launch {
                try {
                    val avatarUrl = com.nexusbiz.nexusbiz.data.remote.SupabaseStorage.uploadPublicImage(
                        context = context,
                        imageUri = currentAvatarUri,
                        pathBuilder = { extension ->
                            "avatars/${user?.id ?: "unknown"}.$extension"
                        },
                        bucketPriority = listOf("avatars", "public", "product-images")
                    )
                    
                    isUploadingAvatar = false
                    if (avatarUrl != null) {
                        onSave(sanitizedAlias, avatarUrl)
                    } else {
                        localError = "Error al subir la imagen. Intenta nuevamente."
                    }
                } catch (e: Exception) {
                    isUploadingAvatar = false
                    localError = "Error al subir la imagen: ${e.message}"
                }
            }
        } else {
            // Es una URL remota o null, guardar directamente
            onSave(sanitizedAlias, currentAvatarUri?.toString())
        }
    }

    Scaffold(
        containerColor = background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = muted
                    )
                }
                Text(
                    text = "Editar perfil",
                    fontSize = 18.sp,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Avatar(
                accent = accent,
                avatarUri = avatarUri,
                onClick = { showImagePickerDialog = true },
                isLoading = isUploadingAvatar
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LabeledField(
                    label = "Alias",
                    value = name,
                    onValueChange = { 
                        val sanitized = Validators.sanitizeAlias(it)
                        name = sanitized
                    },
                    placeholder = "Tu alias",
                    leadingIcon = Icons.Default.Person,
                    accent = accent,
                    muted = muted
                )

                InfoCard(
                    text = "Tu alias se usa para iniciar sesión.",
                    accent = accent
                )

                (localError ?: errorMessage)?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0xFFB91C1C),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { validateAndSave() },
                    enabled = !isLoading && !isUploadingAvatar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isUploadingAvatar -> "Subiendo imagen..."
                            isLoading -> "Guardando..."
                            else -> "Guardar cambios"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Dialog para seleccionar fuente de imagen
    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Seleccionar foto de perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Elige de dónde quieres tomar la foto")
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showImagePickerDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Text("Galería")
                        }
                    }
                    TextButton(
                        onClick = {
                            showImagePickerDialog = false
                            try {
                                val photoFile = ImageUriHelper.createTempImageFile(context, "avatar_photo")
                                if (photoFile != null) {
                                    val photoUri = ImageUriHelper.getUriForFile(context, photoFile)
                                    if (photoUri != null) {
                                        cameraPhotoFile = photoFile
                                        cameraLauncher.launch(photoUri)
                                    } else {
                                        galleryLauncher.launch("image/*")
                                    }
                                } else {
                                    galleryLauncher.launch("image/*")
                                }
                            } catch (e: Exception) {
                                galleryLauncher.launch("image/*")
                            }
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Text("Cámara")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImagePickerDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun Avatar(
    accent: Color,
    avatarUri: Uri?,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    if (avatarUri != null) {
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(accent, Color(0xFF059669))
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White
                    )
                }
                avatarUri != null -> {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Avatar del usuario",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Cambiar foto",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    accent: Color,
    muted: Color,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF1A1A1A)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = muted
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = Color(0xFFF4F4F7),
                cursorColor = accent,
                focusedTextColor = Color(0xFF1A1A1A),
                unfocusedTextColor = Color(0xFF1A1A1A)
            ),
            keyboardOptions = keyboardOptions
        )
    }
}

@Composable
private fun InfoCard(text: String, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Start
        )
    }
}
