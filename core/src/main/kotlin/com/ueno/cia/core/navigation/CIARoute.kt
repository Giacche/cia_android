package com.ueno.cia.core.navigation

import com.ueno.cia.core.model.ChargeProvider
import com.ueno.cia.core.model.ChargeType
import com.ueno.cia.core.model.CongratsInput
import com.ueno.cia.core.model.CpfScreenModel
import com.ueno.cia.core.model.DetailCardItemModel
import com.ueno.cia.core.model.InputAmountScreenModel
import com.ueno.cia.core.model.LinkPreConfirmModel
import com.ueno.cia.core.model.MovementsFilterScreenModel
import com.ueno.cia.core.model.MovementsScreenModel
import com.ueno.cia.core.model.PaymentSelectorScreenModel
import com.ueno.cia.core.model.QRTimeoutModel
import com.ueno.cia.core.model.ReceiptScreenModel
import com.ueno.cia.core.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Rutas de CIA (`CIARoute` en iOS, `go_router` en Flutter). Viven en `core` para que las features se pidan
 * navegación entre sí sin depender unas de otras; `cia-kit` las asocia a pantallas.
 * Las rutas de features todavía no migradas se resuelven con un placeholder "Próximamente".
 */
@Serializable
sealed interface CIARoute {
    @Serializable data object Home : CIARoute

    @Serializable data class ValidateLimitEntryPoint(
        val chargeType: ChargeType,
        val provider: ChargeProvider,
        val canPop: Boolean = false,
    ) : CIARoute

    @Serializable data class Cpf(val input: CPFInput) : CIARoute

    @Serializable data class InputAmount(val input: InputAmountInput) : CIARoute

    @Serializable data class QrGenerate(val input: QRFlowInput) : CIARoute

    @Serializable data class QrTimeout(val screen: QRTimeoutModel) : CIARoute

    @Serializable data class Receipt(val screen: ReceiptScreenModel) : CIARoute

    @Serializable data class LinkPreConfirm(val input: LinkFlowInput) : CIARoute

    @Serializable data class PaymentSelectorEntryPoint(
        val type: String? = null,
        val branchId: String? = null,
        val origin: String? = null,
    ) : CIARoute

    @Serializable data class PaymentSelector(val input: PaymentSelectorInput) : CIARoute

    @Serializable data class PaymentSelectorCountry(val branchId: String? = null, val origin: String? = null) : CIARoute

    @Serializable data object LinkList : CIARoute

    @Serializable data class Movements(val screen: MovementsScreenModel) : CIARoute

    @Serializable data class MovementsFilters(val filterScreen: MovementsFilterScreenModel) : CIARoute

    @Serializable data class LinkGenerate(val input: LinkFlowInput) : CIARoute

    @Serializable data class Detail(val ticketNumber: String, val channel: String? = null) : CIARoute

    @Serializable data object Profile : CIARoute

    @Serializable data class ProfileName(val origin: ProfileOrigin = ProfileOrigin.PROFILE) : CIARoute

    @Serializable data object TaxInfo : CIARoute

    @Serializable data object UpdateField : CIARoute

    @Serializable data class Congrats(val input: CongratsInput) : CIARoute

    @Serializable data class ProductsOnboardingEntryPoint(val productType: String? = null) : CIARoute

    @Serializable data object TapToPhone : CIARoute

    /** Deeplink corto del BFF todavía no mapeado a una ruta (ej. `add_ons/addons`). */
    @Serializable data class Deeplink(val link: String) : CIARoute
}

/** `ProfileOriginEnum`: de dónde se abre la edición del nombre. El valor es el deeplink que manda el BFF. */
@Serializable
enum class ProfileOrigin(val raw: String) {
    HOME("HOME"),
    PROFILE("PROFILE"),
    PROFILE_NAME("PROFILE_NAME"),
}

/** Paths Flutter de pantallas destino usados como `elementDestination` en analytics. */
object CIARouteDestination {
    const val MICROAPP_PATH = "/smart_charge"
    const val INPUT_AMOUNT = "/smart_charge/tap_to_phone/input_amount"
    const val QR_GENERATE = "/smart_charge/qr/generate"
    const val QR_TIMEOUT = "/smart_charge/qr/timeout"
    const val RECEIPT = "/smart_charge/tap_to_phone/receipt"
    const val CPF = "/smart_charge/cpf/number"
    const val LINK_GENERATE = "/smart_charge/link/generate"
    const val PAYMENT_ENTRY_POINT = "/smart_charge/payment/entry_point"
    const val LINK_PRE_CONFIRM = "/smart_charge/link/link_pre_confirm"
    const val PRODUCTS_ONBOARDING = "/smart_charge/products_onboarding"
    const val PROFILE_NAME = "/smart_charge/profile/edit"
    const val MOVEMENTS = "/smart_charge/movements/list"
    const val LINK_LIST = "/smart_charge/link/list"
    const val TAX_INFO = "/smart_charge/ruc_validation/onboarding_tax_info"
    const val UPDATE_FIELD = "/smart_charge/update_field"
    const val HELP_CENTER = "/help-center/sections/Cobros"
}

object ProductType {
    const val LINK = "LINK"
    const val SUBSCRIPTION = "SUBSCRIPTION"
}

// region Argumentos de ruta (`RouteInputs.swift` en iOS)

@Serializable
data class InputAmountInput(
    val screen: InputAmountScreenModel,
    val chargeType: ChargeType,
    val provider: ChargeProvider,
    val linkPreConfirmScreen: LinkPreConfirmModel? = null,
    val cpfNumber: String? = null,
    /** `InputAmountFlowState.canPop`: `true` si se llegó desde un selector (el back vuelve ahí); `false` desde la Home. */
    val canPop: Boolean = false,
)

@Serializable
data class CPFInput(
    val cpfScreen: CpfScreenModel,
    val inputAmountScreen: InputAmountScreenModel,
    val chargeType: ChargeType,
    val provider: ChargeProvider,
    val canPop: Boolean = false,
)

@Serializable
data class QRFlowInput(
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val chargeType: ChargeType,
    val provider: ChargeProvider,
    val cpfNumber: String? = null,
)

/** `LinkFlowState.init(...)`: se arma en el Monto y lo consume la pre-confirmación del link. */
@Serializable
data class LinkFlowInput(
    val chargeType: ChargeType,
    val provider: ChargeProvider,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val amountFormatted: String,
    val description: String,
    val approximateToReceive: String,
    val linkPreConfirmScreen: LinkPreConfirmModel,
    val extraDetailItems: List<DetailCardItemModel>,
)

// endregion

/** Entrada del selector de métodos (`PaymentSelectorFlowState.paymentSelectorScreen` + sucursal y origen). */
@Serializable
data class PaymentSelectorInput(
    val screen: PaymentSelectorScreenModel,
    val branchId: String? = null,
    val origin: String? = null,
)
