package mega.privacy.android.app.imageviewer

import android.app.Activity
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.map
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.usecase.GetImageHandlesUseCase
import mega.privacy.android.app.imageviewer.usecase.GetImageUseCase
import mega.privacy.android.app.usecase.CancelTransferUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.LogUtil.logError

class ImageViewerViewModel @ViewModelInject constructor(
    private val getImageUseCase: GetImageUseCase,
    private val getImageHandlesUseCase: GetImageHandlesUseCase,
    private val getNodeUseCase: GetNodeUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val cancelTransferUseCase: CancelTransferUseCase
) : BaseRxViewModel() {

    private val currentHandle = MutableLiveData<Long>()
    private val initialPosition = MutableLiveData<Int>()
    private val images: MutableLiveData<List<ImageItem>> = MutableLiveData()
    private val switchToolbar: MutableLiveData<Unit> = MutableLiveData()

    fun getCurrentHandle(): LiveData<Long> = currentHandle

    fun getCurrentImage(): LiveData<MegaNodeItem?> =
        Transformations.switchMap(currentHandle) { currentHandle -> getNode(currentHandle) }

    fun getImagesHandle(): LiveData<List<Long>> =
        images.map { items -> items.map(ImageItem::handle) }

    fun getImage(nodeHandle: Long): LiveData<ImageItem?> =
        images.map { items -> items.firstOrNull { it.handle == nodeHandle } }

    fun getInitialPosition(): LiveData<Int> = initialPosition

    fun onSwitchToolbar(): LiveData<Unit> = switchToolbar

    fun retrieveSingleImage(nodeHandle: Long) {
        getImageHandlesUseCase.get(nodeHandles = longArrayOf(nodeHandle))
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

    fun loadSingleImage(nodeHandle: Long, fullSize: Boolean, highPriority: Boolean) {
        getImageUseCase.get(nodeHandle, fullSize, highPriority)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { imageItem ->
                    images.value?.toMutableList()?.let { items ->
                        val index = items.indexOfFirst { it.handle == nodeHandle }
                        items[index] = imageItem
                        images.value = items.toList()
                    }
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun getNode(nodeHandle: Long): LiveData<MegaNodeItem?> {
        val result = MutableLiveData<MegaNodeItem?>()
        getNodeUseCase.getNodeItem(nodeHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { item ->
                    result.value = item
                },
                onError = { error ->
                    result.value = null
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
        return result
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
        getNodeUseCase.setNodeAvailableOffline(activity, nodeHandle, setAvailableOffline)
            .subscribeAndComplete()
    }

    fun removeLink(nodeHandle: Long) {
        exportNodeUseCase.disableExport(nodeHandle)
            .subscribeAndComplete()
    }

    fun shareNode(nodeHandle: Long): LiveData<String> {
        val result = MutableLiveData<String>()
        exportNodeUseCase.export(nodeHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { link ->
                    result.value = link
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
        return result
    }

    fun copyNode(nodeHandle: Long, newParentHandle: Long) {
        getNodeUseCase.copyNode(nodeHandle, newParentHandle)
            .subscribeAndComplete()
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
        images.value?.find { nodeHandle == it.handle }?.transferTag?.let { transferTag ->
            cancelTransferUseCase.cancel(transferTag)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { error ->
                        logError(error.stackTraceToString())
                    }
                )
        }
    }

    fun reloadCurrentImage() {
        currentHandle.value?.let { handle ->
            loadSingleImage(handle, fullSize = false, highPriority = false)
        }
    }

    fun updateCurrentPosition(position: Int) {
        images.value?.get(position)?.handle?.let { handle ->
            if (handle != currentHandle.value) {
                currentHandle.value = handle
            }
        }
    }

    fun switchToolbar() {
        switchToolbar.value = Unit
    }

    private fun Single<List<ImageItem>>.subscribeAndUpdateImages(currentNodeHandle: Long? = null) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { imageItems ->
                    images.value = imageItems.toList()

                    currentNodeHandle?.let { nodeHandle ->
                        val currentIndex = imageItems.indexOfFirst { nodeHandle == it.handle }
                        if (currentIndex != INVALID_POSITION) {
                            currentHandle.value = nodeHandle
                            initialPosition.value = currentIndex
                        }
                    }
                },
                onError = { error ->
                    logError(error.stackTraceToString())
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
