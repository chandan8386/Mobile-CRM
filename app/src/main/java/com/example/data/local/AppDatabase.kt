package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FollowUpStatus
import com.example.data.model.FollowUpType
import com.example.data.model.LeadPriority
import com.example.data.model.LeadStage
import com.example.data.model.NotificationType
import com.example.data.model.ProjectStage
import com.example.data.model.SyncStatus
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        LeadEntity::class,
        LeadFtsEntity::class,
        FollowUpEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        TaskFtsEntity::class,
        ActivityLogEntity::class,
        AppNotificationEntity::class,
        LeadDocumentEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun crmDao(): CrmDao
    abstract fun leadDao(): LeadDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexus_crm_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.crmDao())
                }
            }
        }
    }
}

suspend fun populateInitialData(dao: CrmDao) {
    val now = System.currentTimeMillis()
    val hour = 3600 * 1000L
    val day = 24 * hour

    // Seed Initial Leads
    val lead1 = LeadEntity(
        clientName = "Ananya Roy",
        companyName = "Apex Logistics India",
        email = "ananya.roy@apexlogistics.in",
        phone = "+91 98765 43210",
        dealValue = 450000.0,
        currency = "₹",
        stage = LeadStage.PROPOSAL,
        priority = LeadPriority.HIGH,
        source = "Website Referral",
        assignedTo = "Rahul Sharma",
        notes = "Interested in Enterprise Fleet Tracking Suite. Proposal sent yesterday, waiting for finance clearance."
    )
    val lead2 = LeadEntity(
        clientName = "Vikram Malhotra",
        companyName = "Zenith FinTech Solutions",
        email = "v.malhotra@zenithfin.com",
        phone = "+91 98112 34567",
        dealValue = 1200000.0,
        currency = "₹",
        stage = LeadStage.QUALIFIED,
        priority = LeadPriority.HIGH,
        source = "LinkedIn Campaign",
        assignedTo = "Priya Patel",
        notes = "Needs cloud security and automated compliance module for 250 workstations."
    )
    val lead3 = LeadEntity(
        clientName = "Sunita Rao",
        companyName = "Heritage Organics Pvt Ltd",
        email = "s.rao@heritageorganics.org",
        phone = "+91 94450 98765",
        dealValue = 320000.0,
        currency = "₹",
        stage = LeadStage.WON,
        priority = LeadPriority.MEDIUM,
        source = "Trade Expo 2026",
        assignedTo = "Amit Verma",
        notes = "Deal closed successfully! Initial payment of 40% advance received."
    )
    val lead4 = LeadEntity(
        clientName = "David Miller",
        companyName = "CloudScale Tech US",
        email = "david@cloudscale.io",
        phone = "+1 415 555 0192",
        dealValue = 850000.0,
        currency = "₹",
        stage = LeadStage.CONTACTED,
        priority = LeadPriority.MEDIUM,
        source = "Cold Outreach",
        assignedTo = "Sneha Roy",
        notes = "Had introductory discovery call. Scheduled technical architecture demo next Monday."
    )
    val lead5 = LeadEntity(
        clientName = "Ramesh Gupta",
        companyName = "Bharat Steel Traders",
        email = "rgupta@bharatsteel.co.in",
        phone = "+91 99234 56789",
        dealValue = 280000.0,
        currency = "₹",
        stage = LeadStage.NEW,
        priority = LeadPriority.LOW,
        source = "Website Inbound",
        assignedTo = "Rahul Sharma",
        notes = "Inquired about inventory sync and invoicing module via web contact form."
    )

    val id1 = dao.insertLead(lead1)
    val id2 = dao.insertLead(lead2)
    val id3 = dao.insertLead(lead3)
    val id4 = dao.insertLead(lead4)
    val id5 = dao.insertLead(lead5)

    // Seed Follow-ups
    dao.insertFollowUp(
        FollowUpEntity(
            leadId = id1,
            leadName = "Ananya Roy (Apex Logistics)",
            clientPhone = "+91 98765 43210",
            scheduledDateTime = now + (2 * hour),
            type = FollowUpType.CALL,
            status = FollowUpStatus.PENDING,
            notes = "Follow up on enterprise proposal and discount negotiation."
        )
    )
    dao.insertFollowUp(
        FollowUpEntity(
            leadId = id2,
            leadName = "Vikram Malhotra (Zenith FinTech)",
            clientPhone = "+91 98112 34567",
            scheduledDateTime = now + (26 * hour),
            type = FollowUpType.DEMO,
            status = FollowUpStatus.PENDING,
            notes = "Live demo of security protocol and data migration."
        )
    )
    dao.insertFollowUp(
        FollowUpEntity(
            leadId = id4,
            leadName = "David Miller (CloudScale Tech)",
            clientPhone = "+1 415 555 0192",
            scheduledDateTime = now - (3 * hour), // Overdue
            type = FollowUpType.EMAIL,
            status = FollowUpStatus.OVERDUE,
            notes = "Send technical NDA and architecture whitepaper."
        )
    )

    // Seed Projects
    val proj1 = dao.insertProject(
        ProjectEntity(
            projectName = "Heritage Organics ERP Rollout",
            clientName = "Heritage Organics Pvt Ltd",
            budget = 320000.0,
            currency = "₹",
            stage = ProjectStage.IN_PROGRESS,
            progressPercentage = 45,
            startDate = now - (7 * day),
            deadline = now + (25 * day),
            leadId = id3,
            assignedTeam = "Amit Verma, Priya Patel",
            description = "Multi-warehouse supply chain setup and barcode scanner integration."
        )
    )
    val proj2 = dao.insertProject(
        ProjectEntity(
            projectName = "Apex Fleet Telematics Phase 1",
            clientName = "Apex Logistics India",
            budget = 450000.0,
            currency = "₹",
            stage = ProjectStage.PLANNING,
            progressPercentage = 15,
            startDate = now,
            deadline = now + (45 * day),
            leadId = id1,
            assignedTeam = "Rahul Sharma, Sneha Roy",
            description = "Hardware GPS tracker provisioning and API endpoints setup."
        )
    )

    // Seed Tasks
    dao.insertTask(
        TaskEntity(
            title = "Finalize SLA contract draft",
            description = "Review service level guarantees with legal team for Apex Logistics.",
            priority = TaskPriority.URGENT,
            status = TaskStatus.IN_PROGRESS,
            dueDate = now + (24 * hour),
            assignedTo = "Rahul Sharma",
            relatedProjectId = proj2,
            relatedProjectName = "Apex Fleet Telematics Phase 1",
            relatedLeadId = id1
        )
    )
    dao.insertTask(
        TaskEntity(
            title = "Import catalog database for Heritage Organics",
            description = "Validate SKU entries and stock counts across 3 warehouse locations.",
            priority = TaskPriority.HIGH,
            status = TaskStatus.TODO,
            dueDate = now + (2 * day),
            assignedTo = "Amit Verma",
            relatedProjectId = proj1,
            relatedProjectName = "Heritage Organics ERP Rollout",
            relatedLeadId = id3
        )
    )
    dao.insertTask(
        TaskEntity(
            title = "Schedule Q3 Sales Strategy Sync",
            description = "Review individual rep targets and quarterly pipeline conversion metrics.",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.TODO,
            dueDate = now + (4 * day),
            assignedTo = "Rahul Sharma"
        )
    )

    // Seed Activity Logs (Collaboration)
    dao.insertActivityLog(
        ActivityLogEntity(
            entityType = "LEAD",
            entityId = id1,
            authorName = "Rahul Sharma",
            authorRole = "Sales Manager",
            actionText = "Moved stage to Proposal",
            commentNote = "Client requested payment milestones across 3 quarters."
        )
    )
    dao.insertActivityLog(
        ActivityLogEntity(
            entityType = "LEAD",
            entityId = id3,
            authorName = "Amit Verma",
            authorRole = "Sales Rep",
            actionText = "Marked as WON",
            commentNote = "Advance payment received. Transitioned to project deployment team."
        )
    )

    // Seed Notifications
    dao.insertNotification(
        AppNotificationEntity(
            title = "Follow-up Overdue",
            message = "Email follow-up with David Miller (CloudScale Tech) was due 3 hours ago.",
            type = NotificationType.FOLLOW_UP,
            timestamp = now - (3 * hour),
            isRead = false,
            targetTab = "followups"
        )
    )
    dao.insertNotification(
        AppNotificationEntity(
            title = "Deal Closed Won! 🎉",
            message = "Amit closed deal with Heritage Organics (₹3,20,000). Target +22% achieved!",
            type = NotificationType.DEAL_WON,
            timestamp = now - (6 * hour),
            isRead = false,
            targetTab = "dashboard"
        )
    )
    dao.insertNotification(
        AppNotificationEntity(
            title = "New Inbound Lead Assigned",
            message = "Ramesh Gupta (Bharat Steel) assigned to Rahul Sharma.",
            type = NotificationType.LEAD_ASSIGNED,
            timestamp = now - (12 * hour),
            isRead = true,
            targetTab = "leads"
        )
    )

    // Seed Initial Business Cards / Physical Documents
    dao.insertLeadDocument(
        LeadDocumentEntity(
            leadId = id1,
            title = "Business Card - Ananya Roy",
            docType = "BUSINESS_CARD",
            filePath = "",
            fileUri = "",
            notes = "VP Supply Chain, Apex Logistics. Exchanged card at Bengaluru Tech Summit.",
            capturedAt = now - (2 * day)
        )
    )
    dao.insertLeadDocument(
        LeadDocumentEntity(
            leadId = id2,
            title = "NDA & Compliance Checklist",
            docType = "CONTRACT",
            filePath = "",
            fileUri = "",
            notes = "Signed mutual confidentiality document for cloud security module.",
            capturedAt = now - (1 * day)
        )
    )
}
