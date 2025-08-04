package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.ResolveStalledIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.StalledIssueItemMapper
import mega.privacy.android.feature.sync.ui.synclist.SyncListAction
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SyncStalledIssuesViewModel @Inject constructor(
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val resolveStalledIssueUseCase: ResolveStalledIssueUseCase,
    private val stalledIssueItemMapper: StalledIssueItemMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncStalledIssuesState(emptyList()))
    val state: StateFlow<SyncStalledIssuesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorSyncStalledIssuesUseCase()
                .catch { Timber.e("Error monitoring stalled issues: $it") }
                .distinctUntilChanged()
                .map { stalledIssuesList ->
                    stalledIssuesList.map { stalledIssue ->
                        val nodes = stalledIssue.nodeIds.mapNotNull {
                            getNodeByHandleUseCase(it.longValue)
                        }
                        stalledIssueItemMapper(
                            nodes = nodes,
                            stalledIssueEntity = stalledIssue,
                        )
                    }
                }
                .collect { stalledIssues ->
                    _state.update {
                        it.copy(
                            stalledIssues = stalledIssues
                        )
                    }
                }
        }
    }

    fun handleAction(action: SyncListAction) {
        when (action) {
            is SyncListAction.ResolveStalledIssue -> {
                viewModelScope.launch {
                    if (action.isApplyToAll) {
                        val groupedIssues =
                            _state.value.stalledIssues.filter { it.actions.contains(action.selectedResolution) }
                        val result = groupedIssues.mapAsync { issue ->
                            runCatching {
                                resolveStalledIssueUseCase(
                                    action.selectedResolution, stalledIssueItemMapper(issue)
                                )
                            }.onFailure {
                                Timber.e(it)
                            }
                        }
                        Timber.d("Resolved ${result.count { it.isSuccess }} stalled issues with action: ${action.selectedResolution}")
                        _state.update {
                            it.copy(
                                snackbarMessageContent = triggered(sharedR.string.sync_stalled_issues_resolved)
                            )
                        }
                    } else {
                        runCatching {
                            resolveStalledIssueUseCase(
                                action.selectedResolution, stalledIssueItemMapper(action.uiItem)
                            )
                        }.onFailure {
                            Timber.e(it)
                        }
                        _state.update {
                            it.copy(
                                snackbarMessageContent = triggered(sharedR.string.sync_stalled_issue_resolved)
                            )
                        }
                    }
                }
            }

            SyncListAction.SnackBarShown -> {
                _state.update { state ->
                    state.copy(snackbarMessageContent = consumed())
                }
            }
        }
    }
}
