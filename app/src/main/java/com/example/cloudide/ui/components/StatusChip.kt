package com.example.cloudide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cloudide.R
import com.example.cloudide.domain.BuildStatus
import com.example.cloudide.ui.theme.ChipAmber
import com.example.cloudide.ui.theme.ChipGray
import com.example.cloudide.ui.theme.ChipGreen
import com.example.cloudide.ui.theme.ChipRed
import com.example.cloudide.ui.theme.OnChipAmber
import com.example.cloudide.ui.theme.OnChipGray
import com.example.cloudide.ui.theme.OnChipGreen
import com.example.cloudide.ui.theme.OnChipRed

@Composable
fun AppChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(contentColor, CircleShape)
            )
        }
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun StatusChip(
    status: BuildStatus,
    modifier: Modifier = Modifier
) {
    val label: String
    val container: Color
    val content: Color
    when (status) {
        BuildStatus.Passed -> {
            label = stringResource(R.string.status_passed)
            container = ChipGreen
            content = OnChipGreen
        }
        BuildStatus.Building -> {
            label = stringResource(R.string.status_building)
            container = ChipAmber
            content = OnChipAmber
        }
        BuildStatus.Failed -> {
            label = stringResource(R.string.status_failed)
            container = ChipRed
            content = OnChipRed
        }
        BuildStatus.Pending -> {
            label = stringResource(R.string.status_pending)
            container = ChipGray
            content = OnChipGray
        }
        BuildStatus.Cancelled -> {
            label = stringResource(R.string.status_cancelled)
            container = ChipGray
            content = OnChipGray
        }
    }
    AppChip(
        text = label,
        containerColor = container,
        contentColor = content,
        modifier = modifier,
        showDot = status == BuildStatus.Building
    )
}