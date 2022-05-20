package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.main.megachat.usecase.GetGalleryFilesUseCase
import mega.privacy.android.app.utils.FileUtil
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

    private val _hasPermissionsGranted = MutableStateFlow(false)
    val hasPermissionsGranted: StateFlow<Boolean> get() = _hasPermissionsGranted

    /**
     * Method that receives changes related to read storage permissions
     *
     * @param hasPermissions If permission is granted. False, if not.
     */
    fun updateReadStoragePermissions(hasPermissions: Boolean) {
        if (_hasPermissionsGranted.value == hasPermissions) {
            return
        }

        _hasPermissionsGranted.value = hasPermissions
        loadGallery()
    }

    /**
     * How to get images and videos from the gallery
     */
    private fun loadGallery() {
        if (_hasPermissionsGranted.value && filesGallery.value.isEmpty()) {
            _filesGallery.value = getGalleryFilesUseCase.get().blockingGet()
        }
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