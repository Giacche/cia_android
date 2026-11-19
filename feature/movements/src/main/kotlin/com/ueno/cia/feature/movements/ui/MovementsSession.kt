package com.ueno.cia.feature.movements.ui

import androidx.lifecycle.ViewModel
import com.ueno.cia.core.model.MovementsQuery
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Estado compartido entre el historial y los filtros (`MovementsFlowState`): pestaña elegida y filtros aplicados.
 * Vive en el `ViewModelStore` de la entrada del historial, así los filtros lo obtienen del padre.
 */
class MovementsSession(val pageSize: Int) : ViewModel() {
    var selectedTabId: String = "charges"
    var appliedFilters: MovementsQuery? = null

    private val _didApplyFilters = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Avisa al historial que debe recargar (`ref.invalidate(movementsProvider)`). */
    val didApplyFilters: SharedFlow<Unit> = _didApplyFilters

    fun notifyFiltersApplied() {
        _didApplyFilters.tryEmit(Unit)
    }
}
