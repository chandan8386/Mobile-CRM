package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CrmDao {

    // Leads
    @Query("SELECT * FROM leads ORDER BY updatedAt DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE assignedTo = :repName ORDER BY updatedAt DESC")
    fun getLeadsForRep(repName: String): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE id = :id")
    suspend fun getLeadById(id: Long): LeadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: LeadEntity): Long

    @Update
    suspend fun updateLead(lead: LeadEntity)

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteLeadById(id: Long)

    @Query("SELECT COUNT(*) FROM leads WHERE syncStatus = :status")
    fun getPendingLeadsSyncCount(status: SyncStatus = SyncStatus.PENDING_SYNC): Flow<Int>

    // Follow-ups
    @Query("SELECT * FROM follow_ups ORDER BY scheduledDateTime ASC")
    fun getAllFollowUps(): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups WHERE leadId = :leadId ORDER BY scheduledDateTime DESC")
    fun getFollowUpsForLead(leadId: Long): Flow<List<FollowUpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUpEntity): Long

    @Update
    suspend fun updateFollowUp(followUp: FollowUpEntity)

    @Query("DELETE FROM follow_ups WHERE id = :id")
    suspend fun deleteFollowUpById(id: Long)

    // Projects
    @Query("SELECT * FROM projects ORDER BY deadline ASC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE relatedProjectId = :projectId ORDER BY dueDate ASC")
    fun getTasksForProject(projectId: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    // Activity Logs & Team Collaboration
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentActivityLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE entityType = :type AND entityId = :entityId ORDER BY timestamp DESC")
    fun getActivityLogsForEntity(type: String, entityId: Long): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    // Syncing all pending items
    @Query("UPDATE leads SET syncStatus = :syncedStatus WHERE syncStatus = :pendingStatus")
    suspend fun syncAllPendingLeads(
        pendingStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        syncedStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("UPDATE follow_ups SET syncStatus = :syncedStatus WHERE syncStatus = :pendingStatus")
    suspend fun syncAllPendingFollowUps(
        pendingStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        syncedStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("UPDATE projects SET syncStatus = :syncedStatus WHERE syncStatus = :pendingStatus")
    suspend fun syncAllPendingProjects(
        pendingStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        syncedStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("UPDATE tasks SET syncStatus = :syncedStatus WHERE syncStatus = :pendingStatus")
    suspend fun syncAllPendingTasks(
        pendingStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        syncedStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("UPDATE activity_logs SET syncStatus = :syncedStatus WHERE syncStatus = :pendingStatus")
    suspend fun syncAllPendingLogs(
        pendingStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        syncedStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("SELECT * FROM leads WHERE syncStatus = :status")
    suspend fun getPendingSyncLeads(status: SyncStatus = SyncStatus.PENDING_SYNC): List<LeadEntity>

    @Query("SELECT * FROM tasks WHERE syncStatus = :status")
    suspend fun getPendingSyncTasks(status: SyncStatus = SyncStatus.PENDING_SYNC): List<TaskEntity>
}
