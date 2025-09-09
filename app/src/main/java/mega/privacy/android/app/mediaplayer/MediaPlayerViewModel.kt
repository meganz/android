package mega.privacy.android.app.mediaplayer

import android.annotation.SuppressLint
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.MediaPlayerMenuClickedEvent
import mega.privacy.android.app.mediaplayer.model.MediaPlayerState
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumNodeDataUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * ViewModel for business logic regarding the toolbar.
 */
@HiltViewModel
class MediaPlayerViewModel @Inject constructor(
    private val checkNodesNameCollisionWithActionUseCase: CheckNodesNameCollisionWithActionUseCase,
    private val checkChatNodesNameCollisionAndCopyUseCase: CheckChatNodesNameCollisionAndCopyUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val getPublicAlbumNodeDataUseCase: GetPublicAlbumNodeDataUseCase,
    private val getFileUriUseCase: GetFileUriUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {

    private val collision = SingleLiveEvent<NameCollision>()
    private val throwable = SingleLiveEvent<Throwable>()
    private val snackbarMessage = SingleLiveEvent<Int>()
    private val startChatFileOfflineDownload = SingleLiveEvent<ChatFile>()

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

    private val _state = MutableStateFlow(MediaPlayerState())
    internal val state: StateFlow<MediaPlayerState> = _state

    private val _metadataState = MutableStateFlow(Metadata(null, null, null, ""))
    internal val metadataState: StateFlow<Metadata> = _metadataState

    init {
        handleHiddenNodesUIFlow()
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

    internal fun onStartChatFileOfflineDownload(): LiveData<ChatFile> = startChatFileOfflineDownload

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
     * @param nodeHandle        Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    @SuppressLint("TimberArgCount")
    fun copyNode(
        nodeHandle: Long? = null,
        newParentHandle: Long,
    ) {
        if (nodeHandle == null) return
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(nodeHandle to newParentHandle),
                    type = NodeNameCollisionType.COPY,
                )
            }.onSuccess {
                it.firstNodeCollisionOrNull?.let { item ->
                    collision.value = item
                }
                it.moveRequestResult?.let { result ->
                    snackbarMessage.value = if (result.isSuccess) {
                        R.string.context_correctly_copied
                    } else {
                        R.string.context_no_copied
                    }
                }
            }.onFailure {
                Timber.e(it, "Error not copied")
                if (it is NodeDoesNotExistsException) {
                    snackbarMessage.value = R.string.general_error
                } else {
                    throwable.value = it
                }
            }
        }
    }

    /**
     * Imports a chat node if there is no name collision.
     *
     * @param chatId            Chat ID where the node is.
     * @param messageId         Message ID where the node is.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun importChatNode(
        chatId: Long,
        messageId: Long,
        newParentHandle: NodeId,
    ) = viewModelScope.launch {
        runCatching {
            checkChatNodesNameCollisionAndCopyUseCase(
                chatId = chatId,
                messageIds = listOf(messageId),
                newNodeParent = newParentHandle,
            )
        }.onSuccess {
            it.firstChatNodeCollisionOrNull?.let { item ->
                collision.value = item
            }
            it.moveRequestResult?.let { result ->
                snackbarMessage.value = if (result.isSuccess) {
                    R.string.context_correctly_copied
                } else {
                    R.string.context_no_copied
                }
            }
        }.onFailure {
            throwable.value = it
            Timber.e(it)
        }
    }

    /**
     * Moves a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to move.
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    @SuppressLint("TimberArgCount")
    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(nodeHandle to newParentHandle),
                    type = NodeNameCollisionType.MOVE,
                )
            }.onSuccess { result ->
                result.firstNodeCollisionOrNull?.let { item ->
                    collision.value = item
                }
                result.moveRequestResult?.let {
                    if (it.isSuccess) {
                        _itemToRemove.value = nodeHandle
                        snackbarMessage.value = sharedResR.string.context_correctly_moved
                    } else {
                        snackbarMessage.value = R.string.context_no_moved
                    }
                }
            }.onFailure {
                Timber.e(it, "Error not copied")
                if (it is NodeDoesNotExistsException) {
                    snackbarMessage.value = R.string.general_error
                } else {
                    throwable.value = it
                }
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
        getPublicAlbumNodeDataUseCase(NodeId(handle))

    private fun handleHiddenNodesUIFlow() {
        combine(
            monitorAccountDetailUseCase(),
            monitorShowHiddenItemsUseCase(),
        ) { accountDetail, showHiddenItems ->
            val accountType = accountDetail.levelDetail?.accountType
            val businessStatus =
                if (accountType?.isBusinessAccount == true) {
                    getBusinessStatusUseCase()
                } else null

            _state.update {
                it.copy(
                    accountType = accountDetail.levelDetail?.accountType,
                    isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired,
                    showHiddenItems = showHiddenItems,
                )
            }
        }.catch { Timber.e(it) }
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

    /**
     * Save chat node to offline
     *
     * @param chatId    Chat ID where the node is.
     * @param messageId Message ID where the node is.
     */
    fun saveChatNodeToOffline(chatId: Long, messageId: Long) {
        viewModelScope.launch {
            runCatching {
                val chatFile = getChatFileUseCase(chatId = chatId, messageId = messageId)
                    ?: throw IllegalStateException("Chat file not found")
                val isAvailableOffline = isAvailableOfflineUseCase(chatFile)
                if (isAvailableOffline) {
                    snackbarMessage.value = R.string.file_already_exists
                } else {
                    startChatFileOfflineDownload.value = chatFile
                }
            }.onFailure {
                Timber.e(it)
                throwable.value = it
            }
        }
    }

    internal suspend fun getContentUri(file: File) =
        getFileUriUseCase(file, Constants.AUTHORITY_STRING_FILE_PROVIDER)
}
