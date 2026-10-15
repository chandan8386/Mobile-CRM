package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SyncStatus

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String, // LEAD, PROJECT, TASK, SYSTEM
    val entityId: Long,
    val authorName: String,
    val authorRole: String,
    val actionText: String,
    val commentNote: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
