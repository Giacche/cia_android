package com.ueno.cia.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ueno.cia.core.model.HomeModalModel
import com.ueno.cia.core.model.HomeOptionModel
import com.ueno.cia.designsystem.components.DSBottomSheet
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.QuickActionCard
import com.ueno.cia.designsystem.components.TwoColumnGrid
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.home.R
import com.ueno.cia.feature.home.data.HomeBranchModel
import com.ueno.cia.feature.home.ui.sections.optionsAspectRatio

/**
 * Bottom sheet de una opción con `modal` (`HomeBottomSheetOptionsWidget`; por ejemplo "Elegí el tipo de QR"):
 * las mismas cards de la Home en grilla de dos columnas. Las deshabilitadas se ven apagadas y no responden.
 * La opción elegida se entrega una vez cerrado el sheet.
 */
@Composable
fun HomeOptionsSheet(modal: HomeModalModel, onSelect: (HomeOptionModel) -> Unit, onDismiss: () -> Unit) {
    DSBottomSheet(title = modal.title, onDismiss = onDismiss) { dismiss ->
        TwoColumnGrid(
            aspectRatio = optionsAspectRatio(modal.options),
            modifier = Modifier.testTag("cia_home_options_sheet"),
            items = modal.options.map { option ->
                { cell: Modifier ->
                    QuickActionCard(
                        title = option.title,
                        description = option.description.ifEmpty { null },
                        icon = option.icon,
                        disabled = option.disabled,
                        onClick = { dismiss { onSelect(option) } },
                        modifier = cell.testTag("${option.tag}_${option.id.lowercase()}"),
                    )
                }
            },
        )
    }
}

/** Bottom sheet "Seleccioná una sucursal" (`HomeBranchSelectorBottomSheet`). */
@Composable
fun HomeBranchesSheet(branches: List<HomeBranchModel>, onSelect: (HomeBranchModel) -> Unit, onDismiss: () -> Unit) {
    DSBottomSheet(title = stringResource(R.string.home_branches_sheet_title), onDismiss = onDismiss) { dismiss ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(minOf(branches.size * 68, 420).dp)
                .verticalScroll(rememberScrollState())
                .testTag("cia_home_branches_sheet"),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            branches.forEach { branch ->
                BranchRow(
                    title = branch.name ?: branch.address,
                    subtitle = branch.subscriptionPlan,
                    onClick = { dismiss { onSelect(branch) } },
                    modifier = Modifier.testTag("cia_branch_${branch.branchId}"),
                )
            }
        }
    }
}

/** Fila seleccionable de sucursal (`GuBoxSelector.radio`). */
@Composable
private fun BranchRow(title: String, subtitle: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.xs)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(DSColor.White, shape)
            .border(BorderStroke(1.dp, DSColor.Neutral200), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalArrangement = Arrangement.Center,
    ) {
        DSText(title, DSTextStyle.BodyLStrong, maxLines = 1)
        if (subtitle != null) DSText(subtitle, DSTextStyle.BodyM, color = DSColor.Neutral500, maxLines = 1)
    }
}
