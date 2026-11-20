package com.ueno.cia.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ueno.cia.core.model.HomeModalModel
import com.ueno.cia.core.tenant.Tenant
import com.ueno.cia.feature.home.data.HomeBranchModel

/**
 * Punto de entrada de la Home para el host: conecta el [HomeViewModel] con [HomeScreen], muestra los bottom
 * sheets y delega al host las navegaciones ([HomeDestination.Push], [HomeDestination.Reset], [HomeDestination.Exit],
 * [HomeDestination.HelpCenter]).
 */
@Composable
fun HomeRoute(viewModel: HomeViewModel, tenant: Tenant, onNavigate: (HomeDestination) -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var optionsModal by remember { mutableStateOf<HomeModalModel?>(null) }
    var branches by remember { mutableStateOf<List<HomeBranchModel>?>(null) }
    val navigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(viewModel) { viewModel.start() }
    LaunchedEffect(viewModel) {
        viewModel.destinations.collect { destination ->
            when (destination) {
                is HomeDestination.OptionsSheet -> optionsModal = destination.modal
                is HomeDestination.BranchSheet -> branches = destination.branches
                else -> navigate(destination)
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        tenant = tenant,
        actions = remember(viewModel) { viewModel.actions() },
        modifier = modifier,
    )

    optionsModal?.let { modal ->
        HomeOptionsSheet(
            modal = modal,
            // El sheet se cierra solo y recién después entrega la opción
            onSelect = viewModel::didSelectModalOption,
            onDismiss = { optionsModal = null },
        )
    }
    branches?.let { list ->
        HomeBranchesSheet(
            branches = list,
            onSelect = viewModel::didSelectBranch,
            onDismiss = { branches = null },
        )
    }
}
