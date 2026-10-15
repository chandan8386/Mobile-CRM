package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings

@Composable
fun SyncBanner(
    isOffline: Boolean,
    isSyncing: Boolean,
    pendingCount: Int,
    strings: LocalizedStrings,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline || pendingCount > 0 || isSyncing,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = if (isOffline) Color(0xFFFEF3C7) else Color(0xFFE0F2FE),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sync_banner_strip")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0284C7)
                        )
                    } else if (isOffline) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Pending Sync",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when {
                            isSyncing -> strings.syncingData
                            isOffline && pendingCount > 0 -> "${strings.offlineMode} • $pendingCount ${strings.pendingSyncCount}"
                            isOffline -> "${strings.offlineMode} • Local edits saved to Room"
                            else -> "$pendingCount ${strings.pendingSyncCount}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOffline) Color(0xFF92400E) else Color(0xFF0369A1)
                    )
                }

                if (!isSyncing) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isOffline) Color(0xFFF59E0B) else Color(0xFF0284C7),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSyncClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("banner_sync_now_btn")
                    ) {
                        Text(
                            text = strings.syncNow,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
