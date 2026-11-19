package com.ueno.cia.core.model

import kotlinx.serialization.Serializable

// features/qr/screens/timeout/models (Flutter). Viaja como argumento de la ruta de timeout.

@Serializable
data class QRTimeoutModel(
    val type: String,
    val appBarTitle: String,
    val tag: String,
    val illustration: String,
    val title: String,
    val subtitle: String,
    val primaryButton: ButtonModel,
    val secondaryButton: ButtonModel,
)
