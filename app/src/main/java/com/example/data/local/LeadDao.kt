package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LeadStage
import com.example.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {

    @Query("SELECT * FROM leads ORDER BY updatedAt DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE id = :id")
    fun getLeadById(id: Long): Flow<LeadEntity?>

    @Query("SELECT * FROM leads WHERE stage = :stage ORDER BY updatedAt DESC")
    fun getLeadsByStage(stage: LeadStage): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE assignedTo = :repName ORDER BY updatedAt DESC")
    fun getLeadsForRep(repName: String): Flow<List<LeadEntity>>

    // FTS4 Full-Text Search
    @Query("""
        SELECT leads.* FROM leads
        JOIN leads_fts ON leads.id = leads_fts.rowid
        WHERE leads_fts MATCH :ftsQuery
        ORDER BY leads.updatedAt DESC
    """)
    fun searchLeadsFts(ftsQuery: String): Flow<List<LeadEntity>>

    // Offline data syncing queries
    @Query("SELECT * FROM leads WHERE syncStatus = :status")
    suspend fun getPendingSyncLeads(status: SyncStatus = SyncStatus.PENDING_SYNC): List<LeadEntity>

    @Query("SELECT * FROM leads WHERE syncStatus = :status ORDER BY updatedAt DESC")
    fun getPendingSyncLeadsFlow(status: SyncStatus = SyncStatus.PENDING_SYNC): Flow<List<LeadEntity>>

    @Query("SELECT COUNT(*) FROM leads WHERE syncStatus = :status")
    fun getPendingSyncCount(status: SyncStatus = SyncStatus.PENDING_SYNC): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: LeadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeads(leads: List<LeadEntity>): List<Long>

    @Update
    suspend fun updateLead(lead: LeadEntity)

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteLeadById(id: Long)

    @Query("UPDATE leads SET syncStatus = :syncedStatus WHERE id = :leadId")
    suspend fun markLeadAsSynced(
        leadId: Long,
        syncedStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("UPDATE leads SET syncStatus = :syncedStatus WHERE syncStatus = :pendingStatus")
    suspend fun markAllLeadsAsSynced(
        pendingStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        syncedStatus: SyncStatus = SyncStatus.SYNCED
    )
}
