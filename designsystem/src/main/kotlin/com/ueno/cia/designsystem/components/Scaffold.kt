package com.ueno.cia.designsystem.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.R
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/**
 * Estructura común de las pantallas de CIA (`CIAViewController` en iOS): barra con atrás y título del BFF en
 * mayúsculas, contenido y un footer opcional que sube con el teclado.
 *
 * [onBack] se usa para el botón y para el atrás del sistema, así una pantalla puede reemplazar el comportamiento
 * por defecto (por ejemplo volver a la Home en lugar de al paso anterior).
 */
@Composable
fun CIAScaffold(
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    StatusBarIcons(darkIcons = true)
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DSColor.Neutral50)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Spacing.l)) {
            DSIconButton(
                icon = "arrowLeft",
                mode = DSIconButtonMode.Light,
                contentDescription = stringResource(R.string.ds_back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).testTag("cia_back_btn"),
            )
            DSText(
                text = title.orEmpty().uppercase(),
                style = DSTextStyle.OverlineM,
                color = DSColor.Neutral500,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Spacing.x3l)
                    .semantics { heading() },
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        footer()
    }
}
