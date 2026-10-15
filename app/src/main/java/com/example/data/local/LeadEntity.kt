package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.LeadPriority
import com.example.data.model.LeadStage
import com.example.data.model.SyncStatus

@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientName: String,
    val companyName: String,
    val email: String,
    val phone: String,
    val dealValue: Double,
    val currency: String = "₹",
    val stage: LeadStage = LeadStage.NEW,
    val priority: LeadPriority = LeadPriority.MEDIUM,
    val source: String = "Website",
    val assignedTo: String = "Rahul Sharma",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
