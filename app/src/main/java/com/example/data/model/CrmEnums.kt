package com.example.data.model

enum class LeadStage(val label: String, val colorHex: Long) {
    NEW("New", 0xFF3B82F6),
    CONTACTED("Contacted", 0xFF0284C7),
    QUALIFIED("Qualified", 0xFF8B5CF6),
    PROPOSAL("Proposal", 0xFFF59E0B),
    WON("Won", 0xFF10B981),
    LOST("Lost", 0xFFEF4444)
}

enum class LeadPriority(val label: String, val colorHex: Long) {
    HIGH("High", 0xFFEF4444),
    MEDIUM("Medium", 0xFFF59E0B),
    LOW("Low", 0xFF10B981)
}

enum class FollowUpType(val label: String) {
    CALL("Phone Call"),
    MEETING("In-Person Meeting"),
    WHATSAPP("WhatsApp Message"),
    EMAIL("Email"),
    DEMO("Product Demo")
}

enum class FollowUpStatus(val label: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    OVERDUE("Overdue")
}

enum class ProjectStage(val label: String, val colorHex: Long) {
    PLANNING("Planning", 0xFF3B82F6),
    IN_PROGRESS("In Progress", 0xFFF59E0B),
    REVIEW("Review", 0xFF8B5CF6),
    COMPLETED("Completed", 0xFF10B981)
}

enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Completed")
}

enum class TaskPriority(val label: String, val colorHex: Long) {
    URGENT("Urgent", 0xFFEF4444),
    HIGH("High", 0xFFF97316),
    MEDIUM("Medium", 0xFF3B82F6),
    LOW("Low", 0xFF64748B)
}

enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    SYNCING
}

enum class NotificationType {
    FOLLOW_UP,
    LEAD_ASSIGNED,
    DEAL_WON,
    PROJECT_UPDATE,
    SYSTEM
}
