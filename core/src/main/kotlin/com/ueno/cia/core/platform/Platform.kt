package com.ueno.cia.core.platform

/** Reemplaza a `connectivity_plus`: el polling de QR solo consulta la confirmación si hay red. */
interface ConnectivityChecking {
    val isConnected: Boolean
}

/** Sin información de red: se asume conectado (sandbox y tests). */
object AlwaysConnected : ConnectivityChecking {
    override val isConnected: Boolean = true
}

/**
 * Acciones del sistema que CIA le pide al dispositivo (compartir, copiar, abrir un link, llamar). Las implementa
 * `cia-kit` sobre `Context`; las features reciben la interfaz para poder probarlas sin Android.
 */
interface PlatformActions {
    /** Abre un link https fuera de CIA. */
    fun openUrl(url: String)

    /** Abre el marcador con el número (no llama solo). */
    fun callPhone(number: String)

    /** Hoja de compartir con texto. */
    fun shareText(text: String)

    /** Copia al portapapeles y avisa con un toast ([message] reemplaza el texto por defecto). */
    fun copy(value: String, message: String? = null)

    /** Hoja de compartir con una imagen PNG. [fileName] sin extensión. */
    fun shareImage(png: ByteArray, fileName: String)

    /** Pide al usuario dónde guardar el PNG (`FlutterFileDialog.saveFile`). */
    fun saveImage(png: ByteArray, fileName: String)
}
