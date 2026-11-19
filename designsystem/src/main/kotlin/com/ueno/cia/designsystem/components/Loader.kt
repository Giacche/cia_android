package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.R
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** `GuLoader`: spinner con mensaje, ocupa toda la pantalla (Flutter usa una animación Lottie). */
@Composable
fun LoaderScreen(modifier: Modifier = Modifier, label: String = stringResource(R.string.ds_loading)) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DSColor.Neutral50)
            .semantics { contentDescription = label }
            .testTag("cia_loader"),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(bottom = Spacing.x3l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.x2l),
        ) {
            CircularProgressIndicator(color = DSColor.Black, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
            DSText(label, DSTextStyle.BodyLStrong, textAlign = TextAlign.Center)
        }
    }
}
