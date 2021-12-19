package mega.privacy.android.app.imageviewer

import android.app.Activity
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
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
import mega.privacy.android.app.usecase.CancelTransferUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.LoggedInUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import nz.mega.sdk.MegaNode

/**
 * Main ViewModel to handle all logic related to the ImageViewer.
 * This is shared between ImageViewerActivity behaving as the main container and
 * each individual ImageViewerPageFragment representing a single image within the ViewPager.
 *
 * @property getImageUseCase        Needed to retrieve each individual image based on a node.
 * @property getImageHandlesUseCase Needed to retrieve node handles given sent params.
 * @property getNodeUseCase         Needed to retrieve each individual node based on a node handle,
 *                                  as well as each individual node action required by the menu.
 * @property exportNodeUseCase      Needed to export image node on demand.
 * @property cancelTransferUseCase  Needed to cancel current full image transfer if needed.
 */
class ImageViewerViewModel @ViewModelInject constructor(
    private val getImageUseCase: GetImageUseCase,
    private val getImageHandlesUseCase: GetImageHandlesUseCase,
    private val getNodeUseCase: GetNodeUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val cancelTransferUseCase: CancelTransferUseCase,
    private val loggedInUseCase: LoggedInUseCase
) : BaseRxViewModel() {

    private val images = MutableLiveData<List<ImageItem>?>()
    private val currentPosition = MutableLiveData<Int>()
    private val switchToolbar = MutableLiveData<Unit>()
    private val snackbarMessage = SingleLiveEvent<String>()
    private var isUserLoggedIn = false

    init {
        checkIfUserIsLoggedIn()
    }

    fun onImagesHandle(): LiveData<List<Long>?> =
        images.map { items -> items?.map(ImageItem::handle) }

    fun onImage(nodeHandle: Long): LiveData<ImageItem?> =
        images.map { items -> items?.firstOrNull { it.handle == nodeHandle } }

    fun onCurrentPosition(): LiveData<Pair<Int, Int>> =
        currentPosition.map { position -> Pair(position, images.value?.size ?: 0) }

    fun onCurrentImageNode(): LiveData<MegaNodeItem?> =
        currentPosition.map { images.value?.getOrNull(it)?.nodeItem }

    fun getCurrentNode(): MegaNodeItem? =
        currentPosition.value?.let { images.value?.getOrNull(it)?.nodeItem }

    fun onSnackbarMessage(): LiveData<String> = snackbarMessage

    fun onSwitchToolbar(): LiveData<Unit> = switchToolbar

    fun retrieveSingleImage(nodeHandle: Long, isOffline: Boolean = false) {
        getImageHandlesUseCase.get(nodeHandles = longArrayOf(nodeHandle), isOffline = isOffline)
            .subscribeAndUpdateImages()
    }

    fun retrieveSingleImage(nodeFileLink: String) {
        getImageHandlesUseCase.get(nodeFileLinks = listOf(nodeFileLink))
            .subscribeAndUpdateImages()
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
     * @param nodeHandle    Image node handle to be loaded.
     * @param forceReload   Flag to force reload node if needed.
     */
    fun loadSingleNode(nodeHandle: Long, forceReload: Boolean = false) {
        val existingNode = images.value?.find { it.handle == nodeHandle }
        val subscription = when {
            forceReload || existingNode?.isDirty == true -> {
                if (existingNode?.nodePublicLink?.isNotBlank() == true) {
                    getNodeUseCase.getNodeItem(existingNode.nodePublicLink)
                } else {
                    getNodeUseCase.getNodeItem(nodeHandle)
                }
            }
            existingNode?.nodeItem?.node != null ->
                getNodeUseCase.getNodeItem(existingNode.nodeItem.node)
            existingNode?.nodePublicLink?.isNotBlank() == true ->
                getNodeUseCase.getNodeItem(existingNode.nodePublicLink)
            existingNode?.chatMessageId != null && existingNode.chatRoomId != null ->
                getNodeUseCase.getNodeItem(existingNode.chatRoomId, existingNode.chatMessageId)
            existingNode?.isOffline == true ->
                getNodeUseCase.getOfflineNodeItem(existingNode.handle)
            else ->
                getNodeUseCase.getNodeItem(nodeHandle)
        }

        subscription
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retry(1)
            .subscribeBy(
                onSuccess = { nodeItem ->
                    updateItemIfNeeded(nodeHandle, nodeItem = nodeItem)
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    /**
     * Main method to request an ImageResult given a previously loaded Node handle.
     * This will update the current Image on the main "images" list if it's newer.
     * You must be observing the requested Image to get the updated result.
     *
     * @param nodeHandle    Image node handle to be loaded.
     * @param fullSize      Flag to request full size image.
     * @param highPriority  Flag to request full image with high priority.
     */
    fun loadSingleImage(nodeHandle: Long, fullSize: Boolean, highPriority: Boolean) {
        val existingNode = images.value?.find { it.handle == nodeHandle }
        val subscription = when {
            existingNode?.nodeItem?.node != null ->
                getImageUseCase.get(existingNode.nodeItem.node, fullSize, highPriority)
            existingNode?.nodePublicLink?.isNotBlank() == true ->
                getImageUseCase.get(existingNode.nodePublicLink)
            existingNode?.chatMessageId != null && existingNode.chatRoomId != null ->
                getImageUseCase.get(existingNode.chatRoomId, existingNode.chatMessageId)
            existingNode?.isOffline == true ->
                getImageUseCase.getOffline(existingNode.handle)
            else ->
                getImageUseCase.get(nodeHandle, fullSize, highPriority)
        }

        subscription
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retry(1)
            .subscribeBy(
                onNext = { imageResult ->
                    updateItemIfNeeded(nodeHandle, imageResult = imageResult)
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    /**
     * Update a specific ImageItem from the Images list with the provided
     * MegaNodeItem or ImageResult
     *
     * @param nodeHandle    Item node handle to be updated
     * @param nodeItem      MegaNodeItem to be updated with
     * @param imageResult   ImageResult to be updated with
     */
    private fun updateItemIfNeeded(
        nodeHandle: Long,
        nodeItem: MegaNodeItem? = null,
        imageResult: ImageResult? = null
    ) {
        if (nodeItem == null && imageResult == null) return

        val currentItems = images.value?.toMutableList()
        if (!currentItems.isNullOrEmpty()) {
            val index = currentItems.indexOfFirst { it.handle == nodeHandle }
            if (index != INVALID_POSITION) {
                val currentItem = currentItems[index]
                if (nodeItem != null) {
                    currentItems[index] = currentItem.copy(
                        nodeItem = nodeItem,
                        isDirty = false
                    )
                }
                if (imageResult != null) {
                    currentItems[index] = currentItem.copy(
                        imageResult = imageResult,
                        isDirty = false
                    )
                }
                images.value = currentItems.toList()
                if (index == currentPosition.value) {
                    updateCurrentPosition(index, true)
                }
            } else {
                logWarning("Node $nodeHandle not found")
            }
        } else {
            logWarning("Images are null")
        }
    }

    fun markNodeAsFavorite(nodeHandle: Long, isFavorite: Boolean) {
        getNodeUseCase.markAsFavorite(nodeHandle, isFavorite)
            .subscribeAndComplete()
    }

    fun setNodeAvailableOffline(
        activity: Activity,
        node: MegaNode,
        setAvailableOffline: Boolean
    ) {
        getNodeUseCase.setNodeAvailableOffline(node, setAvailableOffline, activity)
            .subscribeAndComplete {
                loadSingleNode(node.handle, true)
            }
    }

    fun removeLink(nodeHandle: Long) {
        exportNodeUseCase.disableExport(nodeHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getQuantityString(R.plurals.context_link_removal_success, 1)
            }
    }

    fun shareNode(node: MegaNode): LiveData<String?> {
        val result = MutableLiveData<String?>()

        val existingPublicLink = images.value?.find { it.handle == node.handle }?.nodePublicLink
        if (!existingPublicLink.isNullOrBlank()) {
            result.value = existingPublicLink
        } else {
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
        }

        return result
    }

    fun copyNode(nodeHandle: Long, newParentHandle: Long) {
        getNodeUseCase.copyNode(
            node = getExistingNode(nodeHandle),
            nodeHandle = nodeHandle,
            toParentHandle = newParentHandle
        ).subscribeAndComplete {
            snackbarMessage.value = getString(R.string.context_correctly_copied)
        }
    }

    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        getNodeUseCase.moveNode(nodeHandle, newParentHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getString(R.string.context_correctly_moved)
            }
    }

    fun moveNodeToRubbishBin(nodeHandle: Long) {
        getNodeUseCase.moveToRubbishBin(nodeHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getString(R.string.context_correctly_moved_to_rubbish)
            }
    }

    fun removeNode(nodeHandle: Long) {
        getNodeUseCase.removeNode(nodeHandle)
            .subscribeAndComplete {
                snackbarMessage.value = getString(R.string.context_correctly_removed)
            }
    }

    fun stopImageLoading(nodeHandle: Long) {
        images.value?.find { nodeHandle == it.handle }?.imageResult?.transferTag?.let { tag ->
            cancelTransferUseCase.cancel(tag)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { error ->
                        logError(error.stackTraceToString())
                    }
                )
        }
    }

    fun updateCurrentPosition(position: Int, forceUpdate: Boolean = false) {
        if (forceUpdate || position != currentPosition.value) {
            currentPosition.postValue(position)
        }
    }

    fun switchToolbar() {
        switchToolbar.value = Unit
    }

    private fun getExistingNode(nodeHandle: Long): MegaNode? =
        images.value?.find { it.handle == nodeHandle }?.nodeItem?.node

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
     * Reused Extension Function to subscribe to a Flowable<List<ImageItem>> and update each
     * individual ImageItem in the list with the new changes from MegaAPI.
     *
     * @param currentNodeHandle Node handle to be shown on first load.
     */
    private fun Flowable<List<ImageItem>>.subscribeAndUpdateImages(currentNodeHandle: Long? = null) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    when {
                        items.isEmpty() -> {
                            images.value = null
                            updateCurrentPosition(0)
                        }
                        images.value.isNullOrEmpty() -> {
                            images.value = items.toList()

                            val position = items.indexOfFirst { it.handle == currentNodeHandle }
                            if (position != INVALID_POSITION) {
                                updateCurrentPosition(position, true)
                            } else {
                                updateCurrentPosition(0, true)
                            }
                        }
                        else -> {
                            val actualNodeHandle = getCurrentNode()?.handle
                            val currentItemPosition = currentPosition.value ?: 0
                            val foundIndex = items.indexOfFirst { it.handle == actualNodeHandle }
                            val newPosition = when {
                                foundIndex != INVALID_POSITION ->
                                    foundIndex
                                currentItemPosition >= items.size ->
                                    items.size - 1
                                currentItemPosition == 0 ->
                                    currentItemPosition + 1
                                else ->
                                    currentItemPosition
                            }

                            val dirtyNodeHandles = mutableListOf<Long>()
                            images.value = items.map { item ->
                                val existingNode = images.value?.find { it.handle == item.handle }
                                if (item.isDirty) {
                                    dirtyNodeHandles.add(item.handle)
                                }
                                item.copy(
                                    imageResult = existingNode?.imageResult,
                                    nodeItem = existingNode?.nodeItem
                                )
                            }
                            dirtyNodeHandles.forEach(::loadSingleNode)
                            updateCurrentPosition(newPosition, true)
                        }
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
