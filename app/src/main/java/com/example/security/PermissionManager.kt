package com.example.security

import android.util.Log
import com.example.data.local.LeadEntity
import com.example.data.model.RolePermissions
import com.example.data.model.RoleType
import com.example.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PermissionManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "PermissionManager"

    private val _currentUserRole = MutableStateFlow(
        UserRole(
            userId = "1",
            email = "rahul.sharma@crmnexus.in",
            displayName = "Rahul Sharma",
            roleType = RoleType.ADMIN,
            permissions = RolePermissions.forRole(RoleType.ADMIN)
        )
    )
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _currentPermissions = MutableStateFlow(RolePermissions.forRole(RoleType.ADMIN))
    val currentPermissions: StateFlow<RolePermissions> = _currentPermissions.asStateFlow()

    /**
     * Sets or switches the active user role and updates permissions accordingly.
     */
    fun setUserRole(role: UserRole) {
        _currentUserRole.value = role
        _currentPermissions.value = role.permissions
    }

    /**
     * Updates specific permission overrides (e.g. from Admin control panel).
     */
    fun updatePermissions(permissions: RolePermissions) {
        _currentPermissions.value = permissions
        _currentUserRole.value = _currentUserRole.value.copy(permissions = permissions)
        syncRoleToFirestore(_currentUserRole.value)
    }

    /**
     * Checks if a user is permitted to view a specific lead based on their role and assignment.
     */
    fun canViewLead(lead: LeadEntity, user: UserRole = _currentUserRole.value): Boolean {
        return user.permissions.canViewAllLeads || lead.assignedTo.equals(user.displayName, ignoreCase = true)
    }

    /**
     * Checks if the user is authorized to delete a lead.
     */
    fun canDeleteLead(user: UserRole = _currentUserRole.value): Boolean {
        return user.permissions.canDeleteLeads
    }

    /**
     * Checks if the user is authorized to reassign a lead to another team member.
     */
    fun canReassignLead(user: UserRole = _currentUserRole.value): Boolean {
        return user.permissions.canReassignLeads
    }

    /**
     * Checks if the user is authorized to access the Admin / Manager control panel.
     */
    fun canAccessAdminPanel(user: UserRole = _currentUserRole.value): Boolean {
        return user.permissions.canAccessAdminPanel
    }

    /**
     * Checks if the user can manage project deliverables and budgets.
     */
    fun canManageProjects(user: UserRole = _currentUserRole.value): Boolean {
        return user.permissions.canManageProjects
    }

    /**
     * Synchronizes the user role with Firestore collection "user_roles".
     */
    fun syncRoleToFirestore(userRole: UserRole) {
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val roleData = hashMapOf(
                    "userId" to userRole.userId,
                    "email" to userRole.email,
                    "displayName" to userRole.displayName,
                    "roleType" to userRole.roleType.name,
                    "canViewAllLeads" to userRole.permissions.canViewAllLeads,
                    "canReassignLeads" to userRole.permissions.canReassignLeads,
                    "canDeleteLeads" to userRole.permissions.canDeleteLeads,
                    "canManageProjects" to userRole.permissions.canManageProjects,
                    "canViewTeamAnalytics" to userRole.permissions.canViewTeamAnalytics,
                    "canModifySystemSettings" to userRole.permissions.canModifySystemSettings,
                    "department" to userRole.department,
                    "assignedRegion" to userRole.assignedRegion,
                    "updatedAt" to System.currentTimeMillis()
                )

                firestore.collection("user_roles")
                    .document(userRole.userId.ifBlank { userRole.email })
                    .set(roleData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully synced role to Firestore for ${userRole.displayName}")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to sync role to Firestore (offline fallback active): ${e.message}")
                    }
            } catch (e: Exception) {
                // Offline fallback - local RBAC state preserved
                Log.w(TAG, "Firestore unavailable; operating in local RBAC mode: ${e.message}")
            }
        }
    }

    /**
     * Fetches user role from Firestore collection "user_roles" if available.
     */
    fun fetchUserRoleFromFirestore(userId: String, onComplete: ((UserRole?) -> Unit)? = null) {
        scope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("user_roles")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val roleTypeName = document.getString("roleType") ?: RoleType.SALES_REP.name
                            val roleType = try {
                                RoleType.valueOf(roleTypeName)
                            } catch (_: Exception) {
                                RoleType.SALES_REP
                            }

                            val permissions = RolePermissions(
                                canViewAllLeads = document.getBoolean("canViewAllLeads") ?: (roleType != RoleType.SALES_REP),
                                canReassignLeads = document.getBoolean("canReassignLeads") ?: (roleType != RoleType.SALES_REP),
                                canDeleteLeads = document.getBoolean("canDeleteLeads") ?: (roleType == RoleType.ADMIN),
                                canManageProjects = document.getBoolean("canManageProjects") ?: (roleType != RoleType.SALES_REP),
                                canViewTeamAnalytics = document.getBoolean("canViewTeamAnalytics") ?: (roleType != RoleType.SALES_REP),
                                canModifySystemSettings = document.getBoolean("canModifySystemSettings") ?: (roleType == RoleType.ADMIN),
                                canAccessAdminPanel = roleType != RoleType.SALES_REP
                            )

                            val fetchedRole = UserRole(
                                userId = document.getString("userId") ?: userId,
                                email = document.getString("email") ?: "",
                                displayName = document.getString("displayName") ?: "",
                                roleType = roleType,
                                permissions = permissions,
                                department = document.getString("department") ?: "Enterprise Sales",
                                assignedRegion = document.getString("assignedRegion") ?: "India / APAC"
                            )

                            setUserRole(fetchedRole)
                            onComplete?.invoke(fetchedRole)
                        } else {
                            onComplete?.invoke(null)
                        }
                    }
                    .addOnFailureListener {
                        onComplete?.invoke(null)
                    }
            } catch (e: Exception) {
                onComplete?.invoke(null)
            }
        }
    }
}
