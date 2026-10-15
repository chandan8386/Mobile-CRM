package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SyncStatus
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val dueDate: Long = System.currentTimeMillis() + 3L * 24 * 3600 * 1000,
    val assignedTo: String = "Rahul Sharma",
    val relatedProjectId: Long? = null,
    val relatedProjectName: String? = null,
    val relatedLeadId: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
