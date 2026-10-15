package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SyncStatus
import com.example.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDate ASC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE relatedProjectId = :projectId ORDER BY dueDate ASC")
    fun getTasksForProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE assignedTo = :repName ORDER BY dueDate ASC")
    fun getTasksForRep(repName: String): Flow<List<TaskEntity>>

    // Offline data syncing queries
    @Query("SELECT * FROM tasks WHERE syncStatus = :status")
    suspend fun getPendingSyncTasks(status: SyncStatus = SyncStatus.PENDING_SYNC): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE syncStatus = :status ORDER BY dueDate ASC")
    fun getPendingSyncTasksFlow(status: SyncStatus = SyncStatus.PENDING_SYNC): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE syncStatus = :status")
    fun getPendingSyncCount(status: SyncStatus = SyncStatus.PENDING_SYNC): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET syncStatus = :syncedStatus WHERE id = :taskId")
    suspend fun markTaskAsSynced(
        taskId: Long,
        syncedStatus: SyncStatus = SyncStatus.SYNCED
    )

    @Query("UPDATE tasks SET syncStatus = :syncedStatus WHERE syncStatus = :pendingStatus")
    suspend fun markAllTasksAsSynced(
        pendingStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        syncedStatus: SyncStatus = SyncStatus.SYNCED
    )
}
