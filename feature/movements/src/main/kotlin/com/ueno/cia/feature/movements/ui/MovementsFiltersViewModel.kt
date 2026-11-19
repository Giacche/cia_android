package com.ueno.cia.feature.movements.ui

import androidx.lifecycle.ViewModel
import com.ueno.cia.core.model.MovementsFilterOptionModel
import com.ueno.cia.core.model.MovementsFilterScreenModel
import com.ueno.cia.core.model.MovementsQuery
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** Equivale a `MovementsFiltersState`. */
data class MovementsFiltersViewState(
    val screen: MovementsFilterScreenModel,
    val selectedChannel: String? = null,
    val selectedMethod: String? = null,
    val selectedStatus: String? = null,
    val periodFrom: String? = null,
    val periodTo: String? = null,
    val amountFrom: Double? = null,
    val amountTo: Double? = null,
) {
    val hasChannelField: Boolean get() = screen.channel.label.isNotEmpty() && screen.channel.options.isNotEmpty()
    val hasMethodField: Boolean get() = screen.method.label.isNotEmpty() && screen.method.options.isNotEmpty()
    val hasStatusField: Boolean get() = screen.status.label.isNotEmpty() && screen.status.options.isNotEmpty()
    val hasAmountField: Boolean get() = screen.amount.title.isNotEmpty()

    /** Los métodos dependen del canal elegido (`channel == null` aplica a todos). */
    val methodOptions: List<MovementsFilterOptionModel>
        get() {
            val channel = selectedChannel ?: return screen.method.options
            return screen.method.options.filter { it.channel == null || channel in it.channel!! }
        }

    /** Sin canal elegido el método queda deshabilitado (si el formulario tiene canal). */
    val methodEnabled: Boolean get() = !hasChannelField || selectedChannel != null

    fun nameOf(id: String?, options: List<MovementsFilterOptionModel>): String? = options.firstOrNull { it.id == id }?.name
}

sealed interface MovementsFiltersDestination {
    data object Pop : MovementsFiltersDestination
}

/** Equivale a `MovementsFiltersNotifier`. */
class MovementsFiltersViewModel(
    filterScreen: MovementsFilterScreenModel,
    private val session: MovementsSession,
) : ViewModel() {
    private val _state = MutableStateFlow(
        session.appliedFilters.let { applied ->
            MovementsFiltersViewState(
                screen = filterScreen,
                selectedChannel = applied?.channel ?: filterScreen.channel.options.firstOrNull()?.id,
                selectedMethod = applied?.method,
                selectedStatus = applied?.status,
                periodFrom = applied?.periodFrom,
                periodTo = applied?.periodTo,
                amountFrom = applied?.amountFrom,
                amountTo = applied?.amountTo,
            )
        },
    )
    val state: StateFlow<MovementsFiltersViewState> = _state.asStateFlow()

    private val _destinations = Channel<MovementsFiltersDestination>(Channel.BUFFERED)
    val destinations: Flow<MovementsFiltersDestination> = _destinations.receiveAsFlow()

    /** Cambiar de canal descarta el método (los métodos dependen del canal). */
    fun selectChannel(id: String?) = _state.update { it.copy(selectedChannel = id, selectedMethod = null) }
    fun selectMethod(id: String) = _state.update { it.copy(selectedMethod = id) }
    fun selectStatus(id: String) = _state.update { it.copy(selectedStatus = id) }
    fun setPeriodFrom(date: String?) = _state.update { it.copy(periodFrom = date) }
    fun setPeriodTo(date: String?) = _state.update { it.copy(periodTo = date) }
    fun setAmountFrom(amount: Double?) = _state.update { it.copy(amountFrom = amount) }
    fun setAmountTo(amount: Double?) = _state.update { it.copy(amountTo = amount) }

    /** `clearFilters`: vuelve al formulario vacío (también sin canal). */
    fun clearFilters() {
        _state.value = MovementsFiltersViewState(screen = _state.value.screen)
    }

    /** `applyFilters`: guarda los filtros en la sesión, avisa al historial y vuelve. */
    fun applyFilters() {
        val state = _state.value
        session.appliedFilters = MovementsQuery(
            page = 1,
            size = session.pageSize,
            type = session.selectedTabId,
            channel = state.selectedChannel,
            method = state.selectedMethod,
            status = state.selectedStatus,
            periodFrom = state.periodFrom,
            periodTo = state.periodTo,
            amountFrom = state.amountFrom,
            amountTo = state.amountTo,
        )
        session.notifyFiltersApplied()
        _destinations.trySend(MovementsFiltersDestination.Pop)
    }

    companion object {
        /** dd/mm/aa: solo dígitos, máximo 6, con barras en las posiciones 2 y 4. */
        fun dateMask(text: String): String {
            val digits = text.filter { it.isDigit() }.take(6)
            return buildString {
                digits.forEachIndexed { index, char ->
                    if (index == 2 || index == 4) append('/')
                    append(char)
                }
            }
        }

        /** Miles con punto: 1250000 → 1.250.000. */
        fun thousands(text: String): String {
            val digits = text.filter { it.isDigit() }
            return digits.reversed().chunked(3).joinToString(".").reversed()
        }

        fun amountOf(text: String): Double? = text.replace(".", "").takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    }
}
