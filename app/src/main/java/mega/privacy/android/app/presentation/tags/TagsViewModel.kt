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
import mega.privacy.android.domain.usecase.node.GetAllNodeTagsUseCase
import mega.privacy.android.domain.usecase.node.ManageNodeTagUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
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
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    stateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()
    private val nodeId: NodeId = NodeId(stateHandle.get<Long>(NODE_ID) ?: -1L)

    init {
        updateExistingTagsAndErrorState(nodeId)
        viewModelScope.launch {
            monitorNodeUpdatesUseCase().catch {
                Timber.e(it, "Error monitoring node updates")
            }.conflate()
                .collect { nodeChanges ->
                    if (nodeChanges.changes.any { it.key.id == nodeId }) {
                        updateExistingTagsAndErrorState(nodeId)
                    } else {
                        validateTagName()
                    }
                }
        }
    }

    /**
     * Get a node by its handle.
     *
     * @param nodeId    Node ID
     */
    fun updateExistingTagsAndErrorState(nodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                getNodeByIdUseCase(nodeId)
            }.onSuccess { node ->
                _uiState.update { it.copy(nodeTags = node?.tags.orEmpty().toImmutableList()) }
                validateTagName()
            }.onFailure {
                Timber.e(it, "Error getting node by handle")
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
    fun validateTagName(tag: String = uiState.value.searchText) {
        viewModelScope.launch {
            runCatching {
                getAllNodeTagsUseCase(searchString = tag)
            }.onSuccess { userTags ->
                val sortedTags = userTags.orEmpty()
                    .sortedByDescending { it in uiState.value.nodeTags }
                    .toImmutableList()
                val (message, isError) = tagsValidationMessageMapper(
                    tag = tag,
                    nodeTags = uiState.value.nodeTags,
                    userTags = userTags.orEmpty()
                )
                _uiState.update { currentState ->
                    currentState.copy(
                        message = message,
                        isError = isError,
                        tags = sortedTags,
                        searchText = tag
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
            // Validate the tag
            val isTagPresent = uiState.value.nodeTags.contains(tag)

            //Check if the tag is already present and if the maximum tag limit is reached.
            if (!isTagPresent && uiState.value.nodeTags.size >= MAX_TAGS_PER_NODE) {
                Timber.e("Cannot add more tags. Maximum limit reached for tag: $tag")
                _uiState.update { it.copy(showMaxTagsError = triggered) }
                return@launch
            }

            // Add or remove the tag
            runCatching {
                manageNodeTagUseCase(
                    nodeHandle = nodeId,
                    oldTag = tag.takeIf { isTagPresent },
                    newTag = tag.takeUnless { isTagPresent },
                )
                onTagUpdateSuccess(event = if (isTagPresent) TagUpdate.REMOVE else TagUpdate.ADD)
                Timber.d("Tag updated successfully $tag already exists: $isTagPresent")
            }.onFailure {
                Timber.e(it)
            }

            // Update the UI & revalidate the tag
            validateTagName()
        }
    }

    private fun onTagUpdateSuccess(event: TagUpdate) {
        _uiState.update {
            it.copy(
                message = null,
                isError = false,
                searchText = "",
                tagsUpdatedEvent = triggered(event)
            )
        }
    }

    /**
     * Consume the maximum tags error.
     */
    fun consumeMaxTagsError() {
        _uiState.update { it.copy(showMaxTagsError = consumed) }
    }

    /**
     * Consume the tags updated event.
     */
    fun consumeTagsUpdatedEvent() {
        _uiState.update { it.copy(tagsUpdatedEvent = consumed()) }
    }
}