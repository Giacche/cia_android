package com.ueno.cia.designsystem.images

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.svg.SvgDecoder

/**
 * Íconos e ilustraciones extraídos del design system Flutter (`assets/ds/icons` y `assets/ds/illustrations`, SVG).
 * El BFF manda los nombres en minúscula (`qr`, `link`, `user`...); acá se resuelven sin distinguir mayúsculas.
 */
object DSAssets {
    private const val ICONS = "ds/icons"
    private const val ILLUSTRATIONS = "ds/illustrations"

    @Volatile private var icons: Map<String, String>? = null

    @Volatile private var illustrations: Map<String, String>? = null

    @Volatile private var loader: ImageLoader? = null

    /** Ruta del SVG del ícono, o `null` si el nombre no existe en el design system. */
    fun iconPath(context: Context, name: String): String? =
        index(context, ICONS, icons) { icons = it }[name.lowercase()]?.let { "$ICONS/$it" }

    fun illustrationPath(context: Context, name: String): String? {
        val key = illustrationAliases[name.lowercase()] ?: name.lowercase()
        return index(context, ILLUSTRATIONS, illustrations) { illustrations = it }[key]?.let { "$ILLUSTRATIONS/$it" }
    }

    /** Ilustraciones del design system que comparten asset. */
    private val illustrationAliases = mapOf("successillustration" to "semanticsuccess")

    /** `QrFlagIcon`: los íconos `qr_<país>` del BFF son banderas (Flutter las baja de un CDN; acá viajan en la app). */
    fun flagPath(context: Context, iconName: String): String? {
        val prefix = "qr_"
        if (!iconName.lowercase().startsWith(prefix)) return null
        return illustrationPath(context, "flag.${iconName.substring(prefix.length).lowercase()}")
    }

    fun imageLoader(context: Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(context.applicationContext)
            .components { add(SvgDecoder.Factory()) }
            .build()
            .also { loader = it }
    }

    private fun index(
        context: Context,
        dir: String,
        cached: Map<String, String>?,
        store: (Map<String, String>) -> Unit,
    ): Map<String, String> = cached ?: synchronized(this) {
        val files = context.applicationContext.assets.list(dir).orEmpty()
            .filter { it.endsWith(".svg") }
            .associateBy { it.removeSuffix(".svg").lowercase() }
        store(files)
        files
    }
}

private fun assetModel(path: String) = "file:///android_asset/$path"

/** Ícono del design system teñido con [tint]. Si el nombre no existe no dibuja nada (usar [hasDSIcon] para el fallback). */
@Composable
fun DSIcon(name: String, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val path = remember(name) { DSAssets.iconPath(context, name) } ?: return
    AsyncImage(
        model = assetModel(path),
        contentDescription = null,
        imageLoader = DSAssets.imageLoader(context),
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun hasDSIcon(name: String): Boolean {
    val context = LocalContext.current
    return remember(name) { DSAssets.iconPath(context, name) != null }
}

/** Ilustración del design system a color (sin tinte). */
@Composable
fun DSIllustration(name: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val context = LocalContext.current
    val path = remember(name) { DSAssets.illustrationPath(context, name) } ?: return
    AsyncImage(
        model = assetModel(path),
        contentDescription = null,
        imageLoader = DSAssets.imageLoader(context),
        modifier = modifier,
        contentScale = contentScale,
    )
}

/** Ruta de la bandera de un ícono `qr_<país>`, o `null` si el nombre no es una bandera. */
@Composable
fun rememberFlagPath(iconName: String): String? {
    val context = LocalContext.current
    return remember(iconName) { DSAssets.flagPath(context, iconName) }
}

/** Bandera circular (`QrFlagIcon`), sin tinte. [path] sale de [rememberFlagPath]. */
@Composable
fun DSFlag(path: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = assetModel(path),
        contentDescription = null,
        imageLoader = DSAssets.imageLoader(context),
        modifier = modifier.size(size).clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}
