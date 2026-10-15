package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.Brush
import com.example.ui.components.GeminiSummaryDialog
import com.example.ui.components.LeadDocumentsSection
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.ActivityLogEntity
import com.example.data.local.LeadEntity
import com.example.data.model.LeadPriority
import com.example.data.model.LeadStage
import com.example.data.model.SyncStatus
import com.example.data.model.UserRole
import com.example.localization.LocalizedStrings
import com.example.ui.viewmodel.CrmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeadsScreen(
    viewModel: CrmViewModel,
    strings: LocalizedStrings,
    showAddDialogInitially: Boolean = false,
    modifier: Modifier = Modifier
) {
    val leads by viewModel.visibleLeads.collectAsState()
    val searchQuery by viewModel.leadSearchQuery.collectAsState()
    val selectedStage by viewModel.selectedStageFilter.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    val recentLogs by viewModel.recentActivityLogs.collectAsState()

    var showAddDialog by remember { mutableStateOf(showAddDialogInitially) }
    var selectedLeadForDetails by remember { mutableStateOf<LeadEntity?>(null) }
    var selectedLeadForGemini by remember { mutableStateOf<LeadEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setLeadSearchQuery(it) },
                placeholder = { Text(strings.searchLeads, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setLeadSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leads_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stage Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StageFilterChip(
                    label = "All (${leads.size})",
                    isSelected = selectedStage == null,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.setStageFilter(null) },
                    testTag = "stage_filter_all"
                )

                LeadStage.entries.forEach { stage ->
                    val isSelected = selectedStage == stage
                    StageFilterChip(
                        label = stage.label,
                        isSelected = isSelected,
                        color = Color(stage.colorHex),
                        onClick = { viewModel.setStageFilter(stage) },
                        testTag = "stage_filter_${stage.name.lowercase()}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Leads List
            if (leads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No leads found",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + to add a new sales lead",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
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
                    items(leads, key = { it.id }) { lead ->
                        val docCount by viewModel.getDocumentCountForLead(lead.id).collectAsState(initial = 0)
                        LeadCard(
                            lead = lead,
                            documentCount = docCount,
                            onClick = { selectedLeadForDetails = lead },
                            onOpenGemini = { selectedLeadForGemini = lead }
                        )
                    }
                }
            }
        }

        // FAB to add lead
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_lead")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Lead")
        }
    }

    // Lead Details Dialog
    selectedLeadForDetails?.let { currentLead ->
        LeadDetailsDialog(
            lead = currentLead,
            viewModel = viewModel,
            strings = strings,
            canDelete = permissions.canDeleteLeads,
            activityLogs = recentLogs.filter { it.entityType == "LEAD" && it.entityId == currentLead.id },
            onDismiss = { selectedLeadForDetails = null },
            onAdvanceStage = { nextStage ->
                viewModel.advanceLeadStage(currentLead, nextStage)
                selectedLeadForDetails = currentLead.copy(stage = nextStage)
            },
            onAddComment = { comment ->
                viewModel.addTeamComment("LEAD", currentLead.id, comment)
            },
            onDelete = {
                viewModel.deleteLead(currentLead.id)
                selectedLeadForDetails = null
            }
        )
    }

    // Gemini AI Summary Dialog
    selectedLeadForGemini?.let { geminiLead ->
        val docs by viewModel.getDocumentsForLead(geminiLead.id).collectAsState(initial = emptyList())
        GeminiSummaryDialog(
            lead = geminiLead,
            initialNotes = geminiLead.notes,
            availableDocuments = docs,
            strings = strings,
            viewModel = viewModel,
            onDismiss = { selectedLeadForGemini = null }
        )
    }

    // Add Lead Dialog
    if (showAddDialog) {
        AddLeadDialog(
            strings = strings,
            currentRepName = viewModel.currentMember.value.name,
            onDismiss = { showAddDialog = false },
            onSave = { newLead ->
                viewModel.saveLead(newLead)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun StageFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color else color.copy(alpha = 0.1f),
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun LeadCard(
    lead: LeadEntity,
    documentCount: Int = 0,
    onClick: () -> Unit,
    onOpenGemini: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("lead_card_${lead.id}")
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
                // Client Name & Company
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.clientName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = lead.companyName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stage Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(lead.stage.colorHex).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = lead.stage.label,
                        color = Color(lead.stage.colorHex),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Value & Priority & Rep
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${lead.currency} ${formatRupeeNumber(lead.dealValue)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sync icon
                    if (lead.syncStatus == SyncStatus.PENDING_SYNC) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Pending Sync",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Attached Documents / Business Cards Count Badge
                    if (documentCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Documents",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$documentCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    // Priority Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(lead.priority.colorHex).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = lead.priority.label,
                            color = Color(lead.priority.colorHex),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Assigned Rep
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = lead.assignedTo.split(" ").firstOrNull() ?: lead.assignedTo,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Gemini AI Sparkle chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.12f),
                        modifier = Modifier
                            .clickable { onOpenGemini() }
                            .testTag("lead_ai_chip_${lead.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeadDetailsDialog(
    lead: LeadEntity,
    viewModel: CrmViewModel,
    strings: LocalizedStrings,
    canDelete: Boolean,
    activityLogs: List<ActivityLogEntity>,
    onDismiss: () -> Unit,
    onAdvanceStage: (LeadStage) -> Unit,
    onAddComment: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var newCommentText by remember { mutableStateOf("") }
    var showGeminiDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("lead_details_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lead.clientName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = lead.companyName,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(lead.stage.colorHex).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = lead.stage.label,
                            color = Color(lead.stage.colorHex),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // One-touch Communication Buttons (Call, WhatsApp, Email)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CommunicationButton(
                        label = strings.callClient,
                        icon = Icons.Default.Call,
                        color = Color(0xFF10B981),
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "comm_call_button"
                    )

                    CommunicationButton(
                        label = strings.whatsappClient,
                        icon = Icons.Default.Chat,
                        color = Color(0xFF25D366),
                        onClick = {
                            val cleanNumber = lead.phone.replace("[^0-9]".toRegex(), "")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "comm_whatsapp_button"
                    )

                    CommunicationButton(
                        label = strings.emailClient,
                        icon = Icons.Default.Email,
                        color = Color(0xFF3B82F6),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${lead.email}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "comm_email_button"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Deal Details Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Deal Value", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text("${lead.currency} ${formatRupeeNumber(lead.dealValue)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text("Priority", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(lead.priority.label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(lead.priority.colorHex))
                        }
                        Column {
                            Text("Assigned To", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(lead.assignedTo.split(" ").firstOrNull() ?: lead.assignedTo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stage Stepper (Quick Move)
                Text("Pipeline Stage", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LeadStage.entries.forEach { stage ->
                        val isCurrent = lead.stage == stage
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) Color(stage.colorHex) else Color(stage.colorHex).copy(alpha = 0.1f),
                            modifier = Modifier
                                .clickable { onAdvanceStage(stage) }
                                .testTag("step_stage_${stage.name.lowercase()}")
                        ) {
                            Text(
                                text = stage.label,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) Color.White else Color(stage.colorHex),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gemini AI Meeting Summary & Actions Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGeminiDialog = true }
                        .testTag("open_gemini_lead_dialog")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFDB2777))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.geminiAiSummary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Auto-generate summary & CRM actions (Gemini 3.5)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Documents & Physical Business Cards
                LeadDocumentsSection(
                    lead = lead,
                    viewModel = viewModel,
                    strings = strings
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Collaboration / Team Notes input
                Text(strings.teamNotes, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text(strings.addNote, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lead_comment_input")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onAddComment(newCommentText)
                                newCommentText = ""
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(42.dp)
                            .testTag("lead_comment_send_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // Recent Activity Notes List
                if (activityLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activityLogs.take(4).forEach { log ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = log.authorName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(text = log.actionText, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (log.commentNote.isNotBlank()) {
                                        Text(text = log.commentNote, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons (Close, Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canDelete) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("delete_lead_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.delete)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    if (showGeminiDialog) {
        val leadDocuments by viewModel.getDocumentsForLead(lead.id).collectAsState(initial = emptyList())
        GeminiSummaryDialog(
            lead = lead,
            initialNotes = lead.notes,
            availableDocuments = leadDocuments,
            strings = strings,
            viewModel = viewModel,
            onDismiss = { showGeminiDialog = false }
        )
    }
}

@Composable
private fun CommunicationButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun AddLeadDialog(
    strings: LocalizedStrings,
    currentRepName: String,
    onDismiss: () -> Unit,
    onSave: (LeadEntity) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dealValueStr by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(LeadPriority.MEDIUM) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_lead_modal_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.addLead,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text(strings.clientName) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lead_client_name")
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text(strings.company) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lead_company")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(strings.phone) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_lead_phone")
                    )

                    OutlinedTextField(
                        value = dealValueStr,
                        onValueChange = { dealValueStr = it },
                        label = { Text("Value (₹)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_lead_value")
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(strings.email) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_lead_email")
                )

                // Priority selector
                Text("Priority Level", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LeadPriority.entries.forEach { p ->
                        val isSelected = priority == p
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(p.colorHex) else Color(p.colorHex).copy(alpha = 0.12f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { priority = p }
                        ) {
                            Text(
                                text = p.label,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(p.colorHex),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Initial Notes") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (clientName.isBlank()) {
                                errorMsg = "Client name is required"
                            } else {
                                val value = dealValueStr.toDoubleOrNull() ?: 100000.0
                                onSave(
                                    LeadEntity(
                                        clientName = clientName,
                                        companyName = companyName.ifBlank { "Private Account" },
                                        email = email.ifBlank { "contact@client.com" },
                                        phone = phone.ifBlank { "+91 98000 00000" },
                                        dealValue = value,
                                        stage = LeadStage.NEW,
                                        priority = priority,
                                        assignedTo = currentRepName,
                                        notes = notes
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_lead_button")
                    ) {
                        Text(strings.save)
                    }
                }
            }
        }
    }
}

fun formatRupeeNumber(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", amount)
}
