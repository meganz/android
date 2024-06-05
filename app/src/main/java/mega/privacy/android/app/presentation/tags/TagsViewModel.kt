package mega.privacy.android.app.presentation.tags

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.tags.TagsActivity.Companion.NODE_ID
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.UpdateNodeTagUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel to handle tags screen logic.
 *
 * @property updateNodeTagUseCase    Use case to update a node tag
 */
@HiltViewModel
class TagsViewModel @Inject constructor(
    private val updateNodeTagUseCase: UpdateNodeTagUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    stateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()
    private val nodeId: Long? = stateHandle.get<Long>(NODE_ID)

    init {
        nodeId?.let { nodeHandle ->
            getNodeByHandle(NodeId(nodeHandle))
        }
    }

    /**
     * Get a node by its handle.
     *
     * @param nodeId    Node ID
     */
    fun getNodeByHandle(nodeId: NodeId) = viewModelScope.launch {
        runCatching {
            getNodeByIdUseCase(nodeId)
        }.onSuccess { node ->
            _uiState.update { it.copy(tags = node?.tags.orEmpty()) }
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
            requireNotNull(nodeId)
            updateNodeTagUseCase(nodeHandle = NodeId(nodeId), newTag = tag)
        }.onSuccess {
            _uiState.update { it.copy(informationMessage = triggered(InfoToShow.SimpleString(R.string.choose_file))) }
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
        val message: String?
        val isError: Boolean
        val isBlank = tag.isBlank()
        if (isBlank) {
            message =
                "Use tags to help you find and organise your data. Try tagging by year, location, project, or subject."
            isError = false
        } else if (tag.all { it.isLetterOrDigit() }.not()) {
            message = "Tags can only contain letters and numbers."
            isError = true
        } else if (tag.length > 32) {
            message = "Tags can be up to 32 characters long."
            isError = true
        } else if (uiState.value.tags.contains(tag)) {
            message = "Tag already exists"
            isError = true
        } else {
            message = null
            isError = false
        }
        _uiState.update { it.copy(message = message, isError = isError) }
        return !isError && !isBlank

    }
}