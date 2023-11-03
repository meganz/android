package mega.privacy.android.feature.sync.ui.synclist.solvedissues

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.core.R
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import javax.inject.Inject

@HiltViewModel
internal class SyncSolvedIssuesViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(SyncSolvedIssuesState())
    val state: StateFlow<SyncSolvedIssuesState> = _state.asStateFlow()

    init {
        _state.value = SyncSolvedIssuesState(
            getMockSolvedIssuesList()
        )
    }

    private fun getMockSolvedIssuesList() = listOf(
        SolvedIssueUiItem(
            nodeIds = listOf(NodeId(1L)),
            localPaths = listOf("Mock folder name"),
            resolutionExplanation = "Folders were merged",
            icon = R.drawable.ic_folder_list,
        ),
        SolvedIssueUiItem(
            nodeIds = listOf(NodeId(2L)),
            localPaths = listOf("Mock folder name"),
            resolutionExplanation = "Folders were merged",
            icon = R.drawable.ic_folder_list,
        ),
        SolvedIssueUiItem(
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("Mock file name"),
            resolutionExplanation = "All duplicates were removed",
            icon = R.drawable.ic_generic_list
        ),
        SolvedIssueUiItem(
            nodeIds = listOf(NodeId(4L)),
            localPaths = listOf("Mock file name"),
            resolutionExplanation = "All items were renamed",
            icon = R.drawable.ic_generic_list,
        ),
        SolvedIssueUiItem(
            nodeIds = listOf(NodeId(6L)),
            localPaths = listOf("Mock folder name"),
            resolutionExplanation = "All items were renamed",
            icon = R.drawable.ic_generic_list,
        ),
        SolvedIssueUiItem(
            nodeIds = listOf(NodeId(7L)),
            localPaths = listOf("Mock folder name"),
            resolutionExplanation = "All items were renamed",
            icon = R.drawable.ic_generic_list,
        )
    )
}