package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.example.ai.AiSourceType
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.local.LeadDocumentEntity
import com.example.data.local.LeadEntity
import com.example.localization.LocalizedStrings
import com.example.ui.viewmodel.CrmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeadDocumentsSection(
    lead: LeadEntity,
    viewModel: CrmViewModel,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val documents by viewModel.getDocumentsForLead(lead.id).collectAsState(initial = emptyList())

    var activePhotoFile by remember { mutableStateOf<File?>(null) }
    var activePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCapturedFile by remember { mutableStateOf<File?>(null) }
    var pendingCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedDocToView by remember { mutableStateOf<LeadDocumentEntity?>(null) }
    var showGeminiForDoc by remember { mutableStateOf<LeadDocumentEntity?>(null) }

    // Launcher for taking photo with Camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && activePhotoFile != null && activePhotoFile!!.exists()) {
            pendingCapturedFile = activePhotoFile
            pendingCapturedBitmap = null
            showSaveDialog = true
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraCapture(
                context = context,
                leadId = lead.id,
                onUriReady = { file, uri ->
                    activePhotoFile = file
                    activePhotoUri = uri
                    takePictureLauncher.launch(uri)
                }
            )
        } else {
            Toast.makeText(context, strings.cameraPermissionRequired, Toast.LENGTH_SHORT).show()
        }
    }

    // Photo picker launcher (zero-permission)
    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val savedFile = copyUriToInternalFile(context, uri, lead.id)
                withContext(Dispatchers.Main) {
                    if (savedFile != null) {
                        pendingCapturedFile = savedFile
                        pendingCapturedBitmap = null
                        showSaveDialog = true
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.documentsAndCards,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${documents.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            // Quick Camera & Upload Action Row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Take Photo Button
                FilledTonalButton(
                    onClick = {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPermission) {
                            launchCameraCapture(
                                context = context,
                                leadId = lead.id,
                                onUriReady = { file, uri ->
                                    activePhotoFile = file
                                    activePhotoUri = uri
                                    takePictureLauncher.launch(uri)
                                }
                            )
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_capture_business_card")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Take Photo",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.takePhoto, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Attach / Pick Photo Button
                IconButton(
                    onClick = {
                        pickVisualMediaLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .testTag("btn_pick_photo")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Pick Image",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Sample Card Generation (Instant simulation for emulators & test workflows)
                IconButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val sampleCardFile = generateSampleBusinessCard(context, lead)
                            withContext(Dispatchers.Main) {
                                pendingCapturedFile = sampleCardFile
                                pendingCapturedBitmap = null
                                showSaveDialog = true
                            }
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape)
                        .testTag("btn_sample_card_generate")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Generate Sample Card",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFD97706)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Document Cards List or Empty State
        if (documents.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCameraPermission) {
                            launchCameraCapture(
                                context = context,
                                leadId = lead.id,
                                onUriReady = { file, uri ->
                                    activePhotoFile = file
                                    activePhotoUri = uri
                                    takePictureLauncher.launch(uri)
                                }
                            )
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    .testTag("empty_documents_prompt")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.noDocuments,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.snapDocumentHint,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            // Horizontal scrollable cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                documents.forEach { doc ->
                    LeadDocumentCard(
                        doc = doc,
                        onView = { selectedDocToView = doc },
                        onShare = { shareDocumentFile(context, doc) },
                        onDelete = { viewModel.deleteLeadDocument(doc) },
                        onAnalyzeWithGemini = { showGeminiForDoc = doc }
                    )
                }
            }
        }
    }

    // Save Captured Document Dialog
    if (showSaveDialog && pendingCapturedFile != null) {
        SaveDocumentDialog(
            lead = lead,
            capturedFile = pendingCapturedFile!!,
            strings = strings,
            onDismiss = {
                showSaveDialog = false
                pendingCapturedFile = null
            },
            onSave = { title, docType, notes ->
                viewModel.saveLeadDocument(
                    leadId = lead.id,
                    title = title,
                    docType = docType,
                    filePath = pendingCapturedFile!!.absolutePath,
                    notes = notes
                )
                showSaveDialog = false
                pendingCapturedFile = null
                Toast.makeText(context, "Saved document to lead profile", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // View Fullscreen Document Dialog
    selectedDocToView?.let { doc ->
        DocumentViewerDialog(
            doc = doc,
            onDismiss = { selectedDocToView = null },
            onShare = { shareDocumentFile(context, doc) },
            onDelete = {
                viewModel.deleteLeadDocument(doc)
                selectedDocToView = null
            },
            onAnalyzeWithGemini = {
                showGeminiForDoc = doc
                selectedDocToView = null
            }
        )
    }

    // Gemini AI Summary Dialog for Business Card / Document
    showGeminiForDoc?.let { doc ->
        val file = File(doc.filePath)
        val bitmap = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        GeminiSummaryDialog(
            lead = lead,
            initialNotes = doc.notes.ifBlank { "Business card for ${lead.clientName} (${lead.companyName}). Document: ${doc.title}." },
            initialSourceType = AiSourceType.BUSINESS_CARD,
            initialImageBitmap = bitmap,
            availableDocuments = documents,
            strings = strings,
            viewModel = viewModel,
            onDismiss = { showGeminiForDoc = null }
        )
    }
}

@Composable
private fun LeadDocumentCard(
    doc: LeadDocumentEntity,
    onView: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onAnalyzeWithGemini: () -> Unit
) {
    val typeColor = when (doc.docType) {
        "BUSINESS_CARD" -> Color(0xFFF59E0B)
        "CONTRACT" -> Color(0xFF3B82F6)
        "INVOICE" -> Color(0xFF8B5CF6)
        else -> Color(0xFF10B981)
    }

    val typeIcon = when (doc.docType) {
        "BUSINESS_CARD" -> Icons.Default.Badge
        "CONTRACT" -> Icons.Default.Description
        "INVOICE" -> Icons.Default.Receipt
        else -> Icons.Default.FilePresent
    }

    val dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(doc.capturedAt))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .width(180.dp)
            .clickable { onView() }
            .testTag("doc_card_${doc.id}")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Photo Preview Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                val file = File(doc.filePath)
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = doc.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = doc.docType.replace("_", " "),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }

                // Type Badge Pill in top-left
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = typeColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = doc.docType.take(4),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title & Timestamp
            Text(
                text = doc.title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dateFormatted,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.outline
            )

            // Action row (AI Summarize, Share, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gemini AI Quick Summarize Button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF7C3AED).copy(alpha = 0.12f),
                    modifier = Modifier.clickable { onAnalyzeWithGemini() }.testTag("card_ai_btn_${doc.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Summarize",
                            modifier = Modifier.size(11.dp),
                            tint = Color(0xFF7C3AED)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "AI",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveDocumentDialog(
    lead: LeadEntity,
    capturedFile: File,
    strings: LocalizedStrings,
    onDismiss: () -> Unit,
    onSave: (title: String, docType: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("Business Card - ${lead.clientName}") }
    var selectedType by remember { mutableStateOf("BUSINESS_CARD") }
    var notes by remember { mutableStateOf("") }

    val docTypes = listOf(
        "BUSINESS_CARD" to "Business Card",
        "DOCUMENT" to "Document",
        "CONTRACT" to "Contract / NDA",
        "INVOICE" to "Invoice / Receipt"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("save_document_modal_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Store Document in Lead Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Preview Thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = capturedFile,
                        contentDescription = "Captured Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Document Classification Chips
                Text("Document Classification", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    docTypes.forEach { (typeKey, typeLabel) ->
                        val isSelected = selectedType == typeKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                selectedType = typeKey
                                if (typeKey == "BUSINESS_CARD") {
                                    title = "Business Card - ${lead.clientName}"
                                } else if (typeKey == "CONTRACT") {
                                    title = "Contract Draft - ${lead.companyName}"
                                } else if (typeKey == "INVOICE") {
                                    title = "Invoice Receipt - ${lead.companyName}"
                                }
                            }
                        ) {
                            Text(
                                text = typeLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.documentTitle, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_doc_title")
                )

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Meeting Details", fontSize = 12.sp) },
                    placeholder = { Text("e.g., Exchanged at Mumbai Expo booth #14") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_doc_notes")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, selectedType, notes) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_confirm_save_doc")
                    ) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveDocument)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentViewerDialog(
    doc: LeadDocumentEntity,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onAnalyzeWithGemini: () -> Unit
) {
    val dateFormatted = SimpleDateFormat("EEEE, dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(doc.capturedAt))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("view_document_modal_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = doc.docType.replace("_", " "),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High-resolution image preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val file = File(doc.filePath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = doc.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Stored Card Metadata", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata details
                Text(text = "Captured: $dateFormatted", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                if (doc.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Notes: ${doc.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onShare,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                        Button(
                            onClick = onAnalyzeWithGemini,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            modifier = Modifier.testTag("btn_viewer_gemini_summary")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gemini AI", fontSize = 11.sp)
                        }
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates temporary file and FileProvider Uri for camera capture.
 */
private fun launchCameraCapture(
    context: Context,
    leadId: Long,
    onUriReady: (File, Uri) -> Unit
) {
    try {
        val photosDir = File(context.filesDir, "lead_photos").apply { mkdirs() }
        val photoFile = File(photosDir, "lead_${leadId}_${System.currentTimeMillis()}.jpg")
        val photoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        onUriReady(photoFile, photoUri)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Copies picked Uri stream to app internal file.
 */
private fun copyUriToInternalFile(context: Context, uri: Uri, leadId: Long): File? {
    return try {
        val photosDir = File(context.filesDir, "lead_photos").apply { mkdirs() }
        val destFile = File(photosDir, "picked_lead_${leadId}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile
    } catch (_: Exception) {
        null
    }
}

/**
 * Shares document via Android Share Sheet using FileProvider.
 */
private fun shareDocumentFile(context: Context, doc: LeadDocumentEntity) {
    try {
        val file = File(doc.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Photo file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, doc.title)
            putExtra(Intent.EXTRA_TEXT, "Lead Document: ${doc.title}\nNotes: ${doc.notes}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Document / Business Card"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Generates an executive visual business card Bitmap for instant testing and demoing in emulators.
 */
fun generateSampleBusinessCard(context: Context, lead: LeadEntity): File {
    val width = 800
    val height = 480
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background Gradient / Base
    val bgPaint = Paint().apply {
        color = AndroidColor.parseColor("#0F172A") // Deep slate
        isAntiAlias = true
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Metallic Gold Top Accent Stripe
    val goldPaint = Paint().apply {
        color = AndroidColor.parseColor("#F59E0B") // Amber gold
        isAntiAlias = true
    }
    canvas.drawRect(0f, 0f, width.toFloat(), 16f, goldPaint)

    // Left Border Stripe
    val blueStripePaint = Paint().apply {
        color = AndroidColor.parseColor("#3B82F6") // Royal blue
        isAntiAlias = true
    }
    canvas.drawRect(0f, 16f, 18f, height.toFloat(), blueStripePaint)

    // Card Inner Border
    val borderPaint = Paint().apply {
        color = AndroidColor.parseColor("#334155")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    canvas.drawRoundRect(RectF(30f, 30f, width - 30f, height - 30f), 16f, 16f, borderPaint)

    // Company Name
    val companyPaint = Paint().apply {
        color = AndroidColor.parseColor("#FFFFFF")
        textSize = 34f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText(lead.companyName.uppercase(), 60f, 90f, companyPaint)

    // Sub-header tagline
    val tagPaint = Paint().apply {
        color = AndroidColor.parseColor("#94A3B8")
        textSize = 18f
        isAntiAlias = true
    }
    canvas.drawText("CORPORATE BUSINESS CARD", 60f, 120f, tagPaint)

    // Divider
    val divPaint = Paint().apply {
        color = AndroidColor.parseColor("#475569")
        strokeWidth = 1.5f
    }
    canvas.drawLine(60f, 140f, width - 60f, 140f, divPaint)

    // Client Name
    val namePaint = Paint().apply {
        color = AndroidColor.parseColor("#F8FAFC")
        textSize = 38f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText(lead.clientName, 60f, 210f, namePaint)

    // Title / Role
    val rolePaint = Paint().apply {
        color = AndroidColor.parseColor("#38BDF8") // Sky blue
        textSize = 22f
        isAntiAlias = true
    }
    canvas.drawText("VP / Managing Director", 60f, 245f, rolePaint)

    // Contact Details
    val contactPaint = Paint().apply {
        color = AndroidColor.parseColor("#E2E8F0")
        textSize = 22f
        isAntiAlias = true
    }
    canvas.drawText("Phone: ${lead.phone}", 60f, 320f, contactPaint)
    canvas.drawText("Email: ${lead.email}", 60f, 360f, contactPaint)
    canvas.drawText("Deal Stage: ${lead.stage.label}  •  Priority: ${lead.priority.label}", 60f, 400f, contactPaint)

    // Mini QR Code Decorative Box in bottom-right
    val qrBoxPaint = Paint().apply {
        color = AndroidColor.parseColor("#1E293B")
        isAntiAlias = true
    }
    canvas.drawRoundRect(RectF(width - 180f, height - 180f, width - 60f, height - 60f), 12f, 12f, qrBoxPaint)

    val qrTextPaint = Paint().apply {
        color = AndroidColor.parseColor("#F59E0B")
        textSize = 14f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("VERIFIED", width - 120f, height - 125f, qrTextPaint)
    canvas.drawText("LEAD CARD", width - 120f, height - 105f, qrTextPaint)

    // Save to disk
    val photosDir = File(context.filesDir, "lead_photos").apply { mkdirs() }
    val cardFile = File(photosDir, "sample_card_${lead.id}_${System.currentTimeMillis()}.jpg")
    FileOutputStream(cardFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return cardFile
}
