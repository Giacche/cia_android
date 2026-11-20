package com.ueno.cia.feature.home.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ueno.cia.core.model.HomeOptionModel
import com.ueno.cia.core.model.gridAspectRatio
import com.ueno.cia.designsystem.components.DSSection
import com.ueno.cia.designsystem.components.InlineMessage
import com.ueno.cia.designsystem.components.InlineMessageState
import com.ueno.cia.designsystem.components.QuickActionCard
import com.ueno.cia.designsystem.components.RetryView
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.components.TwoColumnGrid
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.home.R
import com.ueno.cia.feature.home.ui.HomeOptionsContent
import com.ueno.cia.feature.home.ui.Loadable

/** Flutter: 3/2, o 2.5/2 si alguna opción tiene descripción. */
fun optionsAspectRatio(options: List<HomeOptionModel>): Float = options.gridAspectRatio()

/** Opciones de cobro (`HomeOptionsWidget`). */
@Composable
fun HomeOptionsSection(
    options: Loadable<HomeOptionsContent>,
    title: String,
    allDisabled: Boolean,
    onOptionTap: (HomeOptionModel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = Spacing.s)) {
        when (options) {
            Loadable.Loading -> OptionsSkeleton()
            Loadable.Failed -> DSSection(title = stringResource(R.string.home_options_title_default)) {
                RetryView(onRetry)
            }
            is Loadable.Loaded -> {
                val message = options.value.message
                if (message != null) {
                    Column(Modifier.padding(horizontal = Spacing.l)) {
                        InlineMessage(
                            title = message.message,
                            description = message.description,
                            state = InlineMessageState.byName(message.type),
                        )
                    }
                } else {
                    val items = options.value.options
                    DSSection(title = title.ifEmpty { null }) {
                        TwoColumnGrid(
                            aspectRatio = optionsAspectRatio(items),
                            items = items.map { option ->
                                { cell: Modifier ->
                                    QuickActionCard(
                                        title = option.title,
                                        description = option.description.ifEmpty { null },
                                        icon = option.icon,
                                        disabled = allDisabled || option.disabled,
                                        onClick = { onOptionTap(option) },
                                        modifier = cell.testTag(option.tag),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionsSkeleton() {
    Column(modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m)) {
        Skeleton(width = 180.dp, height = 20.dp)
        Spacer(Modifier.height(Spacing.xl))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.fillMaxWidth()) {
            repeat(2) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp)
                        .background(DSColor.White, RoundedCornerShape(Radius.m))
                        .padding(Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.l),
                ) {
                    Skeleton(width = 36.dp, height = 36.dp, circle = true)
                    Skeleton(width = 78.dp, height = 14.dp)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}
