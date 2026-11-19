package com.ueno.cia.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing
import kotlinx.coroutines.launch

/**
 * Bottom sheet del design system (`BottomSheetViewController` en iOS): fondo blanco con esquinas superiores de 24 dp,
 * grabber de 36 × 4 dp a 12 dp del borde, título (displayS) a 32 dp, 24 dp hasta el contenido, márgenes de 20 dp y
 * scrim negro al 40 %.
 *
 * El contenido recibe `dismiss`: cierra el sheet con su animación y recién después ejecuta el callback, para navegar
 * una vez cerrado y no presentar sobre una vista que se va (igual que `dismiss(animated:) { onSelect }` en iOS).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DSBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (dismiss: (then: () -> Unit) -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: (() -> Unit) -> Unit = { then ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            then()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = Radius.l, topEnd = Radius.l),
        containerColor = DSColor.White,
        scrimColor = DSColor.Black.copy(alpha = 0.4f),
        dragHandle = null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                // El sheet es otra ventana: hay que volver a exponer los testTag como resource-id (UI tests)
                .semantics { testTagsAsResourceId = true }
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.l)
                .padding(bottom = Spacing.l),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = Spacing.s)
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .background(DSColor.Neutral200, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.height(Spacing.m))
            DSText(title, DSTextStyle.DisplayS, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(Spacing.xl))
            content(dismiss)
        }
    }
}
