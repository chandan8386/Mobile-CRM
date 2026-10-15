package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings

data class NavTabItem(
    val index: Int,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun BottomCrmNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    strings: LocalizedStrings,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavTabItem(0, strings.dashboard, Icons.Default.Dashboard, "nav_tab_dashboard"),
        NavTabItem(1, strings.leads, Icons.Default.Contacts, "nav_tab_leads"),
        NavTabItem(2, strings.followUps, Icons.Default.Schedule, "nav_tab_followups"),
        NavTabItem(3, strings.projectsTasks, Icons.Default.Assignment, "nav_tab_projects"),
        NavTabItem(4, strings.teamAdmin, Icons.Default.AdminPanelSettings, "nav_tab_admin"),
        NavTabItem(5, strings.settings, Icons.Default.Settings, "nav_tab_settings")
    )

    NavigationBar(
        modifier = modifier.testTag("bottom_crm_navigation_bar"),
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            val selected = selectedTab == item.index
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}
