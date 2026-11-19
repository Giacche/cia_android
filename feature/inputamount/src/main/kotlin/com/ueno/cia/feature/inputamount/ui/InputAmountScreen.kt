package com.ueno.cia.feature.inputamount.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.designsystem.components.AmountInput
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.DSTextField
import com.ueno.cia.designsystem.components.FooterData
import com.ueno.cia.designsystem.components.ScreenFooter
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** Punto de entrada del Monto para el host: conecta el [InputAmountViewModel] con la pantalla. */
@Composable
fun InputAmountRoute(
    viewModel: InputAmountViewModel,
    onDestination: (InputAmountDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    InputAmountScreen(
        state = state,
        currency = viewModel.currency,
        onAmountChange = viewModel::didChangeAmount,
        onDescriptionChange = viewModel::didChangeLinkDescription,
        onContinue = viewModel::didTapContinue,
        onBack = viewModel::didTapBack,
        modifier = modifier,
    )
}

/** Equivale a `InputAmountScreen`: título, monto, descripción (solo links) y footer con resumen y botón. */
@Composable
fun InputAmountScreen(
    state: InputAmountViewState,
    currency: com.ueno.cia.core.util.CIACurrency,
    onAmountChange: (java.math.BigDecimal) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screen = state.screen
    CIAScaffold(
        title = screen.appBarTitle,
        onBack = onBack,
        modifier = modifier.testTag(screen.tag),
        footer = {
            ScreenFooter(
                data = if (state.showFooterSummary) {
                    FooterData.Multiple(
                        leftLabel = state.totalAmountTitle,
                        leftValue = state.commissionTitle,
                        rightLabel = state.totalAmountValue,
                        rightValue = state.commissionValue,
                    )
                } else {
                    null
                },
            ) {
                DSButton(
                    text = state.buttonText,
                    onClick = onContinue,
                    style = DSButtonStyle.Primary,
                    enabled = !state.buttonDisabled,
                    modifier = Modifier.fillMaxWidth().testTag(screen.primaryButton.tag),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l)
                .padding(top = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.x2l))
            DSText(screen.data.title, DSTextStyle.DisplayS, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.x2l))
            AmountInput(
                currency = currency,
                onAmountChange = onAmountChange,
                errorMessage = state.errorMessage,
            )
            screen.data.descriptionInput?.let { rule ->
                Spacer(Modifier.height(Spacing.l))
                var description by rememberSaveable { mutableStateOf("") }
                DSTextField(
                    label = rule.title,
                    value = description,
                    onValueChange = {
                        description = it
                        onDescriptionChange(it)
                    },
                    helper = rule.helper,
                    maxLength = rule.maxLength,
                    modifier = Modifier.fillMaxWidth().testTag("cia_link_description_input"),
                )
            }
        }
    }
}
