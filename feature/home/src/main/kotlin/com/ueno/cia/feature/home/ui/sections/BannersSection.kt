package com.ueno.cia.feature.home.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.components.DSSection
import com.ueno.cia.designsystem.components.OverviewBanner
import com.ueno.cia.designsystem.components.OverviewBannerColor
import com.ueno.cia.designsystem.components.OverviewBannerHeight
import com.ueno.cia.designsystem.components.PagingCarousel
import com.ueno.cia.designsystem.components.RetryView
import com.ueno.cia.designsystem.components.Skeleton
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.Radius
import com.ueno.cia.designsystem.tokens.Spacing
import com.ueno.cia.feature.home.data.HomeBannerModel
import com.ueno.cia.feature.home.ui.Loadable

/** Banners (`HomeCarouselWidget`). */
@Composable
fun HomeBannersSection(
    banners: Loadable<List<HomeBannerModel>>,
    title: String,
    onBannerTap: (HomeBannerModel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = Spacing.l)) {
        when (banners) {
            Loadable.Loading -> BannersSkeleton()
            Loadable.Failed -> DSSection(title = title.ifEmpty { null }) { RetryView(onRetry) }
            is Loadable.Loaded -> {
                val items = banners.value
                if (items.isEmpty()) return@Column
                if (items.size > 1) {
                    DSSection(title = title.ifEmpty { null }, padsContent = false) {
                        PagingCarousel(count = items.size, itemHeight = OverviewBannerHeight) { index ->
                            Banner(items[index], onBannerTap)
                        }
                    }
                } else {
                    DSSection(title = title.ifEmpty { null }) { Banner(items[0], onBannerTap) }
                }
            }
        }
    }
}

@Composable
private fun Banner(banner: HomeBannerModel, onTap: (HomeBannerModel) -> Unit) {
    OverviewBanner(
        overline = banner.overline,
        title = banner.title,
        buttonText = banner.buttonText,
        color = OverviewBannerColor.byName(banner.color),
        illustration = banner.asset,
        onClick = { onTap(banner) },
        modifier = Modifier.testTag("cia_banner_${banner.actionType}"),
    )
}

@Composable
private fun BannersSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Skeleton(width = 150.dp, height = 20.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(OverviewBannerHeight)
                .background(DSColor.White, RoundedCornerShape(Radius.m))
                .padding(Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x2s)) {
                Skeleton(width = 160.dp, height = 12.dp)
                Spacer(Modifier.height(Spacing.s - Spacing.x2s))
                Skeleton(width = 150.dp, height = 16.dp)
                Spacer(Modifier.height(Spacing.xl))
                Skeleton(width = 88.dp, height = 34.dp)
            }
            Skeleton(width = 58.dp, height = 58.dp, circle = true)
        }
    }
}
