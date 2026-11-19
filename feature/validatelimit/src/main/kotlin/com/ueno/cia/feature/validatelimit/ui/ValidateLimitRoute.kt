package com.ueno.cia.feature.validatelimit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.designsystem.R as DSR
import com.ueno.cia.designsystem.components.DSIconButton
import com.ueno.cia.designsystem.components.DSIconButtonMode
import com.ueno.cia.designsystem.components.FeedbackErrorScreen
import com.ueno.cia.designsystem.components.LoaderScreen
import com.ueno.cia.designsystem.components.StatusBarIcons
import com.ueno.cia.designsystem.tokens.Spacing

/** Equivale a `ValidateLimitScreen`: sin barra, solo loader y, si falla, el error de pantalla completa. */
@Composable
fun ValidateLimitRoute(
    viewModel: ValidateLimitViewModel,
    onDestination: (ValidateLimitDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }

    Box(modifier.fillMaxSize().testTag("cia_validate_limit")) {
        when (state) {
            ValidateLimitState.Loading -> {
                StatusBarIcons(darkIcons = true)
                LoaderScreen()
            }
            ValidateLimitState.Failed -> {
                StatusBarIcons(darkIcons = true)
                // Igual que en la Home: el botón de reintentar vuelve atrás, no recarga
                FeedbackErrorScreen(onRetry = viewModel::didTapBackFromError, modifier = Modifier.testTag("cia_error_screen"))
            }
        }
    }
}
