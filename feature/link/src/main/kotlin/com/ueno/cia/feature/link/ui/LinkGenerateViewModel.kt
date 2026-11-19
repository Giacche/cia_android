package com.ueno.cia.feature.link.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ueno.cia.core.model.CongratsInput
import com.ueno.cia.core.model.CongratsOrigin
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.core.navigation.LinkFlowInput
import com.ueno.cia.feature.link.data.LinkGenerateRequest
import com.ueno.cia.feature.link.domain.LinkGenerateUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class LinkGenerateState { Loading, Failed }

sealed interface LinkGenerateDestination {
    /** `router.go(Congrats)`. */
    data class Reset(val route: CIARoute) : LinkGenerateDestination
    data object Back : LinkGenerateDestination
}

/** Equivale a `LinkGenerateNotifier`: genera el link y reemplaza el stack por la pantalla de resultado. */
class LinkGenerateViewModel(
    private val input: LinkFlowInput,
    private val useCase: LinkGenerateUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(LinkGenerateState.Loading)
    val state: StateFlow<LinkGenerateState> = _state.asStateFlow()

    private val _destinations = Channel<LinkGenerateDestination>(Channel.BUFFERED)
    val destinations: Flow<LinkGenerateDestination> = _destinations.receiveAsFlow()

    private var job: Job? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        load()
    }

    fun load() {
        job?.cancel()
        _state.value = LinkGenerateState.Loading
        job = viewModelScope.launch {
            val response = try {
                useCase.generate(LinkGenerateRequest("GS", ChargeType.LINK.name, input.amount, input.description))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.value = LinkGenerateState.Failed
                return@launch
            }
            val screen = response.responseScreen
            if (screen == null) {
                _state.value = LinkGenerateState.Failed
            } else {
                _destinations.send(
                    LinkGenerateDestination.Reset(CIARoute.Congrats(CongratsInput(screen, CongratsOrigin.LINK, response.receiptScreen))),
                )
            }
        }
    }

    fun didTapBackFromError() {
        _destinations.trySend(LinkGenerateDestination.Back)
    }
}
