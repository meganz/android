package mega.privacy.android.app.presentation.tags

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.tags.TagsActivity.Companion.MAX_TAGS_PER_NODE
import mega.privacy.android.app.presentation.tags.TagsActivity.Companion.NODE_ID
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.node.GetAllNodeTagsUseCase
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
    private val getAllNodeTagsUseCase: GetAllNodeTagsUseCase,
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
        getNodeTags(nodeId)
        viewModelScope.launch {
            monitorNodeUpdatesById(nodeId).catch {
                Timber.e(it, "Error monitoring node updates by id")
            }.conflate()
                .collect {
                    getNodeTags(nodeId)
                }
        }
    }

    /**
     * Get a node by its handle.
     *
     * @param nodeId    Node ID
     */
    fun getNodeTags(nodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                getNodeByIdUseCase(nodeId)
            }.onSuccess { node ->
                _uiState.update { it.copy(nodeTags = node?.tags.orEmpty().toImmutableList()) }
                getAllNodeTags()
            }.onFailure {
                Timber.e(it, "Error getting node by handle")
            }
        }
    }

    /**
     * Add a tag to a node.
     *
     * @param tag Tag to add
     */
    fun addNodeTag(tag: String) {
        viewModelScope.launch {
            runCatching {
                manageNodeTagUseCase(nodeHandle = nodeId, newTag = tag)
                onTagUpdateSuccess()
            }.onFailure {
                Timber.e(it, "Error adding tag to node")
            }
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
    fun validateTagName(tag: String) {
        viewModelScope.launch {
            val tagString = tag.removePrefix("#").lowercase()
            // Step 1: Validate the tag
            val (message, isError) = tagsValidationMessageMapper(
                tag = tagString,
                nodeTags = uiState.value.nodeTags,
                userTags = uiState.value.tags
            )
            // Step 2: Update UI state with validation result
            _uiState.update {
                it.copy(
                    message = message,
                    isError = isError,
                    searchText = tagString
                )
            }

            // Step 3: Get all node tags
            getAllNodeTags()
        }
    }

    private fun getAllNodeTags() {
        viewModelScope.launch {
            runCatching {
                getAllNodeTagsUseCase(searchString = uiState.value.searchText)
            }.onSuccess { tags ->
                _uiState.update { currentState ->
                    currentState.copy(
                        tags = tags.orEmpty().sortedByDescending { it in currentState.nodeTags }
                            .toImmutableList()
                    )
                }
            }.onFailure {
                Timber.e(it, "Error getting tags")
            }
        }
    }

    /**
     * Removes a tag from the node
     *
     * @param tag the tag to remove
     */
    fun addOrRemoveTag(tag: String) {
        viewModelScope.launch {
            // Step 1: Validate the tag
            val isTagPresent = uiState.value.nodeTags.contains(tag)
            if (!isTagPresent && uiState.value.nodeTags.size >= MAX_TAGS_PER_NODE) {
                _uiState.update { it.copy(showMaxTagsError = triggered) }
                Timber.e("Cannot add more tags. Maximum limit reached for tag: $tag")
                return@launch
            }
            // Step 2: Add or remove the tag
            runCatching {
                manageNodeTagUseCase(
                    nodeHandle = nodeId,
                    oldTag = tag.takeIf { isTagPresent },
                    newTag = tag.takeUnless { isTagPresent },
                )
                onTagUpdateSuccess()
                getAllNodeTags()
                Timber.d("Tag updated successfully $tag already exists: $isTagPresent")
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun onTagUpdateSuccess() {
        _uiState.update {
            it.copy(
                message = null,
                isError = false,
                searchText = ""
            )
        }
    }

    /**
     * Consume the maximum tags error.
     */
    fun consumeMaxTagsError() {
        _uiState.update { it.copy(showMaxTagsError = consumed) }
    }
}