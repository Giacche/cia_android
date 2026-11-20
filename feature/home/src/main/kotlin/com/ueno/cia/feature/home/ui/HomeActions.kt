package com.ueno.cia.feature.home.ui

import com.ueno.cia.core.model.HomeInformationAlertModel
import com.ueno.cia.core.model.HomeOptionModel
import com.ueno.cia.core.model.MovementChargeModel
import com.ueno.cia.feature.home.data.HomeBannerModel
import com.ueno.cia.feature.home.data.HomeBranchModel

/** Todo lo que la pantalla le puede pedir al ViewModel (la UI no lo conoce directamente: facilita previews y tests). */
data class HomeActions(
    val onBack: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onHelp: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onReload: () -> Unit = {},
    val onRetryHeader: () -> Unit = {},
    val onRetryOptions: () -> Unit = {},
    val onRetryAlerts: () -> Unit = {},
    val onRetryBanners: () -> Unit = {},
    val onRetryPendingSettlement: () -> Unit = {},
    val onRetryRecents: () -> Unit = {},
    val onBranchSwiped: (HomeBranchModel) -> Unit = {},
    val onShowAllBranchesPageReached: () -> Unit = {},
    val onShowAllBranches: () -> Unit = {},
    val onOptionTap: (HomeOptionModel) -> Unit = {},
    val onBannerTap: (HomeBannerModel) -> Unit = {},
    val onAlertAction: (HomeInformationAlertModel) -> Unit = {},
    val onShowAllMovements: () -> Unit = {},
    val onMovementTap: (MovementChargeModel) -> Unit = {},
)

fun HomeViewModel.actions() = HomeActions(
    onBack = ::didTapBack,
    onProfile = ::didTapProfile,
    onHelp = ::didTapHelp,
    onRefresh = ::refresh,
    onReload = ::load,
    onRetryHeader = ::retryHeader,
    onRetryOptions = ::retryOptions,
    onRetryAlerts = ::retryAlerts,
    onRetryBanners = ::retryBanners,
    onRetryPendingSettlement = ::retryPendingSettlement,
    onRetryRecents = ::retryRecents,
    onBranchSwiped = ::didSwipeBranch,
    onShowAllBranchesPageReached = ::didReachShowAllBranchesPage,
    onShowAllBranches = ::didTapShowAllBranches,
    onOptionTap = ::didTapOption,
    onBannerTap = ::didTapBanner,
    onAlertAction = ::didTapAlertAction,
    onShowAllMovements = ::didTapShowAllMovements,
    onMovementTap = ::didTapMovement,
)
