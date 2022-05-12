package mega.privacy.android.app.imageviewer

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.facebook.drawee.backends.pipeline.Fresco
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.data.ImageResult
import mega.privacy.android.app.imageviewer.usecase.GetImageHandlesUseCase
import mega.privacy.android.app.imageviewer.usecase.GetImageUseCase
import mega.privacy.android.app.usecase.*
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase.Result
import mega.privacy.android.app.usecase.chat.DeleteChatMessageUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.app.utils.MegaNodeUtil.isValidForImageViewer
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Main ViewModel to handle all logic related to the ImageViewer.
 * This is shared between ImageViewerActivity behaving as the main container and
 * each individual ImageViewerPageFragment representing a single image within the ViewPager.
 *
 * @property getImageUseCase            Needed to retrieve each individual image based on a node
 * @property getImageHandlesUseCase     Needed to retrieve node handles given sent params
 * @property getGlobalChangesUseCase    Use case required to get node changes
 * @property getNodeUseCase             Needed to retrieve each individual node based on a node handle,
 *                                      as well as each individual node action required by the menu
 * @property exportNodeUseCase          Needed to export image node on demand
 * @property cancelTransferUseCase      Needed to cancel current full image transfer if needed
 * @property loggedInUseCase            UseCase required to check when the user is already logged in
 * @property deleteChatMessageUseCase   UseCase required to delete current chat node message
 */
