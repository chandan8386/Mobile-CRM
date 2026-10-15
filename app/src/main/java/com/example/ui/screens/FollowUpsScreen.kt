package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import com.example.ui.components.TeamCalendarView
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import com.example.ai.AiSourceType
import com.example.ui.components.GeminiSummaryDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.FollowUpEntity
import com.example.data.local.LeadEntity
import com.example.data.model.FollowUpStatus
import com.example.data.model.FollowUpType
import com.example.localization.LocalizedStrings
import com.example.ui.viewmodel.CrmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FollowUpsScreen(
    viewModel: CrmViewModel,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier
) {
    val followUps by viewModel.allFollowUps.collectAsState()
    val leads by viewModel.allLeads.collectAsState()

    var isCalendarView by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFollowUpForGemini by remember { mutableStateOf<FollowUpEntity?>(null) }

    val now = System.currentTimeMillis()
    val dayMillis = 24 * 3600 * 1000L

    // Filter by tab
    val filteredFollowUps = remember(followUps, selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> followUps.filter { it.status == FollowUpStatus.PENDING && (it.scheduledDateTime - now) in 0..dayMillis } // Today
            1 -> followUps.filter { it.status == FollowUpStatus.PENDING && (it.scheduledDateTime - now) > dayMillis } // Upcoming
            2 -> followUps.filter { it.status == FollowUpStatus.OVERDUE || (it.status == FollowUpStatus.PENDING && it.scheduledDateTime < now) } // Overdue
            3 -> followUps.filter { it.status == FollowUpStatus.COMPLETED } // Completed
            else -> followUps
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // View Mode Toggle (List View vs Team Calendar)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_view_mode_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isCalendarView) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (!isCalendarView) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isCalendarView = false }
                            .testTag("toggle_list_view")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = if (!isCalendarView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.listView,
                                fontSize = 12.sp,
                                fontWeight = if (!isCalendarView) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isCalendarView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCalendarView) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isCalendarView) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isCalendarView = true }
                            .testTag("toggle_calendar_view")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (isCalendarView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.teamCalendar,
                                fontSize = 12.sp,
                                fontWeight = if (isCalendarView) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCalendarView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isCalendarView) {
                // Shared Team Calendar View (Tasks & Follow-up deadlines synced)
                TeamCalendarView(
                    viewModel = viewModel,
                    strings = strings,
                    modifier = Modifier.weight(1f),
                    onOpenGeminiForFollowUp = { selectedFollowUpForGemini = it }
                )
            } else {
                // Tab Header (Today, Upcoming, Overdue, Completed)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("follow_up_tabs")
                ) {
                    listOf(strings.today, strings.upcoming, strings.overdue, strings.completed).forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (index == 2 && selectedTabIndex == index) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (filteredFollowUps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No follow-ups in this tab",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredFollowUps, key = { it.id }) { item ->
                            FollowUpCard(
                                followUp = item,
                                strings = strings,
                                onComplete = { viewModel.markFollowUpCompleted(item) },
                                onDelete = { viewModel.deleteFollowUp(item.id) },
                                onOpenGemini = { selectedFollowUpForGemini = item }
                            )
                        }
                    }
                }
            }
        }

        if (!isCalendarView) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("fab_schedule_followup")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Schedule Follow-up")
            }
        }
    }

    if (showAddDialog) {
        ScheduleFollowUpDialog(
            leads = leads,
            strings = strings,
            onDismiss = { showAddDialog = false },
            onSave = { newFollowUp ->
                viewModel.saveFollowUp(newFollowUp)
                showAddDialog = false
            }
        )
    }

    selectedFollowUpForGemini?.let { followUp ->
        val associatedLead = leads.firstOrNull { it.id == followUp.leadId }
        val associatedDocs by viewModel.getDocumentsForLead(followUp.leadId).collectAsState(initial = emptyList())
        GeminiSummaryDialog(
            lead = associatedLead,
            initialNotes = followUp.notes.ifBlank { "Call interaction with ${followUp.leadName}. Follow-up type: ${followUp.type.label}." },
            initialSourceType = AiSourceType.CALL_NOTES,
            availableDocuments = associatedDocs,
            strings = strings,
            viewModel = viewModel,
            onDismiss = { selectedFollowUpForGemini = null }
        )
    }
}

