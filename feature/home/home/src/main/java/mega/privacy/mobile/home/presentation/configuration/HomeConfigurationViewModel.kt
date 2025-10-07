package mega.privacy.mobile.home.presentation.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.usecase.home.DeleteWidgetConfigurationUseCase
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
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
    private val deleteWidgetConfigurationUseCase: DeleteWidgetConfigurationUseCase,
) : ViewModel() {
    val state: StateFlow<HomeConfigurationUiState> by lazy {
        monitorHomeWidgetConfigurationUseCase()
            .onEach { Timber.d("Widget configurations: \n ${it.joinToString("\n")}") }
            .map { list ->
                val configuration = list.associateBy { config -> config.widgetIdentifier }

                val items = widgetProviders
                    .flatMap {
                        it.getWidgets().map { widget ->
                            widgetConfigurationItemMapper(
                                homeWidget = widget,
                                widgetConfiguration = configuration[widget.identifier]
                            )
                        }
                    }

                HomeConfigurationUiState.Data(
                    allowRemoval = items.count { widget -> widget.enabled } > 1,
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
        updateWidgets(orderedItems.mapIndexed { index, widgetConfigurationItem ->
            widgetConfigurationItem.copy(index = index)
        })
    }

    private fun updateWidgets(items: List<WidgetConfigurationItem>) {
        Timber.d("Updated widget configurations: \n ${items.joinToString("\n")}")
        viewModelScope.launch {
            runCatching {
                updateWidgetConfigurationsUseCase(items.map {
                    HomeWidgetConfiguration(
                        widgetIdentifier = it.identifier,
                        widgetOrder = it.index,
                        enabled = it.enabled
                    )
                })
            }.onFailure {
                Timber.e(it, "Failed to update widget configurations")
            }
        }
    }

    fun deleteWidget(item: WidgetConfigurationItem) {
        if (!item.canDelete) return
        viewModelScope.launch {
            runCatching {
                for (provider in widgetProviders) {
                    if (provider.deleteWidget(item.identifier)) {
                        deleteWidgetConfigurationUseCase(item.identifier)
                        break
                    }
                }
            }.onFailure {
                Timber.e(it, "Failed to delete widget: ${item.identifier}")
            }
        }
    }

}
