package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FollowUpStatus
import com.example.data.model.FollowUpType
import com.example.data.model.SyncStatus

@Entity(tableName = "follow_ups")
data class FollowUpEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val leadId: Long,
    val leadName: String,
    val clientPhone: String,
    val scheduledDateTime: Long,
    val type: FollowUpType = FollowUpType.CALL,
    val status: FollowUpStatus = FollowUpStatus.PENDING,
    val notes: String = "",
    val completedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