@Composable
private fun FollowUpCard(
    followUp: FollowUpEntity,
    strings: LocalizedStrings,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onOpenGemini: () -> Unit
) {
    val context = LocalContext.current
    val isOverdue = followUp.status == FollowUpStatus.OVERDUE || (followUp.status == FollowUpStatus.PENDING && followUp.scheduledDateTime < System.currentTimeMillis())
    val dateFormat = SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(followUp.scheduledDateTime))

    val (icon, typeColor) = when (followUp.type) {
        FollowUpType.CALL -> Icons.Default.Call to Color(0xFF10B981)
        FollowUpType.MEETING -> Icons.Default.Groups to Color(0xFF3B82F6)
        FollowUpType.WHATSAPP -> Icons.Default.Chat to Color(0xFF25D366)
        FollowUpType.DEMO -> Icons.Default.Videocam to Color(0xFF8B5CF6)
        FollowUpType.EMAIL -> Icons.Default.Email to Color(0xFFF59E0B)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("followup_card_${followUp.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = typeColor, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = followUp.leadName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = followUp.type.label,
                            fontSize = 11.sp,
                            color = typeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Overdue or Status Pill
                if (isOverdue && followUp.status != FollowUpStatus.COMPLETED) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = "Overdue",
                            color = Color(0xFFDC2626),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (followUp.status == FollowUpStatus.COMPLETED) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFD1FAE5)
                    ) {
                        Text(
                            text = "Completed",
                            color = Color(0xFF059669),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scheduled Date & Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (followUp.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = followUp.notes,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (Gemini AI Summary, Call, Complete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gemini AI Quick Button
                OutlinedButton(
                    onClick = onOpenGemini,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("followup_ai_summary_${followUp.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Summary", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                }

                if (followUp.status != FollowUpStatus.COMPLETED) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (followUp.clientPhone.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${followUp.clientPhone}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.testTag("followup_call_btn_${followUp.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = strings.callClient, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = onComplete,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("followup_complete_btn_${followUp.id}")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = strings.markCompleted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleFollowUpDialog(
    leads: List<LeadEntity>,
    strings: LocalizedStrings,
    onDismiss: () -> Unit,
    onSave: (FollowUpEntity) -> Unit
) {
    var selectedLead by remember { mutableStateOf(leads.firstOrNull()) }
    var selectedType by remember { mutableStateOf(FollowUpType.CALL) }
    var selectedDaysAhead by remember { mutableIntStateOf(1) } // 0 = Today, 1 = Tomorrow, 3 = In 3 days
    var notes by remember { mutableStateOf("") }
    var leadMenuExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("schedule_followup_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.scheduleFollowUp,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Select Lead Dropdown
                Text("Select Client / Lead", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                ExposedDropdownMenuBox(
                    expanded = leadMenuExpanded,
                    onExpandedChange = { leadMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedLead?.clientName ?: "Select Lead",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = leadMenuExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = leadMenuExpanded,
                        onDismissRequest = { leadMenuExpanded = false }
                    ) {
                        leads.forEach { lead ->
                            DropdownMenuItem(
                                text = { Text("${lead.clientName} (${lead.companyName})") },
                                onClick = {
                                    selectedLead = lead
                                    leadMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Type of interaction
                Text("Interaction Type", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FollowUpType.entries.forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = type }
                        ) {
                            Text(
                                text = type.name.lowercase().capitalize(Locale.getDefault()),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Quick Scheduled Date
                Text("When to follow up?", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "Today", 1 to "Tomorrow", 3 to "+3 Days", 7 to "+1 Week").forEach { (days, label) ->
                        val isSelected = selectedDaysAhead == days
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDaysAhead = days }
                        ) {
                            Text(
                                text = label,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Agenda / Reminder Notes") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val scheduledTime = System.currentTimeMillis() + (selectedDaysAhead * 24L * 3600 * 1000)
                            onSave(
                                FollowUpEntity(
                                    leadId = selectedLead?.id ?: 0L,
                                    leadName = selectedLead?.let { "${it.clientName} (${it.companyName})" } ?: "General Follow-up",
                                    clientPhone = selectedLead?.phone ?: "",
                                    scheduledDateTime = scheduledTime,
                                    type = selectedType,
                                    status = FollowUpStatus.PENDING,
                                    notes = notes
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_followup_button")
                    ) {
                        Text(strings.save)
                    }
                }
            }
        }
    }
}
