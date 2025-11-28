package com.nexusbiz.nexusbiz.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.data.model.Group
import com.nexusbiz.nexusbiz.data.model.GroupStatus
import com.nexusbiz.nexusbiz.ui.components.ProgressBar
import com.nexusbiz.nexusbiz.ui.components.QRCodeDisplay
import com.nexusbiz.nexusbiz.ui.components.TimerDisplay
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group?,
    currentUserId: String,
    isOwner: Boolean = false,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onShowQR: () -> Unit,
    onValidateQR: (() -> Unit)? = null
) {
    var showShareDialog by remember { mutableStateOf(false) }
    var showQRDialog by remember { mutableStateOf(false) }
    
    if (group == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Grupo no encontrado")
        }
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Grupo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (group.status == GroupStatus.ACTIVE || group.status == GroupStatus.COMPLETED) {
                        IconButton(onClick = { showShareDialog = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Imagen del producto
            AsyncImage(
                model = group.productImage.ifEmpty { "https://via.placeholder.com/400" },
                contentDescription = group.productName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nombre del producto
                Text(
                    text = group.productName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Estado del grupo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (group.status) {
                            GroupStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                            GroupStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                            GroupStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
                            GroupStatus.PICKUP -> MaterialTheme.colorScheme.secondaryContainer
                            GroupStatus.VALIDATED -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ) {
                        Text(
                            text = when (group.status) {
                                GroupStatus.ACTIVE -> "Activo"
                                GroupStatus.COMPLETED -> "Completado"
                                GroupStatus.EXPIRED -> "Expirado"
                                GroupStatus.PICKUP -> "En retiro"
                                GroupStatus.VALIDATED -> "Validado"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (group.status == GroupStatus.ACTIVE && !group.isExpired) {
                        TimerDisplay(
                            timeRemaining = group.timeRemaining,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // Progreso
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${group.reservedUnits}/${group.targetSize} miembros",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(group.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        ProgressBar(
                            progress = group.progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Meta: ${group.targetSize} miembros",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Participantes
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Participantes (${group.participantCount})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ParticipantStat(label = "Participantes", value = group.participantCount)
                            ParticipantStat(label = "Validados", value = group.validatedCount)
                            ParticipantStat(label = "Por retirar", value = group.pendingValidationCount)
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            group.participants.forEach { participant ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AsyncImage(
                                        model = participant.avatar.ifEmpty { "https://via.placeholder.com/50" },
                                        contentDescription = participant.alias,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Text(
                                        text = participant.alias,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Información de la bodega
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bodega",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = group.storeName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Botones según el estado
                when {
                    group.status == GroupStatus.COMPLETED || group.status == GroupStatus.PICKUP -> {
                        Button(
                            onClick = { showQRDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver código QR para retiro")
                        }
                    }
                    
                    group.status == GroupStatus.VALIDATED -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Grupo validado. Puedes retirar tu producto.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    group.status == GroupStatus.EXPIRED -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Grupo Expirado",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Este grupo ha expirado. Puedes buscar bodegas con stock disponible.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Modal compartir
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Compartir Grupo") },
            text = {
                Column {
                    Text("Comparte este grupo con tus amigos:")
                    Spacer(modifier = Modifier.height(16.dp))
                    // TODO: Agregar botones de compartir (WhatsApp, Copiar enlace, etc.)
                    OutlinedTextField(
                        value = "https://nexusbiz.app/grupo/${group.id}",
                        onValueChange = {},
                        label = { Text("Enlace del grupo") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                // TODO: Copiar al portapapeles
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
    
    // Modal QR
    if (showQRDialog) {
        AlertDialog(
            onDismissRequest = { showQRDialog = false },
            title = { Text("Código QR de Retiro") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QRCodeDisplay(
                        data = group.qrCode.ifEmpty { group.id },
                        size = 200
                    )
                    Text(
                        text = "Muestra este código al bodeguero para validar tu retiro",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQRDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun ParticipantStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
