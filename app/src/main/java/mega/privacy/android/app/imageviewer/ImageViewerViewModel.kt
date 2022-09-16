package mega.privacy.android.app.imageviewer

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.facebook.drawee.backends.pipeline.Fresco
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.imageviewer.data.ImageAdapterItem
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.data.ImageResult
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState.NEXT
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState.STARTED
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState.STOPPED
import mega.privacy.android.app.imageviewer.usecase.GetImageHandlesUseCase
import mega.privacy.android.app.imageviewer.usecase.GetImageUseCase
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.CancelTransferUseCase
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase.Result
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.LoggedInUseCase
import mega.privacy.android.app.usecase.MoveNodeUseCase
import mega.privacy.android.app.usecase.RemoveNodeUseCase
import mega.privacy.android.app.usecase.chat.DeleteChatMessageUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.usecase.exception.HttpMegaException
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.ResourceAlreadyExistsMegaException
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.app.utils.MegaNodeUtil.isValidForImageViewer
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.domain.usecase.AreTransfersPaused
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
 * @property copyNodeUseCase            Needed to copy image node on demand
 * @property moveNodeUseCase            Needed to move image node on demand
 * @property removeNodeUseCase          Needed to remove image node on demand
 * @property exportNodeUseCase          Needed to export image node on demand
 * @property cancelTransferUseCase      Needed to cancel current full image transfer if needed
 * @property loggedInUseCase            UseCase required to check when the user is already logged in
 * @property deleteChatMessageUseCase   UseCase required to delete current chat node message
 * @property areTransfersPaused         UseCase required to check if transfers are paused
 * @property copyNodeUseCase            UseCase required to copy nodes
 * @property moveNodeUseCase            UseCase required to move nodes
 * @property removeNodeUseCase          UseCase required to remove nodes
 * @property checkNameCollisionUseCase  UseCase required to check name collisions
 */
