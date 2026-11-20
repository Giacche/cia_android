package com.ueno.cia.feature.home.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.designsystem.components.DSButton
import com.ueno.cia.designsystem.components.DSButtonStyle
import com.ueno.cia.designsystem.components.DSText
import com.ueno.cia.designsystem.components.FullPageCarousel
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.components.SkeletonMode
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.DSTextStyle
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.home.R
import com.ueno.cia.feature.home.data.HomeBranchModel
import com.ueno.cia.feature.home.data.HomeHeaderResponse
import com.ueno.cia.feature.home.ui.HomeHeaderContent
import com.ueno.cia.feature.home.ui.Loadable
import com.ueno.cia.designsystem.R as DSR

/** Flutter muestra hasta 4 sucursales y una página final "Mostrar todas". */
const val MAX_VISIBLE_BRANCHES = 4

/** La sucursal elegida va primero; después las demás hasta completar 4. */
fun visibleBranches(all: List<HomeBranchModel>, pinnedBranchId: String?): List<HomeBranchModel> {
    if (all.size <= MAX_VISIBLE_BRANCHES) return all
    val selected = pinnedBranchId?.let { id -> all.firstOrNull { it.branchId == id } }
        ?: return all.take(MAX_VISIBLE_BRANCHES)
    return listOf(selected) + all.filter { it.branchId != selected.branchId }.take(MAX_VISIBLE_BRANCHES - 1)
}

/** Header oscuro: nombre del comercio (`HomeHeaderWidget`) o carrusel de sucursales (`HomeEmpresasHeaderWidget`). */
@Composable
fun HomeHeader(
    header: Loadable<HomeHeaderContent>,
    tenant: Tenant,
    pinnedBranchId: String?,
    onRetry: () -> Unit,
    onBranchSwiped: (HomeBranchModel) -> Unit,
    onShowAllPageReached: () -> Unit,
    onShowAllTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (header) {
            Loadable.Loading -> HeaderSkeleton()
            Loadable.Failed -> HeaderError(onRetry)
            is Loadable.Loaded -> {
                val carousel = header.value.branchCarousel
                if (carousel != null) {
                    BranchCarousel(carousel, tenant, pinnedBranchId, onBranchSwiped, onShowAllPageReached, onShowAllTap)
                    Spacer(Modifier.height(Spacing.m))
                } else {
                    MerchantHeader(header.value)
                }
            }
        }
    }
}

@Composable
private fun MerchantHeader(content: HomeHeaderContent) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.x2l))
        DSText(
            text = content.merchantName,
            style = DSTextStyle.DisplayL,
            color = DSColor.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("cia_home_merchant_name"),
        )
        Spacer(Modifier.height(Spacing.s))
        content.subscriptionPlan?.let {
            DSText(it, DSTextStyle.OverlineM, color = DSColor.Green600, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(Spacing.l + Spacing.x2l))
    }
}

@Composable
private fun HeaderSkeleton() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(Spacing.x2l))
        Skeleton(width = 190.dp, height = 30.dp, mode = SkeletonMode.Dark)
        Spacer(Modifier.height(Spacing.m))
        Skeleton(width = 170.dp, height = 14.dp, mode = SkeletonMode.Dark)
        Spacer(Modifier.height(Spacing.x4l))
    }
}

@Composable
private fun HeaderError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.x2l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        DSText(
            text = stringResource(R.string.home_header_title_error),
            style = DSTextStyle.BodyM,
            color = DSColor.White,
            textAlign = TextAlign.Center,
        )
        DSButton(
            text = stringResource(DSR.string.ds_retry),
            onClick = onRetry,
            style = DSButtonStyle.SecondaryDark,
            modifier = Modifier.testTag("cia_retry_btn"),
        )
    }
}

@Composable
private fun BranchCarousel(
    carousel: HomeHeaderResponse.BranchCarousel,
    tenant: Tenant,
    pinnedBranchId: String?,
    onBranchSwiped: (HomeBranchModel) -> Unit,
    onShowAllPageReached: () -> Unit,
    onShowAllTap: () -> Unit,
) {
    val branches = visibleBranches(carousel.branches, pinnedBranchId)
    val hasShowAll = carousel.branches.size > MAX_VISIBLE_BRANCHES
    val pages = branches.size + if (hasShowAll) 1 else 0
    FullPageCarousel(
        count = pages,
        height = 150.dp,
        onPageChanged = { index ->
            if (index < branches.size) onBranchSwiped(branches[index]) else onShowAllPageReached()
        },
    ) { index ->
        if (index < branches.size) {
            BranchPage(branches[index], tenant)
        } else {
            Box(Modifier.fillMaxWidth().height(124.dp), contentAlignment = Alignment.Center) {
                DSButton(
                    text = carousel.showAllBranchesButton?.text ?: "Mostrar todas las sucursales",
                    onClick = onShowAllTap,
                    style = DSButtonStyle.SecondaryDark,
                    modifier = Modifier.testTag(carousel.showAllBranchesButton?.tag ?: "cia_show_all_branches"),
                )
            }
        }
    }
}

@Composable
private fun BranchPage(branch: HomeBranchModel, tenant: Tenant) {
    Box(Modifier.fillMaxWidth().height(124.dp).padding(horizontal = Spacing.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            DSText(branch.name ?: branch.address, DSTextStyle.DisplayM, color = DSColor.White, textAlign = TextAlign.Center)
            branch.subscriptionPlan?.let {
                DSText(
                    it,
                    DSTextStyle.OverlineM,
                    color = if (tenant == Tenant.EMPRESAS) DSColor.White else DSColor.Green600,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
