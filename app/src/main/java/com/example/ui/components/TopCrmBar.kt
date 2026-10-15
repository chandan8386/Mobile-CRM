package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.localization.AppLanguage
import com.example.ui.viewmodel.CrmViewModel

@Composable
fun TopCrmBar(
    viewModel: CrmViewModel,
    unreadNotificationsCount: Int,
    isOffline: Boolean,
    isSyncing: Boolean,
    pendingSyncCount: Int,
    onNotificationsClick: () -> Unit,
    onRoleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMember = viewModel.currentMember.value
    val currentLang = viewModel.currentLanguage.value
    var langMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Logo & Role Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("app_brand_section")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "N",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "NexusCRM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Role Pill (Clickable to switch)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (currentMember.role) {
                                com.example.data.model.RoleType.ADMIN -> MaterialTheme.colorScheme.primaryContainer
                                com.example.data.model.RoleType.SALES_MANAGER -> MaterialTheme.colorScheme.secondaryContainer
                                com.example.data.model.RoleType.SALES_REP -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .clickable { onRoleClick() }
                                .testTag("role_badge_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (currentMember.role) {
                                                com.example.data.model.RoleType.ADMIN -> MaterialTheme.colorScheme.primary
                                                com.example.data.model.RoleType.SALES_MANAGER -> MaterialTheme.colorScheme.secondary
                                                com.example.data.model.RoleType.SALES_REP -> MaterialTheme.colorScheme.outline
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (currentMember.role) {
                                        com.example.data.model.RoleType.ADMIN -> "Admin"
                                        com.example.data.model.RoleType.SALES_MANAGER -> "Manager"
                                        com.example.data.model.RoleType.SALES_REP -> "Sales Rep"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (currentMember.role) {
                                        com.example.data.model.RoleType.ADMIN -> MaterialTheme.colorScheme.onPrimaryContainer
                                        com.example.data.model.RoleType.SALES_MANAGER -> MaterialTheme.colorScheme.onSecondaryContainer
                                        com.example.data.model.RoleType.SALES_REP -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                // Right: Action Icons (Sync, Biometric Lock, Language, Notifications)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sync Status Indicator / Button
                    IconButton(
                        onClick = { viewModel.triggerCloudSync() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("sync_action_button")
                    ) {
                        when {
                            isSyncing -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            isOffline -> {
                                BadgedBox(
                                    badge = {
                                        if (pendingSyncCount > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = Color.White
                                            ) {
                                                Text("$pendingSyncCount", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = "Offline Mode",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Online Synced",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Biometric Lock Quick Action
                    IconButton(
                        onClick = { viewModel.securityManager.lockApp() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("biometric_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Lock App",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Language Selector
                    Box {
                        IconButton(
                            onClick = { langMenuExpanded = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("language_switch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = langMenuExpanded,
                            onDismissRequest = { langMenuExpanded = false }
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${lang.nativeName} (${lang.englishName})",
                                            fontWeight = if (lang == currentLang) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setLanguage(lang)
                                        langMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Notifications Bell with Badge
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("notifications_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadNotificationsCount > 9) "9+" else "$unreadNotificationsCount",
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
