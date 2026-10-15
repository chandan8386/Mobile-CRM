package com.example.data.local

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 virtual table for lightning-fast full-text search across leads (client name,
 * company, notes, email, phone, source, and assigned rep) even when completely offline.
 */
@Entity(tableName = "leads_fts")
@Fts4(contentEntity = LeadEntity::class)
data class LeadFtsEntity(
    val clientName: String,
    val companyName: String,
    val email: String,
    val phone: String,
    val source: String,
    val assignedTo: String,
    val notes: String
)
