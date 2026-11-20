package com.ueno.cia.core.model

import com.ueno.cia.core.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

// features/input_amount/models (Flutter).

@Serializable
data class InputAmountScreenModel(
    val tag: String,
    val appBarTitle: String,
    val type: String,
    val primaryButton: ButtonModel,
    val data: InputAmountScreenDataModel,
    val summaryInfo: InputAmountSummaryInfoModel,
) {
    val commission: BigDecimal get() = data.commission
    val maxAmount: BigDecimal get() = data.maxAmount
    val minAmount: BigDecimal get() = data.minAmount
}

@Serializable
data class InputAmountScreenDataModel(
    val title: String,
    @Serializable(with = BigDecimalSerializer::class) val maxAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val minAmount: BigDecimal,
    val currency: String,
    val maxAmountErrorMessage: String,
    val minAmountErrorMessage: String,
    @Serializable(with = BigDecimalSerializer::class) val commission: BigDecimal,
    val descriptionInput: InputAmountDescriptionInputModel? = null,
)

@Serializable
data class InputAmountDescriptionInputModel(
    val title: String,
    val maxLength: Int,
    val minLength: Int,
    val helper: String? = null,
)

@Serializable
data class InputAmountSummaryInfoModel(
    val amountTitle: String,
    val amountValueTemplate: String,
    val commissionTitle: String,
    val commissionValueTemplate: String,
)

// features/cpf/screens/cpf/models (Flutter). La pantalla de CPF se migra después; el modelo viaja en el validate.

@Serializable
data class CpfScreenModel(
    val type: String,
    val appBarTitle: String,
    val title: String,
    val numberInput: NumberInputModel,
    val primaryButton: ButtonModel,
)

@Serializable
data class NumberInputModel(
    val title: String,
    val maxLength: Int,
    val regex: String,
    val errorMessage: String,
    val helper: String,
)

// features/link y features/detail (Flutter): lo que Validate e InputAmount le pasan al flujo de link.

@Serializable
data class DetailCardModel(
    val title: String,
    val items: List<DetailCardItemModel>? = null,
    val inlineMessage: InlineMessageModel? = null,
)

@Serializable
data class DetailCardItemModel(
    val label: String,
    val value: String,
    val actionLink: LinkButtonModel? = null,
    val badge: BadgeModel? = null,
)

@Serializable
data class LinkPreConfirmModel(
    val appBarTitle: String,
    val type: String,
    val title: String,
    val primaryButton: ButtonModel,
    val detailCard: DetailCardModel,
    val tyc: TycModel? = null,
    val subTitle: String? = null,
    val tag: String? = null,
)
