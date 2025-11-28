package com.nexusbiz.nexusbiz.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TermsAndPrivacyScreen(
    onBack: () -> Unit
) {
    val tabs = listOf(
        TermsTab("terms", "Términos de uso"),
        TermsTab("privacy", "Privacidad")
    )
    var selectedTab by rememberSaveable { mutableStateOf(tabs.first().key) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF4F4F7)
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF4F4F7)),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color(0xFF606060)
                        )
                    }
                    Text(
                        text = "Términos y privacidad",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                val selectedIndex = tabs.indexOfFirst { it.key == selectedTab }.coerceAtLeast(0)
                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = Color.White,
                    contentColor = Color(0xFF10B981),
                    indicator = { },
                    divider = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF0F0F2), RoundedCornerShape(16.dp))
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { selectedTab = tab.key },
                            selectedContentColor = Color.White,
                            unselectedContentColor = Color(0xFF6B7280)
                        ) {
                            Text(
                                text = tab.label,
                                color = if (selectedIndex == index) Color.White else Color(0xFF6B7280),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (selectedIndex == index) Color(0xFF10B981) else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    "terms" -> TermsContent()
                    else -> PrivacyContent()
                }
            }
        }
    }
}

@Composable
private fun TermsContent() {
    InfoCard(
        icon = Icons.Default.Description,
        title = "Términos de Servicio",
        subtitle = "Última actualización: Noviembre 2024",
        content = {
            SectionTitle("1. Aceptación de los términos")
            SectionParagraph("Al acceder y utilizar NexusBiz, aceptas estar sujeto a estos Términos de Servicio y todas las leyes y regulaciones aplicables.")

            SectionTitle("2. Uso del servicio")
            SectionParagraph("NexusBiz conecta consumidores con bodegas locales para ofertas grupales. Los usuarios pueden:")
            BulletList(
                listOf(
                    "Reservar cantidades de productos en ofertas grupales",
                    "Crear y compartir grupos de compra",
                    "Gestionar sus reservas activas",
                    "Acumular puntos y recompensas"
                )
            )

            SectionTitle("3. Responsabilidades del usuario")
            SectionParagraph("Los usuarios se comprometen a:")
            BulletList(
                listOf(
                    "Proporcionar información veraz y actualizada",
                    "Mantener la confidencialidad de su cuenta",
                    "Completar las compras reservadas en tiempo y forma",
                    "No abusar del sistema de gamificación"
                )
            )

            SectionTitle("4. Ofertas y precios")
            SectionParagraph("Las ofertas grupales están sujetas a disponibilidad y límites de tiempo. Los precios son fijados por las bodegas participantes y pueden variar. NexusBiz no se hace responsable por cambios en precios o disponibilidad.")

            SectionTitle("5. Sistema de puntos")
            SectionParagraph("El sistema de gamificación es gestionado por NexusBiz y puede modificarse en cualquier momento. Los puntos no tienen valor monetario y no son transferibles.")

            SectionTitle("6. Limitación de responsabilidad")
            SectionParagraph("NexusBiz actúa como intermediario entre consumidores y bodegas. No somos responsables por la calidad de los productos, disponibilidad o disputas entre usuarios y bodegas.")

            SectionTitle("7. Modificaciones")
            SectionParagraph("Nos reservamos el derecho de modificar estos términos en cualquier momento. Los cambios serán notificados a través de la aplicación.")
        }
    )
}

@Composable
private fun PrivacyContent() {
    InfoCard(
        icon = Icons.Default.Security,
        title = "Política de Privacidad",
        subtitle = "Última actualización: Noviembre 2024",
        content = {
            SectionTitle("1. Información que recopilamos")
            SectionParagraph("Recopilamos la siguiente información cuando usas NexusBiz:")
            BulletList(
                listOf(
                    "Nombre y número de teléfono",
                    "Ubicación (distrito) - solo con tu permiso",
                    "Historial de reservas y compras",
                    "Puntos y progreso en gamificación",
                    "Información de uso de la aplicación"
                )
            )

            SectionTitle("2. Cómo usamos tu información")
            SectionParagraph("Utilizamos tu información para:")
            BulletList(
                listOf(
                    "Procesar tus reservas y gestionar ofertas",
                    "Mostrarte ofertas relevantes en tu zona",
                    "Personalizar tu experiencia",
                    "Administrar el sistema de puntos y recompensas",
                    "Mejorar nuestros servicios",
                    "Enviarte notificaciones sobre tus grupos activos"
                )
            )

            SectionTitle("3. Compartir información")
            SectionParagraph("Solo compartimos tu información con:")
            BulletList(
                listOf(
                    "Las bodegas donde realizas reservas (nombre y teléfono)",
                    "Otros miembros de tu grupo (nombre)",
                    "Proveedores de servicios necesarios para operar la plataforma"
                )
            )
            SectionParagraph("Nunca vendemos tu información personal a terceros.")

            SectionTitle("4. Ubicación GPS")
            SectionParagraph("Solo accedemos a tu ubicación GPS si lo autorizas. Usamos esta información para detectar tu distrito y mostrar bodegas cercanas. Puedes elegirlo manualmente sin activar el GPS.")

            SectionTitle("5. Seguridad")
            SectionParagraph("Implementamos medidas para proteger tu información. Ningún sistema es 100% seguro; mantén tu contraseña confidencial.")

            SectionTitle("6. Tus derechos")
            SectionParagraph("Tienes derecho a:")
            BulletList(
                listOf(
                    "Acceder a tu información personal",
                    "Solicitar corrección de datos incorrectos",
                    "Eliminar tu cuenta y datos",
                    "Retirar el permiso de ubicación en cualquier momento"
                )
            )

            SectionTitle("7. Cookies y tecnologías similares")
            SectionParagraph("Usamos cookies y tecnologías similares para mejorar tu experiencia, recordar tus preferencias y analizar el uso de la aplicación.")

            SectionTitle("8. Contacto")
            SectionParagraph("Si tienes preguntas sobre nuestra política de privacidad, contáctanos a través de la sección de soporte.")
        }
    )
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier
                            .padding(14.dp)
                    )
                }
                Column {
                    Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                    Text(text = subtitle, fontSize = 13.sp, color = Color(0xFF6B7280))
                }
            }
            content()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1A1A1A)
    )
}

@Composable
private fun SectionParagraph(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFF606060),
        lineHeight = 20.sp
    )
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "•", color = Color(0xFF10B981), fontSize = 16.sp)
                Text(
                    text = item,
                    fontSize = 14.sp,
                    color = Color(0xFF606060),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class TermsTab(
    val key: String,
    val label: String
)

