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
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.data.ImageResult
import mega.privacy.android.app.imageviewer.usecase.GetImageHandlesUseCase
import mega.privacy.android.app.imageviewer.usecase.GetImageUseCase
import mega.privacy.android.app.usecase.CancelTransferUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import nz.mega.sdk.MegaNode

/**
 * Main ViewModel to handle all logic related to the ImageViewer.
 * This is shared between {@link ImageViewerActivity} behaving as the main container and
 * each individual {@link ImageViewerPageFragment} representing a single image within the ViewPager.
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
    private val cancelTransferUseCase: CancelTransferUseCase
) : BaseRxViewModel() {

    private val images = MutableLiveData<List<ImageItem>?>()
    private val currentPosition = MutableLiveData<Int>()
    private val switchToolbar = MutableLiveData<Unit>()

    fun onImagesHandle(): LiveData<List<Long>?> =
        images.map { items -> items?.map(ImageItem::handle) }

    fun onImage(nodeHandle: Long): LiveData<ImageItem?> =
        images.map { items -> items?.firstOrNull { it.handle == nodeHandle } }

    fun onCurrentPosition(): LiveData<Pair<Int, Int>> =
        currentPosition.map { position -> Pair(position, images.value?.size ?: 0) }

    fun onCurrentImageNode(): LiveData<MegaNodeItem?> =
        currentPosition.map { images.value?.getOrNull(it)?.nodeItem }

    fun getCurrentNode(): MegaNode? =
        currentPosition.value?.let { images.value?.getOrNull(it)?.nodeItem?.node }

    fun onSwitchToolbar(): LiveData<Unit> = switchToolbar

    fun retrieveSingleImage(nodeHandle: Long) {
        getImageHandlesUseCase.get(nodeHandles = longArrayOf(nodeHandle))
            .subscribeAndUpdateImages()
    }

    fun retrieveSingleImage(nodeFileLink: String) {
        getImageHandlesUseCase.get(nodeFileLinks = listOf(nodeFileLink))
            .subscribeAndUpdateImages()
    }

    fun retrieveSingleOfflineImage(nodeHandle: Long) {
        getImageHandlesUseCase.get(nodeHandles = longArrayOf(nodeHandle), isOffline = true)
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
        currentNodeHandle: Long? = null
    ) {
        getImageHandlesUseCase.get(nodeHandles = nodeHandles)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    fun retrieveOfflineImages(
        nodeHandles: LongArray,
        currentNodeHandle: Long? = null
    ) {
        getImageHandlesUseCase.get(nodeHandles = nodeHandles, isOffline = true)
            .subscribeAndUpdateImages(currentNodeHandle)
    }

    /**
     * Main method to request a {@link MegaNodeItem} given a previously loaded Node handle.
     * This will update the current Node on the main "images" list if it's newer.
     * You must be observing the requested Image to get the updated result.
     *
     * @param nodeHandle    Image node handle to be loaded.
     */
    fun loadSingleNode(nodeHandle: Long) {
        val existingNode = images.value?.find { it.handle == nodeHandle }
        val subscription = when {
            existingNode?.nodeItem?.node != null && !existingNode.isDirty ->
                getNodeUseCase.getNodeItem(existingNode.nodeItem.node)
            existingNode?.publicLink != null && existingNode.publicLink.isNotBlank() ->
                getNodeUseCase.getNodeItem(existingNode.publicLink)
            else ->
                getNodeUseCase.getNodeItem(nodeHandle)
        }

        subscription
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { nodeItem ->
                    updateItemIfNeeded(nodeHandle, nodeItem = nodeItem)
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
    }

    /**
     * Main method to request an {@link ImageResult} given a previously loaded Node handle.
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
            existingNode?.publicLink != null && existingNode.publicLink.isNotBlank() ->
                getImageUseCase.get(existingNode.publicLink)
            else ->
                getImageUseCase.get(nodeHandle, fullSize, highPriority)
        }

        subscription
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { imageResult ->
                    updateItemIfNeeded(nodeHandle, imageResult = imageResult)
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
    }

    /**
     * Update a specific {@link ImageItem} from the Images list with the provided
     * {@link MegaNodeItem} or {@link ImageResult}
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
        nodeHandle: Long,
        setAvailableOffline: Boolean
    ) {
        getNodeUseCase.setNodeAvailableOffline(nodeHandle, setAvailableOffline, activity)
            .subscribeAndComplete()
    }

    fun removeLink(nodeHandle: Long): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        exportNodeUseCase.disableExport(nodeHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    result.value = true
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    result.value = false
                }
            )
            .addTo(composite)
        return result
    }

    fun shareNode(nodeHandle: Long): LiveData<String?> {
        val result = MutableLiveData<String?>()
        exportNodeUseCase.export(nodeHandle)
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
        getNodeUseCase.copyNode(
            node = getExistingNode(nodeHandle),
            nodeHandle = nodeHandle,
            toParentHandle = newParentHandle
        ).subscribeAndComplete()
    }

    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        getNodeUseCase.moveNode(nodeHandle, newParentHandle)
            .subscribeAndComplete()
    }

    fun moveNodeToRubbishBin(nodeHandle: Long) {
        getNodeUseCase.moveToRubbishBin(nodeHandle)
            .subscribeAndComplete()
    }

    fun removeNode(nodeHandle: Long) {
        getNodeUseCase.removeNode(nodeHandle)
            .subscribeAndComplete()
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

    private fun Completable.subscribeAndComplete() {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }
}
