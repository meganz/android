package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.main.megachat.usecase.GetGalleryFilesUseCase
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil
import javax.inject.Inject


@HiltViewModel
class ChatRoomToolbarViewModel @Inject constructor(
    getGalleryFilesUseCase: GetGalleryFilesUseCase,
) : BaseRxViewModel() {

    private val imagesGallery: MutableLiveData<List<FileGalleryItem>> = MutableLiveData()

    init {
        getGalleryFilesUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    imagesGallery.value = items
                },
                onError = { error ->
                    LogUtil.logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun imagesGallery(): LiveData<List<FileGalleryItem>> = imagesGallery

    fun getDefaultLocation(): String =
        FileUtil.getDownloadLocation()

    fun getImagesFromGallery(): LiveData<List<FileGalleryItem>> =
        imagesGallery.map { it.filter { item -> item.isImage } }

    fun getVideosFromGallery(): LiveData<List<FileGalleryItem>> =
        imagesGallery.map { it.filter { item -> !item.isImage } }

}