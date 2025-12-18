package mega.privacy.mobile.home.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.mobile.home.presentation.home.model.HomeUiState
import mega.privacy.mobile.home.presentation.home.model.HomeWidgetItem
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val widgetProviders: Set<@JvmSuppressWildcards HomeWidgetProvider>,
    private val monitorHomeWidgetConfigurationUseCase: MonitorHomeWidgetConfigurationUseCase,
) : ViewModel() {

    val state: StateFlow<HomeUiState> by lazy {
        monitorHomeWidgetConfigurationUseCase()
            .map { it.associateBy { config -> config.widgetIdentifier } }
            .mapLatest { configuration ->
                val list: List<HomeWidgetItem> =
                    widgetProviders.map { it.getWidgets() }.flatten().filter { widget ->
                        configuration[widget.identifier]?.enabled
                            ?: true
                    }.sortedBy { widget ->
                        configuration[widget.identifier]?.widgetOrder
                            ?: widget.defaultOrder
                    }.map {
                        HomeWidgetItem(
                            it.identifier,
                            { modifier, navigationHandler, transferHandler ->
                                it.DisplayWidget(
                                    modifier = modifier,
                                    navigationHandler = navigationHandler,
                                    transferHandler = transferHandler
                                )
                            })
                    }
                HomeUiState.Data(list)
            }.asUiStateFlow(
                viewModelScope,
                HomeUiState.Loading,
            )
    }
}
