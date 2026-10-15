package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FollowUpStatus
import com.example.data.model.LeadStage
import com.example.data.model.UserRole
import com.example.localization.LocalizedStrings
import com.example.ui.components.ChartBarData
import com.example.ui.components.RevenueTrendLineChart
import com.example.ui.components.TeamSalesBarChart
import com.example.ui.components.TrendPoint
import com.example.ui.viewmodel.CrmViewModel
import com.example.util.PdfReportGenerator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: CrmViewModel,
    strings: LocalizedStrings,
    onNavigateToLeads: () -> Unit,
    onNavigateToFollowUps: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onAddNewLeadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val leads by viewModel.allLeads.collectAsState()
    val followUps by viewModel.allFollowUps.collectAsState()
    val currentMember by viewModel.currentMember.collectAsState()
    val teamMembers = viewModel.teamMembers
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isGeneratingPdf by remember { mutableStateOf(false) }
    var exportedPdfFile by remember { mutableStateOf<File?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    // 0 = Admin / Team View, 1 = Personal User View
    var dashboardPerspective by remember(currentMember.role) {
        mutableIntStateOf(if (currentMember.role == com.example.data.model.RoleType.ADMIN || currentMember.role == com.example.data.model.RoleType.SALES_MANAGER) 0 else 1)
    }

    // Timeframe selector: 0 = Week, 1 = Month, 2 = Q3, 3 = Year
    var selectedTimeframe by remember { mutableIntStateOf(1) }

    fun triggerPdfExport() {
        if (isGeneratingPdf) return
        isGeneratingPdf = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val timeframeLabel = when (selectedTimeframe) {
                    0 -> "Weekly Performance"
                    1 -> "Monthly Review (Current)"
                    2 -> "Q3 2026 Quarter Review"
                    else -> "Annual Performance Review"
                }
                val file = PdfReportGenerator.generateTeamPerformanceReport(
                    context = context,
                    leads = leads,
                    followUps = followUps,
                    teamMembers = teamMembers,
                    currentUserName = currentMember.name,
                    timeframeTitle = timeframeLabel
                )
                withContext(Dispatchers.Main) {
                    exportedPdfFile = file
                    isGeneratingPdf = false
                    showExportDialog = true
                    viewModel.addTeamComment(
                        entityType = "REPORT",
                        entityId = 0,
                        comment = "Exported Team Performance PDF Report (${file.name}) for offline review & sharing"
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isGeneratingPdf = false
                }
            }
        }
    }

    // Calculations based on perspective
    val filteredLeads = remember(leads, dashboardPerspective, currentMember) {
        if (dashboardPerspective == 0) leads else leads.filter { it.assignedTo == currentMember.name }
    }

    val totalPipelineValue = filteredLeads
        .filter { it.stage != LeadStage.WON && it.stage != LeadStage.LOST }
        .sumOf { it.dealValue }

    val wonRevenue = filteredLeads
        .filter { it.stage == LeadStage.WON }
        .sumOf { it.dealValue }

    val totalClosedDeals = filteredLeads.count { it.stage == LeadStage.WON || it.stage == LeadStage.LOST }
    val wonDealsCount = filteredLeads.count { it.stage == LeadStage.WON }
    val winConversionRate = if (totalClosedDeals > 0) (wonDealsCount.toFloat() / totalClosedDeals * 100).toInt() else 68

    val activeDealsCount = filteredLeads.count { it.stage != LeadStage.WON && it.stage != LeadStage.LOST }
    val overdueFollowUpsCount = followUps.count { it.status == FollowUpStatus.OVERDUE }

    val targetRevenue = if (dashboardPerspective == 0) teamMembers.sumOf { it.targetRevenue } else currentMember.targetRevenue
    val achievedRevenue = if (dashboardPerspective == 0) teamMembers.sumOf { it.achievedRevenue } else currentMember.achievedRevenue
    val targetProgress = ((achievedRevenue / targetRevenue) * 100).toInt().coerceIn(0, 100)

    // Chart Data for Team Bar Chart (in Lakhs: ₹)
    val barChartData = remember(teamMembers) {
        teamMembers.map { member ->
            ChartBarData(
                label = member.name.split(" ").firstOrNull() ?: member.name,
                targetValue = (member.targetRevenue / 100000).toFloat(),
                achievedValue = (member.achievedRevenue / 100000).toFloat()
            )
        }
    }

    // Trend points for Area / Curve Chart
    val trendData = remember(selectedTimeframe) {
        when (selectedTimeframe) {
            0 -> listOf(
                TrendPoint("Mon", 1.2f),
                TrendPoint("Tue", 2.4f),
                TrendPoint("Wed", 3.1f),
                TrendPoint("Thu", 2.8f),
                TrendPoint("Fri", 4.5f),
                TrendPoint("Sat", 5.2f)
            )
            1 -> listOf(
                TrendPoint("Week 1", 3.5f),
                TrendPoint("Week 2", 6.2f),
                TrendPoint("Week 3", 8.9f),
                TrendPoint("Week 4", 14.5f)
            )
            2 -> listOf(
                TrendPoint("Jul", 8.2f),
                TrendPoint("Aug", 11.5f),
                TrendPoint("Sep", 16.5f),
                TrendPoint("Oct (Est)", 21.0f)
            )
            else -> listOf(
                TrendPoint("Q1", 28f),
                TrendPoint("Q2", 42f),
                TrendPoint("Q3", 56f),
                TrendPoint("Q4 (Target)", 75f)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome & Perspective Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_welcome_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Namaste, ${currentMember.name} 👋",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentMember.role.displayName} • NexusCRM Analytics",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentMember.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Admin View vs User View Segmented Switcher
                TabRow(
                    selectedTabIndex = dashboardPerspective,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("dashboard_view_mode_toggle")
                ) {
                    Tab(
                        selected = dashboardPerspective == 0,
                        onClick = { dashboardPerspective = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Admin / Team View", fontSize = 12.sp, fontWeight = if (dashboardPerspective == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = dashboardPerspective == 1,
                        onClick = { dashboardPerspective = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("My Sales Quota", fontSize = 12.sp, fontWeight = if (dashboardPerspective == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }
        }

        // Timeframe Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Week", "Month", "Q3 2026", "Year").forEachIndexed { index, label ->
                val isSelected = selectedTimeframe == index
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTimeframe = index }
                        .testTag("timeframe_chip_$label")
                ) {
                    Text(
                        text = label,
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        // Overdue Follow-up Alert Banner if any
        if (overdueFollowUpsCount > 0) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEE2E2),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToFollowUps() }
                    .testTag("dashboard_overdue_alert_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$overdueFollowUpsCount ${strings.overdueFollowUps}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "Tap to review clients waiting for action",
                            fontSize = 11.sp,
                            color = Color(0xFFB91C1C)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Key Metric Cards Grid & PDF Export Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (dashboardPerspective == 0) "Company Sales Overview" else "My Personal Quota & Pipeline",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // PDF Export Action Pill Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isGeneratingPdf) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clickable(enabled = !isGeneratingPdf) { triggerPdfExport() }
                    .testTag("dashboard_export_pdf_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.exportingPdf,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = strings.exportPdfReport,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Export PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiMetricCard(
                title = strings.totalPipeline,
                value = formatRupee(totalPipelineValue),
                subtitle = "$activeDealsCount active deals",
                icon = Icons.Default.TrendingUp,
                accentColor = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f),
                testTag = "kpi_pipeline_card"
            )

            KpiMetricCard(
                title = strings.wonRevenue,
                value = formatRupee(wonRevenue),
                subtitle = "$wonDealsCount deals closed",
                icon = Icons.Default.EmojiEvents,
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f),
                testTag = "kpi_won_revenue_card"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiMetricCard(
                title = strings.winConversion,
                value = "$winConversionRate%",
                subtitle = "Conversion efficiency",
                icon = Icons.Default.Speed,
                accentColor = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f),
                testTag = "kpi_conversion_rate_card"
            )

            KpiMetricCard(
                title = strings.activeDeals,
                value = "$activeDealsCount",
                subtitle = "In pipeline stages",
                icon = Icons.Default.AttachMoney,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                testTag = "kpi_active_deals_card"
            )
        }

        // Target vs Achieved Progress Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_target_progress_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (dashboardPerspective == 0) "Team Target Achievement" else "My Monthly Quota Target",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$targetProgress%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (targetProgress >= 75) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { targetProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF10B981),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Achieved: ${formatRupee(achievedRevenue)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Target: ${formatRupee(targetRevenue)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Recharts / D3 Style Visualization: Team Comparison Bar Chart (Admin View)
        if (dashboardPerspective == 0) {
            TeamSalesBarChart(data = barChartData)
        }

        // Recharts / D3 Style Visualization: Curved Area Trend Chart (Revenue Growth)
        RevenueTrendLineChart(
            title = if (dashboardPerspective == 0) "Team Revenue Trajectory (₹ Lakhs)" else "Personal Sales Pace (₹ Lakhs)",
            points = trendData
        )

        // Deal Stage Funnel Visualization
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_deals_funnel_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = strings.dealsFunnel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                LeadStage.entries.forEach { stage ->
                    val stageCount = filteredLeads.count { it.stage == stage }
                    val stageValue = filteredLeads.filter { it.stage == stage }.sumOf { it.dealValue }
                    val fraction = if (filteredLeads.isNotEmpty()) stageCount.toFloat() / filteredLeads.size else 0f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stage.label} ($stageCount)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatRupee(stageValue),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(stage.colorHex)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(stage.colorHex))
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions (2x2 Grid for comfort & accessibility)
        Text(
            text = strings.quickActions,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                label = strings.addLead,
                icon = Icons.Default.Add,
                color = MaterialTheme.colorScheme.primary,
                onClick = onAddNewLeadClick,
                modifier = Modifier.weight(1f),
                testTag = "quick_action_add_lead"
            )

            QuickActionButton(
                label = strings.scheduleFollowUp,
                icon = Icons.Default.CalendarMonth,
                color = Color(0xFF0284C7),
                onClick = onNavigateToFollowUps,
                modifier = Modifier.weight(1f),
                testTag = "quick_action_schedule_followup"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                label = strings.syncNow,
                icon = Icons.Default.CloudSync,
                color = Color(0xFF10B981),
                onClick = { viewModel.triggerCloudSync() },
                modifier = Modifier.weight(1f),
                testTag = "quick_action_sync_now"
            )

            QuickActionButton(
                label = strings.exportPdfReport,
                icon = Icons.Default.PictureAsPdf,
                color = Color(0xFFE11D48),
                onClick = { triggerPdfExport() },
                modifier = Modifier.weight(1f),
                testTag = "quick_action_export_pdf"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PDF Export Confirmation & Action Dialog
        if (showExportDialog && exportedPdfFile != null) {
            PdfExportSuccessDialog(
                pdfFile = exportedPdfFile!!,
                strings = strings,
                onDismiss = { showExportDialog = false },
                onShare = {
                    showExportDialog = false
                    PdfReportGenerator.sharePdfReport(context, exportedPdfFile!!)
                },
                onView = {
                    showExportDialog = false
                    PdfReportGenerator.viewPdfReport(context, exportedPdfFile!!)
                }
            )
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1
            )
        }
    }
}

fun formatRupee(amount: Double): String {
    return when {
        amount >= 10000000 -> String.format(Locale.getDefault(), "₹%.2f Cr", amount / 10000000)
        amount >= 100000 -> String.format(Locale.getDefault(), "₹%.1f L", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "₹%.0f K", amount / 1000)
        else -> String.format(Locale.getDefault(), "₹%.0f", amount)
    }
}

@Composable
fun PdfExportSuccessDialog(
    pdfFile: File,
    strings: LocalizedStrings,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onView: () -> Unit
) {
    val fileSizeBytes = pdfFile.length()
    val fileSizeFormatted = if (fileSizeBytes > 1024) "${fileSizeBytes / 1024} KB" else "$fileSizeBytes B"

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = strings.pdfReportReady,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Executive team performance and pipeline report has been generated successfully in A4 format.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Document Metadata Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pdfFile.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Size: $fileSizeFormatted • Location: Cache (Offline Available)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Text(
                    text = "You can share this document directly to WhatsApp, Email, Slack, or Google Drive, or view it on your device.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_share_pdf_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = strings.shareReport, fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onView,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_view_pdf_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "View PDF", fontSize = 13.sp)
            }
        }
    )
}

