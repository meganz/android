package mega.privacy.mobile.home.presentation.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetEnabledFlaggedItemsUseCase
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
import mega.privacy.android.domain.usecase.home.ResetHomeWidgetConfigurationsUseCase
import mega.privacy.android.domain.usecase.home.UpdateWidgetConfigurationsUseCase
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.mobile.home.presentation.configuration.mapper.WidgetConfigurationItemMapper
import mega.privacy.mobile.home.presentation.configuration.model.HomeConfigurationUiState
import mega.privacy.mobile.home.presentation.configuration.model.WidgetConfigurationItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeConfigurationViewModel @Inject constructor(
    private val widgetProviders: Set<@JvmSuppressWildcards HomeWidgetProvider>,
    private val monitorHomeWidgetConfigurationUseCase: MonitorHomeWidgetConfigurationUseCase,
    private val widgetConfigurationItemMapper: WidgetConfigurationItemMapper,
    private val updateWidgetConfigurationsUseCase: UpdateWidgetConfigurationsUseCase,
    private val getEnabledFlaggedItemsUseCase: GetEnabledFlaggedItemsUseCase,
    private val resetHomeWidgetConfigurationsUseCase: ResetHomeWidgetConfigurationsUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    val state: StateFlow<HomeConfigurationUiState> by lazy {
        monitorHomeWidgetConfigurationUseCase()
            .onEach { Timber.d("Widget configurations: \n ${it.joinToString("\n")}") }
            .map { list ->
                val configuration = list.associateBy { config -> config.widgetIdentifier }

                val items = widgetProviders
                    .flatMap { provider ->
                        getEnabledFlaggedItemsUseCase(provider.getWidgets())
                            .first()
                            .map { widget ->
                                widgetConfigurationItemMapper(
                                    homeWidget = widget,
                                    widgetConfiguration = configuration[widget.identifier]
                                )
                            }
                    }
                    .sortedBy { it.index }

                HomeConfigurationUiState.Data(
                    allowRemoval = items
                        .filter { it.isConfigurable }
                        .count { widget -> widget.enabled } > 1,
                    widgets = items,
                )

            }.catch { e ->
                Timber.e(e, "Failed to monitor widget configurations")
            }.asUiStateFlow(
                viewModelScope,
                HomeConfigurationUiState.Loading,
            )
    }

    fun updateEnabledState(item: WidgetConfigurationItem, enabled: Boolean) {
        updateWidgets(
            listOf(
                item.copy(
                    enabled = enabled
                )
            )
        )
    }

    fun updateWidgetOrder(orderedItems: List<WidgetConfigurationItem>) {
        applicationScope.launch {
            runCatching {
                val latestEnabledByIdentifier = monitorHomeWidgetConfigurationUseCase()
                    .first()
                    .associate { it.widgetIdentifier to it.enabled }

                val updated = orderedItems.mapIndexed { index, item ->
                    HomeWidgetConfiguration(
                        widgetIdentifier = item.identifier,
                        widgetOrder = index,
                        enabled = latestEnabledByIdentifier[item.identifier] ?: item.enabled,
                    )
                }
                updateWidgetConfigurationsUseCase(updated)
            }.onFailure {
                Timber.e(it, "Failed to update widget order")
            }
        }
    }

    private fun updateWidgets(items: List<WidgetConfigurationItem>) {
        applicationScope.launch {
            runCatching {
                val updated = items.map {
                    HomeWidgetConfiguration(
                        widgetIdentifier = it.identifier,
                        widgetOrder = it.index,
                        enabled = it.enabled
                    )
                }
                updateWidgetConfigurationsUseCase(updated)
            }.onFailure {
                Timber.e(it, "Failed to update widget configurations")
            }
        }
    }

    fun resetWidgetStateToDefault() {
        viewModelScope.launch {
            runCatching {
                resetHomeWidgetConfigurationsUseCase()
            }.onFailure {
                Timber.e(it, "Failed to reset widget configurations to default")
            }
        }
    }
}
