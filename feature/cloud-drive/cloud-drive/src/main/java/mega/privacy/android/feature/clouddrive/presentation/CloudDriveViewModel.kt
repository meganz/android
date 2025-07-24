package mega.privacy.android.feature.clouddrive.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.toDuration
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.feature.clouddrive.presentation.model.CloudDriveUiState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CloudDriveViewModel @Inject constructor(
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    val fileTypeIconMapper: FileTypeIconMapper,
    savedStateHandle: SavedStateHandle,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    internal val uiState: StateFlow<CloudDriveUiState>
        field = MutableStateFlow(
            CloudDriveUiState(
                currentFolderId = savedStateHandle.get<NodeId>(CLOUD_DRIVE_FOLDER_ID)
                    ?: NodeId(-1L),
            )
        )

    init {
        loadNodes()
    }

    private fun loadNodes() {
        viewModelScope.launch {
            val folderId = uiState.value.currentFolderId
            runCatching {
                getFileBrowserNodeChildrenUseCase(folderId.longValue)
            }.onSuccess {
                uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        items = getNodeUiItems(
                            nodeList = it,
                        ),
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * A temporary mapper for sample screen
     */
    private suspend fun getNodeUiItems(
        nodeList: List<TypedNode>,
        highlightedNodeId: NodeId? = null,
        highlightedNames: Set<String>? = null,
    ): List<NodeUiItem<TypedNode>> = withContext(defaultDispatcher) {
        val existingNodeList = uiState.value.items
        val selectedHandles = uiState.value.selectedItems
        val existingHighlightedIds = existingNodeList.asSequence()
            .filter { it.isHighlighted }
            .map { it.node.id }
            .toSet()

        nodeList.mapIndexed { index, node ->
            val isSelected = selectedHandles.contains(node.id.longValue)
            val fileDuration = if (node is FileNode) {
                node.type.toDuration()?.let { durationInSecondsTextMapper(it) }
            } else null
            val isHighlighted = existingHighlightedIds.contains(node.id) ||
                    node.id == highlightedNodeId ||
                    highlightedNames?.contains(node.name) == true
            val hasCorrespondingIndex = existingNodeList.size > index
            NodeUiItem(
                node = node,
                isSelected = if (hasCorrespondingIndex) isSelected else false,
                isInvisible = if (hasCorrespondingIndex) existingNodeList[index].isInvisible else false,
                fileDuration = fileDuration,
                isHighlighted = isHighlighted,
            )
        }
    }

    companion object {
        const val CLOUD_DRIVE_FOLDER_ID = "CLOUD_DRIVE_FOLDER_ID"
    }
}
