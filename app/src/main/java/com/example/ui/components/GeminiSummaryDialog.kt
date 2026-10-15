package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AiActionItem
import com.example.ai.AiSourceType
import com.example.ai.GeminiAnalysisResult
import com.example.ai.GeminiService
import com.example.ai.RecommendedFollowUp
import com.example.data.local.LeadDocumentEntity
import com.example.data.local.LeadEntity
import com.example.data.model.FollowUpType
import com.example.data.model.TaskPriority
import com.example.localization.LocalizedStrings
import com.example.ui.viewmodel.CrmViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeminiSummaryDialog(
    lead: LeadEntity?,
    initialNotes: String = "",
    initialSourceType: AiSourceType = AiSourceType.CALL_NOTES,
    initialImageBitmap: Bitmap? = null,
    availableDocuments: List<LeadDocumentEntity> = emptyList(),
    strings: LocalizedStrings,
    viewModel: CrmViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedSourceType by remember { mutableStateOf(initialSourceType) }
    var inputNotes by remember {
        mutableStateOf(
            if (initialNotes.isNotBlank()) initialNotes
            else lead?.notes?.ifBlank { "" } ?: ""
        )
    }
    var activeBitmap by remember { mutableStateOf<Bitmap?>(initialImageBitmap) }
    var selectedDocument by remember { mutableStateOf<LeadDocumentEntity?>(null) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<GeminiAnalysisResult?>(null) }
    var addedTasksTitles = remember { mutableStateListOf<String>() }
    var hasScheduledFollowUp by remember { mutableStateOf(false) }
    var hasSavedToNotes by remember { mutableStateOf(false) }

    // Quick samples for rapid review & testing
    val sampleCallNote = "Spoke with client regarding enterprise deployment. Discussed ₹5,00,000 budget for 50 licenses. Client requested ISO certification proof and a 1-year SLA warranty. Needs commercial proposal emailed by Thursday. Agreed to schedule team demo next Monday."
    val sampleCardTranscription = "Dr. Rajesh Singhania | Chief Technology Officer | Apex Cloud Infrastructure Pvt Ltd | Phone: +91 98200 11223 | Email: r.singhania@apexcloud.in | Bangalore, Karnataka. Specializes in hybrid multi-cloud migrations and DevOps security."

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(0.92f)
                .testTag("gemini_summary_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Gemini Gradient Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFDB2777))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.geminiAiSummary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "3.5 Flash",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = lead?.let { "${it.clientName} • ${it.companyName}" } ?: "Sales Notes & Card Analyzer",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_gemini_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Source Type Selector Chips
                    Text(
                        text = "Interaction Source",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiSourceType.entries.forEach { type ->
                            val isSelected = selectedSourceType == type
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .clickable {
                                        selectedSourceType = type
                                        if (type == AiSourceType.CALL_NOTES && inputNotes.isBlank()) {
                                            inputNotes = lead?.notes ?: ""
                                        }
                                    }
                                    .testTag("ai_source_${type.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (type) {
                                            AiSourceType.CALL_NOTES -> Icons.Default.Call
                                            AiSourceType.BUSINESS_CARD -> Icons.Default.Badge
                                            AiSourceType.MEETING_NOTES -> Icons.Default.Description
                                            AiSourceType.ALL_LEAD_DATA -> Icons.Default.AutoAwesome
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = type.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // If business card source and available documents exist, allow selecting attached card
                    if (selectedSourceType == AiSourceType.BUSINESS_CARD && availableDocuments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Select Attached Document or Card",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableDocuments.forEach { doc ->
                                val isSelected = selectedDocument?.id == doc.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFDBEAFE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .clickable {
                                            selectedDocument = doc
                                            if (doc.filePath.isNotBlank()) {
                                                val file = File(doc.filePath)
                                                if (file.exists()) {
                                                    activeBitmap = BitmapFactory.decodeFile(file.absolutePath)
                                                }
                                            }
                                            if (inputNotes.isBlank() && doc.notes.isNotBlank()) {
                                                inputNotes = doc.notes
                                            }
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2563EB))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(doc.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Notes / Transcription textfield
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedSourceType) {
                                AiSourceType.CALL_NOTES -> "Saved Call Notes & Logs"
                                AiSourceType.BUSINESS_CARD -> "Card Text & OCR Transcription"
                                else -> "Meeting Discussion Notes"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Quick sample helper chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.clickable {
                                inputNotes = if (selectedSourceType == AiSourceType.BUSINESS_CARD) sampleCardTranscription else sampleCallNote
                            }
                        ) {
                            Text(
                                text = "Use Sample Notes",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = inputNotes,
                        onValueChange = { inputNotes = it },
                        placeholder = {
                            Text(
                                text = "Enter or paste raw call notes, voice transcription, or business card details for Gemini AI analysis...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 150.dp)
                            .testTag("ai_notes_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Generate Button
                    Button(
                        onClick = {
                            isAnalyzing = true
                            hasSavedToNotes = false
                            hasScheduledFollowUp = false
                            addedTasksTitles.clear()

                            coroutineScope.launch {
                                val result = GeminiService.analyzeLeadInteraction(
                                    clientName = lead?.clientName ?: "Prospective Client",
                                    companyName = lead?.companyName ?: "Business Account",
                                    sourceType = selectedSourceType,
                                    notesText = inputNotes,
                                    imageBitmap = activeBitmap
                                )
                                analysisResult = result
                                isAnalyzing = false
                            }
                        },
                        enabled = !isAnalyzing && inputNotes.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_run_gemini_analysis")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing with Gemini 3.5 Flash...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.generateAiSummary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Analysis Results Section
                    AnimatedVisibility(
                        visible = analysisResult != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        analysisResult?.let { result ->
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {

                                // Executive Summary Card
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().testTag("ai_summary_card")
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingUp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Executive Summary",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            // Sentiment Pill
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = when {
                                                    result.clientSentiment.contains("Positive", ignoreCase = true) || result.clientSentiment.contains("Close", ignoreCase = true) -> Color(0xFFD1FAE5)
                                                    result.clientSentiment.contains("Price", ignoreCase = true) || result.clientSentiment.contains("Sensitive", ignoreCase = true) -> Color(0xFFFEF3C7)
                                                    else -> Color(0xFFE0E7FF)
                                                }
                                            ) {
                                                Text(
                                                    text = result.clientSentiment,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        result.clientSentiment.contains("Positive", ignoreCase = true) || result.clientSentiment.contains("Close", ignoreCase = true) -> Color(0xFF047857)
                                                        result.clientSentiment.contains("Price", ignoreCase = true) || result.clientSentiment.contains("Sensitive", ignoreCase = true) -> Color(0xFFB45309)
                                                        else -> Color(0xFF3730A3)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = result.summary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (result.keyDiscussionPoints.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Key Discussion Topics:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            result.keyDiscussionPoints.forEach { point ->
                                                Row(
                                                    modifier = Modifier.padding(vertical = 2.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(point, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                                                }
                                            }
                                        }
                                    }
                                }

                                // Extracted Contact Info (from business card)
                                if (result.extractedContact != null && (result.extractedContact.fullName.isNotBlank() || result.extractedContact.phone.isNotBlank() || result.extractedContact.email.isNotBlank())) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.fillMaxWidth().testTag("ai_contact_card")
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Extracted Business Card Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (result.extractedContact.fullName.isNotBlank()) {
                                                Text(
                                                    text = "${result.extractedContact.fullName} ${if (result.extractedContact.designation.isNotBlank()) "• ${result.extractedContact.designation}" else ""}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF1E293B)
                                                )
                                            }
                                            if (result.extractedContact.phone.isNotBlank()) {
                                                Text(text = "📞 ${result.extractedContact.phone}", fontSize = 12.sp, color = Color(0xFF334155))
                                            }
                                            if (result.extractedContact.email.isNotBlank()) {
                                                Text(text = "✉️ ${result.extractedContact.email}", fontSize = 12.sp, color = Color(0xFF334155))
                                            }

                                            // Quick button to update lead contact details
                                            if (lead != null && (result.extractedContact.phone.isNotBlank() || result.extractedContact.email.isNotBlank())) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedButton(
                                                    onClick = {
                                                        val updated = lead.copy(
                                                            phone = if (result.extractedContact.phone.isNotBlank()) result.extractedContact.phone else lead.phone,
                                                            email = if (result.extractedContact.email.isNotBlank()) result.extractedContact.email else lead.email
                                                        )
                                                        viewModel.saveLead(updated)
                                                        Toast.makeText(context, "Lead contact info updated from card!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Sync Info to Lead Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Items List
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.aiActionItems,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (result.actionItems.isNotEmpty() && addedTasksTitles.size < result.actionItems.size) {
                                        TextButton(
                                            onClick = {
                                                result.actionItems.forEach { item ->
                                                    if (!addedTasksTitles.contains(item.title)) {
                                                        viewModel.createCrmTaskFromAi(item, lead)
                                                        addedTasksTitles.add(item.title)
                                                    }
                                                }
                                                Toast.makeText(context, "All action items added to CRM Tasks!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.testTag("btn_add_all_tasks")
                                        ) {
                                            Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.addAllTasks, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                result.actionItems.forEachIndexed { index, item ->
                                    val isAdded = addedTasksTitles.contains(item.title)
                                    val priorityColor = Color(item.priority.colorHex)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .testTag("action_item_$index")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = priorityColor.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = item.priority.label,
                                                            color = priorityColor,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Due in ${item.suggestedDueDateDays}d",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = item.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (item.description.isNotBlank()) {
                                                    Text(
                                                        text = item.description,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            if (isAdded) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFD1FAE5),
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Added",
                                                            tint = Color(0xFF059669),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                FilledTonalButton(
                                                    onClick = {
                                                        viewModel.createCrmTaskFromAi(item, lead)
                                                        addedTasksTitles.add(item.title)
                                                        Toast.makeText(context, "Added to Tasks", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .height(34.dp)
                                                        .testTag("btn_add_task_$index")
                                                ) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("Task", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Recommended Follow-Up Card
                                result.recommendedFollowUp?.let { followUp ->
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                        modifier = Modifier.fillMaxWidth().testTag("ai_recommended_followup")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
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
                                                        .background(Color(0xFF7C3AED)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when (followUp.type) {
                                                            FollowUpType.CALL -> Icons.Default.Call
                                                            FollowUpType.DEMO -> Icons.Default.Videocam
                                                            FollowUpType.EMAIL -> Icons.Default.Email
                                                            else -> Icons.Default.Schedule
                                                        },
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "Recommended Next Step: ${followUp.type.label}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF581C87)
                                                    )
                                                    Text(
                                                        text = "${followUp.purpose} (in ${followUp.suggestedDays} days)",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF6B21A8)
                                                    )
                                                }
                                            }

                                            if (hasScheduledFollowUp) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFD1FAE5)
                                                ) {
                                                    Text(
                                                        text = "Scheduled ✓",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF047857),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        if (lead != null) {
                                                            viewModel.scheduleFollowUpFromAi(lead, followUp)
                                                            hasScheduledFollowUp = true
                                                            Toast.makeText(context, "Follow-up added to schedule!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                                    modifier = Modifier.height(34.dp).testTag("btn_schedule_ai_followup")
                                                ) {
                                                    Text(strings.scheduleFollowUpAction, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Buttons Row: Save to Lead Notes, Copy Report
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (lead != null) {
                                        Button(
                                            onClick = {
                                                viewModel.appendAiSummaryToLead(lead, result)
                                                hasSavedToNotes = true
                                                Toast.makeText(context, "Summary saved to Lead Profile!", Toast.LENGTH_SHORT).show()
                                            },
                                            enabled = !hasSavedToNotes,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (hasSavedToNotes) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("btn_save_summary_to_lead")
                                        ) {
                                            Icon(
                                                imageVector = if (hasSavedToNotes) Icons.Default.Check else Icons.Default.Save,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (hasSavedToNotes) "Saved to Notes ✓" else strings.saveToLeadNotes,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val reportText = buildString {
                                                appendLine("GEMINI AI MEETING SUMMARY • ${lead?.clientName ?: ""} (${lead?.companyName ?: ""})")
                                                appendLine("Sentiment: ${result.clientSentiment}")
                                                appendLine("Summary: ${result.summary}")
                                                if (result.keyDiscussionPoints.isNotEmpty()) {
                                                    appendLine("\nKey Points:")
                                                    result.keyDiscussionPoints.forEach { appendLine("- $it") }
                                                }
                                                if (result.actionItems.isNotEmpty()) {
                                                    appendLine("\nAction Items:")
                                                    result.actionItems.forEach { appendLine("• [${it.priority.label}] ${it.title} (Due in ${it.suggestedDueDateDays}d)") }
                                                }
                                            }
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Gemini CRM Summary", reportText))
                                            Toast.makeText(context, "Summary report copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(0.7f)
                                            .height(44.dp)
                                            .testTag("btn_copy_ai_summary")
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", fontSize = 12.sp)
                                    }
                                }

                                if (result.isDemoFallback) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "To enable live cloud analysis, configure your GEMINI_API_KEY in the AI Studio Secrets panel.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
