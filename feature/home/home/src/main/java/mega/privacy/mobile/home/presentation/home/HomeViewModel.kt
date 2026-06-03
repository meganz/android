package mega.privacy.mobile.home.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.MonitorHomeConfigurationTooltipShownUseCase
import mega.privacy.android.domain.usecase.SetHomeConfigurationTooltipShownUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider
import mega.privacy.android.core.coroutine.asUiStateFlow
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
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorHomeConfigurationTooltipShownUseCase: MonitorHomeConfigurationTooltipShownUseCase,
    private val setHomeConfigurationTooltipShownUseCase: SetHomeConfigurationTooltipShownUseCase,
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
                                ?: widget.defaultOrder.ordinal
                        }.map {
                            HomeWidgetItem(
                                it.identifier,
                                { modifier, navigationHandler, transferHandler ->
                                    it.DisplayWidget(
                                        modifier = modifier,
                                        navigationHandler = navigationHandler,
                                        transferHandler = transferHandler
                                    )
                                }
                            )
                        }
                    list
                },
            monitorConnectivityUseCase().catch { Timber.e(it) },
            monitorHomeCustomizationFeatureFlag().catch { Timber.e(it) },
            monitorHomeConfigurationTooltipShownUseCase().catch { Timber.e(it) },
        ) { widgets, hasInternetConnection, isHomeCustomizationEnabled, isTooltipShown ->
            if (hasInternetConnection) {
                HomeUiState.Data(
                    widgets = widgets,
                    isHomeCustomizationEnabled = isHomeCustomizationEnabled,
                    showHomeConfigurationTooltip = isHomeCustomizationEnabled && !isTooltipShown,
                )
            } else {
                val hasOfflineFiles =
                    runCatching { hasOfflineFilesUseCase() }.getOrDefault(false)
                HomeUiState.Offline(hasOfflineFiles = hasOfflineFiles)
            }
        }.asUiStateFlow(
            viewModelScope,
            HomeUiState.Loading,
        )
    }

    private fun monitorHomeCustomizationFeatureFlag() =
        monitorConnectivityUseCase()
            .runningFold(false) { previous, isOnline ->
                if (isOnline) {
                    getFeatureFlagValueUseCase(ApiFeatures.HomeConfiguration)
                } else {
                    previous
                }
            }
            .distinctUntilChanged()


    fun onHomeConfigurationTooltipDismissed() {
        viewModelScope.launch {
            runCatching { setHomeConfigurationTooltipShownUseCase() }
                .onFailure { Timber.e(it) }
        }
    }
}
