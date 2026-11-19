package com.ueno.cia.feature.link.ui

import androidx.lifecycle.ViewModel
import com.ueno.cia.core.model.DetailCardModel
import com.ueno.cia.core.model.LinkPreConfirmModel
import com.ueno.cia.core.model.TycModel
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.LinkFlowInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

data class LinkPreConfirmViewState(
    val screen: LinkPreConfirmModel,
    val accepted: Boolean = false,
) {
    val tyc: TycModel? get() = screen.tyc

    /** Con T&C el botón se habilita al marcar la casilla (Flutter: `disabled: dataTyc != null && !isChecked`). */
    val buttonEnabled: Boolean get() = tyc == null || accepted
}

sealed interface LinkPreConfirmDestination {
    data class Push(val route: CIARoute) : LinkPreConfirmDestination
    data class OpenDocument(val url: String) : LinkPreConfirmDestination
}

/** Equivale a `LinkPreConfirmNotifier` + `LinkPreConfirmScreen`. Une los items del BFF con los que arma el Monto. */
class LinkPreConfirmViewModel(private val input: LinkFlowInput) : ViewModel() {
    private val _state = MutableStateFlow(
        input.linkPreConfirmScreen.let { base ->
            LinkPreConfirmViewState(
                base.copy(
                    detailCard = DetailCardModel(
                        title = base.detailCard.title,
                        items = base.detailCard.items.orEmpty() + input.extraDetailItems,
                        inlineMessage = base.detailCard.inlineMessage,
                    ),
                ),
            )
        },
    )
    val state: StateFlow<LinkPreConfirmViewState> = _state.asStateFlow()

    private val _destinations = Channel<LinkPreConfirmDestination>(Channel.BUFFERED)
    val destinations: Flow<LinkPreConfirmDestination> = _destinations.receiveAsFlow()

    fun didToggleAccepted() = _state.update { it.copy(accepted = !it.accepted) }

    fun didTapTyc() {
        val url = _state.value.tyc?.linkUrl ?: return
        _destinations.trySend(LinkPreConfirmDestination.OpenDocument(url))
    }

    /**
     * `goToNextScreen`: en el flujo de link, la generación. (Los orígenes onboarding PDS, add-ons y productos llegan
     * con sus features.)
     */
    fun didTapContinue() {
        if (!_state.value.buttonEnabled) return
        _destinations.trySend(LinkPreConfirmDestination.Push(CIARoute.LinkGenerate(input)))
    }
}
