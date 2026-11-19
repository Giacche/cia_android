package com.ueno.cia.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.StatusBarIcons
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** Destino de una ruta cuya feature todavía no se migró (`pendingScreenTitle` en iOS). Muestra la ruta que se pidió. */
@Composable
internal fun PendingScreen(route: String, onBack: () -> Unit) {
    StatusBarIcons(darkIcons = true)
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(DSColor.Neutral50).statusBarsPadding().testTag("cia_pending_screen")) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            DSText(stringResource(R.string.cia_pending_title), DSTextStyle.DisplayM, textAlign = TextAlign.Center)
            DSText(stringResource(R.string.cia_pending_description), DSTextStyle.BodyL, color = DSColor.Neutral600, textAlign = TextAlign.Center)
            DSText(route, DSTextStyle.BodyM, color = DSColor.Neutral500, textAlign = TextAlign.Center, modifier = Modifier.testTag("cia_pending_route"))
        }
        DSButton(
            text = stringResource(R.string.cia_pending_accept),
            onClick = onBack,
            style = DSButtonStyle.Primary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = Spacing.l, vertical = Spacing.s)
                .fillMaxWidth()
                .testTag("cia_pending_accept_btn"),
        )
    }
}
