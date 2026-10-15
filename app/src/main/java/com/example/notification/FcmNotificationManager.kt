package com.example.notification

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.AppNotificationEntity
import com.example.data.model.NotificationType
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FcmNotificationManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "FcmNotificationManager"

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    init {
        ensureNotificationChannel(context)
        initializeFcm()
    }

    private fun initializeFcm() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    _fcmToken.value = token
                    Log.d(TAG, "FCM Device Token retrieved: $token")
                } else {
                    Log.w(TAG, "Fetching FCM registration token failed: ${task.exception?.message}")
                }
            }

            // Subscribe to relevant sales topics
            FirebaseMessaging.getInstance().subscribeToTopic("sales_team")
            FirebaseMessaging.getInstance().subscribeToTopic("lead_assignments")
            FirebaseMessaging.getInstance().subscribeToTopic("task_deadlines")
        } catch (e: Exception) {
            Log.w(TAG, "FCM initialization fallback: ${e.message}")
        }
    }

    /**
     * Sends/triggers a real-time notification for a newly assigned lead.
     */
    fun notifyNewLeadAssigned(
        leadName: String,
        companyName: String,
        repName: String,
        dealValue: Double
    ) {
        val title = "🎯 New Lead Assigned"
        val message = "$leadName from $companyName assigned to $repName (₹${dealValue.toInt()})."
        triggerLocalPushNotification(
            title = title,
            body = message,
            type = NotificationType.LEAD_ASSIGNED,
            targetTab = "leads"
        )
    }

    /**
     * Sends/triggers a real-time notification for an upcoming task deadline.
     */
    fun notifyTaskDeadlineApproaching(
        taskTitle: String,
        repName: String,
        dueInHours: Int = 24
    ) {
        val title = "⏰ Task Deadline Approaching"
        val message = "Task \"$taskTitle\" assigned to $repName is due in $dueInHours hours."
        triggerLocalPushNotification(
            title = title,
            body = message,
            type = NotificationType.PROJECT_UPDATE,
            targetTab = "projects"
        )
    }

    /**
     * Stores notification in Room database and presents system tray notification.
     */
    private fun triggerLocalPushNotification(
        title: String,
        body: String,
        type: NotificationType,
        targetTab: String
    ) {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context, scope)
                db.crmDao().insertNotification(
                    AppNotificationEntity(
                        title = title,
                        message = body,
                        type = type,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        targetTab = targetTab
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist notification to Room: ${e.message}")
            }
        }

        // Show Android notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, CrmFirebaseMessagingService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not enabled: ${e.message}")
        }
    }
}