@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    private val getImageUseCase: GetImageUseCase,
    private val getImageHandlesUseCase: GetImageHandlesUseCase,
    private val getGlobalChangesUseCase: GetGlobalChangesUseCase,
    private val getNodeUseCase: GetNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val cancelTransferUseCase: CancelTransferUseCase,
    private val loggedInUseCase: LoggedInUseCase,
    private val deleteChatMessageUseCase: DeleteChatMessageUseCase
) : BaseRxViewModel() {

    private val images = MutableLiveData<List<ImageItem>?>()
    private val currentPosition = MutableLiveData<Int>()
    private val showToolbar = MutableLiveData<Boolean>()
    private val snackbarMessage = SingleLiveEvent<String>()
    private var isUserLoggedIn = false

    init {
        checkIfUserIsLoggedIn()
        subscribeToNodeChanges()
    }

    override fun onCleared() {
        Fresco.getImagePipeline()?.clearMemoryCaches()
        super.onCleared()
    }

    fun onImagesIds(): LiveData<List<Long>?> =
        images.map { items -> items?.map(ImageItem::id) }

    fun onImage(itemId: Long): LiveData<ImageItem?> =
        images.map { items -> items?.firstOrNull { it.id == itemId } }

    fun onCurrentPosition(): LiveData<Pair<Int, Int>> =
        currentPosition.map { position -> Pair(position, images.value?.size ?: 0) }

    fun onCurrentImageItem(): LiveData<ImageItem?> =
        currentPosition.map { images.value?.getOrNull(it) }

    fun getCurrentImageItem(): ImageItem? =
        currentPosition.value?.let { images.value?.getOrNull(it) }

    fun getImageItem(itemId: Long): ImageItem? =
        images.value?.find { it.id == itemId }

    fun onSnackbarMessage(): LiveData<String> = snackbarMessage

    fun onShowToolbar(): LiveData<Boolean> = showToolbar

    fun isToolbarShown(): Boolean = showToolbar.value ?: false

    fun retrieveSingleImage(nodeHandle: Long, isOffline: Boolean = false) {
        getImageHandlesUseCase.get(nodeHandles = longArrayOf(nodeHandle), isOffline = isOffline)
            .subscribeAndUpdateImages()
    }

    fun retrieveSingleImage(nodeFileLink: String) {
        getImageHandlesUseCase.get(nodeFileLinks = listOf(nodeFileLink))
            .subscribeAndUpdateImages()
    }

    fun retrieveFileImage(imageUri: Uri, showNearbyFiles: Boolean? = false, itemId: Long? = null) {
        getImageHandlesUseCase.get(imageFileUri = imageUri, showNearbyFiles = showNearbyFiles)
            .subscribeAndUpdateImages(itemId)
    }

    fun retrieveImagesFromParent(
        parentNodeHandle: Long,
        childOrder: Int? = null,
        currentNodeHandle: Long? = null
    ) {
        getImageHandlesUseCase.get(parentNodeHandle = parentNodeHandle, sortOrder = childOrder)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveImages(
        nodeHandles: LongArray,
        currentNodeHandle: Long? = null,
        isOffline: Boolean = false
    ) {
        getImageHandlesUseCase.get(nodeHandles = nodeHandles, isOffline = isOffline)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveChatImages(
        chatRoomId: Long,
        messageIds: LongArray,
        currentNodeHandle: Long? = null
    ) {
        getImageHandlesUseCase.get(chatRoomId = chatRoomId, chatMessageIds = messageIds)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun isUserLoggedIn(): Boolean = isUserLoggedIn

    /**
     * Main method to request a MegaNodeItem given a previously loaded Node handle.
     * This will update the current Node on the main "images" list if it's newer.
     * You must be observing the requested Image to get the updated result.
     *
     * @param itemId    Item to be loaded.
     */
    fun loadSingleNode(itemId: Long) {
        val imageItem = images.value?.find { it.id == itemId } ?: run {
            logWarning("Null item id: $itemId")
            return
        }

        val subscription = when (imageItem) {
            is ImageItem.PublicNode ->
                getNodeUseCase.getNodeItem(imageItem.nodePublicLink)
            is ImageItem.ChatNode ->
                getNodeUseCase.getNodeItem(imageItem.chatRoomId, imageItem.chatMessageId)
            is ImageItem.OfflineNode ->
                getNodeUseCase.getOfflineNodeItem(imageItem.handle)
            is ImageItem.Node ->
                getNodeUseCase.getNodeItem(imageItem.handle)
            is ImageItem.File -> {
                // do nothing
                return
            }
        }

        subscription
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retry(1)
            .subscribeBy(
                onSuccess = { nodeItem ->
                    updateItemIfNeeded(itemId, nodeItem = nodeItem)
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    if (itemId == getCurrentImageItem()?.id && error is MegaException) {
                        snackbarMessage.value = error.getTranslatedErrorString()
                    }
                }
            )
            .addTo(composite)
    }

    /**
     * Main method to request an ImageResult given a previously loaded Node handle.
     * This will update the current Image on the main "images" list if it's newer.
     * You must be observing the requested Image to get the updated result.
     *
     * @param itemId        Item to be loaded.
     * @param fullSize      Flag to request full size image despite data/size requirements.
     */
    fun loadSingleImage(itemId: Long, fullSize: Boolean) {
        val imageItem = images.value?.find { it.id == itemId } ?: run {
            logWarning("Null item id: $itemId")
            return
        }

        if (imageItem.imageResult?.isFullyLoaded == true
            && imageItem.imageResult?.fullSizeUri != null
            && imageItem.imageResult?.previewUri != null
        ) return // Already downloaded

        val highPriority = itemId == getCurrentImageItem()?.id
        val subscription = when (imageItem) {
            is ImageItem.PublicNode ->
                getImageUseCase.get(imageItem.nodePublicLink, fullSize, highPriority)
            is ImageItem.ChatNode ->
                getImageUseCase.get(imageItem.chatRoomId, imageItem.chatMessageId, fullSize, highPriority)
            is ImageItem.OfflineNode ->
                getImageUseCase.getOfflineNode(imageItem.handle, highPriority).toFlowable()
            is ImageItem.Node ->
                getImageUseCase.get(imageItem.handle, fullSize, highPriority)
            is ImageItem.File ->
                getImageUseCase.getImageUri(imageItem.fileUri, highPriority).toFlowable()
        }

        subscription
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retry(2) { error ->
                error is ResourceAlreadyExistsMegaException || error is HttpMegaException
            }
            .subscribeBy(
                onNext = { imageResult ->
                    updateItemIfNeeded(itemId, imageResult = imageResult)
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    if (itemId == getCurrentImageItem()?.id
                        && error is MegaException && error !is ResourceAlreadyExistsMegaException
                    ) {
                        snackbarMessage.value = error.getTranslatedErrorString()
                    }
                }
            )
            .addTo(composite)
    }

    /**
     * Update a specific ImageItem from the Images list with the provided
     * MegaNodeItem or ImageResult
     *
     * @param itemId        Item to be updated
     * @param nodeItem      MegaNodeItem to be updated with
     * @param imageResult   ImageResult to be updated with
     */
    private fun updateItemIfNeeded(
        itemId: Long,
        nodeItem: MegaNodeItem? = null,
        imageResult: ImageResult? = null
    ) {
        if (nodeItem == null && imageResult == null) return

        val items = images.value?.toMutableList()
        if (!items.isNullOrEmpty()) {
            val index = items.indexOfFirst { it.id == itemId }
            if (index != INVALID_POSITION) {
                val currentItem = items[index]
                if (nodeItem != null) {
                    items[index] = currentItem.copy(
                        nodeItem = nodeItem
                    )
                }
                if (imageResult != null && imageResult != currentItem.imageResult) {
                    items[index] = currentItem.copy(
                        imageResult = imageResult
                    )
                }

                images.value = items.toList()
                if (index == currentPosition.value) {
                    updateCurrentPosition(index, true)
                }
            } else {
                logWarning("Node $itemId not found")
            }
        } else {
            logWarning("Images are null or empty")
            images.value = null
        }
    }

    /**
     * Subscribe to latest node changes to update existing ones.
     */
    @Suppress("SENSELESS_COMPARISON")
    private fun subscribeToNodeChanges() {
        getGlobalChangesUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { change -> change is Result.OnNodesUpdate }
            .subscribeBy(
                onNext = { change ->
                    val items = images.value?.toMutableList() ?: run {
                        logWarning("Images are null or empty")
                        return@subscribeBy
                    }

                    val dirtyNodeHandles = mutableListOf<Long>()
                    (change as Result.OnNodesUpdate).nodes?.forEach { changedNode ->
                        val currentIndex = items.indexOfFirst { changedNode.handle == it.getNodeHandle() }
                        when {
                            currentIndex == INVALID_POSITION -> {
                                return@subscribeBy // Not found
                            }
                            changedNode.hasChanged(MegaNode.CHANGE_TYPE_NEW) -> {
                                val hasSameParent = (changedNode.parentHandle != null
                                        && changedNode.parentHandle == items.firstOrNull()?.nodeItem?.node?.parentHandle)
                                if (hasSameParent && changedNode.isValidForImageViewer()) {
                                    items.add(
                                        ImageItem.Node(
                                            id = changedNode.handle,
                                            handle = changedNode.handle,
                                            name = changedNode.name,
                                            infoText = changedNode.getInfoText()
                                        )
                                    )
                                    dirtyNodeHandles.add(changedNode.handle)
                                }
                            }
                            changedNode.hasChanged(MegaNode.CHANGE_TYPE_PARENT) -> {
                                if (currentIndex != INVALID_POSITION) {
                                    val hasSameParent = (changedNode.parentHandle != null
                                            && changedNode.parentHandle == items.firstOrNull()?.nodeItem?.node?.parentHandle)
                                    if (!hasSameParent) {
                                        items.removeAt(currentIndex)
                                    }
                                }
                            }
                            changedNode.hasChanged(MegaNode.CHANGE_TYPE_REMOVED) -> {
                                if (currentIndex != INVALID_POSITION) {
                                    items.removeAt(currentIndex)
                                }
                            }
                            else -> {
                                dirtyNodeHandles.add(changedNode.handle)
                            }
                        }
                    }

                    if (dirtyNodeHandles.isNotEmpty() || items.size != images.value?.size) {
                        images.value = items.toList()
                        dirtyNodeHandles.forEach(::loadSingleNode)
                        calculateNewPosition(items)
                    }
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun markNodeAsFavorite(nodeHandle: Long, isFavorite: Boolean) {
        getNodeUseCase.markAsFavorite(nodeHandle, isFavorite)
            .subscribeAndComplete()
    }

    fun switchNodeOfflineAvailability(
        nodeItem: MegaNodeItem,
        activity: Activity
    ) {
        getNodeUseCase.setNodeAvailableOffline(
            node = nodeItem.node,
            setOffline = !nodeItem.isAvailableOffline,
            isFromIncomingShares = nodeItem.isFromIncoming,
            isFromInbox = nodeItem.isFromInbox,
            activity = activity
        ).subscribeAndComplete {
            loadSingleNode(nodeItem.handle)
        }
    }

    /**
     * Remove ImageItem from main list given an Index.
     *
     * @param index    Node Handle to be removed from the list
     */
    private fun removeImageItemAt(index: Int) {
        if (index != INVALID_POSITION) {
            val items = images.value!!.toMutableList().apply {
                removeAt(index)
            }
            images.value = items.toList()
            calculateNewPosition(items)
        }
    }

    /**
     * Calculate new ViewPager position based on a new list of items
     *
     * @param newItems  New ImageItems to calculate new position from
     */
    private fun calculateNewPosition(newItems: List<ImageItem>) {
        val items = images.value?.toMutableList()
        val newPosition =
            if (items.isNullOrEmpty()) {
                0
            } else {
                val currentPositionNewIndex = newItems.indexOfFirst { it.id == getCurrentImageItem()?.id }
                val currentItemPosition = currentPosition.value ?: 0
                when {
                    currentPositionNewIndex != INVALID_POSITION ->
                        currentPositionNewIndex
                    currentItemPosition >= items.size ->
                        items.size - 1
                    currentItemPosition == 0 ->
                        currentItemPosition + 1
                    else ->
                        currentItemPosition
                }
            }

        updateCurrentPosition(newPosition, true)
    }

    fun removeOfflineNode(nodeHandle: Long, activity: Activity) {
        getNodeUseCase.removeOfflineNode(nodeHandle, activity)
            .subscribeAndComplete {
                val index = images.value?.indexOfFirst { nodeHandle == it.getNodeHandle() } ?: INVALID_POSITION
                removeImageItemAt(index)
            }
    }

    fun removeLink(nodeHandle: Long) {
        exportNodeUseCase.disableExport(nodeHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getQuantityString(R.plurals.context_link_removal_success, 1)
            }
    }

    fun removeChatMessage(nodeHandle: Long) {
        val imageItem = images.value?.firstOrNull { nodeHandle == it.getNodeHandle() } as? ImageItem.ChatNode ?: return
        deleteChatMessageUseCase.delete(imageItem.chatRoomId, imageItem.chatMessageId)
            .subscribeAndComplete {
                val index = images.value?.indexOfFirst { it.id == imageItem.id } ?: INVALID_POSITION
                removeImageItemAt(index)

                snackbarMessage.value = getString(R.string.context_correctly_removed)
            }
    }

    fun exportNode(node: MegaNode): LiveData<String?> {
        val result = MutableLiveData<String?>()
        exportNodeUseCase.export(node)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { link ->
                    result.value = link
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    result.value = null
                }
            )
            .addTo(composite)
        return result
    }

    fun copyNode(nodeHandle: Long, newParentHandle: Long) {
        moveNodeUseCase.copyNode(
            node = getExistingNode(nodeHandle),
            nodeHandle = nodeHandle,
            toParentHandle = newParentHandle
        ).subscribeAndComplete {
            snackbarMessage.value = getString(R.string.context_correctly_copied)
        }
    }

    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        moveNodeUseCase.move(nodeHandle, newParentHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getString(R.string.context_correctly_moved)
            }
    }

    fun moveNodeToRubbishBin(nodeHandle: Long) {
        moveNodeUseCase.moveToRubbishBin(nodeHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getString(R.string.context_correctly_moved_to_rubbish)
            }
    }

    fun removeNode(nodeHandle: Long) {
        moveNodeUseCase.remove(nodeHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getString(R.string.context_correctly_removed)
            }
    }

    fun stopImageLoading(itemId: Long) {
        images.value?.find { itemId == it.id }?.imageResult?.let { imageResult ->
            imageResult.transferTag?.let { tag ->
                cancelTransferUseCase.cancel(tag)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onError = { error ->
                            logError(error.stackTraceToString())
                        }
                    )
            }
            imageResult.fullSizeUri?.let { fullSizeImageUri ->
                Fresco.getImagePipeline()?.evictFromMemoryCache(fullSizeImageUri)
            }
        }
    }

    fun onLowMemory() {
        getCurrentImageItem()?.imageResult?.fullSizeUri?.lastPathSegment?.let { fileName ->
            Fresco.getImagePipeline()?.bitmapMemoryCache?.removeAll {
                !it.uriString.contains(fileName)
            }
        }
    }

    fun updateCurrentPosition(position: Int, forceUpdate: Boolean) {
        if (forceUpdate || position != currentPosition.value) {
            currentPosition.value = position
        }
    }

    fun switchToolbar(show: Boolean? = null) {
        showToolbar.value = show ?: showToolbar.value?.not() ?: true
    }

    private fun getExistingNode(nodeHandle: Long): MegaNode? =
        images.value?.find { it.getNodeHandle() == nodeHandle }?.nodeItem?.node

    /**
     * Check if current user is logged in
     */
    private fun checkIfUserIsLoggedIn() {
        loggedInUseCase.isUserLoggedIn()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { isLoggedIn ->
                    isUserLoggedIn = isLoggedIn
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    /**
     * Reused Extension Function to subscribe to a Single<List<ImageItem>>.
     *
     * @param currentNodeHandle Node handle to be shown on first load.
     */
    private fun Single<List<ImageItem>>.subscribeAndUpdateImages(currentNodeHandle: Long? = null) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { items ->
                    images.value = items.toList()

                    val position = items.indexOfFirst { currentNodeHandle == it.getNodeHandle() || currentNodeHandle == it.id }
                    if (position != INVALID_POSITION) {
                        updateCurrentPosition(position, true)
                    } else {
                        updateCurrentPosition(0, true)
                    }
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    images.value = null
                }
            )
            .addTo(composite)
    }

    private fun Completable.subscribeAndComplete(completeAction: (() -> Unit)? = null) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    completeAction?.invoke()
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }
}
