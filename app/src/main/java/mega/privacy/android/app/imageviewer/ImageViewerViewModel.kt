package mega.privacy.android.app.imageviewer

import android.app.Activity
import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.map
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.getLink.useCase.ExportNodeUseCase
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.usecase.GetImageHandlesUseCase
import mega.privacy.android.app.imageviewer.usecase.GetImageUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.MegaNodeItem
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

class ImageViewerViewModel @ViewModelInject constructor(
    private val getImageUseCase: GetImageUseCase,
    private val getImageHandlesUseCase: GetImageHandlesUseCase,
    private val getNodeUseCase: GetNodeUseCase,
    private val exportNodeUseCase: ExportNodeUseCase
) : BaseRxViewModel() {

    private var currentHandle = INVALID_HANDLE
    private val currentPosition = MutableLiveData(0)
    private val images: MutableLiveData<List<ImageItem>> = MutableLiveData()

    fun getCurrentHandle(): Long = currentHandle

    fun getCurrentImage(): LiveData<ImageItem?> =
        Transformations.switchMap(currentPosition) { position -> getImage(position) }

    fun getImagesHandle(): LiveData<List<Long>> =
        images.map { items -> items.map(ImageItem::handle) }

    fun getImage(nodeHandle: Long): LiveData<ImageItem?> =
        images.map { items -> items.firstOrNull { it.handle == nodeHandle } }

    fun getImage(position: Int): LiveData<ImageItem?> =
        images.map { items -> items.getOrNull(position) }

    fun retrieveSingleImage(nodeHandle: Long) {
        getImageHandlesUseCase.get(listOf(nodeHandle))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { imageItems ->
                    images.value = imageItems.toList()
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun retrieveImagesFromParent(parentNodeHandle: Long, childOrder: Int) {
        getImageHandlesUseCase.getChildren(parentNodeHandle, childOrder)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { imageItems ->
                    images.value = imageItems.toList()
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun retrieveImages(nodeHandles: List<Long>) {
        getImageHandlesUseCase.get(nodeHandles)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { imageItems ->
                    images.value = imageItems.toList()
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun loadSingleImage(nodeHandle: Long, fullSize: Boolean) {
        getImageUseCase.get(nodeHandle, fullSize)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { imageItem ->
                    val currentImages = images.value?.toMutableList()!!
                    val index = currentImages.indexOfFirst { it.handle == nodeHandle }
                    currentImages[index] = imageItem

                    images.value = currentImages.toList()
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun updateCurrentImage() {
        loadSingleImage(currentHandle, true)
    }

    fun getNode(context: Context, nodeHandle: Long): LiveData<MegaNodeItem?> {
        val result = MutableLiveData<MegaNodeItem?>()
        getNodeUseCase.get(context, nodeHandle)
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
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun setNodeAvailableOffline(
        activity: Activity,
        nodeHandle: Long,
        setAvailableOffline: Boolean
    ) {
        getNodeUseCase.setNodeAvailableOffline(activity, nodeHandle, setAvailableOffline)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun removeLink(nodeHandle: Long) {
        exportNodeUseCase.disableExport(nodeHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
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
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        getNodeUseCase.moveNode(nodeHandle, newParentHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun moveNodeToRubishBin(nodeHandle: Long) {
        getNodeUseCase.moveToRubbishBin(nodeHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun setCurrentPosition(position: Int) {
        currentPosition.value = position
        images.value?.get(position)?.handle?.let { handle ->
            currentHandle = handle
        }
    }
}
