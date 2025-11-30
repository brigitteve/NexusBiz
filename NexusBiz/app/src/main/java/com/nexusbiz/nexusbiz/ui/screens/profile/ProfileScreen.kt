package com.nexusbiz.nexusbiz.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexusbiz.nexusbiz.data.model.User
import com.nexusbiz.nexusbiz.ui.components.BottomNavBar
import com.nexusbiz.nexusbiz.ui.screens.store.ModeSwitchTarget
import com.nexusbiz.nexusbiz.ui.screens.store.ModeSwitchingScreen
import com.nexusbiz.nexusbiz.util.Screen
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onChangeToStoreMode: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToMyGroups: () -> Unit,
    onNavigateToLogin: (() -> Unit)? = null
) {
    // Si el usuario es null, navegar a Login automáticamente (solo si se proporciona el callback)
    LaunchedEffect(user, onNavigateToLogin) {
        if (user == null && onNavigateToLogin != null) {
            // Pequeño delay para evitar navegación durante la transición
            kotlinx.coroutines.delay(100)
            onNavigateToLogin()
        }
    }
    
    if (user == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F4F7)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF10B981))
        }
        return
    }

    val trimmedAlias = if (user.alias.length >= 4) user.alias.takeLast(4) else user.alias
    val gamification: GamificationStats = remember(user) {
        buildGamification(user.points, user.totalSavings, user.completedGroups, user.streak)
    }
    val progress = (gamification.points / gamification.nextLevelPoints.toFloat()).coerceIn(0f, 1f)
    val scrollState = rememberScrollState()
    val headerColors = remember(gamification.level) { gradientForLevel(gamification.level) }

    var showTransition by remember { mutableStateOf(false) }

    if (showTransition) {
        ModeSwitchingScreen(
            targetMode = ModeSwitchTarget.BODEGUERO,
            onFinish = {
                showTransition = false
                onChangeToStoreMode()
            }
        )
        return
    }

    Scaffold(
        containerColor = Color(0xFFF4F4F7),
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.Profile.route,
                onItemClick = { route ->
                    when (route) {
                        Screen.Home.route -> onNavigateToHome()
                        Screen.MyGroups.route -> onNavigateToMyGroups()
                        Screen.Profile.route -> Unit
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F4F7))
                .verticalScroll(scrollState)
                .padding(paddingValues)
        ) {
            val headerGradient = Brush.verticalGradient(headerColors)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 14.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "Mi perfil",
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = user.avatar?.ifEmpty { "https://via.placeholder.com/120" } ?: "https://via.placeholder.com/120",
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = user.alias.ifEmpty { "Usuario" },
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Nivel",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Nivel ${gamification.level} • ${user.points} pts",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-4).dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Puntos acumulados",
                        fontSize = 13.sp,
                        color = Color(0xFF7C7C7C)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = gamification.points.toString(),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE6FFF0)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Puntos",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${gamification.points}/${gamification.nextLevelPoints}",
                                fontSize = 12.sp,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                text = "Nivel siguiente",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        LinearProgressIndicator(
                            progress = progress,
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFFF4F4F7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    icon = Icons.Default.TrendingUp,
                    label = "Ahorrado",
                    value = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
                        .format(gamification.totalSavings),
                    iconTint = Color(0xFF10B981),
                    background = Color(0xFFE6FFF0),
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    icon = Icons.Default.Store,
                    label = "Grupos",
                    value = gamification.completedGroups.toString(),
                    iconTint = Color(0xFF0FA16E),
                    background = Color(0xFFECFDF3),
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    icon = Icons.Default.Whatshot,
                    label = "Racha",
                    value = gamification.weekStreak.toString(),
                    iconTint = Color(0xFFF59E0B),
                    background = Color(0xFFFDE68A),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cómo ganar puntos",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1A1A1A)
                    )
                    PointsRulesSection()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileActionItem(
                    icon = Icons.Default.Edit,
                    label = "Editar perfil",
                    onClick = onEditProfile
                )
                ProfileActionItem(
                    icon = Icons.Default.Lock,
                    label = "Cambiar contraseña",
                    onClick = onChangePassword
                )
                // Botón "Cambiar a modo bodeguero" oculto según requerimiento
                ProfileActionItem(
                    icon = Icons.Default.Description,
                    label = "Términos y privacidad",
                    onClick = onTermsAndPrivacy
                )
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private data class GamificationStats(
    val level: String,
    val points: Int,
    val nextLevelPoints: Int,
    val totalSavings: Double,
    val completedGroups: Int,
    val weekStreak: Int
)


@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun StatsCard(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = background
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(8.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color(0xFF606060)
                )
            }
        }
    }
}


@Composable
private fun PointsRulesSection() {
    val pointsRules = listOf(
        "Unirse al grupo" to "+5",
        "Grupo completado" to "+20",
        "QR validado" to "+15",
        "Compartir grupo" to "+5",
        "Abrir app diario" to "+1"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pointsRules.forEach { rule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F4F7), RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rule.first,
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A1A)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981)
                ) {
                    Text(
                        text = rule.second,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF606060),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir a $label",
                tint = Color(0xFF606060),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun GradientActionItem(
    label: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(colors),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ir a $label",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun buildGamification(
    points: Int,
    savings: Double,
    groups: Int,
    streak: Int
): GamificationStats {
    // Usar el nuevo sistema de niveles: Bronce (0-99), Plata (100-199), Oro (200+)
    val level = when {
        points >= 200 -> "Oro"
        points >= 100 -> "Plata"
        else -> "Bronce"
    }
    val nextLevel = when (level) {
        "Bronce" -> 100
        "Plata" -> 200
        "Oro" -> Int.MAX_VALUE // No hay siguiente nivel después de Oro
        else -> 100
    }
    return GamificationStats(
        level = level,
        points = points,
        nextLevelPoints = nextLevel,
        totalSavings = savings,
        completedGroups = groups,
        weekStreak = streak
    )
}

private fun gradientForLevel(level: String): List<Color> {
    return when (level) {
        "Bronce" -> listOf(Color(0xFFCD7F32), Color(0xFF8B4513))
        "Plata" -> listOf(Color(0xFFC0C0C0), Color(0xFF808080))
        "Oro" -> listOf(Color(0xFFFACC15), Color(0xFFB8860B))
        else -> listOf(Color(0xFFCD7F32), Color(0xFF8B4513)) // Bronce por defecto
    }
}

