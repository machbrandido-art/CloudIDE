package com.example.cloudide.ui.connect

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cloudide.R
import com.example.cloudide.ui.components.AppCard
import com.example.cloudide.ui.components.PrimaryPillButton
import com.example.cloudide.ui.theme.CardWhite
import com.example.cloudide.ui.theme.Cream
import com.example.cloudide.ui.theme.Ink
import com.example.cloudide.ui.theme.InkMuted
import com.example.cloudide.ui.theme.Lavender
import com.example.cloudide.ui.theme.OnChipRed
import com.example.cloudide.ui.theme.OutlineSoft
import com.example.cloudide.util.SecureWindowEffect

private const val TOKEN_PAGE_URL = "https://github.com/settings/personal-access-tokens/new"

@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: ConnectViewModel = hiltViewModel()
) {
    SecureWindowEffect()

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnConnected by rememberUpdatedState(onConnected)
    val uriHandler = LocalUriHandler.current
    var browserMissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ConnectEvent.Connected) currentOnConnected()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp)
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Ink
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_connect),
                style = MaterialTheme.typography.displaySmall,
                color = Ink
            )

            AppCard(modifier = Modifier.fillMaxWidth(), containerColor = CardWhite) {
                Text(
                    text = stringResource(R.string.connect_token_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.token,
                    onValueChange = viewModel::onTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    isError = state.error != null,
                    visualTransformation = if (state.tokenVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.connect() }),
                    trailingIcon = {
                        IconButton(onClick = viewModel::toggleTokenVisibility) {
                            Icon(
                                imageVector = if (state.tokenVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = stringResource(
                                    if (state.tokenVisible) {
                                        R.string.connect_hide_token
                                    } else {
                                        R.string.connect_show_token
                                    }
                                ),
                                tint = InkMuted
                            )
                        }
                    },
                    supportingText = if (state.error != null) {
                        {
                            Text(
                                text = stringResource((state.error as ConnectError).messageRes()),
                                color = OnChipRed
                            )
                        }
                    } else {
                        null
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Ink,
                        unfocusedBorderColor = OutlineSoft,
                        errorBorderColor = OnChipRed,
                        focusedContainerColor = Cream,
                        unfocusedContainerColor = Cream,
                        errorContainerColor = Cream,
                        disabledContainerColor = Cream,
                        cursorColor = Ink,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    )
                )
            }

            AppCard(modifier = Modifier.fillMaxWidth(), containerColor = Lavender) {
                Text(
                    text = stringResource(R.string.connect_how_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
                Spacer(Modifier.height(12.dp))
                StepRow(number = 1, text = stringResource(R.string.connect_step_1))
                Spacer(Modifier.height(8.dp))
                StepRow(number = 2, text = stringResource(R.string.connect_step_2))
                Spacer(Modifier.height(8.dp))
                StepRow(number = 3, text = stringResource(R.string.connect_step_3))
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            try {
                                uriHandler.openUri(TOKEN_PAGE_URL)
                                browserMissing = false
                            } catch (e: Exception) {
                                browserMissing = true
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.connect_open_github),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.OpenInBrowser,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (browserMissing) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.connect_error_no_browser),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnChipRed
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryPillButton(
                text = stringResource(R.string.action_connect),
                onClick = viewModel::connect,
                enabled = state.token.isNotBlank() && !state.loading,
                loading = state.loading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.connect_stored_securely),
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted
            )
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Ink,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun ConnectError.messageRes(): Int = when (this) {
    ConnectError.InvalidToken -> R.string.connect_error_invalid
    ConnectError.Network -> R.string.connect_error_network
    ConnectError.Unknown -> R.string.connect_error_unknown
}