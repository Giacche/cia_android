package com.ueno.cia.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.designsystem.components.DSIconButton
import com.ueno.cia.designsystem.components.DSIconButtonMode
import com.ueno.cia.designsystem.components.FeedbackErrorScreen
import com.ueno.cia.designsystem.components.StatusBarIcons
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.Palette
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.home.data.HomeLayoutSectionModel
import com.ueno.cia.feature.home.data.HomeSectionID
import com.ueno.cia.feature.home.ui.sections.HomeAlertsSection
import com.ueno.cia.feature.home.ui.sections.HomeBannersSection
import com.ueno.cia.feature.home.ui.sections.HomeHeader
import com.ueno.cia.feature.home.ui.sections.HomeOptionsSection
import com.ueno.cia.feature.home.ui.sections.HomePendingSettlementSection
import com.ueno.cia.feature.home.ui.sections.HomeRecentsSection
import com.ueno.cia.feature.home.ui.sections.HomeTopBar
import com.ueno.cia.designsystem.R as DSR

/** Mientras carga el layout se muestra la estructura por defecto en skeleton (`onLoading` de Flutter). */
private val LoadingPlaceholder = HomeState(
    appBarTitle = "Cobros",
    sections = listOf("header", "options", "banners", "recents").map { HomeLayoutSectionModel(id = it) },
)

/** Equivale a `HomeScreen` + `HomeScreenScaffold`. Sin estado propio: todo llega por parámetros. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uiState: HomeUiState, isRefreshing: Boolean, tenant: Tenant, actions: HomeActions, modifier: Modifier = Modifier) {
    val palette = remember(tenant) { Palette(tenant) }
    val failed = uiState is HomeUiState.Failed
    StatusBarIcons(darkIcons = failed)

    Box(modifier = modifier.fillMaxSize().background(DSColor.Neutral50).testTag("cia_home")) {
        if (failed) {
            FeedbackErrorScreen(onRetry = actions.onReload, modifier = Modifier.testTag("cia_error_screen"))
            DSIconButton(
                icon = "arrowLeft",
                mode = DSIconButtonMode.Light,
                contentDescription = stringResource(DSR.string.ds_back),
                onClick = actions.onBack,
                modifier = Modifier.statusBarsPadding().padding(start = Spacing.l, top = Spacing.s).align(Alignment.TopStart),
            )
            return@Box
        }
        val state = (uiState as? HomeUiState.Loaded)?.state ?: LoadingPlaceholder
        // Fondo oscuro detrás de la mitad superior: el overscroll no muestra gris arriba
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.5f).background(palette.homeHeaderBackground))
        PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = actions.onRefresh, modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val minSheetHeight = maxHeight
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .background(palette.homeHeaderBackground),
                ) {
                    HomeHeaderArea(state, tenant, actions)
                    // La hoja gris ocupa al menos el alto de pantalla (minHeight en Flutter)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = minSheetHeight)
                            .clip(RoundedCornerShape(topStart = Radius.l, topEnd = Radius.l))
                            .background(DSColor.Neutral50),
                    ) {
                        HomeSections(state, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeaderArea(state: HomeState, tenant: Tenant, actions: HomeActions) {
    Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        val content = state.header.valueOrNull
        HomeTopBar(
            title = state.appBarTitle,
            isEmpresas = tenant == Tenant.EMPRESAS,
            profile = content?.profileButton,
            help = content?.helpButton,
            onBack = actions.onBack,
            onProfile = actions.onProfile,
            onHelp = actions.onHelp,
        )
        if (state.hasHeader) {
            HomeHeader(
                header = state.header,
                tenant = tenant,
                pinnedBranchId = state.pinnedBranchId,
                onRetry = actions.onRetryHeader,
                onBranchSwiped = actions.onBranchSwiped,
                onShowAllPageReached = actions.onShowAllBranchesPageReached,
                onShowAllTap = actions.onShowAllBranches,
            )
        }
    }
}

/** Una sección por id en el orden del layout (`_buildContentSections`). Un id desconocido del BFF se ignora. */
@Composable
private fun HomeSections(state: HomeState, actions: HomeActions) {
    val contentIds = state.sectionIds.filterNot { it.isHeader }
    contentIds.forEachIndexed { index, id ->
        val isFirst = index == 0
        when (id) {
            HomeSectionID.Alerts -> {
                if (isFirst) Spacer(Modifier.height(Spacing.s))
                HomeAlertsSection(state.alerts, actions.onAlertAction, actions.onRetryAlerts)
            }
            HomeSectionID.Options -> {
                if (isFirst) Spacer(Modifier.height(Spacing.s))
                HomeOptionsSection(
                    options = state.options,
                    title = state.optionsTitle,
                    allDisabled = state.isBranchSelectionActive,
                    onOptionTap = actions.onOptionTap,
                    onRetry = actions.onRetryOptions,
                )
            }
            HomeSectionID.Banners -> if (state.showsBanners) {
                if (isFirst) Spacer(Modifier.height(Spacing.s))
                HomeBannersSection(state.banners, state.bannersTitle, actions.onBannerTap, actions.onRetryBanners)
            }
            HomeSectionID.Recents -> if (state.showsRecents) {
                HomeRecentsSection(
                    state = state,
                    isFirst = isFirst,
                    onMovementTap = actions.onMovementTap,
                    onShowAllTap = actions.onShowAllMovements,
                    onRetry = actions.onRetryRecents,
                )
            }
            HomeSectionID.TotalSales, HomeSectionID.PendingSettlement -> {
                if (isFirst) Spacer(Modifier.height(Spacing.s))
                HomePendingSettlementSection(state.pendingSettlement, actions.onRetryPendingSettlement)
            }
            HomeSectionID.Header, HomeSectionID.HeaderEmpresas, HomeSectionID.HeaderCarousel -> Unit
        }
    }
}
