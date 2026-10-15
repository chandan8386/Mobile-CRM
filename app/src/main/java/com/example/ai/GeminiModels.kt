package com.example.ai

import com.example.data.model.FollowUpType
import com.example.data.model.TaskPriority

enum class AiSourceType(val label: String) {
    CALL_NOTES("Call Notes"),
    BUSINESS_CARD("Business Card"),
    MEETING_NOTES("Meeting Notes"),
    ALL_LEAD_DATA("All Lead Context")
}

data class ExtractedContactInfo(
    val fullName: String = "",
    val designation: String = "",
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val website: String = "",
    val servicesOrTags: List<String> = emptyList()
)

data class AiActionItem(
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val suggestedDueDateDays: Int = 2
)

data class RecommendedFollowUp(
    val type: FollowUpType = FollowUpType.CALL,
    val suggestedDays: Int = 2,
    val purpose: String = ""
)

data class GeminiAnalysisResult(
    val clientName: String,
    val companyName: String,
    val summary: String,
    val clientSentiment: String, // e.g. "Positive / High Intent", "Price Sensitive", "Deliberating", "Ready to Close"
    val keyDiscussionPoints: List<String> = emptyList(),
    val actionItems: List<AiActionItem> = emptyList(),
    val extractedContact: ExtractedContactInfo? = null,
    val recommendedFollowUp: RecommendedFollowUp? = null,
    val isDemoFallback: Boolean = false,
    val errorMessage: String? = null
)
