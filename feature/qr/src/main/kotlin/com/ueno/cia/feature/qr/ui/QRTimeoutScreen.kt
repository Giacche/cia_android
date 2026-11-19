package com.ueno.cia.feature.qr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.images.DSIllustration
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** Equivale a `QrTimeoutScreen`: ilustración, título y subtítulo centrados, y dos botones abajo. */
@Composable
fun QRTimeoutRoute(
    viewModel: QRTimeoutViewModel,
    onDestination: (QRTimeoutDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    val screen = viewModel.screen
    CIAScaffold(title = screen.appBarTitle, onBack = onBack, modifier = modifier.testTag(screen.tag)) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = Spacing.l).padding(bottom = Spacing.x3l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DSIllustration(screen.illustration, modifier = Modifier.height(160.dp))
                Spacer(Modifier.height(Spacing.x2l))
                DSText(screen.title, DSTextStyle.DisplayM, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.l))
                DSText(screen.subtitle, DSTextStyle.BodyL, textAlign = TextAlign.Center)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                DSButton(
                    text = screen.primaryButton.text,
                    onClick = viewModel::didTapPrimary,
                    style = DSButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth().testTag(screen.primaryButton.tag),
                )
                DSButton(
                    text = screen.secondaryButton.text,
                    onClick = viewModel::didTapSecondary,
                    style = DSButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth().testTag(screen.secondaryButton.tag),
                )
            }
        }
    }
}
