package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityLogEntity
import com.example.data.local.AppNotificationEntity
import com.example.data.local.FollowUpEntity
import com.example.data.local.LeadEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.TaskEntity
import com.example.data.model.FollowUpStatus
import com.example.data.model.FollowUpType
import com.example.data.model.LeadPriority
import com.example.data.model.LeadStage
import com.example.data.model.NotificationType
import com.example.data.model.RolePermissions
import com.example.data.model.RoleType
import com.example.data.model.SyncStatus
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TeamMember
import com.example.data.model.UserRole
import com.example.data.repository.CrmRepository
import com.example.localization.AppLanguage
import com.example.localization.LanguageManager
import com.example.localization.LocalizedStrings
import com.example.notification.FcmNotificationManager
import com.example.security.BiometricSecurityManager
import com.example.security.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CrmViewModel(
    private val repository: CrmRepository,
    val securityManager: BiometricSecurityManager,
    val permissionManager: PermissionManager = PermissionManager(),
    val fcmNotificationManager: FcmNotificationManager? = null
) : ViewModel() {

    // Team Members Roster: Admin, Sales Manager, and Sales Representatives
    val teamMembers = listOf(
        TeamMember(
            id = "1",
            name = "Rahul Sharma",
            email = "rahul.sharma@crmnexus.in",
            role = RoleType.ADMIN,
            phone = "+91 98765 00001",
            targetRevenue = 2000000.0,
            achievedRevenue = 1650000.0,
            dealsWonCount = 7,
            activeLeadsCount = 12
        ),
        TeamMember(
            id = "2",
            name = "Priya Patel",
            email = "priya.patel@crmnexus.in",
            role = RoleType.SALES_MANAGER,
            phone = "+91 98765 00002",
            targetRevenue = 1500000.0,
            achievedRevenue = 1200000.0,
            dealsWonCount = 5,
            activeLeadsCount = 9
        ),
        TeamMember(
            id = "3",
            name = "Amit Verma",
            email = "amit.verma@crmnexus.in",
            role = RoleType.SALES_REP,
            phone = "+91 98765 00003",
            targetRevenue = 1200000.0,
            achievedRevenue = 850000.0,
            dealsWonCount = 4,
            activeLeadsCount = 8
        ),
        TeamMember(
            id = "4",
            name = "Sneha Roy",
            email = "sneha.roy@crmnexus.in",
            role = RoleType.SALES_REP,
            phone = "+91 98765 00004",
            targetRevenue = 1000000.0,
            achievedRevenue = 620000.0,
            dealsWonCount = 3,
            activeLeadsCount = 7
        )
    )

    // Current User & Role
    private val _currentMember = MutableStateFlow(teamMembers[0])
    val currentMember: StateFlow<TeamMember> = _currentMember.asStateFlow()

    // Permissions (managed via PermissionManager with Firestore sync)
    private val _permissions = MutableStateFlow(RolePermissions.forRole(RoleType.ADMIN))
    val permissions: StateFlow<RolePermissions> = _permissions.asStateFlow()

    fun switchUser(member: TeamMember) {
        _currentMember.value = member
        permissionManager.setUserRole(member.userRole)
        _permissions.value = member.userRole.permissions
        permissionManager.fetchUserRoleFromFirestore(member.id) { updatedRole ->
            if (updatedRole != null) {
                _permissions.value = updatedRole.permissions
            }
        }
    }

    fun updatePermissions(newPermissions: RolePermissions) {
        _permissions.value = newPermissions
        permissionManager.updatePermissions(newPermissions)
    }

    // Multilingual State
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _strings = MutableStateFlow(LanguageManager.getStrings(AppLanguage.ENGLISH))
    val strings: StateFlow<LocalizedStrings> = _strings.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        _strings.value = LanguageManager.getStrings(language)
    }

    // Offline / Sync State
    val isOfflineMode: StateFlow<Boolean> = repository.isOfflineMode
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val lastSyncTimestamp: StateFlow<Long> = repository.lastSyncTimestamp

    fun toggleOfflineMode() {
        val next = !isOfflineMode.value
        repository.setOfflineMode(next)
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            repository.syncPendingData()
        }
    }

    // Leads Data
    val allLeads: StateFlow<List<LeadEntity>> = repository.allLeads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search & Filter
    private val _leadSearchQuery = MutableStateFlow("")
    val leadSearchQuery: StateFlow<String> = _leadSearchQuery.asStateFlow()

    private val _selectedStageFilter = MutableStateFlow<LeadStage?>(null)
    val selectedStageFilter: StateFlow<LeadStage?> = _selectedStageFilter.asStateFlow()

    fun setLeadSearchQuery(query: String) {
        _leadSearchQuery.value = query
    }

    fun setStageFilter(stage: LeadStage?) {
        _selectedStageFilter.value = stage
    }

    // Visible Leads (filtered by search, stage, and role access)
    val visibleLeads: StateFlow<List<LeadEntity>> = combine(
        allLeads,
        _leadSearchQuery,
        _selectedStageFilter,
        currentMember,
        permissions
    ) { leads, query, stage, member, perms ->
        leads.filter { lead ->
            val matchesRole = perms.canViewAllLeads || lead.assignedTo == member.name
            val matchesStage = stage == null || lead.stage == stage
            val matchesSearch = query.isBlank() ||
                lead.clientName.contains(query, ignoreCase = true) ||
                lead.companyName.contains(query, ignoreCase = true) ||
                lead.phone.contains(query, ignoreCase = true) ||
                lead.email.contains(query, ignoreCase = true)

            matchesRole && matchesStage && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Pending Sync Count
    val pendingSyncCount: StateFlow<Int> = allLeads.combine(repository.allFollowUps) { leads, followUps ->
        leads.count { it.syncStatus == SyncStatus.PENDING_SYNC } +
            followUps.count { it.syncStatus == SyncStatus.PENDING_SYNC }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Lead Actions
    fun saveLead(lead: LeadEntity) {
        viewModelScope.launch {
            repository.saveLead(
                lead = lead,
                authorName = _currentMember.value.name,
                authorRole = _currentMember.value.role.displayName
            )
        }
    }

    fun advanceLeadStage(lead: LeadEntity, newStage: LeadStage) {
        viewModelScope.launch {
            repository.updateLead(
                lead = lead.copy(stage = newStage),
                authorName = _currentMember.value.name,
                authorRole = _currentMember.value.role.displayName,
                changeNote = "Advanced stage from ${lead.stage.label} to ${newStage.label}"
            )
        }
    }

    fun deleteLead(id: Long) {
        viewModelScope.launch {
            repository.deleteLead(id)
        }
    }

    // Follow-ups Data
    val allFollowUps: StateFlow<List<FollowUpEntity>> = repository.allFollowUps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveFollowUp(followUp: FollowUpEntity) {
        viewModelScope.launch {
            repository.saveFollowUp(followUp)
        }
    }

    fun markFollowUpCompleted(followUp: FollowUpEntity) {
        viewModelScope.launch {
            repository.updateFollowUp(
                followUp.copy(
                    status = FollowUpStatus.COMPLETED,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteFollowUp(id: Long) {
        viewModelScope.launch {
            repository.deleteFollowUp(id)
        }
    }

    // Projects Data
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.saveProject(project)
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    // Tasks Data
    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.saveTask(task)
        }
    }

    fun toggleTaskStatus(task: TaskEntity) {
        val nextStatus = if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE
        viewModelScope.launch {
            repository.updateTask(task.copy(status = nextStatus))
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    // Activity Logs & Team Collaboration
    val recentActivityLogs: StateFlow<List<ActivityLogEntity>> = repository.recentActivityLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTeamComment(entityType: String, entityId: Long, comment: String) {
        if (comment.isNotBlank()) {
            viewModelScope.launch {
                repository.addComment(
                    entityType = entityType,
                    entityId = entityId,
                    authorName = _currentMember.value.name,
                    authorRole = _currentMember.value.role.displayName,
                    comment = comment
                )
            }
        }
    }

    // Notifications
    val notifications: StateFlow<List<AppNotificationEntity>> = repository.allNotifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun markNotificationRead(id: Long) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun clearNotifications() {
        viewModelScope.launch { repository.clearNotifications() }
    }
}
