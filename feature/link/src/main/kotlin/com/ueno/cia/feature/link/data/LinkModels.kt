package com.ueno.cia.feature.link.data

import com.ueno.cia.core.model.ButtonModel
import com.ueno.cia.core.model.CongratsCardItemActionButtonModel
import com.ueno.cia.core.model.EmptyStateModel
import com.ueno.cia.core.util.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

// features/link/screens/link_list/models (Flutter).

@Serializable
data class LinkListResponse(val linkListScreen: LinkListScreenModel? = null)

@Serializable
data class LinkListScreenModel(
    val tag: String,
    val appBarTitle: String,
    val type: String,
    val tabsOptions: List<LinkListTabModel>,
    val tabItems: List<LinkListItemModel>? = null,
    val emptyState: EmptyStateModel? = null,
    val primaryButton: ButtonModel? = null,
    val secondaryButton: ButtonModel? = null,
)

@Serializable
data class LinkListTabModel(
    val label: String,
    val id: String,
    val disabled: Boolean,
    val selected: Boolean,
    val message: LinkListMessageModel? = null,
)

@Serializable
data class LinkListMessageModel(val id: String, val type: String, val message: String)

@Serializable
data class LinkListItemModel(
    val label: String,
    val value: String,
    val actionButton: CongratsCardItemActionButtonModel? = null,
)

/** `LinkGenerateRequestModel`. */
@Serializable
data class LinkGenerateRequest(
    val currency: String,
    val type: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val description: String,
)
