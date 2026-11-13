package com.example.cloudide.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cloudide.R
import com.example.cloudide.ui.components.AppChip
import com.example.cloudide.ui.components.PrimaryPillButton
import com.example.cloudide.ui.theme.CardShape
import com.example.cloudide.ui.theme.ChipAmber
import com.example.cloudide.ui.theme.ChipGreen
import com.example.cloudide.ui.theme.ChipRed
import com.example.cloudide.ui.theme.Cream
import com.example.cloudide.ui.theme.Ink
import com.example.cloudide.ui.theme.InkMuted
import com.example.cloudide.ui.theme.Lavender
import com.example.cloudide.ui.theme.LightBlue
import com.example.cloudide.ui.theme.OnChipGreen
import com.example.cloudide.ui.theme.OutlineSoft
import com.example.cloudide.ui.theme.Peach
import com.example.cloudide.ui.theme.ShadowSoft
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val front: Color,
    val middle: Color,
    val back: Color
)

private val pages = listOf(
    OnboardingPage(
        R.string.onboarding_title_1, R.string.onboarding_subtitle_1,
        front = Lavender, middle = Peach, back = LightBlue
    ),
    OnboardingPage(
        R.string.onboarding_title_2, R.string.onboarding_subtitle_2,
        front = Peach, middle = Lavender, back = LightBlue
    ),
    OnboardingPage(
        R.string.onboarding_title_3, R.string.onboarding_subtitle_3,
        front = LightBlue, middle = Lavender, back = Peach
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(onClick = onFinish) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            PageContent(page = pages[index], index = index)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { i ->
                val selected = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (selected) Ink else OutlineSoft)
                )
            }
        }

        PrimaryPillButton(
            text = stringResource(R.string.action_continue),
            onClick = {
                if (pagerState.currentPage >= pages.lastIndex) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun PageContent(page: OnboardingPage, index: Int) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.displaySmall,
            color = Ink
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = InkMuted
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CardStack(page = page, index = index)
        }
    }
}

@Composable
private fun CardStack(page: OnboardingPage, index: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        StackCard(color = page.back, rotation = 8f, modifier = Modifier.fillMaxSize(0.88f))
        StackCard(color = page.middle, rotation = -6f, modifier = Modifier.fillMaxSize(0.9f))
        StackCard(color = page.front, rotation = 3f, modifier = Modifier.fillMaxSize(0.92f)) {
            when (index) {
                0 -> CodeWindow()
                1 -> PhoneToCloud()
                else -> BuildSteps()
            }
        }
    }
}

@Composable
private fun StackCard(
    color: Color,
    rotation: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .rotate(rotation)
            .shadow(
                elevation = 6.dp,
                shape = CardShape,
                ambientColor = ShadowSoft,
                spotColor = ShadowSoft
            )
            .background(color, CardShape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

private val codeLines = listOf(
    listOf(56 to ChipRed, 84 to ChipGreen),
    listOf(40 to LightBlue, 100 to ChipAmber),
    listOf(72 to Lavender, 48 to ChipGreen),
    listOf(32 to ChipRed, 96 to LightBlue),
    listOf(64 to ChipAmber, 40 to Lavender)
)

@Composable
private fun CodeWindow() {
    Column(
        modifier = Modifier
            .fillMaxSize(0.72f)
            .clip(RoundedCornerShape(16.dp))
            .background(Ink)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        codeLines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                line.forEach { (widthDp, color) ->
                    Box(
                        modifier = Modifier
                            .width(widthDp.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneToCloud() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.PhoneAndroid,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(72.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(28.dp)
        )
        Icon(
            imageVector = Icons.Outlined.Cloud,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(72.dp)
        )
    }
}

@Composable
private fun BuildSteps() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf(
            R.string.onboarding_step_deps,
            R.string.onboarding_step_build,
            R.string.onboarding_step_apk
        ).forEach { res ->
            AppChip(
                text = stringResource(res),
                containerColor = ChipGreen,
                contentColor = OnChipGreen
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Ink)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_install),
                style = MaterialTheme.typography.labelLarge,
                color = Cream
            )
        }
    }
}