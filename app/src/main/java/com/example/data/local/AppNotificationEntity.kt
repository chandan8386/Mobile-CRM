package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.NotificationType

@Entity(tableName = "notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.SYSTEM,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetTab: String = "dashboard"
)
