package com.nexusbiz.nexusbiz.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class NotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "nexusbiz_notifications"
        private const val CHANNEL_NAME = "NexusBiz Notificaciones"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de grupos y ofertas"
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    fun showGroupCompletedNotification(groupName: String, groupId: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Grupo completado!")
            .setContentText("El grupo '$groupName' está completo. Puedes retirar tu producto.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(groupId.hashCode(), notification)
    }
    
    fun showGroupExpiredNotification(groupName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Grupo expirado")
            .setContentText("El grupo '$groupName' ha expirado. Busca bodegas con stock disponible.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((groupName.hashCode()), notification)
    }
    
    fun showNewMemberNotification(groupName: String, memberAlias: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nuevo miembro")
            .setContentText("$memberAlias se unió al grupo '$groupName'")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((groupName.hashCode()), notification)
    }
    
    fun showGroupAlmostCompleteNotification(groupName: String, remaining: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Falta poco!")
            .setContentText("Al grupo '$groupName' le faltan $remaining miembros para completarse.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((groupName.hashCode()), notification)
    }
}

