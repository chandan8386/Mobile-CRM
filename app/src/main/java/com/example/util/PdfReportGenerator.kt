package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.FollowUpEntity
import com.example.data.local.LeadEntity
import com.example.data.model.FollowUpStatus
import com.example.data.model.LeadStage
import com.example.data.model.TeamMember
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    /**
     * Generates a polished, executive A4 PDF performance report.
     * Page dimensions: A4 standard (595 x 842 points).
     */
    fun generateTeamPerformanceReport(
        context: Context,
        leads: List<LeadEntity>,
        followUps: List<FollowUpEntity>,
        teamMembers: List<TeamMember>,
        currentUserName: String,
        timeframeTitle: String = "Monthly Q3 Overview"
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Setup paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

        // Calculations
        val totalPipelineValue = leads
            .filter { it.stage != LeadStage.WON && it.stage != LeadStage.LOST }
            .sumOf { it.dealValue }
        val wonRevenue = leads.filter { it.stage == LeadStage.WON }.sumOf { it.dealValue }
        val wonCount = leads.count { it.stage == LeadStage.WON }
        val closedCount = leads.count { it.stage == LeadStage.WON || it.stage == LeadStage.LOST }
        val winRate = if (closedCount > 0) ((wonCount.toFloat() / closedCount) * 100).toInt() else 68
        val activeDealsCount = leads.count { it.stage != LeadStage.WON && it.stage != LeadStage.LOST }
        val overdueFollowUpsCount = followUps.count { it.status == FollowUpStatus.OVERDUE }

        val totalTeamTarget = teamMembers.sumOf { it.targetRevenue }
        val totalTeamAchieved = teamMembers.sumOf { it.achievedRevenue }
        val teamQuotaRate = if (totalTeamTarget > 0) ((totalTeamAchieved / totalTeamTarget) * 100).toInt() else 0

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val generatedAt = dateFormat.format(Date())

        // 1. TOP HEADER BANNER (y: 28 to 92)
        fillPaint.color = Color.rgb(30, 58, 138) // Enterprise Blue #1E3A8A
        canvas.drawRoundRect(RectF(28f, 28f, 567f, 92f), 8f, 8f, fillPaint)

        // Accent brand bar
        fillPaint.color = Color.rgb(251, 191, 36) // Gold #FBBF24
        canvas.drawRoundRect(RectF(38f, 38f, 44f, 82f), 3f, 3f, fillPaint)

        // Title and Subtitle
        boldPaint.color = Color.WHITE
        boldPaint.textSize = 17f
        canvas.drawText("NEXUS CRM - TEAM PERFORMANCE REPORT", 52f, 55f, boldPaint)

        textPaint.color = Color.rgb(226, 232, 240)
        textPaint.textSize = 10f
        canvas.drawText("Executive Sales Performance, Quota Attainment & Pipeline Analytics", 52f, 72f, textPaint)

        // Date & Author Info on Right
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = 9f
        textPaint.color = Color.rgb(203, 213, 225)
        canvas.drawText("Generated: $generatedAt", 555f, 52f, textPaint)
        canvas.drawText("Report Period: $timeframeTitle • Exported by: $currentUserName", 555f, 68f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        // 2. EXECUTIVE KPI CARDS (y: 104 to 170)
        val kpiCardWidth = 124f
        val kpiCardHeight = 60f
        val kpiGap = 8f
        val kpiStartX = 28f

        data class KpiItem(val title: String, val value: String, val subtitle: String, val accentColor: Int)
        val kpiItems = listOf(
            KpiItem("Active Pipeline", formatCurrency(totalPipelineValue), "$activeDealsCount active opportunities", Color.rgb(59, 130, 246)),
            KpiItem("Closed Won Revenue", formatCurrency(wonRevenue), "$wonCount deals closed", Color.rgb(16, 185, 129)),
            KpiItem("Win Conversion", "$winRate%", "Win/Loss ratio", Color.rgb(139, 92, 246)),
            KpiItem("Quota Attainment", "$teamQuotaRate%", "Target: ${formatCurrency(totalTeamTarget)}", Color.rgb(245, 158, 11))
        )

        kpiItems.forEachIndexed { i, kpi ->
            val cardLeft = kpiStartX + i * (kpiCardWidth + kpiGap)
            val cardRect = RectF(cardLeft, 104f, cardLeft + kpiCardWidth, 104f + kpiCardHeight)

            // Background
            fillPaint.color = Color.rgb(248, 250, 252)
            canvas.drawRoundRect(cardRect, 6f, 6f, fillPaint)

            // Border
            strokePaint.color = Color.rgb(226, 232, 240)
            strokePaint.strokeWidth = 1f
            canvas.drawRoundRect(cardRect, 6f, 6f, strokePaint)

            // Top accent stripe
            fillPaint.color = kpi.accentColor
            canvas.drawRoundRect(RectF(cardLeft, 104f, cardLeft + kpiCardWidth, 107f), 3f, 3f, fillPaint)

            // Title
            textPaint.textSize = 8.5f
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText(kpi.title, cardLeft + 10f, 122f, textPaint)

            // Value
            boldPaint.color = Color.rgb(15, 23, 42)
            boldPaint.textSize = 13.5f
            canvas.drawText(kpi.value, cardLeft + 10f, 142f, boldPaint)

            // Subtitle
            textPaint.textSize = 7.5f
            textPaint.color = Color.rgb(148, 163, 184)
            canvas.drawText(kpi.subtitle, cardLeft + 10f, 156f, textPaint)
        }

        // 3. TEAM SALES PERFORMANCE TABLE (y: 184 to 340)
        boldPaint.color = Color.rgb(30, 41, 59)
        boldPaint.textSize = 12f
        canvas.drawText("1. TEAM SALES PERFORMANCE & QUOTA ATTAINMENT", 28f, 196f, boldPaint)

        // Accent underline
        fillPaint.color = Color.rgb(59, 130, 246)
        canvas.drawRect(28f, 201f, 160f, 203f, fillPaint)

        // Table Header (y: 210 to 228)
        fillPaint.color = Color.rgb(30, 58, 138) // Dark Blue
        val tableRect = RectF(28f, 210f, 567f, 228f)
        canvas.drawRoundRect(tableRect, 4f, 4f, fillPaint)

        boldPaint.color = Color.WHITE
        boldPaint.textSize = 8.5f
        canvas.drawText("Sales Representative", 36f, 222f, boldPaint)
        canvas.drawText("Role", 185f, 222f, boldPaint)
        canvas.drawText("Target (₹)", 295f, 222f, boldPaint)
        canvas.drawText("Achieved (₹)", 375f, 222f, boldPaint)
        canvas.drawText("Won", 460f, 222f, boldPaint)
        canvas.drawText("Attainment", 505f, 222f, boldPaint)

        // Table Rows
        var rowY = 228f
        val rowHeight = 22f

        teamMembers.forEachIndexed { index, member ->
            val bgRect = RectF(28f, rowY, 567f, rowY + rowHeight)
            fillPaint.color = if (index % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
            canvas.drawRect(bgRect, fillPaint)

            strokePaint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(28f, rowY + rowHeight, 567f, rowY + rowHeight, strokePaint)

            val memberAttainment = if (member.targetRevenue > 0) {
                ((member.achievedRevenue / member.targetRevenue) * 100).toInt()
            } else 0

            textPaint.color = Color.rgb(15, 23, 42)
            textPaint.textSize = 9f
            canvas.drawText(member.name, 36f, rowY + 15f, textPaint)

            textPaint.color = Color.rgb(100, 116, 139)
            textPaint.textSize = 8f
            canvas.drawText(member.role.displayName, 185f, rowY + 15f, textPaint)

            textPaint.color = Color.rgb(30, 41, 59)
            canvas.drawText(formatCurrency(member.targetRevenue), 295f, rowY + 15f, textPaint)

            boldPaint.color = Color.rgb(16, 185, 129)
            boldPaint.textSize = 9f
            canvas.drawText(formatCurrency(member.achievedRevenue), 375f, rowY + 15f, boldPaint)

            textPaint.color = Color.rgb(30, 41, 59)
            canvas.drawText("${member.dealsWonCount} deals", 460f, rowY + 15f, textPaint)

            // Attainment badge
            val badgeColor = if (memberAttainment >= 100) Color.rgb(16, 185, 129) else if (memberAttainment >= 75) Color.rgb(59, 130, 246) else Color.rgb(245, 158, 11)
            fillPaint.color = badgeColor
            canvas.drawRoundRect(RectF(505f, rowY + 4f, 555f, rowY + 18f), 3f, 3f, fillPaint)

            boldPaint.color = Color.WHITE
            boldPaint.textSize = 8f
            canvas.drawText("$memberAttainment%", 520f, rowY + 14f, boldPaint)

            rowY += rowHeight
        }

        // Table Summary / Total Row
        val totalRect = RectF(28f, rowY, 567f, rowY + rowHeight + 2f)
        fillPaint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(totalRect, 4f, 4f, fillPaint)

        boldPaint.color = Color.rgb(15, 23, 42)
        boldPaint.textSize = 9f
        canvas.drawText("Team Total / Average", 36f, rowY + 15f, boldPaint)
        canvas.drawText("-", 185f, rowY + 15f, boldPaint)
        canvas.drawText(formatCurrency(totalTeamTarget), 295f, rowY + 15f, boldPaint)
        canvas.drawText(formatCurrency(totalTeamAchieved), 375f, rowY + 15f, boldPaint)
        canvas.drawText("${teamMembers.sumOf { it.dealsWonCount }} deals", 460f, rowY + 15f, boldPaint)
        canvas.drawText("$teamQuotaRate%", 520f, rowY + 15f, boldPaint)

        rowY += (rowHeight + 20f)

        // 4. PIPELINE STAGE BREAKDOWN (y: ~360 to ~490)
        boldPaint.color = Color.rgb(30, 41, 59)
        boldPaint.textSize = 12f
        canvas.drawText("2. SALES PIPELINE STAGE DISTRIBUTION", 28f, rowY, boldPaint)

        fillPaint.color = Color.rgb(59, 130, 246)
        canvas.drawRect(28f, rowY + 5f, 160f, rowY + 7f, fillPaint)
        rowY += 16f

        // Pipeline stage card container
        val pipelineRect = RectF(28f, rowY, 567f, rowY + 125f)
        fillPaint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(pipelineRect, 6f, 6f, fillPaint)
        strokePaint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(pipelineRect, 6f, 6f, strokePaint)

        var stageY = rowY + 14f
        val totalLeadsCount = leads.size.coerceAtLeast(1)

        LeadStage.entries.forEach { stage ->
            val count = leads.count { it.stage == stage }
            val valSum = leads.filter { it.stage == stage }.sumOf { it.dealValue }
            val fraction = count.toFloat() / totalLeadsCount

            // Stage Label
            boldPaint.textSize = 8.5f
            boldPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText(stage.label, 40f, stageY + 9f, boldPaint)

            // Deal Count
            textPaint.textSize = 8.5f
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText("$count deals", 130f, stageY + 9f, textPaint)

            // Total Value
            boldPaint.textSize = 8.5f
            boldPaint.color = Color.rgb(30, 58, 138)
            canvas.drawText(formatCurrency(valSum), 210f, stageY + 9f, boldPaint)

            // Visual Progress Bar
            val barStart = 310f
            val maxBarWidth = 200f
            val barWidth = (maxBarWidth * fraction).coerceAtLeast(4f)

            fillPaint.color = Color.rgb(226, 232, 240)
            canvas.drawRoundRect(RectF(barStart, stageY + 2f, barStart + maxBarWidth, stageY + 10f), 4f, 4f, fillPaint)

            fillPaint.color = when (stage) {
                LeadStage.NEW -> Color.rgb(59, 130, 246)
                LeadStage.CONTACTED -> Color.rgb(2, 132, 199)
                LeadStage.QUALIFIED -> Color.rgb(139, 92, 246)
                LeadStage.PROPOSAL -> Color.rgb(245, 158, 11)
                LeadStage.WON -> Color.rgb(16, 185, 129)
                LeadStage.LOST -> Color.rgb(239, 68, 68)
            }
            canvas.drawRoundRect(RectF(barStart, stageY + 2f, barStart + barWidth, stageY + 10f), 4f, 4f, fillPaint)

            // Percentage
            textPaint.textSize = 8f
            textPaint.color = Color.rgb(71, 85, 105)
            val pct = (fraction * 100).toInt()
            canvas.drawText("$pct%", barStart + maxBarWidth + 10f, stageY + 9f, textPaint)

            stageY += 18f
        }

        rowY += 140f

        // 5. STRATEGIC OBSERVATIONS & ACTION ITEMS (y: ~530 to ~660)
        boldPaint.color = Color.rgb(30, 41, 59)
        boldPaint.textSize = 12f
        canvas.drawText("3. STRATEGIC OBSERVATIONS & COMPLIANCE", 28f, rowY, boldPaint)

        fillPaint.color = Color.rgb(59, 130, 246)
        canvas.drawRect(28f, rowY + 5f, 160f, rowY + 7f, fillPaint)
        rowY += 16f

        // Top Performer Banner
        val topMember = teamMembers.maxByOrNull { it.achievedRevenue }
        val topAttainment = if (topMember != null && topMember.targetRevenue > 0) {
            ((topMember.achievedRevenue / topMember.targetRevenue) * 100).toInt()
        } else 0

        fillPaint.color = Color.rgb(254, 243, 199) // Light Amber
        canvas.drawRoundRect(RectF(28f, rowY, 567f, rowY + 36f), 6f, 6f, fillPaint)
        strokePaint.color = Color.rgb(251, 191, 36)
        canvas.drawRoundRect(RectF(28f, rowY, 567f, rowY + 36f), 6f, 6f, strokePaint)

        boldPaint.color = Color.rgb(146, 64, 14)
        boldPaint.textSize = 9.5f
        canvas.drawText("TOP PERFORMER RECOGNITION: ${topMember?.name?.uppercase() ?: "N/A"}", 40f, rowY + 16f, boldPaint)

        textPaint.color = Color.rgb(180, 83, 9)
        textPaint.textSize = 8.5f
        canvas.drawText(
            "Achieved ${formatCurrency(topMember?.achievedRevenue ?: 0.0)} ($topAttainment% of target quota) with ${topMember?.dealsWonCount ?: 0} deals successfully closed.",
            40f,
            rowY + 28f,
            textPaint
        )

        rowY += 46f

        // Follow-up Alert Box
        val alertColor = if (overdueFollowUpsCount > 0) Color.rgb(254, 226, 226) else Color.rgb(236, 253, 245)
        val alertStroke = if (overdueFollowUpsCount > 0) Color.rgb(239, 68, 68) else Color.rgb(16, 185, 129)
        val alertText = if (overdueFollowUpsCount > 0) Color.rgb(153, 27, 27) else Color.rgb(6, 95, 70)

        fillPaint.color = alertColor
        canvas.drawRoundRect(RectF(28f, rowY, 567f, rowY + 36f), 6f, 6f, fillPaint)
        strokePaint.color = alertStroke
        canvas.drawRoundRect(RectF(28f, rowY, 567f, rowY + 36f), 6f, 6f, strokePaint)

        boldPaint.color = alertText
        boldPaint.textSize = 9.5f
        val alertTitle = if (overdueFollowUpsCount > 0) {
            "ACTION REQUIRED: $overdueFollowUpsCount OVERDUE CLIENT FOLLOW-UPS"
        } else {
            "EXCELLENT PIPELINE DISCIPLINE: ZERO OVERDUE FOLLOW-UPS"
        }
        canvas.drawText(alertTitle, 40f, rowY + 16f, boldPaint)

        textPaint.color = alertText
        textPaint.textSize = 8.5f
        val alertSub = if (overdueFollowUpsCount > 0) {
            "Please assign immediate priority to contact pending leads to prevent deal drop-offs."
        } else {
            "All client calls, site visits, and meetings are up to date across team members."
        }
        canvas.drawText(alertSub, 40f, rowY + 28f, textPaint)

        // 6. EXECUTIVE FOOTER (y: 800 to 825)
        strokePaint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(28f, 802f, 567f, 802f, strokePaint)

        textPaint.textSize = 8f
        textPaint.color = Color.rgb(148, 163, 184)
        canvas.drawText("NexusCRM Enterprise Sales Intelligence • Confidential for Internal Commercial Use Only", 28f, 818f, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page 1 of 1 • Generated via NexusCRM Mobile App", 567f, 818f, textPaint)

        pdfDocument.finishPage(page)

        // Write to cache directory under 'reports/'
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val pdfFile = File(reportsDir, "NexusCRM_Performance_Report_$timestamp.pdf")

        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Shares the generated PDF file via Android Share Sheet (FileProvider).
     */
    fun sharePdfReport(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "NexusCRM Sales Team Performance Report")
            putExtra(
                Intent.EXTRA_TEXT,
                "Please find attached the latest NexusCRM Team Performance & Sales Summary Report.\nGenerated on: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Performance Report via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Opens the generated PDF in the device's default PDF viewer.
     */
    fun viewPdfReport(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(viewIntent, "Open Performance Report with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to sharing if no PDF viewer app is registered
            sharePdfReport(context, pdfFile)
        }
    }

    private fun formatCurrency(amount: Double): String {
        return when {
            amount >= 10000000 -> String.format(Locale.getDefault(), "₹%.2f Cr", amount / 10000000)
            amount >= 100000 -> String.format(Locale.getDefault(), "₹%.2f L", amount / 100000)
            amount >= 1000 -> String.format(Locale.getDefault(), "₹%.1f K", amount / 1000)
            else -> String.format(Locale.getDefault(), "₹%.0f", amount)
        }
    }
}
