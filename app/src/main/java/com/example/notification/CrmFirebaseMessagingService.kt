package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.AppNotificationEntity
import com.example.data.model.NotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CrmFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "CrmFCMService"
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "crm_notifications_channel"
        const val CHANNEL_NAME = "CRM Sales Alerts & Notifications"
        const val CHANNEL_DESC = "Notifications for new leads, follow-up reminders, and task deadlines"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token: $token")
        // Token can be saved to Firestore or backend
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "NexusCRM Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "You have a new update in your sales pipeline."

        val typeStr = remoteMessage.data["type"] ?: "LEAD_ASSIGNED"
        val targetTab = remoteMessage.data["targetTab"] ?: when (typeStr) {
            "LEAD_ASSIGNED" -> "leads"
            "TASK_DEADLINE" -> "projects"
            "FOLLOW_UP" -> "followups"
            else -> "dashboard"
        }

        val notifType = when (typeStr) {
            "LEAD_ASSIGNED" -> NotificationType.LEAD_ASSIGNED
            "TASK_DEADLINE" -> NotificationType.PROJECT_UPDATE
            "FOLLOW_UP" -> NotificationType.FOLLOW_UP
            "DEAL_WON" -> NotificationType.DEAL_WON
            else -> NotificationType.SYSTEM
        }

        // 1. Save notification locally in Room database for in-app notification center
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext, scope)
                db.crmDao().insertNotification(
                    AppNotificationEntity(
                        title = title,
                        message = body,
                        type = notifType,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        targetTab = targetTab
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving FCM notification to Room: ${e.message}")
            }
        }

        // 2. Show Android system tray notification
        showSystemNotification(title, body)
    }

    private fun showSystemNotification(title: String, body: String) {
        ensureNotificationChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted: ${e.message}")
        }
    }
}

fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CrmFirebaseMessagingService.CHANNEL_ID,
            CrmFirebaseMessagingService.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CrmFirebaseMessagingService.CHANNEL_DESC
            enableVibration(true)
            enableLights(true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.createNotificationChannel(channel)
    }
}
