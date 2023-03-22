package mega.privacy.android.app.presentation.settings.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.advanced.model.SettingsAdvancedState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.IsUseHttpsEnabled
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.SetUseHttps
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import javax.inject.Inject

/**
 * Settings advanced view model
 *
 *
 * @param isUseHttpsEnabled use case to check current status of the preference
 * @param rootNodeExistsUseCase use case to determine if the root node exists
 * @param monitorConnectivityUseCase use case to monitor connectivity
 * @param ioDispatcher coroutine dispatcher for io code
 *
 * @property setUseHttps use case that is called when the preference is updated
 *
 * @property state The current UI state
 */
@HiltViewModel
class SettingsAdvancedViewModel @Inject constructor(
    private val setUseHttps: SetUseHttps,
    isUseHttpsEnabled: IsUseHttpsEnabled,
    rootNodeExistsUseCase: RootNodeExistsUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsAdvancedState())
    val state: StateFlow<SettingsAdvancedState> = _state

    init {
        viewModelScope.launch(ioDispatcher) {
            merge(
                checkPreferenceCheckedState(isUseHttpsEnabled),
                checkPreferenceEnabledState(monitorConnectivityUseCase, rootNodeExistsUseCase),
            ).collect {
                _state.update(it)
            }
        }
    }

    private suspend fun checkPreferenceEnabledState(
        monitorConnectivityUseCase: MonitorConnectivityUseCase,
        rootNodeExistsUseCase: RootNodeExistsUseCase,
    ) = combine(
        monitorConnectivityUseCase(),
        flowOf(rootNodeExistsUseCase())
    ) { online, exists ->
        online && exists
    }.map { enabled ->
        { state: SettingsAdvancedState -> state.copy(useHttpsEnabled = enabled) }
    }

    private suspend fun checkPreferenceCheckedState(isUseHttpsEnabled: IsUseHttpsEnabled) =
        flowOf(isUseHttpsEnabled())
            .map { enabled ->
                { state: SettingsAdvancedState -> state.copy(useHttpsChecked = enabled) }
            }

    /**
     * Use https preference changed
     *
     * @param enabled new status
     */
    fun useHttpsPreferenceChanged(enabled: Boolean) {
        viewModelScope.launch {
            setUseHttps(enabled)
        }
    }
}
