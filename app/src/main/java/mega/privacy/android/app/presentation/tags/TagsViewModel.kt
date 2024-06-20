package mega.privacy.android.app.presentation.tags

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.tags.TagsActivity.Companion.NODE_ID
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.node.ManageNodeTagUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel to handle tags screen logic.
 *
 * @property manageNodeTagUseCase    Use case to update a node tag
 */
@HiltViewModel
class TagsViewModel @Inject constructor(
    private val manageNodeTagUseCase: ManageNodeTagUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val tagsValidationMessageMapper: TagsValidationMessageMapper,
    monitorNodeUpdatesById: MonitorNodeUpdatesById,
    stateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()
    private val nodeId: NodeId = NodeId(stateHandle.get<Long>(NODE_ID) ?: -1L)

    init {
        getNodeByHandle(nodeId, true)
        viewModelScope.launch {
            monitorNodeUpdatesById(nodeId).conflate().collect {
                getNodeByHandle(nodeId)
            }
        }
    }

    /**
     * Get a node by its handle.
     *
     * @param nodeId    Node ID
     */
    fun getNodeByHandle(nodeId: NodeId, fromInit: Boolean = false) = viewModelScope.launch {
        runCatching {
            getNodeByIdUseCase(nodeId)
        }.onSuccess { node ->
            _uiState.update { it.copy(tags = node?.tags.orEmpty()) }
            if (fromInit) {
                validateTagName("")
            }
        }.onFailure {
            Timber.e(it, "Error getting node by handle")
        }
    }

    /**
     * Add a tag to a node.
     *
     * @param tag           Tag to add
     */
    fun addNodeTag(tag: String) = viewModelScope.launch {
        runCatching {
            manageNodeTagUseCase(nodeHandle = nodeId, newTag = tag)
        }.onFailure {
            Timber.e(it, "Error adding tag to node")
        }
    }

    /**
     * Consume the information message.
     */
    fun consumeInfoMessage() {
        _uiState.update { it.copy(informationMessage = consumed()) }
    }

    /**
     * Validate the tag name.
     *
     * @param tag   Tag to validate
     * @return      True if the tag is valid, false otherwise
     */
    fun validateTagName(tag: String): Boolean {
        val (message, isError) = tagsValidationMessageMapper(
            tag = tag,
            nodeTags = uiState.value.tags,
            userTags = uiState.value.tags
        )
        _uiState.update { it.copy(message = message, isError = isError) }
        return !isError && tag.isNotBlank()
    }

    /**
     * Removes a tag from the node
     *
     * @param tag the tag to remove
     */
    fun removeTag(tag: String) = viewModelScope.launch {
        runCatching {
            manageNodeTagUseCase(nodeId, tag, null)
        }.onSuccess {
            Timber.i("$tag removed from node $nodeId")
        }.onFailure {
            Timber.e(it)
        }
    }
}