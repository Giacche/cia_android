package com.ueno.cia.feature.qr.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.core.platform.PlatformActions
import com.ueno.cia.designsystem.components.CIAScaffold
import com.ueno.cia.designsystem.components.DSBottomSheet
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSShadow
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.FeedbackErrorScreen
import com.ueno.cia.designsystem.components.FooterData
import com.ueno.cia.designsystem.components.OptionRow
import com.ueno.cia.designsystem.components.ScreenFooter
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.components.capturable
import com.ueno.cia.designsystem.components.dsShadow
import com.ueno.cia.designsystem.components.rememberCaptureLayer
import com.ueno.cia.designsystem.components.toPng
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.qr.data.QRBottomSheetModel
import kotlinx.coroutines.launch

/** Punto de entrada del QR para el host: conecta el [QRGenerateViewModel], el sheet de proveedores y compartir. */
@Composable
fun QRGenerateRoute(
    viewModel: QRGenerateViewModel,
    platform: PlatformActions,
    onDestination: (QRGenerateDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onDestination)
    var sheet by remember { mutableStateOf<QRBottomSheetModel?>(null) }

    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) {
        viewModel.destinations.collect { destination ->
            if (destination is QRGenerateDestination.ProviderSheet) sheet = destination.sheet else navigate(destination)
        }
    }

    QRGenerateScreen(
        state = state,
        platform = platform,
        onShare = viewModel::didTapShare,
        onEdit = viewModel::didTapEdit,
        onBack = viewModel::didTapBack,
        onSelectProvider = viewModel::didTapProviderSelector,
        onErrorBack = viewModel::didTapBackFromError,
        modifier = modifier,
    )
    sheet?.let {
        QRProvidersSheet(
            sheet = it,
            onSelect = viewModel::didSelectProvider,
            onDismiss = { sheet = null },
        )
    }
}

/** Equivale a `QrGenerateScreen` + `QrGenerateSkeletonWidget`. */
@Composable
fun QRGenerateScreen(
    state: QRState,
    platform: PlatformActions,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onSelectProvider: () -> Unit,
    onErrorBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loaded = (state as? QRState.Loaded)?.value
    val capture = rememberCaptureLayer()
    val scope = rememberCoroutineScope()

    CIAScaffold(
        title = loaded?.qrScreen?.appBarTitle.orEmpty(),
        onBack = onBack,
        modifier = modifier.testTag(loaded?.qrScreen?.tag ?: "cia_qr"),
        footer = {
            when (state) {
                QRState.Loading -> FooterSkeleton()
                is QRState.Loaded -> {
                    val screen = state.value.qrScreen
                    ScreenFooter(data = FooterData.Single(screen.summaryInfo.amountTitle, state.value.amountValue)) {
                        val primary = screen.primaryButton
                        if (primary != null && primary.text.isNotEmpty()) {
                            DSButton(
                                text = primary.text,
                                onClick = {
                                    onShare()
                                    // Se captura la tarjeta del QR y se abre la hoja de compartir
                                    scope.launch { platform.shareImage(capture.toPng(), "qr") }
                                },
                                style = DSButtonStyle.Primary,
                                modifier = Modifier.fillMaxWidth().testTag(primary.tag),
                            )
                            DSButton(
                                text = screen.secondaryButton.text,
                                onClick = onEdit,
                                style = DSButtonStyle.Secondary,
                                modifier = Modifier.fillMaxWidth().testTag(screen.secondaryButton.tag),
                            )
                        } else {
                            // Sin botón primario el único botón es el de editar, con estilo secundario
                            DSButton(
                                text = screen.secondaryButton.text,
                                onClick = onEdit,
                                style = DSButtonStyle.Secondary,
                                modifier = Modifier.fillMaxWidth().testTag(screen.secondaryButton.tag),
                            )
                        }
                    }
                }
                QRState.Failed -> Unit
            }
        },
    ) {
        when (state) {
            QRState.Loading -> QRSkeleton()
            QRState.Failed -> FeedbackErrorScreen(onRetry = onErrorBack, modifier = Modifier.testTag("cia_error_screen"))
            is QRState.Loaded -> QRContent(state.value, capture, onSelectProvider)
        }
    }
}

