package com.example.cloudide.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cloudide.R
import com.example.cloudide.domain.BuildInfo
import com.example.cloudide.domain.BuildStatus
import com.example.cloudide.domain.Repo
import com.example.cloudide.ui.components.AppCard
import com.example.cloudide.ui.components.AppTopBar
import com.example.cloudide.ui.components.PrimaryPillButton
import com.example.cloudide.ui.components.StatCard
import com.example.cloudide.ui.components.StatusChip
import com.example.cloudide.ui.theme.CardShape
import com.example.cloudide.ui.theme.CardWhite
import com.example.cloudide.ui.theme.ChipAmber
import com.example.cloudide.ui.theme.ChipGray
import com.example.cloudide.ui.theme.Cream
import com.example.cloudide.ui.theme.Ink
import com.example.cloudide.ui.theme.InkMuted
import com.example.cloudide.ui.theme.Lavender
import com.example.cloudide.ui.theme.LightBlue
import com.example.cloudide.ui.theme.OnChipAmber
import com.example.cloudide.ui.theme.OutlineSoft
import com.example.cloudide.ui.theme.Peach
import com.example.cloudide.util.formatClock
import com.example.cloudide.util.relativeTime

private data class ProjectStats(val building: Int, val passRate: Int?)

private fun computeStats(repos: List<Repo>, runs: Map<String, RunState>): ProjectStats {
    var building = 0
    var passed = 0
    var failed = 0
    repos.forEach { repo ->
        val run = (runs[repo.fullName] as? RunState.Loaded)?.info as? BuildInfo.Run
            ?: return@forEach
        when (run.status) {
            BuildStatus.Building -> building++
            BuildStatus.Passed -> passed++
            BuildStatus.Failed -> failed++
            else -> Unit
        }
    }
    val finished = passed + failed
    return ProjectStats(building, if (finished == 0) null else passed * 100 / finished)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onMenuClick: () -> Unit,
    onOpenEditor: () -> Unit,
    onViewError: () -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnOpenEditor by rememberUpdatedState(onOpenEditor)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ProjectsEvent.OpenEditor) currentOnOpenEditor()
        }
    }

    val filtered = remember(state.repos, state.query) {
        val q = state.query.trim()
        if (q.isEmpty()) {
            state.repos
        } else {
            state.repos.filter {
                it.name.contains(q, ignoreCase = true) || it.owner.contains(q, ignoreCase = true)
            }
        }
    }
    val stats = remember(state.repos, state.runs) { computeStats(state.repos, state.runs) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "", onMenuClick = onMenuClick) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddDialog,
                containerColor = Ink,
                contentColor = Cream,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.projects_add)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                state.isInitialLoading -> {
                    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        ScreenTitle()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Ink)
                        }
                    }
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.repos.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ScreenTitle()
                                val error = state.error
                                if (error != null) {
                                    MessageCard(
                                        icon = error.icon(),
                                        message = error.message(),
                                        actionLabel = stringResource(R.string.projects_retry),
                                        onAction = viewModel::refresh
                                    )
                                } else {
                                    MessageCard(
                                        icon = Icons.Outlined.Folder,
                                        message = stringResource(R.string.projects_empty),
                                        actionLabel = null,
                                        onAction = null
                                    )
                                }
                            }
                        } else {
                            val bannerMessage: String? = when {
                                state.error != null -> state.error!!.message()
                                state.rateLimitResetAt != null -> stringResource(
                                    R.string.projects_error_rate_limit,
                                    formatClock(state.rateLimitResetAt!!)
                                )
                                else -> null
                            }
                            ProjectsGrid(
                                state = state,
                                filtered = filtered,
                                stats = stats,
                                bannerMessage = bannerMessage,
                                showRetry = state.error != null,
                                viewModel = viewModel,
                                onViewError = onViewError
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAddDialog,
            containerColor = CardWhite,
            shape = CardShape,
            title = { Text(stringResource(R.string.projects_new_title)) },
            text = { Text(stringResource(R.string.projects_new_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissAddDialog) {
                    Text(text = stringResource(R.string.action_ok), color = Ink)
                }
            }
        )
    }
}

