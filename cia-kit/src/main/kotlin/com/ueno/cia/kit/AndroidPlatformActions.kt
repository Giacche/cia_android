package com.ueno.cia.kit

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.ueno.cia.core.platform.PlatformActions
import java.io.File

/** Acciones del sistema de CIA sobre [Context] (`router.share/copy/openInApp/callPhone` en iOS). */
internal class AndroidPlatformActions(
    private val context: Context,
    private val onSaveImage: (fileName: String, png: ByteArray) -> Unit,
) : PlatformActions {

    override fun openUrl(url: String) = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    override fun callPhone(number: String) = start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))

    override fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        start(Intent.createChooser(send, context.getString(R.string.cia_share_chooser)))
    }

    override fun copy(value: String, message: String?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("CIA", value))
        when {
            message != null -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            // Desde Android 13 el sistema ya muestra su propio aviso al copiar
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> Toast.makeText(context, R.string.cia_copied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun shareImage(png: ByteArray, fileName: String) {
        val file = writeToCache(png, fileName)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.cia.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(Intent.createChooser(send, context.getString(R.string.cia_share_chooser)))
    }

    override fun saveImage(png: ByteArray, fileName: String) = onSaveImage(fileName, png)

    private fun writeToCache(png: ByteArray, fileName: String): File {
        val dir = File(context.cacheDir, "cia_share").apply { mkdirs() }
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "$safe.png").apply { writeBytes(png) }
    }

    private fun start(intent: Intent) {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

/** Crea las acciones y registra el selector de archivos para "descargar" (`FlutterFileDialog.saveFile`). */
@Composable
internal fun rememberPlatformActions(): PlatformActions {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val bytes = pending
        pending = null
        if (uri != null && bytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
        }
    }
    return remember(context, launcher) {
        AndroidPlatformActions(context) { name, png ->
            pending = png
            launcher.launch("$name.png")
        }
    }
}
