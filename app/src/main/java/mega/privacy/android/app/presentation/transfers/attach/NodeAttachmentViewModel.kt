package mega.privacy.android.app.presentation.transfers.attach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.StorageStatePayWallException
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.GetNodesToAttachUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachContactsUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Node attachment view model
 *
 */
@HiltViewModel
class NodeAttachmentViewModel @Inject constructor(
    private val getNodesToAttachUseCase: GetNodesToAttachUseCase,
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val getContactHandleUseCase: GetContactHandleUseCase,
    private val attachContactsUseCase: AttachContactsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NodeAttachmentUiState())

    /**
     * Ui state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Start attach nodes
     *
     * @param nodeIds
     */
    fun startAttachNodes(nodeIds: List<NodeId>) {
        _uiState.update { state ->
            state.copy(event = NodeAttachmentEvent.AttachNode(nodeIds))
        }
    }

    /**
     * Attach nodes to chat by email
     */
    fun attachNodesToChatByEmail(nodeIds: List<NodeId>, email: String) {
        viewModelScope.launch {
            runCatching {
                getContactHandleUseCase(email)
            }.onSuccess {
                if (it != null) {
                    attachNodesToChat(nodeIds, longArrayOf(), longArrayOf(it))
                } else {
                    Timber.d("No contact handle found")
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Attach contact to chat
     *
     * @param email
     * @param chatIds
     * @param userHandles
     */
    fun attachContactToChat(email: String, chatIds: LongArray, userHandles: LongArray) {
        processAttachNodesToChat(
            chatIds = chatIds,
            userHandles = userHandles,
            onAction = { allChatIds ->
                allChatIds.forEach {
                    attachContactsUseCase(it, listOf(email))
                }
            }
        )
    }

    /**
     * Get nodes to attach
     *
     */
    fun getNodesToAttach(nodeIds: List<NodeId>) {
        viewModelScope.launch {
            runCatching {
                getNodesToAttachUseCase(nodeIds)
            }.onSuccess {
                if (it.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(event = NodeAttachmentEvent.SelectChat(it))
                    }
                } else {
                    Timber.d("No nodes to attach")
                }
            }.onFailure {
                if (it is StorageStatePayWallException) {
                    _uiState.update { state ->
                        state.copy(event = NodeAttachmentEvent.ShowOverDiskQuotaPaywall)
                    }
                }
                Timber.e(it)
            }
        }
    }

    /**
     * Attach nodes to chat
     *
     * @param nodeIds
     * @param chatIds
     */
    fun attachNodesToChat(nodeIds: List<NodeId>, chatIds: LongArray, userHandles: LongArray) {
        processAttachNodesToChat(
            chatIds,
            userHandles,
            onAction = { allChatIds ->
                attachMultipleNodesUseCase(nodeIds, allChatIds)
            }
        )
    }

    private fun processAttachNodesToChat(
        chatIds: LongArray,
        userHandles: LongArray,
        onAction: suspend (List<Long>) -> Unit,
    ) {
        viewModelScope.launch {
            // ignore the user handles that create chat failed
            val chatIdsFromUserHandles = userHandles.map { userHandle ->
                runCatching {
                    get1On1ChatIdUseCase(userHandle)
                }.onFailure {
                    Timber.e(it)
                }.getOrNull()
            }.filterNotNull()
            val allChatIds = chatIdsFromUserHandles + chatIds.toList()
            runCatching {
                onAction(allChatIds)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(event = NodeAttachmentEvent.AttachNodeSuccess(allChatIds))
                }
                Timber.d("Nodes attached to chat")
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Mark event handled
     *
     */
    fun markEventHandled() {
        _uiState.update { state ->
            state.copy(event = null)
        }
    }
}