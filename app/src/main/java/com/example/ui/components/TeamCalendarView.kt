package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.FollowUpEntity
import com.example.data.local.LeadEntity
import com.example.data.local.TaskEntity
import com.example.data.model.FollowUpStatus
import com.example.data.model.FollowUpType
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TeamMember
import com.example.localization.LocalizedStrings
import com.example.ui.viewmodel.CrmViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Type of schedule item synced into the Team Calendar
 */
enum class ScheduleItemType {
    TASK,
    FOLLOW_UP
}

/**
 * Unified schedule item combining tasks and follow-up deadlines for the team calendar.
 */
data class SyncedScheduleItem(
    val id: String,
    val originalId: Long,
    val itemType: ScheduleItemType,
    val title: String,
    val subtitle: String,
    val dateTimeMillis: Long,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val priority: TaskPriority? = null,
    val followUpType: FollowUpType? = null,
    val clientPhone: String? = null,
    val assignedTo: String,
    val leadId: Long? = null,
    val rawTask: TaskEntity? = null,
    val rawFollowUp: FollowUpEntity? = null
)

/**
 * Shared Team Calendar View:
 * Displays synced tasks and follow-up deadlines in an interactive weekly / monthly view,
 * with real-time sync, team member filtering, deadline tracking, and direct CRM actions.
 */
