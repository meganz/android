package mega.privacy.android.app.image

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    fun retrieveSingleImage(nodeHandle: Long) {
        getImageUseCase.getImages(listOf(nodeHandle), 0)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { imageItems ->
                    images.value = imageItems.toList()
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun retrieveImagesFromParent(parentNodeHandle: Long) {
        getImageUseCase.getImages(parentNodeHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { imageItems ->
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
                onNext = { imageItems ->
                    images.value = imageItems.toList()
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun loadNearbyImages(position: Int) {
        images.value?.forEachIndexed { index, item ->
            if (index in position - 1..position + 1) {
                getImageUseCase.getProgressiveImage(item.handle)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onNext = { imageUri ->
                            val currentImages = images.value!!.toMutableList()
                            val currentItem = currentImages[index]
                            currentImages[index] = currentItem.copy(
                                uri = imageUri
                            )
                            images.value = currentImages
                        },
                        onError = { error ->
                            logError(error.stackTraceToString())
                        }
                    )
                    .addTo(composite)
            }
        }
    }
}
