package com.ueno.cia.feature.home.ui

import com.ueno.cia.core.navigation.CIARoute
import com.ueno.cia.feature.home.data.HomeBranchModel
import com.ueno.cia.core.model.HomeModalModel

/** Navegación de una sola vez que la Home le pide al host (`Signal<Destination>` en iOS). */
sealed interface HomeDestination {
    data class Push(val route: CIARoute) : HomeDestination
    data class Reset(val route: CIARoute) : HomeDestination
    data object Exit : HomeDestination
    data class OptionsSheet(val modal: HomeModalModel) : HomeDestination
    data class BranchSheet(val branches: List<HomeBranchModel>) : HomeDestination
    data class HelpCenter(val deeplink: String) : HomeDestination
}
