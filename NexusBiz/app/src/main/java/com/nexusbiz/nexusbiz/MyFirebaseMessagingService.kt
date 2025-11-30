package com.nexusbiz.nexusbiz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationRepository = NotificationRepository()
    private val authRepository = AuthRepository()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Se llama cuando se recibe un nuevo token FCM.
     * Debes guardar este token en Supabase para poder enviar notificaciones al usuario.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM: $token")
        
        serviceScope.launch {
            try {
                val currentUser = authRepository.currentUser.first()
                if (currentUser != null) {
                    notificationRepository.saveFCMToken(currentUser.id, token)
                    Log.d(TAG, "Token guardado para usuario: ${currentUser.id}")
                } else {
                    Log.w(TAG, "No hay usuario autenticado, token no guardado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar token FCM: ${e.message}", e)
            }
        }
    }

    /**
     * Se llama cuando se recibe una notificación push.
     * Maneja tanto notificaciones en primer plano como en segundo plano.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Notificación recibida de: ${remoteMessage.from}")

        // Verificar si el mensaje contiene datos
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Datos del mensaje: ${remoteMessage.data}")
            handleNotificationData(remoteMessage.data)
        }

        // Verificar si el mensaje contiene notificación
        remoteMessage.notification?.let {
            Log.d(TAG, "Título: ${it.title}, Cuerpo: ${it.body}")
            sendNotification(
                title = it.title ?: "NexusBiz",
                message = it.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    /**
     * Maneja los datos adicionales de la notificación.
     */
    private fun handleNotificationData(data: Map<String, String>) {
        val type = data["type"]
        val offerId = data["offer_id"]
        val groupId = data["group_id"]
        
        Log.d(TAG, "Tipo de notificación: $type, Oferta: $offerId, Grupo: $groupId")
        
        // Aquí puedes manejar acciones específicas según el tipo de notificación
        // Por ejemplo, navegar a una pantalla específica cuando el usuario toque la notificación
    }

    /**
     * Crea y muestra la notificación local.
     */
    private fun sendNotification(
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            // Agregar datos extras para navegación
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "nexusbiz_notifications"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Cambia esto por tu ícono
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    /**
     * Crea el canal de notificaciones (requerido para Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "nexusbiz_notifications"
            val channelName = "Notificaciones NexusBiz"
            val channelDescription = "Notificaciones de ofertas, grupos y actividades"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "FCMService"
    }
}