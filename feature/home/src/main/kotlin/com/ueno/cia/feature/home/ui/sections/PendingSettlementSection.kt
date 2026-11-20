package com.ueno.cia.feature.home.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.DataCard
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.home.R
import com.ueno.cia.feature.home.data.HomePendingSettlementModel
import com.ueno.cia.feature.home.ui.Loadable
import com.ueno.cia.designsystem.R as DSR

/** Total de ventas (`HomePendingSettlementWidget`). */
@Composable
fun HomePendingSettlementSection(pending: Loadable<HomePendingSettlementModel>, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.l, vertical = Spacing.s)) {
        val defaultTitle = stringResource(R.string.home_pending_settlement_title_default)
        when (pending) {
            Loadable.Loading -> DataCard(defaultTitle) { Skeleton(width = 300.dp, height = 36.dp) }
            Loadable.Failed -> DataCard(defaultTitle) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    DSButton(stringResource(DSR.string.ds_retry), onRetry, style = DSButtonStyle.Secondary)
                }
            }
            is Loadable.Loaded -> DataCard(pending.value.title) {
                DSText(pending.value.amount, DSTextStyle.DisplayL)
            }
        }
    }
}
