package com.example.cloudide.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cloudide.R
import com.example.cloudide.domain.BuildStatus
import com.example.cloudide.ui.components.AppCard
import com.example.cloudide.ui.components.AppTopBar
import com.example.cloudide.ui.components.OutlinedPillButton
import com.example.cloudide.ui.components.PrimaryPillButton
import com.example.cloudide.ui.components.StatCard
import com.example.cloudide.ui.components.StatusChip
import com.example.cloudide.ui.theme.CardWhite
import com.example.cloudide.ui.theme.Ink
import com.example.cloudide.ui.theme.LightBlue
import com.example.cloudide.ui.theme.Lavender
import com.example.cloudide.ui.theme.Peach

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentsGalleryScreen(onMenuClick: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.gallery_title),
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.action_notifications),
                            tint = Ink
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(stringResource(R.string.gallery_section_chips))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BuildStatus.entries.forEach { StatusChip(status = it) }
            }

            SectionTitle(stringResource(R.string.gallery_section_buttons))
            PrimaryPillButton(
                text = stringResource(R.string.gallery_button_primary),
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryPillButton(
                text = stringResource(R.string.gallery_button_loading),
                onClick = {},
                loading = true,
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryPillButton(
                text = stringResource(R.string.gallery_button_disabled),
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedPillButton(
                text = stringResource(R.string.gallery_button_outlined),
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle(stringResource(R.string.gallery_section_stats))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = stringResource(R.string.stat_projects),
                    value = "18",
                    color = Lavender,
                    icon = Icons.Outlined.Folder,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.stat_building),
                    value = "2",
                    color = Peach,
                    icon = Icons.Outlined.Build,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.stat_passed_rate),
                    value = stringResource(R.string.percent_format, 74),
                    color = LightBlue,
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }

            SectionTitle(stringResource(R.string.gallery_section_cards))
            SampleCard(Lavender)
            SampleCard(Peach)
            SampleCard(LightBlue)
            SampleCard(CardWhite)

            SectionTitle(stringResource(R.string.gallery_section_top_bar))
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                AppTopBar(
                    title = stringResource(R.string.gallery_top_bar_sample),
                    onMenuClick = {},
                    applyStatusBarPadding = false,
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.action_notifications),
                                tint = Ink
                            )
                        }
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = Ink
    )
}

@Composable
private fun SampleCard(color: androidx.compose.ui.graphics.Color) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = color
    ) {
        Text(
            text = stringResource(R.string.gallery_card_title),
            style = MaterialTheme.typography.titleMedium,
            color = Ink
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.gallery_card_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Ink
        )
    }
}