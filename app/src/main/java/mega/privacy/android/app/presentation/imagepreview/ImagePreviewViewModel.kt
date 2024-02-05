package mega.privacy.android.app.presentation.imagepreview

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.offline.SetNodeAvailableOffline
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.menu.ImagePreviewMenu
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewState
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.favourites.AddFavouritesUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.CheckFileUriUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ResetTotalDownloadsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class ImagePreviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageNodeFetchers: Map<@JvmSuppressWildcards ImagePreviewFetcherSource, @JvmSuppressWildcards ImageNodeFetcher>,
    private val imagePreviewMenuMap: Map<@JvmSuppressWildcards ImagePreviewMenuSource, @JvmSuppressWildcards ImagePreviewMenu>,
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
    private val checkUri: CheckFileUriUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getPublicChildNodeFromIdUseCase: GetPublicChildNodeFromIdUseCase,
    private val getPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase,
    private val resetTotalDownloadsUseCase: ResetTotalDownloadsUseCase,
) : ViewModel() {
    private val imagePreviewFetcherSource: ImagePreviewFetcherSource
        get() = savedStateHandle[IMAGE_NODE_FETCHER_SOURCE] ?: ImagePreviewFetcherSource.TIMELINE

    private val params: Bundle
        get() = savedStateHandle[FETCHER_PARAMS] ?: Bundle()

    private val currentImageNodeIdValue: Long
        get() = savedStateHandle[PARAMS_CURRENT_IMAGE_NODE_ID_VALUE] ?: 0L

    private val imagePreviewMenuSource: ImagePreviewMenuSource
        get() = savedStateHandle[IMAGE_PREVIEW_MENU_OPTIONS] ?: ImagePreviewMenuSource.TIMELINE

    private val isFromFolderLink = savedStateHandle[IMAGE_PREVIEW_IS_FOREIGN] ?: false

    private val imageNodesOffline: MutableMap<NodeId, Boolean> = mutableMapOf()

    private val _state = MutableStateFlow(ImagePreviewState())

    val state: StateFlow<ImagePreviewState> = _state

    private val menu: ImagePreviewMenu?
        get() = imagePreviewMenuMap[imagePreviewMenuSource]

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
        val currentImageNodeIdValue = if (_state.value.isInitialized) {
            _state.value.currentImageNode?.id?.longValue ?: currentImageNodeIdValue
        } else {
            currentImageNodeIdValue
        }
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

    suspend fun isInfoMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isInfoMenuVisible(imageNode) ?: false
    }

    suspend fun isSlideshowMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isSlideshowMenuVisible(imageNode) ?: false && _state.value.imageNodes.size > 1
    }

    suspend fun isFavouriteMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isFavouriteMenuVisible(imageNode) ?: false
    }

    suspend fun isLabelMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isLabelMenuVisible(imageNode) ?: false
    }

    suspend fun isDisputeMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isDisputeMenuVisible(imageNode) ?: false
    }

    suspend fun isOpenWithMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isOpenWithMenuVisible(imageNode) ?: false
    }

    suspend fun isForwardMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isForwardMenuVisible(imageNode) ?: false
    }

    suspend fun isSaveToDeviceMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isSaveToDeviceMenuVisible(imageNode) ?: false
    }

    suspend fun isImportMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isImportMenuVisible(imageNode) ?: false
    }

    suspend fun isGetLinkMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isGetLinkMenuVisible(imageNode) ?: false
    }

    suspend fun isSendToChatMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isSendToChatMenuVisible(imageNode) ?: false
    }

    suspend fun isShareMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isShareMenuVisible(imageNode) ?: false
    }

    suspend fun isRenameMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isRenameMenuVisible(imageNode) ?: false
    }

    suspend fun isMoveMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isMoveMenuVisible(imageNode) ?: false
    }

    suspend fun isCopyMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isCopyMenuVisible(imageNode) ?: false
    }

    suspend fun isRestoreMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isRestoreMenuVisible(imageNode) ?: false
    }

    suspend fun isRemoveMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isRemoveMenuVisible(imageNode) ?: false
    }

    suspend fun isAvailableOfflineMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isAvailableOfflineMenuVisible(imageNode) ?: false
    }

    suspend fun isRemoveOfflineMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isRemoveOfflineMenuVisible(imageNode) ?: false
    }

    suspend fun isMoreMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isMoreMenuVisible(imageNode) ?: false
    }

    suspend fun isMoveToRubbishBinMenuVisible(imageNode: ImageNode): Boolean {
        return menu?.isMoveToRubbishBinMenuVisible(imageNode) ?: false
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
    fun executeTransfer(transferMessage: String, legacyTransferAction: () -> Unit) {
        viewModelScope.launch {
            if (areTransfersPausedUseCase()) {
                setTransferMessage(transferMessage)
            } else {
                if (getFeatureFlagValueUseCase(AppFeatures.DownloadWorker)) {
                    triggerDownloadEvent(
                        imageNode = _state.value.currentImageNode,
                    ) {
                        TransferTriggerEvent.StartDownloadNode(listOf(it))
                    }
                } else {
                    legacyTransferAction()
                }
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
                if (getFeatureFlagValueUseCase(AppFeatures.DownloadWorker)) {
                    triggerDownloadEvent(
                        imageNode,
                    ) {
                        TransferTriggerEvent.StartDownloadForOffline(it)
                    }
                } else {
                    setNodeAvailableOffline(
                        nodeId = imageNode.id,
                        availableOffline = true,
                        activity = activity
                    )
                }
            } else {
                removeOfflineNodeUseCase(imageNode.id)
            }
        }
    }

    private suspend fun triggerDownloadEvent(
        imageNode: ImageNode?,
        eventBuilder: (TypedNode) -> TransferTriggerEvent,
    ) {
        imageNode?.let { node ->
            //addImageTypeUseCase is not adding chat or link types, so we need to do it here for now
            // chat nodes are not using this screen for now
            when {
                isFromFolderLink -> getPublicChildNodeFromIdUseCase(node.id)
                imagePreviewMenuSource == ImagePreviewMenuSource.PUBLIC_FILE -> {
                    node.serializedData?.let {
                        getPublicNodeFromSerializedDataUseCase(it)
                    }
                }

                else -> addImageTypeUseCase(node)
            }
        }?.let { typedNode ->
            val event = eventBuilder(typedNode)
            _state.update {
                it.copy(
                    downloadEvent = triggered(event)
                )
            }
        } ?: run {
            Timber.e("Current Image node not found")
        }
    }

    /**
     * Consume download event
     */
    fun consumeDownloadEvent() {
        _state.update {
            it.copy(downloadEvent = consumed())
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

    fun importNode(context: Context, importHandle: Long, toHandle: Long) {
        viewModelScope.launch {
            checkForNameCollision(
                context = context,
                nodeHandle = importHandle,
                newParentHandle = toHandle,
                type = NameCollisionType.COPY,
                completeAction = {
                    handleImportNodeNameCollision(
                        importHandle,
                        toHandle,
                        context
                    )
                }
            )
        }
    }

    private suspend fun handleImportNodeNameCollision(
        importHandle: Long,
        toHandle: Long,
        context: Context,
    ) {
        runCatching {
            copyNodeUseCase(
                nodeToCopy = NodeId(importHandle),
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

    suspend fun getFallbackImagePath(imageResult: ImageResult?): String? {
        return imageResult?.run {
            checkUri(previewUri) ?: checkUri(thumbnailUri)
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

    /**
     * Move to rubbish
     */
    fun moveToRubbishBin(nodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                moveNodesToRubbishUseCase(listOf(nodeId.longValue))
            }.onSuccess { data ->
                setResultMessage(moveRequestMessageMapper(data))
            }.onFailure {
                Timber.e(it)
                setCopyMoveException(it)
            }
        }
    }

    fun saveToDevice(nodeSaver: NodeSaver, node: MegaNode, isForeign: Boolean) {
        viewModelScope.launch {
            resetTotalDownloadsUseCase()
            nodeSaver.saveNode(
                node,
                highPriority = false,
                isFolderLink = isForeign,
                fromMediaViewer = false,
                needSerialize = true,
            )
        }
    }

    companion object {
        const val IMAGE_NODE_FETCHER_SOURCE = "image_node_fetcher_source"
        const val IMAGE_PREVIEW_MENU_OPTIONS = "image_preview_menu_options"
        const val FETCHER_PARAMS = "fetcher_params"
        const val PARAMS_CURRENT_IMAGE_NODE_ID_VALUE = "currentImageNodeIdValue"
        const val IMAGE_PREVIEW_IS_FOREIGN = "image_preview_is_foreign"
    }
}
