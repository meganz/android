package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.main.megachat.usecase.GetGalleryFilesUseCase
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil
import javax.inject.Inject


@HiltViewModel
class ChatRoomToolbarViewModel @Inject constructor(
        private val getGalleryFilesUseCase: GetGalleryFilesUseCase,
) : BaseRxViewModel() {

    private val _filesGallery =
            MutableStateFlow<List<FileGalleryItem>>(ArrayList())

    val filesGallery: StateFlow<List<FileGalleryItem>>
        get() = _filesGallery

    private val _showSendImagesButton = MutableStateFlow(false)
    val showSendImagesButton: StateFlow<Boolean> get() = _showSendImagesButton

    /**
     * How to get images and videos from the gallery
     */
    fun loadGallery() {
        getGalleryFilesUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = { items ->
                            _filesGallery.value = items
                        },
                        onError = { error ->
                            LogUtil.logError(error.stackTraceToString())
                        }
                )
                .addTo(composite)
    }

    fun getDefaultLocation(): String =
            FileUtil.getDownloadLocation()

    fun longClickItem(fileToUpload: FileGalleryItem) {
        _filesGallery.value = _filesGallery.value.map { file ->
            return@map when (file.id) {
                fileToUpload.id -> {
                    file.copy(isSelected = !file.isSelected)
                }
                else -> file
            }
        }.toMutableList()

        checkSendButtonVisibility()
    }

    /**
     * Method of getting the selected files
     *
     * @return list of selected files
     */
    fun getSelectedFiles(): ArrayList<FileGalleryItem> {
        val list = _filesGallery.value.filter { item ->
            item.isSelected
        }.toMutableList()

        return ArrayList(list)
    }

    /**
     * Method to control whether to show or hide the send file button
     */
    fun checkSendButtonVisibility() {
        val list = getSelectedFiles()

        _showSendImagesButton.value = list.isNotEmpty()
    }
}