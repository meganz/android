package mega.privacy.android.feature.sync.ui.synclist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.ResolveStalledIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.ClearSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.MonitorSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.StalledIssueItemMapper
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SyncListViewModel @Inject constructor(
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase,
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val resolveStalledIssueUseCase: ResolveStalledIssueUseCase,
    private val stalledIssueItemMapper: StalledIssueItemMapper,
    private val monitorSyncSolvedIssuesUseCase: MonitorSyncSolvedIssuesUseCase,
    private val clearSyncSolvedIssuesUseCase: ClearSyncSolvedIssuesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncListState())
    val state: StateFlow<SyncListState> = _state.asStateFlow()

    init {
        observeOnboardingFlow()
        monitorStalledIssue()
        monitorSolvedIssue()
    }

    private fun observeOnboardingFlow() {
        viewModelScope.launch { setOnboardingShownUseCase(true) }
    }

    private fun monitorStalledIssue() {
        viewModelScope.launch {
            monitorSyncStalledIssuesUseCase()
                .collectLatest { stalledIssues ->
                    _state.update { SyncListState(stalledIssues.size) }
                }
        }
    }

    private fun monitorSolvedIssue() {
        viewModelScope.launch {
            monitorSyncSolvedIssuesUseCase().collect {
                _state.update { state ->
                    state.copy(
                        shouldShowCleanSolvedIssueMenuItem = it.isNotEmpty()
                    )
                }
            }
        }
    }

    fun handleAction(action: SyncListAction) {
        when (action) {
            is SyncListAction.ResolveStalledIssue -> {
                viewModelScope.launch {
                    resolveStalledIssueUseCase(
                        action.selectedResolution,
                        stalledIssueItemMapper(action.uiItem)
                    )
                }

                _state.update { SyncListState(snackbarMessage = "Conflict resolved") }
            }

            SyncListAction.SnackBarShown -> {
                _state.update { SyncListState(snackbarMessage = null) }
            }
        }
    }

    fun onClearSyncOptionsPressed() {
        viewModelScope.launch {
            runCatching {
                clearSyncSolvedIssuesUseCase()
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}