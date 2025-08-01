package mega.privacy.android.feature.sync.ui.synclist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceNameUseCase
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.MonitorSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SyncListViewModel @Inject constructor(
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase,
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val monitorSyncSolvedIssuesUseCase: MonitorSyncSolvedIssuesUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
    private val getDeviceNameUseCase: GetDeviceNameUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncListState())
    val state: StateFlow<SyncListState> = _state.asStateFlow()

    init {
        observeOnboardingFlow()
        monitorStalledIssue()
        monitorSolvedIssue()
        getDeviceName()
    }

    private fun observeOnboardingFlow() {
        viewModelScope.launch { setOnboardingShownUseCase(true) }
    }

    private fun monitorStalledIssue() {
        viewModelScope.launch {
            monitorSyncStalledIssuesUseCase().catch { Timber.e(it) }
                .collect { stalledIssues ->
                    _state.update {
                        it.copy(
                            stalledIssuesCount = stalledIssues.size
                        )
                    }
                }
        }
    }

    private fun monitorSolvedIssue() {
        viewModelScope.launch {
            monitorSyncSolvedIssuesUseCase().catch { Timber.e(it) }.collect {
                _state.update { state ->
                    state.copy(
                        shouldShowCleanSolvedIssueMenuItem = it.isNotEmpty()
                    )
                }
            }
        }
    }

    private fun getDeviceName() {
        viewModelScope.launch {
            runCatching {
                getDeviceIdUseCase()?.let { deviceId ->
                    val deviceName = getDeviceNameUseCase(deviceId).orEmpty()
                    _state.update { it.copy(deviceName = deviceName) }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}
