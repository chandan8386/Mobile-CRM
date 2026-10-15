package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ProjectStage
import com.example.data.model.SyncStatus

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectName: String,
    val clientName: String,
    val budget: Double,
    val currency: String = "₹",
    val stage: ProjectStage = ProjectStage.PLANNING,
    val progressPercentage: Int = 0,
    val startDate: Long = System.currentTimeMillis(),
    val deadline: Long = System.currentTimeMillis() + 30L * 24 * 3600 * 1000,
    val leadId: Long? = null,
    val assignedTeam: String = "Rahul Sharma",
    val description: String = "",
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
