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
}
