package com.example.cloudide.ui.drawer

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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cloudide.BuildConfig
import com.example.cloudide.R
import com.example.cloudide.ui.components.AppChip
import com.example.cloudide.ui.navigation.Routes
import com.example.cloudide.ui.theme.ChipGreen
import com.example.cloudide.ui.theme.Cream
import com.example.cloudide.ui.theme.CreamDark
import com.example.cloudide.ui.theme.Ink
import com.example.cloudide.ui.theme.InkMuted
import com.example.cloudide.ui.theme.OnChipGreen
import com.example.cloudide.ui.theme.OutlineSoft
import com.example.cloudide.ui.theme.Peach

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
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                DrawerHeader(state)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = OutlineSoft)
                Spacer(Modifier.height(16.dp))

                DrawerItem(
                    label = stringResource(R.string.nav_projects),
                    icon = Icons.Outlined.Folder,
                    selected = currentRoute == Routes.Projects,
                    onClick = { onNavigate(Routes.Projects) }
                )
                DrawerItem(
                    label = stringResource(R.string.nav_editor),
                    icon = Icons.Outlined.Code,
                    selected = currentRoute == Routes.Editor,
                    onClick = { onNavigate(Routes.Editor) }
                )
                DrawerItem(
                    label = stringResource(R.string.nav_builds),
                    icon = Icons.Outlined.Inventory2,
                    selected = currentRoute == Routes.Build,
                    badge = state.activeBuilds,
                    onClick = { onNavigate(Routes.Build) }
                )
                DrawerItem(
                    label = stringResource(R.string.nav_settings),
                    icon = Icons.Outlined.Settings,
                    selected = currentRoute == Routes.Settings,
                    onClick = { onNavigate(Routes.Settings) }
                )
                if (BuildConfig.DEBUG) {
                    DrawerItem(
                        label = stringResource(R.string.nav_gallery),
                        icon = Icons.Outlined.Palette,
                        selected = currentRoute == Routes.Gallery,
                        onClick = { onNavigate(Routes.Gallery) }
                    )
                }

                if (state.recentRepos.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.drawer_recent_repos).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    state.recentRepos.forEach { repo ->
                        DrawerItem(
                            label = repo,
                            icon = Icons.Outlined.Book,
                            selected = false,
                            onClick = { onRepoClick(repo) }
                        )
                    }
                }
            }

            DrawerItem(
                label = stringResource(R.string.drawer_sign_out),
                icon = Icons.AutoMirrored.Outlined.Logout,
                selected = false,
                onClick = onSignOut
            )
        }
    }
}

@Composable
private fun DrawerHeader(state: DrawerUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CreamDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(32.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = state.username,
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.isConnected) {
                AppChip(
                    text = stringResource(R.string.drawer_connected),
                    containerColor = ChipGreen,
                    contentColor = OnChipGreen,
                    showDot = true
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Int = 0
) {
    val background = if (selected) Ink else Color.Transparent
    val foreground = if (selected) Cream else Ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = foreground)
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Peach),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Ink
                )
            }
        }
    }
}