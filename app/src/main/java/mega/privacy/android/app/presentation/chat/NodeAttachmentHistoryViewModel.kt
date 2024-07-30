package mega.privacy.android.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.namecollision.data.LegacyNameCollision
import mega.privacy.android.app.namecollision.data.toLegacyImport
import mega.privacy.android.app.presentation.copynode.CopyRequestState
import mega.privacy.android.app.presentation.copynode.toCopyRequestResult
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import timber.log.Timber
import javax.inject.Inject


/**
 * View Model for [mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity]
 */
@HiltViewModel
class NodeAttachmentHistoryViewModel @Inject constructor(
    private val checkChatNodesNameCollisionAndCopyUseCase: CheckChatNodesNameCollisionAndCopyUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
) : ViewModel() {

    private val _copyResultFlow = MutableStateFlow<CopyRequestState?>(null)

    /**
     * Flow of [CopyRequestState] to notify the result of the copy operation.
     */
    val copyResultFlow = _copyResultFlow.asStateFlow()

    private val _collisionsFlow = MutableStateFlow<List<LegacyNameCollision>>(emptyList())

    /**
     * Flow of [LegacyNameCollision] to notify the name collisions
     */
    val collisionsFlow = _collisionsFlow.asStateFlow()

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = isConnectedToInternetUseCase()

    /**
     * Imports a chat node if there is no name collision.
     *
     * @param chatId            Chat ID where the node is.
     * @param messageIds        Message IDs where the node is.
     * @param newParentHandle   Parent handle in which the nodes will be copied.
     */
    fun importChatNodes(
        chatId: Long,
        messageIds: MutableList<Long>,
        newParentHandle: Long,
    ) {
        viewModelScope.launch {
            runCatching {
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = messageIds,
                    newNodeParent = NodeId(newParentHandle),
                )
            }.onSuccess { result ->
                result.collisionResult.conflictNodes.values
                    .filterIsInstance<NodeNameCollision.Chat>()
                    .map { it.toLegacyImport() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { _collisionsFlow.update { _ -> it } }

                result.moveRequestResult?.let {
                    _copyResultFlow.update { _ ->
                        CopyRequestState(result = it.toCopyRequestResult())
                    }
                }
            }.onFailure { throwable ->
                Timber.e(throwable)
                _copyResultFlow.update {
                    CopyRequestState(error = throwable)
                }
            }
        }
    }

    /**
     * Clears the copy result after consuming the value
     */
    fun copyResultConsumed() {
        _copyResultFlow.value = null
    }

    /**
     * Clears the collisions after consuming the value
     */
    fun nodeCollisionsConsumed() {
        _collisionsFlow.update {
            emptyList()
        }
    }

}
