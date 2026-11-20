package com.ueno.cia.feature.cpf.ui

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.DSTextField
import com.ueno.cia.designsystem.components.ScreenFooter
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing

/** Punto de entrada del CPF para el host. */
@Composable
fun CPFRoute(viewModel: CPFViewModel, onDestination: (CPFDestination) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    LaunchedEffect(viewModel) { viewModel.destinations.collect { navigate(it) } }
    CPFScreen(state, viewModel::didChangeCpf, viewModel::didTapContinue, onBack, modifier)
}

/** Equivale a `CpfScreen`: título, campo de CPF y footer con un botón. */
@Composable
fun CPFScreen(
    state: CPFViewState,
    onCpfChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screen = state.screen
    var text by rememberSaveable { mutableStateOf("") }
    CIAScaffold(
        title = screen.appBarTitle,
        onBack = onBack,
        modifier = modifier.testTag("cia_cpf"),
        footer = {
            ScreenFooter {
                DSButton(
                    text = screen.primaryButton.text,
                    onClick = onContinue,
                    style = DSButtonStyle.Primary,
                    enabled = state.buttonEnabled,
                    modifier = Modifier.fillMaxWidth().testTag("cia_cpf_btn"),
                )
            }
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l).padding(top = Spacing.xs),
        ) {
            Spacer(Modifier.height(Spacing.xl))
            DSText(screen.title, DSTextStyle.DisplayS, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.xl))
            DSTextField(
                label = screen.numberInput.title,
                value = text,
                onValueChange = {
                    text = it
                    onCpfChange(it)
                },
                helper = state.helper,
                error = state.error,
                maxLength = screen.numberInput.maxLength,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.testTag("cia_cpf_input"),
            )
        }
    }
}
