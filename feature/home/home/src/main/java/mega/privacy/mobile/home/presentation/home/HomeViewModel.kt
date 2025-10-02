package mega.privacy.mobile.home.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
            .flatMapLatest { configuration ->
                combine(widgetProviders.map { it.getWidgets() }.flatten().filter { widget ->
                    configuration[widget.identifier]?.enabled
                        ?: true
                }.sortedBy { widget ->
                    configuration[widget.identifier]?.widgetOrder
                        ?: widget.defaultOrder
                }.map { widget ->
                    widget.getWidget().map { viewHolder -> widget.identifier to viewHolder }
                }) { it }
            }.map { list ->
                HomeUiState.Data(list.map {
                    HomeWidgetItem(it.first, it.second.widgetFunction)
                })
            }.asUiStateFlow(
                viewModelScope,
                HomeUiState.Loading,
            )
    }
}
