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
import mega.privacy.android.feature.sync.ui.mapper.StalledIssueItemMapper
import javax.inject.Inject

@HiltViewModel
internal class SyncListViewModel @Inject constructor(
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase,
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val resolveStalledIssueUseCase: ResolveStalledIssueUseCase,
    private val stalledIssueItemMapper: StalledIssueItemMapper
) : ViewModel() {

    private val _state = MutableStateFlow(SyncListState())
    val state: StateFlow<SyncListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            setOnboardingShownUseCase(true)
        }
        viewModelScope.launch {
            monitorSyncStalledIssuesUseCase()
                .collectLatest { stalledIssues ->
                    _state.update { SyncListState(stalledIssues.size) }
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
}