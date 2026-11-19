package com.ueno.cia.feature.qr.ui

import androidx.lifecycle.ViewModel
import com.ueno.cia.core.model.QRTimeoutModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface QRTimeoutDestination {
    /** "Reintentar": `pop` vuelve al monto. */
    data object Back : QRTimeoutDestination

    /** "Ir a cobros": `go(home)`. */
    data object GoHome : QRTimeoutDestination
}

/** Equivale a `QrTimeoutScreen`: el modelo ya viene en el response de generar (`timeoutScreen`). */
class QRTimeoutViewModel(val screen: QRTimeoutModel) : ViewModel() {
    private val _destinations = Channel<QRTimeoutDestination>(Channel.BUFFERED)
    val destinations: Flow<QRTimeoutDestination> = _destinations.receiveAsFlow()

    fun didTapPrimary() {
        _destinations.trySend(QRTimeoutDestination.Back)
    }

    fun didTapSecondary() {
        _destinations.trySend(QRTimeoutDestination.GoHome)
    }
}
