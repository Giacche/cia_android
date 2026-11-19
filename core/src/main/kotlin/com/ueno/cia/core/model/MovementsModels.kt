package com.ueno.cia.core.model

import kotlinx.serialization.Serializable

// features/movements (Flutter). La Home muestra la primera página de cobros; Movimientos usa el resto.

@Serializable
data class MovementsScreenModel(
    val type: String,
    val tag: String,
    val appBarTitle: String,
    val title: String,
    val pageSize: Int,
)

@Serializable
data class MovementChargeModel(
    val title: String,
    val subtitle: String,
    val amount: String,
    val date: String,
    val hour: String,
    val ticketNumber: String,
    val id: String? = null,
)

@Serializable
data class MovementsResponse(
    val charges: List<MovementChargeModel>,
    val emptyState: EmptyStateModel? = null,
    val page: Int? = null,
    val size: Int? = null,
    // Solo en la pantalla completa de movimientos (la Home no los usa)
    val tabsOptions: List<MovementFiltersModel>? = null,
    val filterButton: ButtonModel? = null,
    val activeFilters: List<MovementActiveFilterModel>? = null,
    val clearFiltersButton: ButtonModel? = null,
    val filterScreen: MovementsFilterScreenModel? = null,
)

/** `MovementFiltersModel`: pestaña del historial (Cobros / Servicios). */
@Serializable
data class MovementFiltersModel(
    val id: String,
    val title: String,
    val icon: String = "",
    val active: Boolean = false,
    val disabled: Boolean = false,
)

@Serializable
data class MovementActiveFilterModel(val id: String, val label: String)

// features/movements/screens/movements_filters/models (Flutter).

@Serializable
data class MovementsFilterScreenModel(
    val type: String,
    val appBarTitle: String,
    val tag: String,
    val channel: MovementsFilterSelectModel,
    val method: MovementsFilterSelectModel,
    val status: MovementsFilterSelectModel,
    val period: MovementsFilterPeriodModel,
    val amount: MovementsFilterAmountModel,
    val secondaryButton: ButtonModel,
    val primaryButton: ButtonModel,
)

@Serializable
data class MovementsFilterSelectModel(
    val label: String,
    val hint: String,
    val options: List<MovementsFilterOptionModel>,
)

@Serializable
data class MovementsFilterOptionModel(
    val id: String,
    val name: String,
    /** Canales en los que aplica (`null` = todos). */
    val channel: List<String>? = null,
)

@Serializable
data class MovementsFilterInputModel(val title: String, val hint: String)

@Serializable
data class MovementsFilterPeriodModel(
    val title: String,
    val from: MovementsFilterInputModel,
    val to: MovementsFilterInputModel,
    val description: String,
)

@Serializable
data class MovementsFilterAmountModel(
    val title: String,
    val from: MovementsFilterInputModel,
    val to: MovementsFilterInputModel,
)

/** `MovementsRequestModel`: página + filtros aplicados. Viaja como query. */
data class MovementsQuery(
    val page: Int,
    val size: Int? = null,
    val type: String? = null,
    val channel: String? = null,
    val method: String? = null,
    val status: String? = null,
    val periodFrom: String? = null,
    val periodTo: String? = null,
    val amountFrom: Double? = null,
    val amountTo: Double? = null,
) {
    fun queryItems(): Map<String, String> = buildMap {
        put("page", page.toString())
        put("history-tab", type ?: "charges")
        size?.let { put("size", it.toString()) }
        channel?.let { put("channel", it) }
        method?.let { put("method", it) }
        status?.let { put("status", it) }
        periodFrom?.let { put("periodFrom", it) }
        periodTo?.let { put("periodTo", it) }
        // Dart serializa el double con decimales ("5000.0")
        amountFrom?.let { put("amountFrom", it.toString()) }
        amountTo?.let { put("amountTo", it.toString()) }
    }
}
