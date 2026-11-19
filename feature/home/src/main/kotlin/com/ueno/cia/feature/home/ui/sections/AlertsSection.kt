package com.ueno.cia.feature.home.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.components.InlineMessage
import com.ueno.cia.designsystem.components.InlineMessageState
import com.ueno.cia.designsystem.components.PagingCarousel
import com.ueno.cia.designsystem.components.RetryView
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.core.model.HomeInformationAlertModel
import com.ueno.cia.feature.home.ui.Loadable

/** Alertas (`HomeAlertsWidget`): una sola alerta fija o varias en carrusel. */
@Composable
fun HomeAlertsSection(
    alerts: Loadable<List<HomeInformationAlertModel>>,
    onAlertAction: (HomeInformationAlertModel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (alerts) {
            Loadable.Loading -> Skeleton(height = 64.dp, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s))
            Loadable.Failed -> RetryView(onRetry, Modifier.padding(start = Spacing.l, end = Spacing.l, bottom = Spacing.s))
            is Loadable.Loaded -> when (alerts.value.size) {
                0 -> Unit
                1 -> Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s)) {
                    Alert(alerts.value[0], onAlertAction)
                }
                else -> Column(Modifier.padding(bottom = Spacing.s)) {
                    PagingCarousel(count = alerts.value.size, itemHeight = 150.dp) { index ->
                        Alert(alerts.value[index], onAlertAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun Alert(alert: HomeInformationAlertModel, onAction: (HomeInformationAlertModel) -> Unit) {
    InlineMessage(
        title = alert.title,
        description = alert.description,
        state = InlineMessageState.byName(alert.type),
        actionTitle = alert.actionLink?.text,
        onAction = { onAction(alert) },
        modifier = Modifier.testTag(alert.actionLink?.tag ?: "cia_home_alert"),
    )
}
