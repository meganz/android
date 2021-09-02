package mega.privacy.android.app.image

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.GetImageUseCase
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.image.data.ImageItem
import mega.privacy.android.app.utils.LogUtil.logError

class ImageViewerViewModel @ViewModelInject constructor(
    private val getImageUseCase: GetImageUseCase
) : BaseRxViewModel() {

    var defaultPosition = 0

    private val images: MutableLiveData<List<ImageItem>> = MutableLiveData()

    fun getImages(): LiveData<List<ImageItem>> = images

    fun getImage(nodeHandle: Long): LiveData<ImageItem?> =
        images.map { items -> items.firstOrNull { it.handle == nodeHandle } }

    fun retrieveSingleImage(nodeHandle: Long) {
        getImageUseCase.getImages(listOf(nodeHandle))
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

    fun retrieveImagesFromParent(parentNodeHandle: Long) {
        getImageUseCase.getChildImages(parentNodeHandle)
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
        getImageUseCase.getImages(nodeHandles)
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
        getImageUseCase.getProgressiveImage(nodeHandle, fullSize)
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
}