@Composable
fun TeamCalendarView(
    viewModel: CrmViewModel,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier,
    onOpenGeminiForFollowUp: ((FollowUpEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()
    val followUps by viewModel.allFollowUps.collectAsState()
    val leads by viewModel.allLeads.collectAsState()
    val currentMember by viewModel.currentMember.collectAsState()
    val teamMembers = viewModel.teamMembers

    // Selected Date & Anchor Date for Week
    val todayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var selectedDateMillis by remember { mutableLongStateOf(todayMillis) }
    var anchorWeekMillis by remember { mutableLongStateOf(todayMillis) }

    // Calendar Display Mode: 0 = Week Strip, 1 = Month Grid
    var calendarDisplayMode by remember { mutableIntStateOf(0) }

    // Filters
    // selectedTeamMember: null = "All Team Members", string = member name
    var selectedTeamMember by remember { mutableStateOf<String?>(null) }
    // itemTypeFilter: 0 = All, 1 = Tasks Only, 2 = Follow-ups Only
    var itemTypeFilter by remember { mutableIntStateOf(0) }
    // statusFilter: 0 = All, 1 = Pending, 2 = Completed
    var statusFilter by remember { mutableIntStateOf(0) }

    // Dialog state for adding new task / follow-up for the selected day
    var showAddScheduleItemDialog by remember { mutableStateOf(false) }
    var scheduleItemTypeToAdd by remember { mutableStateOf(ScheduleItemType.FOLLOW_UP) }

    // Map Tasks and Follow-ups to SyncedScheduleItem
    val allSyncedItems = remember(tasks, followUps, leads) {
        val list = mutableListOf<SyncedScheduleItem>()
        val now = System.currentTimeMillis()

        // Sync Follow-ups
        followUps.forEach { fu ->
            val lead = leads.firstOrNull { it.id == fu.leadId }
            val assigned = lead?.assignedTo ?: "Rahul Sharma"
            val isDone = fu.status == FollowUpStatus.COMPLETED
            val isOver = !isDone && fu.scheduledDateTime < now

            list.add(
                SyncedScheduleItem(
                    id = "followup_${fu.id}",
                    originalId = fu.id,
                    itemType = ScheduleItemType.FOLLOW_UP,
                    title = fu.leadName,
                    subtitle = if (fu.notes.isNotBlank()) fu.notes else "Scheduled ${fu.type.name.lowercase().capitalize(Locale.getDefault())}",
                    dateTimeMillis = fu.scheduledDateTime,
                    isCompleted = isDone,
                    isOverdue = isOver,
                    followUpType = fu.type,
                    clientPhone = fu.clientPhone,
                    assignedTo = assigned,
                    leadId = fu.leadId,
                    rawFollowUp = fu
                )
            )
        }

        // Sync Tasks
        tasks.forEach { t ->
            val isDone = t.status == TaskStatus.DONE
            val isOver = !isDone && t.dueDate < now

            list.add(
                SyncedScheduleItem(
                    id = "task_${t.id}",
                    originalId = t.id,
                    itemType = ScheduleItemType.TASK,
                    title = t.title,
                    subtitle = if (t.description.isNotBlank()) t.description else t.relatedProjectName?.let { "Project: $it" } ?: "CRM Task",
                    dateTimeMillis = t.dueDate,
                    isCompleted = isDone,
                    isOverdue = isOver,
                    priority = t.priority,
                    assignedTo = t.assignedTo,
                    leadId = t.relatedLeadId,
                    rawTask = t
                )
            )
        }

        list.sortedBy { it.dateTimeMillis }
    }

    // Days in current week
    val weekDays = remember(anchorWeekMillis) {
        getWeekDays(anchorWeekMillis)
    }

    // Filtered by Team Member and Item Type
    val teamFilteredItems = remember(allSyncedItems, selectedTeamMember, itemTypeFilter, statusFilter) {
        allSyncedItems.filter { item ->
            val matchesMember = selectedTeamMember == null || item.assignedTo.equals(selectedTeamMember, ignoreCase = true)
            val matchesType = when (itemTypeFilter) {
                1 -> item.itemType == ScheduleItemType.TASK
                2 -> item.itemType == ScheduleItemType.FOLLOW_UP
                else -> true
            }
            val matchesStatus = when (statusFilter) {
                1 -> !item.isCompleted
                2 -> item.isCompleted
                else -> true
            }
            matchesMember && matchesType && matchesStatus
        }
    }

    // Items for selected day
    val selectedDayItems = remember(teamFilteredItems, selectedDateMillis) {
        teamFilteredItems.filter { isSameDay(it.dateTimeMillis, selectedDateMillis) }
    }

    // Items for current week
    val weekItems = remember(teamFilteredItems, weekDays) {
        if (weekDays.isEmpty()) emptyList()
        else {
            val start = weekDays.first()
            val end = weekDays.last() + 24 * 3600 * 1000L - 1
            teamFilteredItems.filter { it.dateTimeMillis in start..end }
        }
    }

    // Weekly metrics
    val weeklyTotal = weekItems.size
    val weeklyCompleted = weekItems.count { it.isCompleted }
    val weeklyTasks = weekItems.count { it.itemType == ScheduleItemType.TASK }
    val weeklyFollowUps = weekItems.count { it.itemType == ScheduleItemType.FOLLOW_UP }
    val weeklyOverdue = weekItems.count { it.isOverdue }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Header: Month Title, Navigation (< >), Jump to Today, Mode Toggle
            CalendarHeaderRow(
                anchorWeekMillis = anchorWeekMillis,
                calendarDisplayMode = calendarDisplayMode,
                strings = strings,
                onPreviousWeek = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = anchorWeekMillis
                        if (calendarDisplayMode == 0) add(Calendar.WEEK_OF_YEAR, -1)
                        else add(Calendar.MONTH, -1)
                    }
                    anchorWeekMillis = cal.timeInMillis
                },
                onNextWeek = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = anchorWeekMillis
                        if (calendarDisplayMode == 0) add(Calendar.WEEK_OF_YEAR, 1)
                        else add(Calendar.MONTH, 1)
                    }
                    anchorWeekMillis = cal.timeInMillis
                },
                onJumpToday = {
                    anchorWeekMillis = todayMillis
                    selectedDateMillis = todayMillis
                },
                onToggleDisplayMode = {
                    calendarDisplayMode = if (calendarDisplayMode == 0) 1 else 0
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Component (Week Strip or Month Grid)
            if (calendarDisplayMode == 0) {
                // Interactive 7-Day Week Strip
                WeekStripView(
                    weekDays = weekDays,
                    selectedDateMillis = selectedDateMillis,
                    todayMillis = todayMillis,
                    items = teamFilteredItems,
                    onSelectDay = { selectedDateMillis = it }
                )
            } else {
                // Month Grid View
                MonthGridView(
                    anchorMillis = anchorWeekMillis,
                    selectedDateMillis = selectedDateMillis,
                    todayMillis = todayMillis,
                    items = teamFilteredItems,
                    onSelectDay = {
                        selectedDateMillis = it
                        anchorWeekMillis = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shared Team & Category Filter Chips
            SharedTeamFiltersBar(
                teamMembers = teamMembers,
                currentMember = currentMember,
                selectedTeamMember = selectedTeamMember,
                itemTypeFilter = itemTypeFilter,
                statusFilter = statusFilter,
                strings = strings,
                onSelectTeamMember = { selectedTeamMember = it },
                onSelectTypeFilter = { itemTypeFilter = it },
                onSelectStatusFilter = { statusFilter = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Weekly Sync Progress Banner
            WeeklySyncProgressBanner(
                weeklyTotal = weeklyTotal,
                weeklyCompleted = weeklyCompleted,
                weeklyTasks = weeklyTasks,
                weeklyFollowUps = weeklyFollowUps,
                weeklyOverdue = weeklyOverdue,
                strings = strings
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Day Agenda Header & Count
            val dayFormat = remember { SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dayFormat.format(Date(selectedDateMillis)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${selectedDayItems.size} items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scheduled Items List
            if (selectedDayItems.isEmpty()) {
                EmptyScheduleDayView(
                    strings = strings,
                    onAddTask = {
                        scheduleItemTypeToAdd = ScheduleItemType.TASK
                        showAddScheduleItemDialog = true
                    },
                    onAddFollowUp = {
                        scheduleItemTypeToAdd = ScheduleItemType.FOLLOW_UP
                        showAddScheduleItemDialog = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("team_calendar_items_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedDayItems, key = { it.id }) { item ->
                        CalendarScheduleItemCard(
                            item = item,
                            strings = strings,
                            onToggleComplete = {
                                if (item.itemType == ScheduleItemType.TASK && item.rawTask != null) {
                                    viewModel.toggleTaskStatus(item.rawTask)
                                } else if (item.itemType == ScheduleItemType.FOLLOW_UP && item.rawFollowUp != null) {
                                    viewModel.markFollowUpCompleted(item.rawFollowUp)
                                }
                            },
                            onDelete = {
                                if (item.itemType == ScheduleItemType.TASK) {
                                    viewModel.deleteTask(item.originalId)
                                } else {
                                    viewModel.deleteFollowUp(item.originalId)
                                }
                            },
                            onCall = { phone ->
                                if (phone.isNotBlank()) {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(dialIntent)
                                }
                            },
                            onOpenGemini = {
                                if (item.rawFollowUp != null) {
                                    onOpenGeminiForFollowUp?.invoke(item.rawFollowUp)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button: Quick Add for Selected Day
        FloatingActionButton(
            onClick = {
                scheduleItemTypeToAdd = ScheduleItemType.FOLLOW_UP
                showAddScheduleItemDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("fab_calendar_add_schedule")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task or Follow-up"
            )
        }
    }

    // Modal to add Task or Follow-up for the selected calendar date
    if (showAddScheduleItemDialog) {
        AddScheduleItemModal(
            initialType = scheduleItemTypeToAdd,
            selectedDateMillis = selectedDateMillis,
            leads = leads,
            teamMembers = teamMembers,
            currentMember = currentMember,
            strings = strings,
            onDismiss = { showAddScheduleItemDialog = false },
            onSaveTask = { task ->
                viewModel.saveTask(task)
                showAddScheduleItemDialog = false
            },
            onSaveFollowUp = { followUp ->
                viewModel.saveFollowUp(followUp)
                showAddScheduleItemDialog = false
            }
        )
    }
}

/**
 * Calendar Header Row: Week range, navigation arrows, Today jump, Mode switch
 */
@Composable
private fun CalendarHeaderRow(
    anchorWeekMillis: Long,
    calendarDisplayMode: Int,
    strings: LocalizedStrings,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onJumpToday: () -> Unit,
    onToggleDisplayMode: () -> Unit
) {
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val weekTitle = remember(anchorWeekMillis, calendarDisplayMode) {
        if (calendarDisplayMode == 0) {
            val weekDays = getWeekDays(anchorWeekMillis)
            val d1 = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(weekDays.first()))
            val d2 = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(weekDays.last()))
            "$d1 - $d2"
        } else {
            monthYearFormat.format(Date(anchorWeekMillis))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_header_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = monthYearFormat.format(Date(anchorWeekMillis)),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = weekTitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Jump to Today
            OutlinedButton(
                onClick = onJumpToday,
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("calendar_jump_today_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = strings.jumpToday, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Navigation Arrows
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row {
                    IconButton(
                        onClick = onPreviousWeek,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("calendar_nav_prev")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextWeek,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("calendar_nav_next")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Month / Week mode toggle
            IconButton(
                onClick = onToggleDisplayMode,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("calendar_mode_toggle_btn")
            ) {
                Icon(
                    imageVector = if (calendarDisplayMode == 0) Icons.Default.CalendarMonth else Icons.Default.CalendarToday,
                    contentDescription = "Toggle View",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Interactive 7-Day Week Strip
 */
@Composable
private fun WeekStripView(
    weekDays: List<Long>,
    selectedDateMillis: Long,
    todayMillis: Long,
    items: List<SyncedScheduleItem>,
    onSelectDay: (Long) -> Unit
) {
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayNumberFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_week_strip"),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekDays.forEach { dayMillis ->
            val isSelected = isSameDay(dayMillis, selectedDateMillis)
            val isToday = isSameDay(dayMillis, todayMillis)

            // Calculate items for this day
            val dayItems = items.filter { isSameDay(it.dateTimeMillis, dayMillis) }
            val taskCount = dayItems.count { it.itemType == ScheduleItemType.TASK }
            val followUpCount = dayItems.count { it.itemType == ScheduleItemType.FOLLOW_UP }
            val hasOverdue = dayItems.any { it.isOverdue }
            val allDone = dayItems.isNotEmpty() && dayItems.all { it.isCompleted }

            val bgColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }

            val textColor = when {
                isSelected -> Color.White
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectDay(dayMillis) }
                    .testTag("calendar_day_card_${dayNumberFormat.format(Date(dayMillis))}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayNameFormat.format(Date(dayMillis)).uppercase(Locale.getDefault()),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor.copy(alpha = if (isSelected) 0.9f else 0.7f)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = dayNumberFormat.format(Date(dayMillis)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Indicator Dots / Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (allDone) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = if (isSelected) Color(0xFF86EFAC) else Color(0xFF16A34A),
                                modifier = Modifier.size(8.dp)
                            )
                        } else {
                            if (taskCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF93C5FD) else Color(0xFF2563EB))
                                )
                            }
                            if (followUpCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFFDE047) else Color(0xFFF59E0B))
                                )
                            }
                            if (hasOverdue) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Month Grid View for holistic monthly schedule scanning
 */
@Composable
private fun MonthGridView(
    anchorMillis: Long,
    selectedDateMillis: Long,
    todayMillis: Long,
    items: List<SyncedScheduleItem>,
    onSelectDay: (Long) -> Unit
) {
    val monthCal = remember(anchorMillis) {
        Calendar.getInstance().apply {
            timeInMillis = anchorMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_month_grid")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Day Labels (M T W T F S S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Days Grid
            val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
            val rows = totalCells / 7

            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - firstDayOfWeek + 1

                        if (dayNum in 1..daysInMonth) {
                            val cellCal = (monthCal.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, dayNum)
                            }
                            val cellMillis = cellCal.timeInMillis
                            val isSelected = isSameDay(cellMillis, selectedDateMillis)
                            val isToday = isSameDay(cellMillis, todayMillis)

                            val dayItems = items.filter { isSameDay(it.dateTimeMillis, cellMillis) }
                            val hasItems = dayItems.isNotEmpty()

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onSelectDay(cellMillis) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> Color.White
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (hasItems) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter Chips Bar for Team Member & Item Type
 */
@Composable
private fun SharedTeamFiltersBar(
    teamMembers: List<TeamMember>,
    currentMember: TeamMember,
    selectedTeamMember: String?,
    itemTypeFilter: Int,
    statusFilter: Int,
    strings: LocalizedStrings,
    onSelectTeamMember: (String?) -> Unit,
    onSelectTypeFilter: (Int) -> Unit,
    onSelectStatusFilter: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Team Members Horizontal Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_team_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // All Team Members Chip
            item {
                FilterChipPill(
                    label = strings.allTeamMembers,
                    isSelected = selectedTeamMember == null,
                    icon = Icons.Default.Group,
                    onClick = { onSelectTeamMember(null) }
                )
            }

            // My Schedule Chip
            item {
                FilterChipPill(
                    label = "${strings.mySchedule} (${currentMember.name.split(" ").first()})",
                    isSelected = selectedTeamMember.equals(currentMember.name, ignoreCase = true),
                    icon = Icons.Default.Person,
                    onClick = { onSelectTeamMember(currentMember.name) }
                )
            }

            // Other Team Members
            items(teamMembers.filter { it.name != currentMember.name }) { member ->
                FilterChipPill(
                    label = member.name.split(" ").first(),
                    isSelected = selectedTeamMember.equals(member.name, ignoreCase = true),
                    icon = null,
                    onClick = { onSelectTeamMember(member.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Type Filter (All, Tasks, Follow-ups) + Status Filter (Pending, Done)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "All", 1 to "Tasks", 2 to "Follow-ups").forEach { (type, label) ->
                    val isSelected = itemTypeFilter == type
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clickable { onSelectTypeFilter(type) }
                            .testTag("filter_type_$type")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0 to "All", 1 to "Pending", 2 to "Done").forEach { (status, label) ->
                    val isSelected = statusFilter == status
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.clickable { onSelectStatusFilter(status) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Weekly Sync Progress Banner
 */
@Composable
private fun WeeklySyncProgressBanner(
    weeklyTotal: Int,
    weeklyCompleted: Int,
    weeklyTasks: Int,
    weeklyFollowUps: Int,
    weeklyOverdue: Int,
    strings: LocalizedStrings
) {
    val progress = if (weeklyTotal > 0) weeklyCompleted.toFloat() / weeklyTotal else 0f
    val percent = (progress * 100).toInt()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_weekly_metrics_banner")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${strings.weeklySchedule}: $weeklyTotal deadlines ($weeklyTasks tasks, $weeklyFollowUps follow-ups)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (weeklyOverdue > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$weeklyOverdue Overdue",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF16A34A),
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = "$percent% done ($weeklyCompleted/$weeklyTotal)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Calendar Schedule Item Card: Displays synced task or follow-up deadline
 */
@Composable
private fun CalendarScheduleItemCard(
    item: SyncedScheduleItem,
    strings: LocalizedStrings,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onCall: (String) -> Unit,
    onOpenGemini: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = timeFormat.format(Date(item.dateTimeMillis))

    val isTask = item.itemType == ScheduleItemType.TASK
    val itemTypeColor = if (isTask) Color(0xFF2563EB) else Color(0xFFD97706)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isCompleted) 1.dp else 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Type badge, Time, Priority/FollowupType, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = itemTypeColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isTask) Icons.Default.Assignment else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = itemTypeColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (isTask) strings.taskDeadline else strings.followUpDeadline,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = itemTypeColor
                            )
                        }
                    }

                    // Scheduled Time
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status / Priority Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.priority != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(item.priority.colorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.priority.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(item.priority.colorHex),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (item.followUpType != null) {
                        val icon = when (item.followUpType) {
                            FollowUpType.CALL -> Icons.Default.Call
                            FollowUpType.MEETING -> Icons.Default.Person
                            FollowUpType.WHATSAPP -> Icons.Default.Call
                            FollowUpType.EMAIL -> Icons.Default.Schedule
                            FollowUpType.DEMO -> Icons.Default.Schedule
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (item.isOverdue) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "OVERDUE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Complete Checkbox Button
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("calendar_item_toggle_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (item.isCompleted) Color(0xFF16A34A) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Assigned Rep Chip & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assigned Rep Tag
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initials = item.assignedTo.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.assignedTo,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // For Follow-up: Quick Call button
                    if (!item.clientPhone.isNullOrBlank()) {
                        IconButton(
                            onClick = { onCall(item.clientPhone) },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("calendar_call_btn_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call Client",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // For Follow-up: Gemini AI Summary Button
                    if (item.rawFollowUp != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF7C3AED).copy(alpha = 0.12f),
                            modifier = Modifier
                                .clickable { onOpenGemini() }
                                .testTag("calendar_gemini_btn_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Summary",
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty View for selected calendar day
 */
@Composable
private fun EmptyScheduleDayView(
    strings: LocalizedStrings,
    onAddTask: () -> Unit,
    onAddFollowUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(44.dp)
            )

            Text(
                text = strings.noScheduleItemsDay,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAddFollowUp,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("empty_schedule_add_followup_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "+ Follow-up", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddTask,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("empty_schedule_add_task_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "+ Task", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Modal to add a Task or Follow-up pre-scheduled for the selected date
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleItemModal(
    initialType: ScheduleItemType,
    selectedDateMillis: Long,
    leads: List<LeadEntity>,
    teamMembers: List<TeamMember>,
    currentMember: TeamMember,
    strings: LocalizedStrings,
    onDismiss: () -> Unit,
    onSaveTask: (TaskEntity) -> Unit,
    onSaveFollowUp: (FollowUpEntity) -> Unit
) {
    var selectedItemType by remember { mutableStateOf(initialType) }

    // Follow-up fields
    var selectedLead by remember { mutableStateOf(leads.firstOrNull()) }
    var followUpType by remember { mutableStateOf(FollowUpType.CALL) }
    var followUpNotes by remember { mutableStateOf("") }
    var leadMenuExpanded by remember { mutableStateOf(false) }

    // Task fields
    var taskTitle by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf(TaskPriority.HIGH) }
    var assignedRep by remember { mutableStateOf(currentMember.name) }
    var repMenuExpanded by remember { mutableStateOf(false) }

    val formattedDate = remember(selectedDateMillis) {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("modal_add_schedule_item")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with Selected Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add to Calendar",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Type Toggle: [ Follow-up ] [ Task ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedItemType == ScheduleItemType.FOLLOW_UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedItemType = ScheduleItemType.FOLLOW_UP }
                            .testTag("add_item_toggle_followup")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (selectedItemType == ScheduleItemType.FOLLOW_UP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.followUpDeadline,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedItemType == ScheduleItemType.FOLLOW_UP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedItemType == ScheduleItemType.TASK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedItemType = ScheduleItemType.TASK }
                            .testTag("add_item_toggle_task")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = if (selectedItemType == ScheduleItemType.TASK) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.taskDeadline,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedItemType == ScheduleItemType.TASK) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedItemType == ScheduleItemType.FOLLOW_UP) {
                    // Lead Selection
                    Text("Select Lead / Client", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    ExposedDropdownMenuBox(
                        expanded = leadMenuExpanded,
                        onExpandedChange = { leadMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLead?.let { "${it.clientName} (${it.companyName})" } ?: "Select Lead",
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

                    // Follow-up interaction type
                    Text("Interaction Type", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FollowUpType.entries.forEach { type ->
                            val isSelected = followUpType == type
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { followUpType = type }
                            ) {
                                Text(
                                    text = type.name.lowercase().capitalize(Locale.getDefault()),
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = followUpNotes,
                        onValueChange = { followUpNotes = it },
                        label = { Text("Agenda / Call Notes") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Task fields
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Title *") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = taskDescription,
                        onValueChange = { taskDescription = it },
                        label = { Text("Task Description") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Priority
                    Text("Priority Level", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TaskPriority.entries.forEach { p ->
                            val isSelected = taskPriority == p
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(p.colorHex) else Color(p.colorHex).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { taskPriority = p }
                            ) {
                                Text(
                                    text = p.label,
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(p.colorHex),
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Assign to Team Member
                    Text("Assign to Team Member", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    ExposedDropdownMenuBox(
                        expanded = repMenuExpanded,
                        onExpandedChange = { repMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = assignedRep,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repMenuExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = repMenuExpanded,
                            onDismissRequest = { repMenuExpanded = false }
                        ) {
                            teamMembers.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text("${member.name} (${member.role.displayName})") },
                                    onClick = {
                                        assignedRep = member.name
                                        repMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Action Buttons
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
                        onClick = {
                            if (selectedItemType == ScheduleItemType.FOLLOW_UP) {
                                onSaveFollowUp(
                                    FollowUpEntity(
                                        leadId = selectedLead?.id ?: 0L,
                                        leadName = selectedLead?.let { "${it.clientName} (${it.companyName})" } ?: "General Follow-up",
                                        clientPhone = selectedLead?.phone ?: "",
                                        scheduledDateTime = selectedDateMillis + 10 * 3600 * 1000L, // 10:00 AM on that day
                                        type = followUpType,
                                        status = FollowUpStatus.PENDING,
                                        notes = followUpNotes
                                    )
                                )
                            } else {
                                if (taskTitle.isNotBlank()) {
                                    onSaveTask(
                                        TaskEntity(
                                            title = taskTitle,
                                            description = taskDescription,
                                            priority = taskPriority,
                                            status = TaskStatus.TODO,
                                            dueDate = selectedDateMillis + 17 * 3600 * 1000L, // 5:00 PM on that day
                                            assignedTo = assignedRep
                                        )
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_calendar_item_btn")
                    ) {
                        Text(strings.save)
                    }
                }
            }
        }
    }
}

/**
 * Calculates the 7 calendar days (Monday to Sunday) for the given anchor timestamp
 */
fun getWeekDays(anchorMillis: Long): List<Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = anchorMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    return (0..6).map {
        val time = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        time
    }
}

/**
 * Checks if two millisecond timestamps represent the same calendar day
 */
fun isSameDay(time1: Long, time2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
