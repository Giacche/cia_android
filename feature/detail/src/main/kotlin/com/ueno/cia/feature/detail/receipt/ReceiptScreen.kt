package com.ueno.cia.feature.detail.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.core.model.ReceiptItemModel
import com.ueno.cia.core.model.ReceiptScreenModel
import com.ueno.cia.core.platform.PlatformActions
import com.ueno.cia.core.util.CIACurrency
import com.ueno.cia.core.util.CurrencyFormatter
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSShadow
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.capturable
import com.ueno.cia.designsystem.components.dsShadow
import com.ueno.cia.designsystem.components.rememberCaptureLayer
import com.ueno.cia.designsystem.components.toPng
import com.ueno.cia.designsystem.images.DSIcon
import com.ueno.cia.designsystem.images.DSIllustration
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing
import kotlinx.coroutines.launch

/**
 * Equivale a `ReceiptScreen` + `GuReceipt`: tarjeta con logo, monto y datos del cobro, y descargar / compartir como
 * imagen. (Falta la marca de agua en mosaico de la tarjeta.)
 */
@Composable
fun ReceiptScreen(screen: ReceiptScreenModel, platform: PlatformActions, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val capture = rememberCaptureLayer()
    val scope = rememberCoroutineScope()
    CIAScaffold(
        title = screen.appBarTitle,
        onBack = onBack,
        modifier = modifier.testTag(screen.tag),
        footer = {
            ReceiptFooter(
                downloadLabel = screen.downloadLabel,
                shareLabel = screen.shareLabel,
                onDownload = { scope.launch { platform.saveImage(capture.toPng(), screen.receiptName) } },
                onShare = { scope.launch { platform.shareImage(capture.toPng(), screen.receiptName) } },
            )
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.l)) {
            ReceiptCard(screen, Modifier.capturable(capture))
        }
    }
}

@Composable
private fun ReceiptCard(screen: ReceiptScreenModel, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.m)
    val currency = CIACurrency.of(screen.currency)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dsShadow(DSShadow.M, shape)
            .then(modifier)
            .background(DSColor.White, shape)
            .padding(start = Spacing.l, end = Spacing.l, top = Spacing.xl, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Column {
            DSIllustration(
                "uenoLogo",
                modifier = Modifier.height(20.dp).size(width = 132.dp, height = 20.dp).semantics {
                    contentDescription = "ueno bank"
                },
            )
            Spacer(Modifier.height(Spacing.m))
            DSText(screen.receiptName, DSTextStyle.BodyLStrong)
            Spacer(Modifier.height(Spacing.m))
            val amount = CurrencyFormatter(currency).digits(screen.amount)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2s),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .semantics(mergeDescendants = true) { contentDescription = "${currency.symbol} $amount" }
                    .testTag("cia_receipt_amount"),
            ) {
                DSText(currency.symbol, DSTextStyle.BodyLStrong, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
                DSText(amount, DSTextStyle.DisplayXL, maxLines = 1)
            }
            Spacer(Modifier.height(Spacing.x2s))
            DSText(screen.subTitle, DSTextStyle.BodyMStrong)
        }
        Divider(light = false)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
            screen.items.forEach { ReceiptRow(it) }
        }
    }
}

/** `GuListItem.double` / `GuDivider` / espacio chico (cualquier otro tipo). */
@Composable
private fun ReceiptRow(item: ReceiptItemModel) {
    when (item.type) {
        "divider" -> Divider(light = item.light ?: false)
        "double" -> Column(
            modifier = Modifier.fillMaxWidth().alpha(if (item.enabled ?: true) 1f else 0.4f),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2s),
        ) {
            if (!item.subtitle.isNullOrEmpty()) DSText(item.subtitle!!, DSTextStyle.BodyM, color = DSColor.Neutral500)
            DSText(item.value.orEmpty(), DSTextStyle.BodyL)
        }
        else -> Spacer(Modifier.height(Spacing.x3s))
    }
}

@Composable
private fun Divider(light: Boolean) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(if (light) DSColor.Neutral100 else DSColor.Neutral200))
}

/** `ReceiptFooter`: dos acciones circulares (ícono con borde) con su texto debajo, centradas sobre fondo blanco. */
@Composable
private fun ReceiptFooter(downloadLabel: String, shareLabel: String, onDownload: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DSColor.White)
            .navigationBarsPadding()
            .padding(vertical = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        ReceiptAction("download", downloadLabel, onDownload, Modifier.testTag("cia_receipt_download_btn"))
        ReceiptAction("share", shareLabel, onShare, Modifier.testTag("cia_receipt_share_btn"))
    }
}

@Composable
private fun ReceiptAction(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box(
            modifier = modifier
                .size(56.dp)
                .border(1.dp, DSColor.Neutral200, CircleShape)
                .semantics { contentDescription = label }
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { DSIcon(icon, tint = DSColor.Black, size = 24.dp) }
        DSText(label, DSTextStyle.BodyL, textAlign = TextAlign.Center)
    }
}
