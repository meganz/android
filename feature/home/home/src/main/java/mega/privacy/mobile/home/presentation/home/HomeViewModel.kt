package mega.privacy.mobile.home.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.mobile.home.presentation.home.model.HomeUiState
import mega.privacy.mobile.home.presentation.home.model.HomeWidgetItem
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val widgetProviders: Set<@JvmSuppressWildcards HomeWidgetProvider>,
    private val monitorHomeWidgetConfigurationUseCase: MonitorHomeWidgetConfigurationUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val hasOfflineFilesUseCase: HasOfflineFilesUseCase,
) : ViewModel() {

    val state: StateFlow<HomeUiState> by lazy {
        combine(
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
                    list
                },
            monitorConnectivityUseCase().catch { Timber.e(it) },
        ) { widgets, hasInternetConnection ->
            if (hasInternetConnection) {
                HomeUiState.Data(widgets = widgets)
            } else {
                val hasOfflineFiles = runCatching { hasOfflineFilesUseCase() }.getOrDefault(false)
                HomeUiState.Offline(hasOfflineFiles = hasOfflineFiles)
            }
        }.asUiStateFlow(
            viewModelScope,
            HomeUiState.Loading,
        )
    }
}
