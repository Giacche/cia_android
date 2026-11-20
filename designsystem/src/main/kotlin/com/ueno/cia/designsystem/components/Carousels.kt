package com.ueno.cia.designsystem.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ueno.cia.designsystem.tokens.DSColor
import com.ueno.cia.designsystem.tokens.Spacing

/** Indicador de página (puntos). */
@Composable
fun PageIndicator(count: Int, current: Int, selectedColor: Color, unselectedColor: Color, modifier: Modifier = Modifier) {
    if (count <= 1) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            androidx.compose.foundation.layout.Box(
                Modifier.size(7.dp).background(if (index == current) selectedColor else unselectedColor, CircleShape),
            )
        }
    }
}

/**
 * Carrusel con "peek" del siguiente ítem e indicador debajo (`PagingCarouselView`):
 * cada ítem mide el ancho menos 2 × 20 dp de margen y 16 dp de asomo; 8 dp entre ítems.
 */
@Composable
fun PagingCarousel(count: Int, modifier: Modifier = Modifier, itemHeight: Dp, item: @Composable (Int) -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.x2s),
    ) {
        val state = rememberPagerState(pageCount = { count })
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = state,
                contentPadding = PaddingValues(horizontal = Spacing.l),
                pageSpacing = Spacing.xs,
                pageSize = PageSize.Fixed(maxWidth - (Spacing.l * 2 + Spacing.m)),
                modifier = Modifier.fillMaxWidth().height(itemHeight),
            ) { page -> item(page) }
        }
        PageIndicator(count, state.currentPage, DSColor.Black, DSColor.Neutral300)
    }
}

/**
 * `GuCarousels.navigation`: páginas de ancho completo con indicador, sobre fondo oscuro.
 * [onPageChanged] se emite solo cuando el usuario termina de cambiar de página (no en la composición inicial).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPageCarousel(
    count: Int,
    height: Dp,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (Int) -> Unit,
) {
    val state: PagerState = rememberPagerState(pageCount = { count })
    val callback by rememberUpdatedState(onPageChanged)
    LaunchedEffect(state) {
        var first = true
        snapshotFlow { state.settledPage }.collect {
            if (first) first = false else callback(it)
        }
    }
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = state, modifier = Modifier.fillMaxWidth().height(height - 26.dp)) { page(it) }
        PageIndicator(count, state.currentPage, DSColor.White, DSColor.Neutral600, Modifier.height(26.dp))
    }
}
