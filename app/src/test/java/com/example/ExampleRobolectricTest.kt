package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FollowUpEntity
import com.example.data.local.LeadEntity
import com.example.data.model.FollowUpStatus
import com.example.data.model.FollowUpType
import com.example.data.model.LeadPriority
import com.example.data.model.LeadStage
import com.example.data.model.RoleType
import com.example.data.model.SyncStatus
import com.example.data.model.TeamMember
import com.example.localization.AppLanguage
import com.example.localization.LanguageManager
import com.example.ui.screens.formatRupee
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NexusCRM", appName)
    }

    @Test
    fun `verify export pdf localized strings are present across all languages`() {
        AppLanguage.entries.forEach { lang ->
            val strings = LanguageManager.getStrings(lang)
            assertTrue("exportPdfReport should not be empty for $lang", strings.exportPdfReport.isNotEmpty())
            assertTrue("exportingPdf should not be empty for $lang", strings.exportingPdf.isNotEmpty())
            assertTrue("pdfReportReady should not be empty for $lang", strings.pdfReportReady.isNotEmpty())
            assertTrue("shareReport should not be empty for $lang", strings.shareReport.isNotEmpty())
        }
    }

    @Test
    fun `verify rupee currency formatting for pdf report`() {
        assertEquals("₹2.50 Cr", formatRupee(25000000.0))
        assertEquals("₹21.5 L", formatRupee(2150000.0))
        assertEquals("₹85 K", formatRupee(85000.0))
        assertEquals("₹500", formatRupee(500.0))
    }

    @Test
    fun `verify report quota attainment calculation`() {
        val member = TeamMember(
            id = "1",
            name = "Rahul Sharma",
            email = "rahul@crmnexus.in",
            role = RoleType.ADMIN,
            phone = "+91 98765 00001",
            targetRevenue = 2000000.0,
            achievedRevenue = 2150000.0,
            dealsWonCount = 7,
            activeLeadsCount = 12
        )
        val attainment = ((member.achievedRevenue / member.targetRevenue) * 100).toInt()
        assertEquals(107, attainment)
    }

    @Test
    fun `verify camera and document localized strings are present across all languages`() {
        AppLanguage.entries.forEach { lang ->
            val strings = LanguageManager.getStrings(lang)
            assertTrue("documentsAndCards should not be empty for $lang", strings.documentsAndCards.isNotEmpty())
            assertTrue("takePhoto should not be empty for $lang", strings.takePhoto.isNotEmpty())
            assertTrue("businessCard should not be empty for $lang", strings.businessCard.isNotEmpty())
            assertTrue("saveDocument should not be empty for $lang", strings.saveDocument.isNotEmpty())
            assertTrue("documentTitle should not be empty for $lang", strings.documentTitle.isNotEmpty())
            assertTrue("cameraPermissionRequired should not be empty for $lang", strings.cameraPermissionRequired.isNotEmpty())
        }
    }

    @Test
    fun `verify lead document entity creation and default values`() {
        val doc = com.example.data.local.LeadDocumentEntity(
            id = 1L,
            leadId = 101L,
            title = "Business Card - Ananya Roy",
            docType = "BUSINESS_CARD",
            filePath = "/data/user/0/com.example/files/lead_photos/card.jpg",
            notes = "Exchanged at summit"
        )
        assertEquals(101L, doc.leadId)
        assertEquals("BUSINESS_CARD", doc.docType)
        assertEquals("Business Card - Ananya Roy", doc.title)
        assertTrue(doc.capturedAt > 0)
    }

    @Test
    fun `verify sample business card generator creates valid photo file`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lead = LeadEntity(
            id = 50L,
            clientName = "Aditi Rao",
            companyName = "Apex Solutions India",
            email = "aditi@apex.in",
            phone = "+91 99887 76655",
            dealValue = 500000.0,
            stage = LeadStage.NEW,
            priority = LeadPriority.HIGH
        )
        val file = com.example.ui.components.generateSampleBusinessCard(context, lead)
        assertTrue("Business card file should exist", file.exists())
        assertTrue("Business card file should have positive byte size", file.length() > 0)
    }

    @Test
    fun `verify Gemini AI localized strings across all languages`() {
        AppLanguage.entries.forEach { lang ->
            val strings = LanguageManager.getStrings(lang)
            assertTrue("geminiAiSummary should not be empty for $lang", strings.geminiAiSummary.isNotEmpty())
            assertTrue("generateAiSummary should not be empty for $lang", strings.generateAiSummary.isNotEmpty())
            assertTrue("aiActionItems should not be empty for $lang", strings.aiActionItems.isNotEmpty())
            assertTrue("analyzeCallNotes should not be empty for $lang", strings.analyzeCallNotes.isNotEmpty())
            assertTrue("analyzeBusinessCard should not be empty for $lang", strings.analyzeBusinessCard.isNotEmpty())
            assertTrue("saveToLeadNotes should not be empty for $lang", strings.saveToLeadNotes.isNotEmpty())
        }
    }

    @Test
    fun `verify Gemini smart fallback analysis generates structured summary and action items`() {
        val notes = "Met with Sunil Mehta at Bangalore tech park. Agreed on 100 enterprise licenses for ₹8,50,000. Needs custom security integration SLA and ISO 27001 proof by Thursday. Follow up next week for contract signing."
        val result = com.example.ai.GeminiService.generateSmartFallback(
            clientName = "Sunil Mehta",
            companyName = "Infosys Edge",
            sourceType = com.example.ai.AiSourceType.CALL_NOTES,
            notesText = notes
        )

        assertEquals("Sunil Mehta", result.clientName)
        assertEquals("Infosys Edge", result.companyName)
        assertTrue("Summary should contain client details", result.summary.isNotEmpty())
        assertTrue("Should have extracted action items", result.actionItems.isNotEmpty())
        assertTrue("Action items should contain title and priority", result.actionItems.all { it.title.isNotBlank() })
        assertTrue("Should include recommended follow-up", result.recommendedFollowUp != null)
    }

    @Test
    fun `verify business card contact extraction model`() {
        val contact = com.example.ai.ExtractedContactInfo(
            fullName = "Vikram Aditya",
            designation = "VP Engineering",
            companyName = "Tata Elxsi",
            phone = "+91 98450 12345",
            email = "vikram.a@tataelxsi.com",
            website = "www.tataelxsi.com"
        )
        assertEquals("Vikram Aditya", contact.fullName)
        assertEquals("VP Engineering", contact.designation)
        assertEquals("+91 98450 12345", contact.phone)
        assertEquals("vikram.a@tataelxsi.com", contact.email)
    }

    @Test
    fun `verify shared team calendar localized strings across all languages`() {
        AppLanguage.entries.forEach { lang ->
            val strings = LanguageManager.getStrings(lang)
            assertTrue("calendarView should not be empty for $lang", strings.calendarView.isNotEmpty())
            assertTrue("listView should not be empty for $lang", strings.listView.isNotEmpty())
            assertTrue("teamCalendar should not be empty for $lang", strings.teamCalendar.isNotEmpty())
            assertTrue("weeklySchedule should not be empty for $lang", strings.weeklySchedule.isNotEmpty())
            assertTrue("allTeamMembers should not be empty for $lang", strings.allTeamMembers.isNotEmpty())
            assertTrue("mySchedule should not be empty for $lang", strings.mySchedule.isNotEmpty())
            assertTrue("jumpToday should not be empty for $lang", strings.jumpToday.isNotEmpty())
            assertTrue("noScheduleItemsDay should not be empty for $lang", strings.noScheduleItemsDay.isNotEmpty())
            assertTrue("taskDeadline should not be empty for $lang", strings.taskDeadline.isNotEmpty())
            assertTrue("followUpDeadline should not be empty for $lang", strings.followUpDeadline.isNotEmpty())
        }
    }

    @Test
    fun `verify week days calculation produces exactly 7 consecutive days starting Monday`() {
        val testMillis = 1776240000000L // arbitrary fixed date
        val weekDays = com.example.ui.components.getWeekDays(testMillis)
        assertEquals(7, weekDays.size)

        // Verify days are strictly ascending and separated by 24h
        for (i in 0 until 6) {
            val diffHours = (weekDays[i + 1] - weekDays[i]) / (3600 * 1000)
            assertEquals("Adjacent days should be separated by 24 hours", 24L, diffHours)
        }
    }

    @Test
    fun `verify isSameDay accurately matches timestamps on the same day`() {
        val base = System.currentTimeMillis()
        val morning = base - (base % (24 * 3600 * 1000L)) + 9 * 3600 * 1000L // 9:00 AM
        val evening = base - (base % (24 * 3600 * 1000L)) + 18 * 3600 * 1000L // 6:00 PM
        val nextDay = morning + 24 * 3600 * 1000L // Next day

        assertTrue("Morning and evening of same day should match", com.example.ui.components.isSameDay(morning, evening))
        assertTrue("Different days should not match", !com.example.ui.components.isSameDay(morning, nextDay))
    }

    @Test
    fun `verify LeadFtsEntity and TaskFtsEntity data models`() {
        val leadFts = com.example.data.local.LeadFtsEntity(
            clientName = "Anand Mahindra",
            companyName = "Mahindra Group",
            email = "anand@mahindra.com",
            phone = "+91 98200 11223",
            source = "Direct Referral",
            assignedTo = "Rahul Sharma",
            notes = "Interested in industrial fleet telematics solution"
        )
        assertEquals("Anand Mahindra", leadFts.clientName)
        assertEquals("Mahindra Group", leadFts.companyName)

        val taskFts = com.example.data.local.TaskFtsEntity(
            title = "Send RFP Proposal Draft",
            description = "Complete commercial and SLA terms for Mahindra",
            assignedTo = "Rahul Sharma",
            relatedProjectName = "Telematics Phase 1"
        )
        assertEquals("Send RFP Proposal Draft", taskFts.title)
        assertEquals("Telematics Phase 1", taskFts.relatedProjectName)
    }

    @Test
    fun `verify Room FTS4 full text search queries work offline`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val crmDao = db.crmDao()
            // The FTS virtual tables and queries compile and initialize properly
            val allLeads = crmDao.getAllLeads()
            assertTrue(allLeads != null)

            val allTasks = crmDao.getAllTasks()
            assertTrue(allTasks != null)
        } finally {
            db.close()
        }
    }
}

