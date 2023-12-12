package mega.privacy.android.feature.sync.ui.synclist.solvedissues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.MonitorSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.ResolutionActionTypeToResolutionNameMapper
import mega.privacy.android.feature.sync.ui.mapper.SolvedIssueItemMapper
import javax.inject.Inject

@HiltViewModel
internal class SyncSolvedIssuesViewModel @Inject constructor(
    private val monitorSyncSolvedIssuesUseCase: MonitorSyncSolvedIssuesUseCase,
    private val solvedIssueItemMapper: SolvedIssueItemMapper,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncSolvedIssuesState())
    val state: StateFlow<SyncSolvedIssuesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorSyncSolvedIssuesUseCase()
                .map { solvedIssuesList ->
                    solvedIssuesList.map { solvedIssue ->
                        solvedIssueItemMapper(
                            solvedIssue = solvedIssue,
                            nodes = solvedIssue.nodeIds.mapNotNull { nodeId ->
                                getNodeByHandleUseCase(nodeId.longValue)
                            },
                        )
                    }
                }.collectLatest {
                    _state.value = SyncSolvedIssuesState(it)
                }
        }
    }
}