@Composable
private fun ProjectsGrid(
    state: ProjectsUiState,
    filtered: List<Repo>,
    stats: ProjectStats,
    bannerMessage: String?,
    showRetry: Boolean,
    viewModel: ProjectsViewModel,
    onViewError: () -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        item(key = "title", span = StaggeredGridItemSpan.FullLine) {
            ScreenTitle()
        }

        item(key = "stats", span = StaggeredGridItemSpan.FullLine) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stat_projects),
                    value = state.repos.size.toString() + if (state.hasMore) "+" else "",
                    color = Lavender,
                    icon = Icons.Outlined.Folder,
                    modifier = Modifier.width(140.dp)
                )
                StatCard(
                    title = stringResource(R.string.stat_building),
                    value = stats.building.toString(),
                    color = Peach,
                    icon = Icons.Outlined.Build,
                    modifier = Modifier.width(140.dp)
                )
                StatCard(
                    title = stringResource(R.string.stat_passed_rate),
                    value = stats.passRate?.let { stringResource(R.string.percent_format, it) }
                        ?: "—",
                    color = LightBlue,
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    modifier = Modifier.width(140.dp)
                )
            }
        }

        item(key = "search", span = StaggeredGridItemSpan.FullLine) {
            SearchField(query = state.query, onChange = viewModel::onQueryChange)
        }

        if (bannerMessage != null) {
            item(key = "banner", span = StaggeredGridItemSpan.FullLine) {
                InfoBanner(
                    message = bannerMessage,
                    actionLabel = if (showRetry) stringResource(R.string.projects_retry) else null,
                    onAction = if (showRetry) viewModel::refresh else null
                )
            }
        }

        if (filtered.isEmpty() && state.query.isNotBlank() && !state.hasMore) {
            item(key = "no_match", span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = stringResource(R.string.projects_no_match),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkMuted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        items(filtered, key = { it.id }) { repo ->
            // Lazy: runs only while this card is composed (visible), cancelled when scrolled away.
            LaunchedEffect(repo.fullName, state.generation) {
                viewModel.loadRun(repo)
            }
            RepoCard(
                repo = repo,
                run = state.runs[repo.fullName],
                onClick = { viewModel.openRepo(repo) },
                onViewError = onViewError
            )
        }

        if (state.hasMore) {
            item(key = "more", span = StaggeredGridItemSpan.FullLine) {
                // Composed only when the user reaches the end of the list.
                LaunchedEffect(state.repos.size) { viewModel.loadMore() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Ink,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenTitle() {
    Text(
        text = stringResource(R.string.nav_projects),
        style = MaterialTheme.typography.displaySmall,
        color = Ink,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = CircleShape,
        placeholder = {
            Text(
                text = stringResource(R.string.projects_search_hint),
                color = InkMuted
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = InkMuted
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.projects_search_clear),
                        tint = InkMuted
                    )
                }
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = OutlineSoft,
            focusedContainerColor = CardWhite,
            unfocusedContainerColor = CardWhite,
            cursorColor = Ink,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink
        )
    )
}

@Composable
private fun RepoCard(
    repo: Repo,
    run: RunState?,
    onClick: () -> Unit,
    onViewError: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = CardWhite,
        onClick = onClick
    ) {
        Text(
            text = repo.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))

        when (run) {
            null, RunState.Loading -> {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(ChipGray.copy(alpha = 0.6f))
                )
            }

            RunState.Unavailable -> {
                Text(
                    text = stringResource(R.string.projects_status_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted
                )
            }

            is RunState.Loaded -> when (val info = run.info) {
                BuildInfo.NoRuns -> {
                    Text(
                        text = stringResource(R.string.projects_no_builds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted
                    )
                }

                is BuildInfo.Run -> {
                    StatusChip(status = info.status)
                    if (info.commitMessage.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = info.commitMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (info.branch.isNotEmpty()) {
                            BranchPill(
                                branch = info.branch,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        val time = remember(info.timeMillis) { relativeTime(info.timeMillis) }
                        if (time.isNotEmpty()) {
                            Text(
                                text = time,
                                style = MaterialTheme.typography.labelMedium,
                                color = InkMuted,
                                maxLines = 1
                            )
                        }
                    }
                    if (info.status == BuildStatus.Failed) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.projects_view_error),
                            style = MaterialTheme.typography.labelLarge,
                            color = Ink,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClick = onViewError)
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchPill(branch: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, OutlineSoft, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = branch,
            style = MaterialTheme.typography.labelMedium,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoBanner(message: String, actionLabel: String?, onAction: (() -> Unit)?) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = ChipAmber,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = OnChipAmber
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = OnChipAmber,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    icon: ImageVector,
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = CardWhite,
        contentPadding = PaddingValues(24.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryPillButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun ProjectsError.icon(): ImageVector = when (this) {
    ProjectsError.Offline -> Icons.Outlined.CloudOff
    is ProjectsError.RateLimited -> Icons.Outlined.Schedule
    ProjectsError.Unknown -> Icons.Outlined.ErrorOutline
}

@Composable
private fun ProjectsError.message(): String = when (this) {
    ProjectsError.Offline -> stringResource(R.string.projects_error_offline)
    is ProjectsError.RateLimited ->
        stringResource(R.string.projects_error_rate_limit, formatClock(resetAtMillis))
    ProjectsError.Unknown -> stringResource(R.string.projects_error_unknown)
}