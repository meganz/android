package mega.privacy.android.app.imageviewer

import android.app.Activity
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
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
import mega.privacy.android.app.imageviewer.usecase.GetImageHandlesUseCase
import mega.privacy.android.app.imageviewer.usecase.GetImageUseCase
import mega.privacy.android.app.usecase.CancelTransferUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

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

    fun getCurrentImage(): LiveData<MegaNodeItem?> =
        Transformations.switchMap(currentPosition) { currentPosition ->
            images.value?.getOrNull(currentPosition)?.let { getNode(it.handle) }
        }

    fun getImagesHandle(): LiveData<List<Long>?> =
        images.map { items -> items?.map(ImageItem::handle) }

    fun getImage(nodeHandle: Long): LiveData<ImageItem?> =
        images.map { items -> items?.firstOrNull { it.handle == nodeHandle } }

    fun getCurrentPosition(): LiveData<Pair<Int, Int>> =
        currentPosition.map { position ->
            Pair(position, images.value?.size ?: 0)
        }

    fun getCurrentNodeHandle(): Long? =
        currentPosition.value?.let { images.value?.getOrNull(it)?.handle }

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
                    val currentImages = images.value?.toMutableList()
                    if (!currentImages.isNullOrEmpty()) {
                        val index = currentImages.indexOfFirst { it.handle == nodeHandle }
                        if (index != INVALID_POSITION) {
                            currentImages[index] = imageItem
                            images.value = currentImages.toList()
                        } else {
                            logWarning("Image ${imageItem.handle} was not found")
                        }
                    } else {
                        logWarning("Images are null")
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
                    logError(error.stackTraceToString())
                    result.value = null
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

    fun updateCurrentPosition(position: Int, forceUpdate: Boolean = false) {
        if (forceUpdate || position != currentPosition.value) {
            currentPosition.postValue(position)
        }
    }

    fun switchToolbar() {
        switchToolbar.value = Unit
    }

    private fun Flowable<List<ImageItem>>.subscribeAndUpdateImages(currentNodeHandle: Long? = null) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { imageItems ->
                    when {
                        imageItems.isEmpty() -> {
                            images.value = null
                            updateCurrentPosition(0)
                        }
                        images.value.isNullOrEmpty() -> {
                            images.value = imageItems.toList()

                            val position = imageItems.indexOfFirst { it.handle == currentNodeHandle }
                            if (position != INVALID_POSITION) {
                                updateCurrentPosition(position, true)
                            } else {
                                updateCurrentPosition(0, true)
                            }
                        }
                        else -> {
                            val actualNodeHandle = getCurrentNodeHandle()
                            val currentItemPosition = currentPosition.value ?: 0
                            val foundIndex = imageItems.indexOfFirst { it.handle == actualNodeHandle }
                            val newPosition = when {
                                foundIndex != INVALID_POSITION ->
                                    foundIndex
                                currentItemPosition >= imageItems.size ->
                                    imageItems.size - 1
                                currentItemPosition == 0 ->
                                    currentItemPosition + 1
                                else ->
                                    currentItemPosition
                            }

                            images.value = imageItems.toList()
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
