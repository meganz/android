package mega.privacy.android.app.presentation.imagepreview

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.app.domain.usecase.offline.SetNodeAvailableOffline
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenuOptions
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewState
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.favourites.AddFavouritesUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI
import javax.inject.Inject

@HiltViewModel
class ImagePreviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageNodeFetchers: Map<@JvmSuppressWildcards ImagePreviewFetcherSource, @JvmSuppressWildcards ImageNodeFetcher>,
    private val imagePreviewMenuOptionsMap: Map<@JvmSuppressWildcards ImagePreviewMenuSource, @JvmSuppressWildcards ImagePreviewMenuOptions>,
    private val addImageTypeUseCase: AddImageTypeUseCase,
    private val getImageUseCase: GetImageUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val checkNameCollision: CheckNameCollision,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val addFavouritesUseCase: AddFavouritesUseCase,
    private val removeFavouritesUseCase: RemoveFavouritesUseCase,
    private val setNodeAvailableOffline: SetNodeAvailableOffline,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val disableExportNodesUseCase: DisableExportNodesUseCase,
    private val removePublicLinkResultMapper: RemovePublicLinkResultMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val imagePreviewFetcherSource: ImagePreviewFetcherSource
        get() = savedStateHandle[IMAGE_NODE_FETCHER_SOURCE] ?: ImagePreviewFetcherSource.TIMELINE

    private val params: Bundle
        get() = savedStateHandle[FETCHER_PARAMS] ?: Bundle()

    private val currentImageNodeIdValue: Long
        get() = savedStateHandle[PARAMS_CURRENT_IMAGE_NODE_ID_VALUE] ?: 0L

    private val imagePreviewMenuSource: ImagePreviewMenuSource
        get() = savedStateHandle[IMAGE_PREVIEW_MENU_OPTIONS] ?: ImagePreviewMenuSource.TIMELINE

    private val imageNodesOffline: MutableMap<NodeId, Boolean> = mutableMapOf()

    private val _state = MutableStateFlow(ImagePreviewState())

    val state: StateFlow<ImagePreviewState> = _state

    private val menuOptions: ImagePreviewMenuOptions?
        get() = imagePreviewMenuOptionsMap[imagePreviewMenuSource]

    init {
        monitorImageNodes()
        monitorOfflineNodeUpdates()
    }

    private fun monitorOfflineNodeUpdates() {
        monitorOfflineNodeUpdatesUseCase()
            .catch { Timber.e(it) }
            .mapLatest { offlineList ->
                Timber.d("IP monitorOfflineNodeUpdates:$offlineList")
                _state.value.currentImageNode?.let { currentImageNode ->
                    val handle = currentImageNode.id.longValue.toString()
                    val index = offlineList.indexOfFirst { handle == it.handle }
                    val isAvailableOffline = index != -1 && isAvailableOffline(currentImageNode)

                    imageNodesOffline[currentImageNode.id] = isAvailableOffline
                    _state.update {
                        it.copy(
                            isCurrentImageNodeAvailableOffline = isAvailableOffline
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun monitorImageNodes() {
        val imageFetcher = imageNodeFetchers[imagePreviewFetcherSource] ?: return
        imageFetcher.monitorImageNodes(params)
            .catch { Timber.e(it) }
            .mapLatest { imageNodes ->
                val (currentImageNodeIndex, currentImageNode) = findCurrentImageNode(imageNodes)
                val isCurrentImageNodeAvailableOffline =
                    currentImageNode?.isAvailableOffline ?: false
                _state.update {
                    it.copy(
                        isInitialized = true,
                        imageNodes = imageNodes,
                        currentImageNodeIndex = currentImageNodeIndex,
                        currentImageNode = currentImageNode,
                        isCurrentImageNodeAvailableOffline = isCurrentImageNodeAvailableOffline
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun findCurrentImageNode(imageNodes: List<ImageNode>): Pair<Int, ImageNode?> {
        val index = imageNodes.indexOfFirst { currentImageNodeIdValue == it.id.longValue }

        if (index != -1) {
            return index to imageNodes[index]
        }

        // If the image node is not found, calculate the target index based on the current state
        val currentImageNodeIndex = _state.value.currentImageNodeIndex
        val targetImageNodeIndex =
            if (currentImageNodeIndex > imageNodes.lastIndex) imageNodes.lastIndex else currentImageNodeIndex

        return targetImageNodeIndex to imageNodes.getOrNull(targetImageNodeIndex)
    }

    suspend fun isSlideshowOptionVisible(imageNode: ImageNode): Boolean {
        return menuOptions?.isSlideshowOptionVisible(imageNode) ?: false
                && _state.value.imageNodes.size > 1
    }

    suspend fun isGetLinkOptionVisible(imageNode: ImageNode): Boolean {
        return menuOptions?.isGetLinkOptionVisible(imageNode) ?: false
    }

    suspend fun isSaveToDeviceOptionVisible(imageNode: ImageNode): Boolean {
        return menuOptions?.isSaveToDeviceOptionVisible(imageNode) ?: false
    }

    suspend fun isForwardOptionVisible(imageNode: ImageNode): Boolean {
        return menuOptions?.isForwardOptionVisible(imageNode) ?: false
    }

    suspend fun isSendToOptionVisible(imageNode: ImageNode): Boolean {
        return menuOptions?.isSendToOptionVisible(imageNode) ?: false
    }

    suspend fun isMoreOptionVisible(imageNode: ImageNode): Boolean {
        return menuOptions?.isMoreOptionVisible(imageNode) ?: false
    }

    suspend fun monitorImageResult(imageNode: ImageNode): Flow<ImageResult> {
        val typedNode = addImageTypeUseCase(imageNode)
        return getImageUseCase(
            node = typedNode,
            fullSize = true,
            highPriority = true,
            resetDownloads = {},
        ).catch { Timber.e("Failed to load image: $it") }
    }

    fun switchFullScreenMode() {
        val inFullScreenMode = _state.value.inFullScreenMode
        _state.update {
            it.copy(
                inFullScreenMode = !inFullScreenMode,
            )
        }
    }

    fun setCurrentImageNodeIndex(currentImageNodeIndex: Int) {
        _state.update {
            it.copy(
                currentImageNodeIndex = currentImageNodeIndex,
            )
        }
    }

    fun setCurrentImageNode(currentImageNode: ImageNode) {
        _state.update {
            it.copy(
                currentImageNode = currentImageNode,
            )
        }
    }

    fun setTransferMessage(message: String) {
        _state.update { it.copy(transferMessage = message) }
    }

    fun clearTransferMessage() = _state.update { it.copy(transferMessage = "") }

    fun setResultMessage(message: String) =
        _state.update { it.copy(resultMessage = message) }

    fun clearResultMessage() = _state.update { it.copy(resultMessage = "") }

    private fun setCopyMoveException(throwable: Throwable) {
        _state.update {
            it.copy(
                copyMoveException = throwable
            )
        }
    }

    /**
     * Check if transfers are paused.
     */
    fun executeTransfer(transferMessage: String, transferAction: () -> Unit) {
        viewModelScope.launch {
            if (areTransfersPausedUseCase()) {
                setTransferMessage(transferMessage)
            } else {
                transferAction()
            }
        }
    }

    fun favouriteNode(imageNode: ImageNode) {
        viewModelScope.launch {
            if (imageNode.isFavourite) {
                removeFavouritesUseCase(listOf(imageNode.id))
            } else {
                addFavouritesUseCase(listOf(imageNode.id))
            }
        }
    }

    fun setNodeAvailableOffline(
        activity: WeakReference<Activity>,
        setOffline: Boolean,
        imageNode: ImageNode,
    ) {
        viewModelScope.launch {
            if (setOffline) {
                setNodeAvailableOffline(
                    nodeId = imageNode.id,
                    availableOffline = true,
                    activity = activity
                )
            } else {
                removeOfflineNodeUseCase(imageNode.id)
            }
        }
    }

    fun moveNode(context: Context, moveHandle: Long, toHandle: Long) {
        viewModelScope.launch {
            checkForNameCollision(
                context = context,
                nodeHandle = moveHandle,
                newParentHandle = toHandle,
                type = NameCollisionType.MOVE,
                completeAction = { handleMoveNodeNameCollision(context, moveHandle, toHandle) }
            )
        }
    }

    private suspend fun handleMoveNodeNameCollision(
        context: Context,
        moveHandle: Long,
        toHandle: Long,
    ) {
        runCatching {
            moveNodeUseCase(
                nodeToMove = NodeId(moveHandle),
                newNodeParent = NodeId(toHandle),
            )
        }.onSuccess {
            setResultMessage(context.getString(R.string.context_correctly_moved))
        }.onFailure { throwable ->
            Timber.d("Move node failure $throwable")
            setCopyMoveException(throwable)
        }
    }

    fun copyNode(context: Context, copyHandle: Long, toHandle: Long) {
        viewModelScope.launch {
            checkForNameCollision(
                context = context,
                nodeHandle = copyHandle,
                newParentHandle = toHandle,
                type = NameCollisionType.COPY,
                completeAction = { handleCopyNodeNameCollision(copyHandle, toHandle, context) }
            )
        }
    }

    private suspend fun handleCopyNodeNameCollision(
        copyHandle: Long,
        toHandle: Long,
        context: Context,
    ) {
        runCatching {
            copyNodeUseCase(
                nodeToCopy = NodeId(copyHandle),
                newNodeParent = NodeId(toHandle),
                newNodeName = null,
            )
        }.onSuccess {
            setResultMessage(context.getString(R.string.context_correctly_copied))
        }.onFailure { throwable ->
            Timber.e("Error not copied $throwable")
            setCopyMoveException(throwable)
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
        context: Context,
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
        }.onSuccess { nameCollision ->
            _state.update {
                it.copy(nameCollision = nameCollision)
            }
        }.onFailure {
            when (it) {
                is MegaNodeException.ChildDoesNotExistsException -> completeAction.invoke()

                is MegaNodeException.ParentDoesNotExistException -> {
                    setResultMessage(context.getString(R.string.general_error))
                }

                else -> Timber.e(it)
            }
        }
    }

    fun setCurrentImageNodeAvailableOffline(imageNode: ImageNode) {
        viewModelScope.launch {
            val isAvailableOffline = if (imageNodesOffline[imageNode.id] != null) {
                imageNodesOffline[imageNode.id] ?: false
            } else {
                isAvailableOffline(imageNode)
            }
            imageNodesOffline[imageNode.id] = isAvailableOffline
            _state.update {
                it.copy(
                    isCurrentImageNodeAvailableOffline = isAvailableOffline
                )
            }
        }
    }

    suspend fun isAvailableOffline(imageNode: ImageNode): Boolean {
        val typedNode = addImageTypeUseCase(imageNode)
        return isAvailableOfflineUseCase(typedNode)
    }

    /**
     * Disable export nodes
     *
     */
    fun disableExport(imageNode: ImageNode) {
        viewModelScope.launch {
            runCatching {
                disableExportNodesUseCase(listOf(imageNode.id))
            }.onFailure {
                Timber.e(it)
            }.onSuccess { result ->
                val message = removePublicLinkResultMapper(result)
                _state.update { state ->
                    state.copy(
                        resultMessage = message,
                    )
                }
            }
        }
    }

    suspend fun getHighestResolutionImagePath(imageResult: ImageResult?): String? {
        return imageResult?.run {
            checkUri(fullSizeUri) ?: checkUri(previewUri) ?: checkUri(thumbnailUri)
        }
    }

    suspend fun getLowestResolutionImagePath(imageResult: ImageResult?): String? {
        return imageResult?.run {
            checkUri(thumbnailUri) ?: checkUri(previewUri) ?: checkUri(fullSizeUri)
        }
    }

    private suspend fun checkUri(uriPath: String?): String? = withContext(ioDispatcher) {
        try {
            if (File(URI.create(uriPath).path).exists()) {
                uriPath
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val IMAGE_NODE_FETCHER_SOURCE = "image_node_fetcher_source"
        const val IMAGE_PREVIEW_MENU_OPTIONS = "image_preview_menu_options"
        const val FETCHER_PARAMS = "fetcher_params"
        const val PARAMS_CURRENT_IMAGE_NODE_ID_VALUE = "currentImageNodeIdValue"
    }
}
