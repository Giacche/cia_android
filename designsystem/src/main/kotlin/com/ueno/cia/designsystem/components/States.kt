package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.R
import com.ueno.cia.designsystem.images.DSIllustration
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** `RetryWidget`: error de una sección con botón de reintento. */
@Composable
fun RetryView(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.x2l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DSIllustration("plugIllustration", modifier = Modifier.height(80.dp))
        DSText(stringResource(R.string.ds_retry_title), DSTextStyle.BodyLStrong, textAlign = TextAlign.Center)
        DSText(stringResource(R.string.ds_retry_description), DSTextStyle.BodyM, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.l))
        DSButton(
            text = stringResource(R.string.ds_retry),
            onClick = onRetry,
            style = DSButtonStyle.Secondary,
        )
    }
}

/** Error de pantalla completa (`FeedBackWrapper.error`). */
@Composable
fun FeedbackErrorScreen(onRetry: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Box(modifier = modifier.fillMaxSize().background(DSColor.Neutral50)) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = Spacing.xl).padding(bottom = Spacing.x3l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            DSIllustration("plugIllustration", modifier = Modifier.height(120.dp))
            Spacer(Modifier.height(Spacing.xs))
            DSText(stringResource(R.string.ds_error_screen_title), DSTextStyle.DisplayM, textAlign = TextAlign.Center)
            DSText(
                stringResource(R.string.ds_error_screen_description),
                DSTextStyle.BodyL,
                color = DSColor.Neutral600,
                textAlign = TextAlign.Center,
            )
        }
        DSButton(
            text = stringResource(R.string.ds_retry),
            onClick = onRetry,
            style = DSButtonStyle.Primary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = Spacing.l, vertical = Spacing.s)
                .fillMaxWidth(),
        )
        content()
    }
}
