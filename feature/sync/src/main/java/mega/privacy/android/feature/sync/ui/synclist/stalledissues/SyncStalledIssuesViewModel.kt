package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.StalledIssueItemMapper
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SyncStalledIssuesViewModel @Inject constructor(
    private val monitorStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val stalledIssueItemMapper: StalledIssueItemMapper,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncStalledIssuesState(emptyList()))
    val state: StateFlow<SyncStalledIssuesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorStalledIssuesUseCase()
                .catch {
                    Timber.d("Error monitoring stalled issues: $it")
                }
                .map { stalledIssuesList ->
                    stalledIssuesList.map { stalledIssue ->
                        val areAllNodesFolders =
                            stalledIssue
                                .nodeIds
                                .map {
                                    getNodeByHandleUseCase(it.longValue)
                                }
                                .all { node ->
                                    node is FolderNode
                                }

                        stalledIssueItemMapper(
                            stalledIssueEntity = stalledIssue,
                            areAllNodesFolders = areAllNodesFolders
                        )
                    }
                }
                .collectLatest { stalledIssues ->
                    _state.update { SyncStalledIssuesState(stalledIssues) }
                }
        }
    }
}
