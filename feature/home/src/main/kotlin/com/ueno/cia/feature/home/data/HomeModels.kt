package com.ueno.cia.feature.home.data

import com.ueno.cia.core.model.BadgeModel
import com.ueno.cia.core.model.ButtonModel
import com.ueno.cia.core.model.CongratsScreenModel
import com.ueno.cia.core.model.EmptyStateModel
import com.ueno.cia.core.model.HomeInformationAlertModel
import com.ueno.cia.core.model.HomeOptionModel
import com.ueno.cia.core.model.LinkButtonModel
import com.ueno.cia.core.model.MovementsScreenModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Modelos de features/home/screens/home/models (Flutter). Mismos nombres de campo y nulabilidad.

// region Layout (GET /v4/home/layout)

@Serializable
data class HomeLayoutResponse(val layout: HomeLayoutModel)

@Serializable
data class HomeLayoutModel(
    val appBarTitle: String,
    val type: String,
    val sections: List<HomeLayoutSectionModel>,
    val errorScreen: CongratsScreenModel? = null,
)

@Serializable
data class HomeLayoutSectionModel(
    val id: String,
    val title: String? = null,
    val linkText: LinkButtonModel? = null,
)

/** Ids de sección que conoce la Home. Un id nuevo del BFF se ignora (ver [fromId]). */
enum class HomeSectionID(val id: String) {
    Header("header"),
    HeaderEmpresas("header_empresas"),
    HeaderCarousel("header_carousel"),
    Alerts("alerts"),
    Options("options"),
    Banners("banners"),
    Recents("recents"),
    TotalSales("total_sales"),

    /** Flutter marca loading con este id pero carga con `total_sales`; se aceptan ambos. */
    PendingSettlement("pending_settlement");

    val isHeader: Boolean get() = this == Header || this == HeaderEmpresas || this == HeaderCarousel
    val isPendingSettlement: Boolean get() = this == TotalSales || this == PendingSettlement

    companion object {
        fun fromId(id: String): HomeSectionID? = entries.firstOrNull { it.id == id }
    }
}

// endregion

// region Header (GET /v4/home/header)

@Serializable
data class HomeHeaderResponse(
    val type: String,
    val individuos: Individuos? = null,
    val empresas: BranchCarousel? = null,
    val individuosCarousel: BranchCarousel? = null,
    val subscriptionPlan: String? = null,
    val editProfileButton: ButtonModel,
    val helpButton: ButtonModel? = null,
) {
    @Serializable
    data class Individuos(val merchantName: String)

    @Serializable
    data class BranchCarousel(
        val branches: List<HomeBranchModel>,
        val showAllBranchesButton: ButtonModel? = null,
    )

    val isEmpresas: Boolean get() = type == "HOME_HEADER_EMPRESAS"
    val isIndividuosCarousel: Boolean get() = type == "HOME_HEADER_INDIVIDUOS_CAROUSEL"
    val isBranchCarousel: Boolean get() = isEmpresas || isIndividuosCarousel
    val branchCarouselData: BranchCarousel? get() = if (isEmpresas) empresas else individuosCarousel
}

@Serializable
data class HomeBranchModel(
    val address: String,
    val branchId: String,
    val subscriptionPlan: String? = null,
    val name: String? = null,
)

// endregion

// region Options (GET /v4/home/options)

@Serializable
data class HomeOptionsResponse(
    val type: String,
    val messages: List<HomeMessageModel>,
    val options: List<HomeOptionModel>,
)

@Serializable
data class HomeMessageModel(
    val id: String,
    val type: String,
    val message: String,
    val description: String,
) {
    val isUserInBlacklist: Boolean get() = id == "USER_IN_BLACKLIST"
    val isEmptyOptions: Boolean get() = id == "EMPTY_OPTIONS"
}

// endregion

// region Banners (GET /v4/home/banners)

@Serializable
data class HomeBannersResponse(
    val type: String,
    val banners: List<HomeBannerModel> = emptyList(),
)

@Serializable
data class HomeBannerModel(
    val overline: String,
    val title: String,
    val color: String,
    val asset: String,
    val size: String,
    val buttonText: String,
    val actionType: String,
    val type: String? = null,
)

// endregion

// region Recents (GET /v4/home/recents)

@Serializable
data class HomeRecentsResponse(
    val type: String,
    val emptyState: EmptyStateModel,
    val historyScreen: MovementsScreenModel,
)

// endregion

// region Alerts (GET /v3/home/alerts)

@Serializable
data class HomeAlertsResponse(
    val type: String,
    @SerialName("informationAlert") val alerts: List<HomeInformationAlertModel>,
)

// endregion

// region Pendiente de liquidación (GET /v4/home/total_sales)

/**
 * El contrato del sandbox Flutter usa la clave `pendingSettlement`; el modelo espera `totalSales`.
 * Se aceptan ambas ([value] devuelve la que venga).
 */
@Serializable
data class HomePendingSettlementResponse(
    val type: String,
    val totalSales: HomePendingSettlementModel? = null,
    val pendingSettlement: HomePendingSettlementModel? = null,
) {
    val value: HomePendingSettlementModel
        get() = totalSales ?: pendingSettlement ?: error("Falta totalSales / pendingSettlement")
}

@Serializable
data class HomePendingSettlementModel(val title: String, val amount: String)

// endregion