@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    private val getImageUseCase: GetImageUseCase,
    private val getImageHandlesUseCase: GetImageHandlesUseCase,
    private val getGlobalChangesUseCase: GetGlobalChangesUseCase,
    private val getNodeUseCase: GetNodeUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val cancelTransferUseCase: CancelTransferUseCase,
    private val loggedInUseCase: LoggedInUseCase,
    private val deleteChatMessageUseCase: DeleteChatMessageUseCase,
    private val areTransfersPaused: AreTransfersPaused,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val removeNodeUseCase: RemoveNodeUseCase,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
) : BaseRxViewModel() {

    companion object {
        private const val SLIDESHOW_DELAY = 4L
    }

    private val images = MutableLiveData<List<ImageItem>?>()
    private val currentImageId = MutableLiveData<Long?>()
    private val showToolbar = MutableLiveData<Boolean>(true)
    private val snackBarMessage = SingleLiveEvent<String>()
    private val actionBarMessage = SingleLiveEvent<Int>()
    private val copyMoveException = SingleLiveEvent<Throwable>()
    private val collision = SingleLiveEvent<NameCollision>()
    private val slideShowState = MutableLiveData(STOPPED)
    private val timerComposite = CompositeDisposable()

    private var isUserLoggedIn = false

    init {
        checkIfUserIsLoggedIn()
        subscribeToNodeChanges()
    }

    override fun onCleared() {
        timerComposite.dispose()
        Fresco.getImagePipeline()?.clearMemoryCaches()
        super.onCleared()
    }

    /**
     * Get an updated LiveData of adapter images filtered
     *
     * @param filterVideos  Flag to filter videos and only get images
     * @return              LiveData
     */
    fun onAdapterImages(filterVideos: Boolean): LiveData<List<ImageAdapterItem>?> =
        images.map { items ->
            if (filterVideos) {
                items?.filter { it.imageResult?.isVideo != true }
                    ?.map { ImageAdapterItem(it.id, it.hashCode()) }
            } else {
                items?.map { ImageAdapterItem(it.id, it.hashCode()) }
            }
        }

    /**
     * Get an updated LiveData of a specific Image
     *
     * @param itemId    Item id to find the specific Image
     * @return          LiveData
     */
    fun onImage(itemId: Long?): LiveData<ImageItem?> =
        images.map { items -> items?.firstOrNull { it.id == itemId } }

    /**
     * Get the amount of images
     *
     * @param filterVideos  Flag to filter videos and only count images
     * @return              Number of image items
     */
    fun getImagesSize(filterVideos: Boolean): Int =
        if (filterVideos) {
            images.value?.count { it.imageResult?.isVideo != true } ?: 0
        } else {
            images.value?.size ?: 0
        }

    /**
     * Get current position
     *
     * @param filterVideos  Flag to filter videos and get images position only
     * @return              Current position
     */
    fun getCurrentPosition(filterVideos: Boolean): Int =
        if (filterVideos) {
            images.value?.filter { it.imageResult?.isVideo != true }
                ?.indexOfFirst { it.id == currentImageId.value } ?: 0
        } else {
            images.value?.indexOfFirst { it.id == currentImageId.value } ?: 0
        }

    fun getCurrentImageItem(): ImageItem? =
        currentImageId.value?.let { imageId -> images.value?.find { it.id == imageId } }

    fun onCurrentImageItem(): LiveData<ImageItem?> =
        currentImageId.switchMap(::onImage)

    fun getImageItem(itemId: Long): ImageItem? =
        images.value?.find { it.id == itemId }

    fun onSnackBarMessage(): SingleLiveEvent<String> = snackBarMessage

    fun onActionBarMessage(): SingleLiveEvent<Int> = actionBarMessage

    fun onCopyMoveException(): LiveData<Throwable> = copyMoveException

    fun onCollision(): LiveData<NameCollision> = collision

    fun onSlideshowState(): LiveData<ImageSlideshowState> = slideShowState

    fun onShowToolbar(): LiveData<Boolean> = showToolbar

    fun isToolbarShown(): Boolean = showToolbar.value ?: false

    fun showToolbar(show: Boolean) {
        showToolbar.value = show
    }

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
        currentNodeHandle: Long? = null,
    ) {
        getImageHandlesUseCase.get(parentNodeHandle = parentNodeHandle, sortOrder = childOrder)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveImagesFromTimeline(
        currentNodeHandle: Long? = null,
    ) {
        getImageHandlesUseCase.get(isTimeline = true)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveImages(
        nodeHandles: LongArray,
        currentNodeHandle: Long? = null,
        isOffline: Boolean = false,
    ) {
        getImageHandlesUseCase.get(nodeHandles = nodeHandles, isOffline = isOffline)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveChatImages(
        chatRoomId: Long,
        messageIds: LongArray,
        currentNodeHandle: Long? = null,
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
            Timber.w("Null item id: $itemId")
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
                    Timber.e(error)
                    if (itemId == getCurrentImageItem()?.id && error is MegaException) {
                        snackBarMessage.value = error.getTranslatedErrorString()
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
            Timber.w("Null item id: $itemId")
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
                getImageUseCase.get(
                    imageItem.chatRoomId,
                    imageItem.chatMessageId,
                    fullSize,
                    highPriority
                )
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
                    Timber.e(error)
                    if (itemId == getCurrentImageItem()?.id
                        && error is MegaException && error !is ResourceAlreadyExistsMegaException
                    ) {
                        snackBarMessage.value = error.getTranslatedErrorString()
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
        imageResult: ImageResult? = null,
    ) {
        if (nodeItem == null && imageResult == null) return

        val items = images.value?.toMutableList()
        if (!items.isNullOrEmpty()) {
            val index = items.indexOfFirst { it.id == itemId }
            if (index != INVALID_POSITION) {
                val currentItem = items[index]
                if (nodeItem != null && nodeItem != currentItem.nodeItem) {
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
            } else {
                Timber.w("Node $itemId not found")
            }
        } else {
            Timber.w("Images are null or empty")
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
                        Timber.w("Images are null or empty")
                        return@subscribeBy
                    }

                    val dirtyNodeHandles = mutableListOf<Long>()
                    (change as Result.OnNodesUpdate).nodes?.forEach { changedNode ->
                        val currentIndex =
                            items.indexOfFirst { changedNode.handle == it.getNodeHandle() }
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
                onError = Timber::e
            )
            .addTo(composite)
    }

    fun markNodeAsFavorite(nodeHandle: Long, isFavorite: Boolean) {
        getNodeUseCase.markAsFavorite(nodeHandle, isFavorite)
            .subscribeAndComplete()
    }

    fun switchNodeOfflineAvailability(
        nodeItem: MegaNodeItem,
        activity: Activity,
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
        val existingItems = images.value?.toMutableList()
        val existingImageId = currentImageId.value
        if (existingItems.isNullOrEmpty() || existingImageId == null) {
            currentImageId.value = null
        } else if (existingItems.size == newItems.size) {
            return // Nothing to update
        } else {
            val currentItemPosition = images.value?.indexOfFirst { it.id == existingImageId }
            val newCurrentItemPosition = newItems.indexOfFirst { it.id == existingImageId }
            when {
                currentItemPosition == newCurrentItemPosition ->
                    return // Nothing to update
                newCurrentItemPosition != INVALID_POSITION ->
                    currentImageId.notifyObserver()
                newCurrentItemPosition >= existingItems.size ->
                    currentImageId.value = newItems.last().id
                currentItemPosition == 0 ->
                    currentImageId.value = newItems.first().id
            }
        }
    }

    fun showTransfersAction() {
        actionBarMessage.value = R.string.resume_paused_transfers_text
    }

    fun removeOfflineNode(nodeHandle: Long, activity: Activity) {
        getNodeUseCase.removeOfflineNode(nodeHandle, activity)
            .subscribeAndComplete {
                val index = images.value?.indexOfFirst { nodeHandle == it.getNodeHandle() }
                    ?: INVALID_POSITION
                removeImageItemAt(index)
            }
    }

    fun removeLink(nodeHandle: Long) {
        exportNodeUseCase.disableExport(nodeHandle)
            .subscribeAndComplete {
                snackBarMessage.value = getQuantityString(R.plurals.context_link_removal_success, 1)
            }
    }

    fun removeChatMessage(nodeHandle: Long) {
        val imageItem =
            images.value?.firstOrNull { nodeHandle == it.getNodeHandle() } as? ImageItem.ChatNode
                ?: return
        deleteChatMessageUseCase.delete(imageItem.chatRoomId, imageItem.chatMessageId)
            .subscribeAndComplete {
                val index = images.value?.indexOfFirst { it.id == imageItem.id } ?: INVALID_POSITION
                removeImageItemAt(index)

                snackBarMessage.value = getString(R.string.context_correctly_removed)
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
                    Timber.e(error)
                    result.value = null
                }
            )
            .addTo(composite)
        return result
    }

    /**
     * Copies a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun copyNode(nodeHandle: Long, newParentHandle: Long) {
        val node = getExistingNode(nodeHandle) ?: return

        checkNameCollision(
            node = node,
            newParentHandle = newParentHandle,
            type = NameCollisionType.COPY
        ) {
            copyNodeUseCase.copy(node = node, parentHandle = newParentHandle)
                .subscribeAndComplete(
                    completeAction = {
                        snackBarMessage.value = getString(R.string.context_correctly_copied)
                    }, errorAction = { error -> copyMoveException.value = error })
        }
    }

    /**
     * Moves a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to move.
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        val node = getExistingNode(nodeHandle) ?: return

        checkNameCollision(
            node = node,
            newParentHandle = newParentHandle,
            type = NameCollisionType.MOVE
        ) {
            moveNodeUseCase.move(node = node, parentHandle = newParentHandle)
                .subscribeAndComplete(
                    completeAction = {
                        snackBarMessage.value = getString(R.string.context_correctly_moved)
                    }, errorAction = { error -> copyMoveException.value = error }
                )
        }
    }

    /**
     * Checks if there is a name collision before proceeding with the action.
     *
     * @param node              Node to check the name collision.
     * @param newParentHandle   Handle of the parent folder in which the action will be performed.
     * @param type              [NameCollisionType]
     * @param completeAction    Action to complete after checking the name collision.
     */
    private fun checkNameCollision(
        node: MegaNode,
        newParentHandle: Long,
        type: NameCollisionType,
        completeAction: (() -> Unit),
    ) {
        checkNameCollisionUseCase.check(
            node = node,
            parentHandle = newParentHandle,
            type = type
        ).observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { collisionResult -> collision.value = collisionResult },
                onError = { error ->
                    when (error) {
                        is MegaNodeException.ChildDoesNotExistsException -> completeAction.invoke()
                        else -> Timber.e(error)
                    }
                }
            )
            .addTo(composite)
    }

    fun moveNodeToRubbishBin(nodeHandle: Long) {
        moveNodeUseCase.moveToRubbishBin(nodeHandle)
            .subscribeAndComplete(false) {
                snackBarMessage.value = getString(R.string.context_correctly_moved_to_rubbish)
            }
    }

    fun removeNode(nodeHandle: Long) {
        removeNodeUseCase.remove(nodeHandle)
            .subscribeAndComplete(false) {
                snackBarMessage.value = getString(R.string.context_correctly_removed)
            }
    }

    fun stopImageLoading(itemId: Long) {
        images.value?.find { itemId == it.id }?.imageResult?.let { imageResult ->
            imageResult.transferTag?.let { tag ->
                cancelTransferUseCase.cancel(tag)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onError = Timber::e
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

    /**
     * Update current image being shown based on the current position
     *
     * @param position      Position of the current image
     * @param filterVideos  Flag to filter videos and count images only
     */
    fun updateCurrentImage(position: Int, filterVideos: Boolean) {
        if (filterVideos) {
            currentImageId.value = images.value
                ?.filter { it.imageResult?.isVideo != true }
                ?.getOrNull(position)
                ?.id
        } else {
            currentImageId.value = images.value
                ?.getOrNull(position)
                ?.id
        }
    }

    private fun getExistingNode(nodeHandle: Long): MegaNode? =
        images.value?.find { it.getNodeHandle() == nodeHandle }?.nodeItem?.node

    /**
     * Check if transfers are paused.
     */
    fun executeTransfer(transferAction: () -> Unit) {
        viewModelScope.launch {
            if (areTransfersPaused()) {
                showTransfersAction()
            } else {
                transferAction()
            }
        }
    }

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
                onError = Timber::e
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
                    items.find {
                        currentNodeHandle == it.getNodeHandle() || currentNodeHandle == it.id
                    }?.let {
                        currentImageId.value = it.id
                    }
                    images.value = items.toList()
                },
                onError = { error ->
                    Timber.e(error)
                    images.value = null
                }
            )
            .addTo(composite)
    }

    private fun Completable.subscribeAndComplete(
        addToComposite: Boolean = false,
        completeAction: (() -> Unit)? = null,
        errorAction: ((Throwable) -> Unit)? = null,
    ) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    completeAction?.invoke()
                },
                onError = { error ->
                    errorAction?.invoke(error)
                    Timber.e(error)
                }
            ).also {
                if (addToComposite) it.addTo(composite)
            }
    }

    /**
     * Start slideshow
     */
    fun startSlideshow() {
        stopSlideshow()
        Observable.interval(SLIDESHOW_DELAY, SLIDESHOW_DELAY, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { slideShowState.value = STARTED }
            .subscribeBy(
                onNext = { slideShowState.value = NEXT },
                onComplete = { slideShowState.value = STOPPED },
                onError = { error ->
                    Timber.e(error)
                    slideShowState.value = STOPPED
                }
            )
            .addTo(timerComposite)
    }

    /**
     * Stop slideshow
     */
    fun stopSlideshow() {
        timerComposite.clear()
        slideShowState.value = STOPPED
    }
}
