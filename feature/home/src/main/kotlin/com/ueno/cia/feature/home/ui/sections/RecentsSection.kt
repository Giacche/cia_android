package com.ueno.cia.feature.home.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ueno.cia.core.model.MovementChargeModel
import com.ueno.cia.designsystem.components.DSSection
import com.ueno.cia.designsystem.components.EmptyState
import com.ueno.cia.designsystem.components.MovementRow
import com.ueno.cia.designsystem.components.RetryView
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.home.ui.HomeState
import com.ueno.cia.feature.home.ui.Loadable
import com.ueno.cia.feature.home.ui.valueOrNull

/** Últimos cobros (`MovementsListWidget`). */
@Composable
fun HomeRecentsSection(
    state: HomeState,
    isFirst: Boolean,
    onMovementTap: (MovementChargeModel) -> Unit,
    onShowAllTap: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.recentsTitle.ifEmpty { null }
    Column(modifier = modifier.fillMaxWidth()) {
        if (isFirst) Spacer(Modifier.height(Spacing.l))
        when {
            state.recentsIsLoading -> RecentsSkeleton()
            state.charges == Loadable.Failed -> DSSection(title = title) {
                RetryView(onRetry, Modifier.padding(vertical = Spacing.x4l - Spacing.x2l))
            }
            else -> {
                val charges = state.visibleCharges
                if (charges.isEmpty()) {
                    val empty = state.recents.valueOrNull?.emptyState ?: return@Column
                    DSSection(title = title) { EmptyState(empty.illustration, empty.title, empty.subtitle) }
                } else {
                    DSSection(title = title, actionTitle = state.recentsLink?.text, onAction = onShowAllTap, actionTag = state.recentsLink?.tag) {
                        Column {
                            charges.forEach { charge ->
                                MovementRow(
                                    title = charge.title,
                                    subtitle = charge.subtitle,
                                    amount = charge.amount,
                                    date = charge.date,
                                    onClick = { onMovementTap(charge) },
                                    modifier = Modifier.testTag("cia_movement_${charge.ticketNumber}"),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun RecentsSkeleton() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l)) {
        Skeleton(width = 140.dp, height = 20.dp)
        Spacer(Modifier.height(Spacing.xl - Spacing.s))
        repeat(4) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Skeleton(width = 120.dp, height = 12.dp)
                    Skeleton(width = 90.dp, height = 12.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Skeleton(width = 80.dp, height = 12.dp)
                    Skeleton(width = 60.dp, height = 12.dp)
                }
            }
        }
    }
}
