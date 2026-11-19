package com.ueno.cia.designsystem.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import java.io.ByteArrayOutputStream

/**
 * Captura de un composable como imagen (Flutter: `RepaintBoundary` + `toImage`; iOS: `drawHierarchy`).
 *
 * ```
 * val capture = rememberCaptureLayer()
 * Box(Modifier.capturable(capture)) { ... }
 * val png = capture.toPng()
 * ```
 */
@Composable
fun rememberCaptureLayer(): GraphicsLayer = rememberGraphicsLayer()

/** Dibuja el contenido normalmente y además lo graba en [layer]. */
fun Modifier.capturable(layer: GraphicsLayer): Modifier = drawWithContent {
    layer.record { this@drawWithContent.drawContent() }
    drawLayer(layer)
}

/** PNG de lo último que se grabó en la capa. */
suspend fun GraphicsLayer.toPng(): ByteArray {
    val bitmap = toImageBitmap().asAndroidBitmap()
    return ByteArrayOutputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        it.toByteArray()
    }
}
