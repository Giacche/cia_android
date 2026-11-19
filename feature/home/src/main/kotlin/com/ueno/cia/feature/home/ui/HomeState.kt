package com.ueno.cia.feature.home.ui

import com.ueno.cia.core.model.ButtonModel
import com.ueno.cia.core.model.LinkButtonModel
import com.ueno.cia.core.model.MovementChargeModel
import com.ueno.cia.feature.home.data.HomeBannerModel
import com.ueno.cia.feature.home.data.HomeBranchModel
import com.ueno.cia.feature.home.data.HomeHeaderResponse
import com.ueno.cia.core.model.HomeInformationAlertModel
import com.ueno.cia.feature.home.data.HomeLayoutSectionModel
import com.ueno.cia.feature.home.data.HomeMessageModel
import com.ueno.cia.core.model.HomeOptionModel
import com.ueno.cia.feature.home.data.HomePendingSettlementModel
import com.ueno.cia.feature.home.data.HomeRecentsResponse
import com.ueno.cia.feature.home.data.HomeSectionID

/** Estado de una sección (equivale a `HomeStateStatus` + su dato). */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Loaded<T>(val value: T) : Loadable<T>
    data object Failed : Loadable<Nothing>
}

val <T> Loadable<T>.valueOrNull: T? get() = (this as? Loadable.Loaded<T>)?.value
val Loadable<*>.isLoading: Boolean get() = this is Loadable.Loading

data class HomeHeaderContent(
    val merchantName: String,
    val subscriptionPlan: String?,
    val profileButton: ButtonModel,
    val helpButton: ButtonModel?,
    /** Header de sucursales (Empresas o carousel de Individuos). `null` en el header simple. */
    val branchCarousel: HomeHeaderResponse.BranchCarousel?,
)

data class HomeOptionsContent(
    val options: List<HomeOptionModel>,
    /** Si hay mensaje, se muestra en lugar de las opciones. */
    val message: HomeMessageModel?,
)

/** Equivale a `HomeState` de Flutter. */
data class HomeState(
    val appBarTitle: String,
    val sections: List<HomeLayoutSectionModel>,
    val header: Loadable<HomeHeaderContent> = Loadable.Loading,
    val options: Loadable<HomeOptionsContent> = Loadable.Loading,
    val banners: Loadable<List<HomeBannerModel>> = Loadable.Loading,
    val alerts: Loadable<List<HomeInformationAlertModel>> = Loadable.Loading,
    val pendingSettlement: Loadable<HomePendingSettlementModel> = Loadable.Loading,
    val recents: Loadable<HomeRecentsResponse> = Loadable.Loading,
    val charges: Loadable<List<MovementChargeModel>> = Loadable.Loading,
    val selectedBranchId: String? = null,
    /** Sucursal elegida desde la lista: va primera en el carrusel (Flutter usa `selectedBranchAddress` como key). */
    val pinnedBranchId: String? = null,
    val isBranchSelectionActive: Boolean = false,
) {
    /** Ids conocidos en el orden del layout. */
    val sectionIds: List<HomeSectionID> = sections.mapNotNull { HomeSectionID.fromId(it.id) }

    fun has(id: HomeSectionID): Boolean = id in sectionIds

    val hasHeader: Boolean get() = sectionIds.any { it.isHeader }
    val hasPendingSettlement: Boolean get() = sectionIds.any { it.isPendingSettlement }

    private fun section(id: HomeSectionID): HomeLayoutSectionModel? = sections.firstOrNull { it.id == id.id }

    val optionsTitle: String get() = section(HomeSectionID.Options)?.title.orEmpty()
    val bannersTitle: String get() = section(HomeSectionID.Banners)?.title.orEmpty()
    val recentsTitle: String get() = section(HomeSectionID.Recents)?.title.orEmpty()
    val recentsLink: LinkButtonModel? get() = section(HomeSectionID.Recents)?.linkText

    /** Flutter oculta el carrusel solo cuando cargó y vino vacío, o durante la selección de sucursal. */
    val showsBanners: Boolean get() = banners != Loadable.Loaded(emptyList<HomeBannerModel>()) && !isBranchSelectionActive
    val showsRecents: Boolean get() = !isBranchSelectionActive

    val visibleCharges: List<MovementChargeModel>
        get() = charges.valueOrNull.orEmpty().take(MAX_VISIBLE_CHARGES)

    /** Recientes muestra skeleton mientras carga cualquiera de sus dos fuentes. */
    val recentsIsLoading: Boolean get() = charges.isLoading || recents.isLoading

    companion object {
        const val MAX_VISIBLE_CHARGES = 5
    }
}

/** Estado de la pantalla: el layout se pide primero; falla entero si el layout falla. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Loaded(val state: HomeState) : HomeUiState
    data object Failed : HomeUiState
}
