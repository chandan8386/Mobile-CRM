package com.example.data.model

enum class RoleType(val displayName: String, val level: Int) {
    ADMIN("Admin / Executive", 3),
    SALES_MANAGER("Sales Manager", 2),
    SALES_REP("Sales Representative", 1)
}

data class RolePermissions(
    val canViewAllLeads: Boolean = false,
    val canReassignLeads: Boolean = false,
    val canDeleteLeads: Boolean = false,
    val canManageProjects: Boolean = false,
    val canViewTeamAnalytics: Boolean = false,
    val canModifySystemSettings: Boolean = false,
    val canAccessAdminPanel: Boolean = false,
    val canExportReports: Boolean = false
) {
    companion object {
        fun forRole(role: RoleType): RolePermissions = when (role) {
            RoleType.ADMIN -> RolePermissions(
                canViewAllLeads = true,
                canReassignLeads = true,
                canDeleteLeads = true,
                canManageProjects = true,
                canViewTeamAnalytics = true,
                canModifySystemSettings = true,
                canAccessAdminPanel = true,
                canExportReports = true
            )
            RoleType.SALES_MANAGER -> RolePermissions(
                canViewAllLeads = true,
                canReassignLeads = true,
                canDeleteLeads = false,
                canManageProjects = true,
                canViewTeamAnalytics = true,
                canModifySystemSettings = false,
                canAccessAdminPanel = true,
                canExportReports = true
            )
            RoleType.SALES_REP -> RolePermissions(
                canViewAllLeads = false,
                canReassignLeads = false,
                canDeleteLeads = false,
                canManageProjects = false,
                canViewTeamAnalytics = false,
                canModifySystemSettings = false,
                canAccessAdminPanel = false,
                canExportReports = false
            )
        }
    }
}

data class UserRole(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val roleType: RoleType = RoleType.SALES_REP,
    val permissions: RolePermissions = RolePermissions.forRole(roleType),
    val department: String = "Enterprise Sales",
    val assignedRegion: String = "India / APAC",
    val isActive: Boolean = true
) {
    val isAdmin: Boolean get() = roleType == RoleType.ADMIN
    val isSalesManager: Boolean get() = roleType == RoleType.SALES_MANAGER
    val isSalesRep: Boolean get() = roleType == RoleType.SALES_REP

    companion object {
        val ADMIN = RoleType.ADMIN
        val SALES_MANAGER = RoleType.SALES_MANAGER
        val SALES_REP = RoleType.SALES_REP
    }
}

data class TeamMember(
    val id: String,
    val name: String,
    val email: String,
    val role: RoleType,
    val phone: String,
    val targetRevenue: Double,
    val achievedRevenue: Double,
    val dealsWonCount: Int,
    val activeLeadsCount: Int,
    val region: String = "India"
) {
    val userRole: UserRole
        get() = UserRole(
            userId = id,
            email = email,
            displayName = name,
            roleType = role,
            permissions = RolePermissions.forRole(role)
        )
}
