package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SyncStatus

@Entity(tableName = "lead_documents")
data class LeadDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val leadId: Long,
    val title: String,
    val docType: String = "BUSINESS_CARD", // BUSINESS_CARD, DOCUMENT, CONTRACT, INVOICE, NOTES
    val filePath: String,
    val fileUri: String = "",
    val notes: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
