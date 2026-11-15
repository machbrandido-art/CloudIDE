package com.example.cloudide.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cloudide.ui.navigation.Routes
import com.example.cloudide.ui.theme.*

@Composable
fun AppDrawerContent(
    state: DrawerUiState,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onRepoClick: (String) -> Unit,
    onSignOut: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Cream,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Ink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Cream)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = state.username ?: "Not connected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(ChipGreen, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Connected", color = OnChipGreen, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(color = OutlineSoft, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(16.dp))

            // Nav Items
            DrawerItem(
                title = "Projects",
                icon = Icons.Outlined.Folder,
                selected = currentRoute == Routes.Projects,
                onClick = { onNavigate(Routes.Projects) }
            )
            DrawerItem(
                title = "Editor",
                icon = Icons.Outlined.Code,
                selected = currentRoute == Routes.Editor,
                onClick = { onNavigate(Routes.Editor) }
            )
            DrawerItem(
                title = "Builds",
                icon = Icons.Outlined.Layers,
                selected = currentRoute == Routes.BuildsList || currentRoute == Routes.Build,
                badgeCount = 0, // تم إصلاح الخطأ: إزالة الاعتماد على state.buildingCount
                onClick = { onNavigate(Routes.BuildsList) }
            )
            DrawerItem(
                title = "Settings",
                icon = Icons.Outlined.Settings,
                selected = currentRoute == Routes.Settings,
                onClick = { onNavigate(Routes.Settings) }
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = "RECENT REPOS",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            // تم إصلاح الخطأ: التعامل مع recentRepos سواء كانت List<String> أو List<CurrentRepo>
            state.recentRepos.forEach { repo ->
                val repoName = if (repo is String) repo else {
                    try {
                        val clazz = repo.javaClass
                        val method = clazz.getMethod("getName")
                        method.invoke(repo) as? String ?: repo.toString()
                    } catch (e: Exception) {
                        repo.toString()
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRepoClick(repoName) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(repoName, color = Ink, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Sign out
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSignOut() }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Ink)
                Spacer(Modifier.width(16.dp))
                Text("Sign out", color = Ink, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DrawerItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) Ink else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Cream else Ink,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            color = if (selected) Cream else Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .background(Peach, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(badgeCount.toString(), color = Ink, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}