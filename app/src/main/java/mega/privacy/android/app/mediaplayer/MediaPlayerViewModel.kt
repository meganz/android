package mega.privacy.android.app.mediaplayer

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.mediaplayer.model.MediaPlayerMenuClickedEvent
import mega.privacy.android.app.mediaplayer.model.MediaPlayerState
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.photos.util.LegacyPublicAlbumPhotoNodeProvider
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.CopyChatNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for business logic regarding the toolbar.
 *
 * @property checkNameCollision Required for checking name collisions.
 * @property copyNodeUseCase Required for copying nodes.
 * @property moveNodeUseCase Required for moving nodes.
 */
@HiltViewModel
class MediaPlayerViewModel @Inject constructor(
    private val checkNameCollision: CheckNameCollision,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val copyChatNodeUseCase: CopyChatNodeUseCase,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val legacyPublicAlbumPhotoNodeProvider: LegacyPublicAlbumPhotoNodeProvider,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
) : ViewModel() {

    private val collision = SingleLiveEvent<NameCollision>()
    private val throwable = SingleLiveEvent<Throwable>()
    private val snackbarMessage = SingleLiveEvent<Int>()

    /**
     * The flow for clicked event
     */
    val menuClickEventFlow = MutableSharedFlow<MediaPlayerMenuClickedEvent>()

    private val _itemToRemove = MutableLiveData<Long>()

    /**
     * Removed item update
     */
    val itemToRemove: LiveData<Long> = _itemToRemove

    private val _renameUpdate = MutableLiveData<MegaNode?>()

    /**
     * Rename update
     */
    val renameUpdate: LiveData<MegaNode?> = _renameUpdate

    private val _state = MutableStateFlow(MediaPlayerState(null, false))
    internal val state: StateFlow<MediaPlayerState> = _state

    private val _metadataState = MutableStateFlow(Metadata(null, null, null, ""))
    internal val metadataState: StateFlow<Metadata> = _metadataState

    init {
        monitorAccountDetail()
        monitorIsHiddenNodesOnboarded()
    }

    /**
     * Update clicked event flow
     *
     * @param menuId menu view id
     * @param adapterType the type of adapter
     * @param playingHandle the current playing item handle
     * @param launchIntent the launched Intent
     */
    fun updateMenuClickEventFlow(
        menuId: Int,
        adapterType: Int,
        playingHandle: Long,
        launchIntent: Intent,
    ) {
        viewModelScope.launch {
            menuClickEventFlow.emit(
                MediaPlayerMenuClickedEvent(
                    menuId = menuId,
                    adapterType = adapterType,
                    playingHandle = playingHandle,
                    launchIntent = launchIntent
                )
            )
        }
    }

    internal fun getCollision(): LiveData<NameCollision> = collision

    internal fun onSnackbarMessage(): LiveData<Int> = snackbarMessage

    internal fun onExceptionThrown(): LiveData<Throwable> = throwable

    /**
     * Rename update
     *
     * @param node the renamed node
     */
    fun renameUpdate(node: MegaNode?) {
        _renameUpdate.value = node
    }

    /**
     * Copies a node if there is no name collision.
     *
     * @param node              Node to copy.
     * @param nodeHandle        Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun copyNode(
        node: MegaNode? = null,
        nodeHandle: Long? = null,
        newParentHandle: Long,
    ) {
        viewModelScope.launch {
            val nodeId = node?.handle ?: nodeHandle ?: return@launch
            checkForNameCollision(
                nodeHandle = nodeId,
                newParentHandle = newParentHandle,
                type = NameCollisionType.COPY
            ) {
                runCatching {
                    copyNodeUseCase(
                        nodeToCopy = NodeId(nodeId),
                        newNodeParent = NodeId(newParentHandle),
                        newNodeName = null,
                    )
                }.onSuccess {
                    snackbarMessage.value = R.string.context_correctly_copied
                }.onFailure {
                    throwable.value = it
                    Timber.e("Error not copied $it")
                }
            }
        }
    }

    /**
     * Imports a chat node if there is no name collision.
     *
     * @param node              Node handle to copy.
     * @param chatId            Chat ID where the node is.
     * @param messageId         Message ID where the node is.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun importChatNode(
        node: MegaNode,
        chatId: Long,
        messageId: Long,
        newParentHandle: NodeId,
    ) = viewModelScope.launch {
        runCatching {
            checkNameCollisionUseCase.check(
                node = node,
                parentHandle = newParentHandle.longValue,
                type = NameCollisionType.COPY,
            )
        }.onSuccess { collisionResult ->
            collision.value = collisionResult
        }.onFailure { throwable ->
            when (throwable) {
                is MegaNodeException.ChildDoesNotExistsException -> {
                    copyChatNode(chatId, messageId, newParentHandle)
                }

                else -> Timber.e(throwable)
            }
        }
    }

    /**
     * Copies a chat node
     * @param chatId Chat ID where the node is.
     * @param messageId Message ID where the node is.
     * @param newParentNodeId Parent handle in which the node will be copied.
     */
    private fun copyChatNode(chatId: Long, messageId: Long, newParentNodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId,
                    newNodeParent = newParentNodeId,
                )
            }.onSuccess {
                snackbarMessage.value = R.string.context_correctly_copied
            }.onFailure { copyError ->
                Timber.e(copyError, "The chat node is not copied")
                throwable.value = copyError
            }
        }
    }


    /**
     * Moves a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to move.
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            checkForNameCollision(
                nodeHandle = nodeHandle,
                newParentHandle = newParentHandle,
                type = NameCollisionType.MOVE,
            ) {
                runCatching {
                    moveNodeUseCase(
                        nodeToMove = NodeId(nodeHandle),
                        newNodeParent = NodeId(newParentHandle)
                    )
                }.onSuccess {
                    _itemToRemove.value = nodeHandle
                    snackbarMessage.value = R.string.context_correctly_moved
                }.onFailure {
                    throwable.value = it
                    Timber.e("Error not moved $it")
                }
            }
        }
    }

    /**
     * Checks if there is a name collision before proceeding with the action.
     *
     * @param nodeHandle        Handle of the node to check the name collision.
     * @param newParentHandle   Handle of the parent folder in which the action will be performed.
     * @param completeAction    Action to complete after checking the name collision.
     */
    private suspend fun checkForNameCollision(
        nodeHandle: Long,
        newParentHandle: Long,
        type: NameCollisionType,
        completeAction: suspend (() -> Unit),
    ) {
        runCatching {
            checkNameCollision(
                nodeHandle = NodeId(nodeHandle),
                parentHandle = NodeId(newParentHandle),
                type = type,
            )
        }.onSuccess {
            collision.value = it
        }.onFailure {
            when (it) {
                is MegaNodeException.ChildDoesNotExistsException -> completeAction.invoke()
                is MegaNodeException.ParentDoesNotExistException -> {
                    snackbarMessage.value = R.string.general_error
                }

                else -> Timber.e(it)
            }
        }
    }

    /**
     * Get node for album sharing
     * Because the MegaNode cannot be got by getNodeByHandle if the album shares from others,
     * using legacyPublicAlbumPhotoNodeProvider to get MegaNode
     *
     * @param handle node handle
     */
    fun getNodeForAlbumSharing(handle: Long) =
        legacyPublicAlbumPhotoNodeProvider.getPublicNode(handle)

    private fun monitorAccountDetail() {
        monitorAccountDetailUseCase()
            .onEach { accountDetail ->
                _state.update {
                    it.copy(accountType = accountDetail.levelDetail?.accountType)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun monitorIsHiddenNodesOnboarded() {
        viewModelScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            _state.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
            }
        }
    }

    fun setHiddenNodesOnboarded() {
        _state.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }

    internal fun updateMetaData(metadata: Metadata) = _metadataState.update {
        it.copy(
            title = metadata.title,
            artist = metadata.artist,
            album = metadata.album,
            nodeName = metadata.nodeName,
        )
    }
}
