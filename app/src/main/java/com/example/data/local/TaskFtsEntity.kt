package com.example.data.local

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 virtual table for full-text search across tasks (title, description,
 * assigned rep, related project name) even when offline.
 */
@Entity(tableName = "tasks_fts")
@Fts4(contentEntity = TaskEntity::class)
data class TaskFtsEntity(
    val title: String,
    val description: String,
    val assignedTo: String,
    val relatedProjectName: String?
)
