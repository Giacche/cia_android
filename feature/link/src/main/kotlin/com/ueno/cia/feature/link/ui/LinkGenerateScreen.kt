package com.ueno.cia.feature.link.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.designsystem.components.FeedbackErrorScreen
import com.ueno.cia.designsystem.components.LoaderScreen
import com.ueno.cia.designsystem.components.StatusBarIcons

/** Equivale a `LinkGenerateScreen`: solo loader mientras se genera el link y, si falla, el error de pantalla completa. */
@Composable
fun LinkGenerateRoute(
    viewModel: LinkGenerateViewModel,
    onDestination: (LinkGenerateDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    StatusBarIcons(darkIcons = true)
    // Sin gesto de volver mientras se genera (`allowsInteractivePop = false`)
    BackHandler(enabled = state == LinkGenerateState.Loading) {}
    Box(modifier.fillMaxSize().testTag("cia_link_generate")) {
        when (state) {
            LinkGenerateState.Loading -> LoaderScreen()
            LinkGenerateState.Failed -> FeedbackErrorScreen(onRetry = viewModel::didTapBackFromError, modifier = Modifier.testTag("cia_error_screen"))
        }
    }
}
