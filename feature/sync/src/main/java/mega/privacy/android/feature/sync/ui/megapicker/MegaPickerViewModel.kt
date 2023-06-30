package mega.privacy.android.feature.sync.ui.megapicker

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import javax.inject.Inject

@HiltViewModel
internal class MegaPickerViewModel @Inject constructor(
//    private val setSelectedMegaFolderUseCase: SetSelectedMegaFolderUseCase // The line is added in the next MR
) : ViewModel() {

    private val _state = MutableStateFlow(MegaPickerState())
    val state: StateFlow<MegaPickerState> = _state.asStateFlow()

    init {
        _state.update {
            val mockState = getMockState()
            it.copy(
                currentFolder = mockState.currentFolder, nodes = mockState.nodes
            )
        }
    }

    fun handleAction(action: MegaPickerAction) {
        when (action) {
            is MegaPickerAction.FolderClicked -> {
                // Change current folder
            }

            MegaPickerAction.CurrentFolderSelected -> {
                val id = state.value.currentFolder?.id?.longValue
                val name = state.value.currentFolder?.name
                if (id != null && name != null) {
//                    setSelectedMegaFolderUseCase(RemoteFolder(id, name)) // The line is added in the next MR
                }
            }
        }
    }

    private fun getMockState(): MegaPickerState = MegaPickerState(
        object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val device = null
            override val childFolderCount = 1
            override val childFileCount = 1
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(1L)
            override val name = "My important files"
            override val parentId = NodeId(2L)
            override val base64Id = "11L"
            override val label = 1
            override val hasVersion = true
            override val isFavourite = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
        }, getMockData()
    )


    private fun getMockData(): List<TypedNode> = SampleNodeDataProvider.values

}