@Composable
private fun QRContent(state: QRViewState, capture: androidx.compose.ui.graphics.layer.GraphicsLayer, onSelectProvider: () -> Unit) {
    val screen = state.qrScreen
    val bitmap = remember(state.qrCodeImage) {
        BitmapFactory.decodeByteArray(state.qrCodeImage, 0, state.qrCodeImage.size)?.asImageBitmap()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.l))
        DSText(screen.title, DSTextStyle.DisplayS, textAlign = TextAlign.Center)
        if (!screen.subtitle.isNullOrEmpty()) {
            Spacer(Modifier.height(Spacing.l))
            DSText(screen.subtitle!!, DSTextStyle.BodyL, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(Spacing.l))
        // `Flexible(AspectRatio(1))`: la tarjeta ocupa el espacio libre entre el texto y el selector, siempre cuadrada
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            val shape = RoundedCornerShape(Radius.s)
            Box(
                modifier = Modifier
                    .size(side)
                    .dsShadow(DSShadow.M, shape)
                    .clip(shape)
                    .background(DSColor.White)
                    .capturable(capture)
                    .padding(Spacing.xs)
                    .semantics { contentDescription = "QR" }
                    .testTag("cia_qr_image"),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    // El QR se ve nítido al escalar
                    Image(bitmap = bitmap, contentDescription = null, filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize())
                }
            }
        }
        Spacer(Modifier.height(Spacing.l))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            if (!screen.description.isNullOrEmpty()) {
                DSText(screen.description!!, DSTextStyle.BodyL, textAlign = TextAlign.Center)
            }
            if (state.selectorEnabled) {
                DSButton(
                    text = state.selectedOptionLabel,
                    onClick = onSelectProvider,
                    style = DSButtonStyle.SecondarySmall,
                    icon = "chevronDown",
                    modifier = Modifier.testTag("cia_qr_provider_selector"),
                )
            }
        }
        Spacer(Modifier.height(Spacing.l))
    }
}

/** Selector de proveedor de QR (`QrBottomSheetOptionsWidget`). */
@Composable
fun QRProvidersSheet(sheet: QRBottomSheetModel, onSelect: (com.ueno.cia.core.model.ChargeProvider) -> Unit, onDismiss: () -> Unit) {
    DSBottomSheet(title = sheet.title, onDismiss = onDismiss) { dismiss ->
        Column(Modifier.testTag("cia_qr_providers_sheet")) {
            sheet.options.orEmpty().forEach { option ->
                OptionRow(
                    icon = option.icon,
                    title = option.title,
                    subtitle = option.subtitle,
                    onClick = { dismiss { onSelect(option.providerType) } },
                    modifier = Modifier.testTag("${option.tag}_${option.provider.lowercase()}"),
                )
            }
        }
    }
}

// region Skeleton

@Composable
private fun QRSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(Spacing.l))
        Skeleton(width = 220.dp, height = 24.dp)
        Spacer(Modifier.height(Spacing.l))
        Skeleton(height = 16.dp)
        Spacer(Modifier.height(Spacing.x3s))
        Skeleton(width = 180.dp, height = 16.dp)
        Spacer(Modifier.height(Spacing.l))
        BoxWithConstraints(Modifier.weight(1f, fill = false).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Skeleton(
                modifier = Modifier.size(minOf(maxWidth, maxHeight)),
                width = minOf(maxWidth, maxHeight),
                height = minOf(maxWidth, maxHeight),
            )
        }
        Spacer(Modifier.height(Spacing.l))
        Skeleton(width = 140.dp, height = 14.dp)
        Spacer(Modifier.height(Spacing.m))
        Skeleton(width = 160.dp, height = 40.dp)
        Spacer(Modifier.height(Spacing.l))
    }
}

@Composable
private fun FooterSkeleton() {
    ScreenFooter {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Skeleton(width = 100.dp, height = 16.dp)
            Skeleton(width = 80.dp, height = 16.dp)
        }
        Skeleton(height = 50.dp)
        Skeleton(height = 50.dp)
    }
}

// endregion
