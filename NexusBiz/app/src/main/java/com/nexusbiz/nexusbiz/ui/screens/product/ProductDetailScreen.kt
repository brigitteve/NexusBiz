package com.nexusbiz.nexusbiz.ui.screens.product

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.data.model.Group
import com.nexusbiz.nexusbiz.data.model.Offer
import com.nexusbiz.nexusbiz.data.model.Product
import com.nexusbiz.nexusbiz.data.model.User
import com.nexusbiz.nexusbiz.data.model.GamificationLevel
import com.nexusbiz.nexusbiz.ui.components.TimerDisplay
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product?,
    group: Group? = null, // @Deprecated - mantener temporalmente
    offer: Offer? = null,
    user: User?,
    timeRemaining: Long = 0, // Mantener para compatibilidad
    onJoinGroup: (Int) -> Unit = {}, // @Deprecated
    onCreateReservation: (Int) -> Unit = {},
    onCreateGroup: (Int) -> Unit = {}, // @Deprecated
    onShareGroup: () -> Unit = {},
    onViewStores: () -> Unit,
    onBack: () -> Unit
) {
    // Usar el tiempo de la oferta si está disponible, sino del grupo (deprecated)
    var currentTimeRemaining by remember(offer?.id) { 
        mutableStateOf(offer?.timeRemaining ?: group?.timeRemaining ?: timeRemaining) 
    }
    
    // Actualizar el tiempo periódicamente basándose en expiresAt de la oferta o grupo
    // Incluir offer?.id para que se recalcule cuando la oferta cambie en tiempo real
    LaunchedEffect(offer?.id, offer?.expiresAt, group?.expiresAt) {
        val expiresAt = offer?.expiresAt?.let { 
            try {
                java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
            } catch (e: Exception) {
                null
            }
        } ?: group?.expiresAt ?: 0L
        
        if (expiresAt > 0) {
            currentTimeRemaining = maxOf(0, expiresAt - System.currentTimeMillis())
        } else {
            currentTimeRemaining = timeRemaining
        }
        
        while (true) {
            // Recalcular expiresAt en cada iteración para capturar cambios en tiempo real
            val currentOffer = offer
            val expiresAtValue = currentOffer?.expiresAt?.let { 
                try {
                    java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    null
                }
            } ?: group?.expiresAt ?: 0L
            if (expiresAtValue > 0) {
                val newTime = maxOf(0, expiresAtValue - System.currentTimeMillis())
                currentTimeRemaining = newTime
            } else {
                currentTimeRemaining = timeRemaining
            }
            kotlinx.coroutines.delay(1000)
        }
    }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    val accentColor = Color(0xFF10B981)
    val backgroundColor = Color(0xFFF4F4F7)
    val mutedTextColor = Color(0xFF606060)
    val yellowColor = Color(0xFFFACC15)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Mostrar indicador de carga mientras se busca el producto
    if (product == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = accentColor
                )
                Text(
                    text = "Cargando producto...",
                    color = mutedTextColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        return
    }

    var quantity by rememberSaveable { mutableStateOf(1) }
    // Usar tier del usuario o calcularlo desde puntos
    val userTier = remember(user) { 
        user?.tier ?: user?.calculateTier() ?: com.nexusbiz.nexusbiz.data.model.UserTier.BRONZE
    }
    val maxQuantity = remember(userTier) { 
        when (userTier) {
            com.nexusbiz.nexusbiz.data.model.UserTier.BRONZE -> 2
            com.nexusbiz.nexusbiz.data.model.UserTier.SILVER -> 4
            com.nexusbiz.nexusbiz.data.model.UserTier.GOLD -> 6
        }
    }

    LaunchedEffect(maxQuantity) {
        if (quantity > maxQuantity) quantity = maxQuantity
    }

    // Usar datos de la oferta si está disponible, sino del grupo (deprecated)
    val currentUnits = offer?.reservedUnits ?: group?.currentSize ?: 0
    val targetUnits = (offer?.targetUnits ?: group?.targetSize ?: product.minGroupSize).coerceAtLeast(1)
    val progress = offer?.progress ?: (if (targetUnits > 0) (currentUnits.toFloat() / targetUnits).coerceIn(0f, 1f) else 0f)
    val previewProgress = if (targetUnits > 0) ((currentUnits + quantity).toFloat() / targetUnits).coerceIn(0f, 1f) else 0f
    val unitsNeeded = offer?.unitsNeeded ?: (targetUnits - currentUnits).coerceAtLeast(0)
    val totalPrice = product.groupPrice * quantity
    val shareLink = "https://nexusbiz.app/oferta/${product.id}"
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showShareSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF1A1A1A)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = Color(0xFF1A1A1A)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { 
                        if (offer != null) {
                            onCreateReservation(quantity)
                        } else {
                            onJoinGroup(quantity) // @Deprecated
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Reservar $quantity ${if (quantity == 1) "unidad" else "unidades"}",
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color.White
                    )
                }
                if (offer != null) {
                    OutlinedButton(
                        onClick = { showShareSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, backgroundColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                    ) {
                        Text("Compartir oferta")
                    }
                }
                TextButton(onClick = onViewStores, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "¿Lo necesitas urgente? Ver bodegas con stock ahora →",
                        color = accentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = product.imageUrl?.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/400",
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = "Bodega",
                        tint = mutedTextColor
                    )
                    Text(
                        text = product.storeName.ifEmpty { "Bodega" },
                        color = mutedTextColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = currencyFormat.format(product.groupPrice),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Text(
                                text = currencyFormat.format(product.normalPrice),
                                style = MaterialTheme.typography.titleMedium,
                                color = mutedTextColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "Precio grupal por unidad",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "¿Cuántas unidades deseas reservar?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (userTier) {
                                        com.nexusbiz.nexusbiz.data.model.UserTier.BRONZE -> Color(0xFFCD7F32).copy(alpha = 0.15f)
                                        com.nexusbiz.nexusbiz.data.model.UserTier.SILVER -> Color(0xFFC0C0C0).copy(alpha = 0.15f)
                                        com.nexusbiz.nexusbiz.data.model.UserTier.GOLD -> Color(0xFFFACC15).copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = when (userTier) {
                                            com.nexusbiz.nexusbiz.data.model.UserTier.BRONZE -> "Bronce (máx. $maxQuantity)"
                                            com.nexusbiz.nexusbiz.data.model.UserTier.SILVER -> "Plata (máx. $maxQuantity)"
                                            com.nexusbiz.nexusbiz.data.model.UserTier.GOLD -> "Oro (máx. $maxQuantity)"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (userTier) {
                                            com.nexusbiz.nexusbiz.data.model.UserTier.BRONZE -> Color(0xFF8B4513)
                                            com.nexusbiz.nexusbiz.data.model.UserTier.SILVER -> Color(0xFF696969)
                                            com.nexusbiz.nexusbiz.data.model.UserTier.GOLD -> Color(0xFFB8860B)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1,
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(2.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = Color(0xFF1A1A1A),
                                    disabledContentColor = Color(0xFFBDBDBD)
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Restar")
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$quantity",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                                Text(
                                    text = if (quantity == 1) "unidad" else "unidades",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mutedTextColor
                                )
                            }

                            IconButton(
                                onClick = { if (quantity < maxQuantity) quantity++ },
                                enabled = quantity < maxQuantity,
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(accentColor, RoundedCornerShape(12.dp)),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Sumar")
                            }
                        }

                        Text(
                            text = "Nivel ${when (userTier) {
                                com.nexusbiz.nexusbiz.data.model.UserTier.BRONZE -> "Bronce"
                                com.nexusbiz.nexusbiz.data.model.UserTier.SILVER -> "Plata"
                                com.nexusbiz.nexusbiz.data.model.UserTier.GOLD -> "Oro"
                            }}: máx. $maxQuantity unidades por oferta",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = backgroundColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Con tu reserva:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mutedTextColor
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${currentUnits} → ${currentUnits + quantity}",
                                        color = accentColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "/ $targetUnits unidades",
                                        color = mutedTextColor,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = previewProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = accentColor,
                                    trackColor = Color(0xFFD9D9D9)
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(yellowColor)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Faltan $unitsNeeded unidades",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF1A1A1A)
                                    )
                                }
                            }
                            TimerDisplay(
                                timeRemaining = currentTimeRemaining,
                                modifier = Modifier,
                                onExpired = {}
                            )
                        }

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = accentColor,
                            trackColor = Color(0xFFE5E7EB)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = mutedTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$currentUnits de $targetUnits unidades reservadas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = mutedTextColor
                            )
                        }

                        Divider(color = backgroundColor)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total a pagar:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = mutedTextColor
                            )
                            Text(
                                text = currencyFormat.format(totalPrice),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            if (showShareSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showShareSheet = false },
                    sheetState = shareSheetState,
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Comparte esta oferta",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    text = "Tus amigos podrán reservar con este enlace",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mutedTextColor
                                )
                            }
                            IconButton(onClick = { showShareSheet = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = mutedTextColor
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AsyncImage(
                                        model = product.imageUrl?.takeIf { it.isNotBlank() } ?: "https://via.placeholder.com/200",
                                        contentDescription = product.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = currencyFormat.format(product.groupPrice),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                    Text(
                                        text = currencyFormat.format(product.normalPrice),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = mutedTextColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = "Reservar cupo en NexusBiz",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = mutedTextColor
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val message = "Reserva ${product.name} en grupo y ahorra ${currencyFormat.format(product.normalPrice - product.groupPrice)} por unidad. Únete aquí: $shareLink"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message)
                                    setPackage("com.whatsapp")
                                }
                                try {
                                    context.startActivity(sendIntent)
                                } catch (_: Exception) {
                                    val fallback = Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, message)
                                        },
                                        "Compartir oferta"
                                    )
                                    context.startActivity(fallback)
                                }
                                onShareGroup()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Compartir por WhatsApp", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(shareLink))
                                Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, Color(0xFFE5E7EB)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A1A1A))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                                tint = Color(0xFF1A1A1A)
                            )
                            Text("Copiar enlace", fontWeight = FontWeight.SemiBold)
                        }

                        Text(
                            text = "Ganas +2 puntos por compartir",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

