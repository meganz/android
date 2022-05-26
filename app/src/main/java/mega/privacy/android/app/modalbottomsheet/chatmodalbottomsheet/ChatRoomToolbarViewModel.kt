package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.Manifest
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

    private val _hasReadStoragePermissionsGranted = MutableStateFlow(false)
    val hasReadStoragePermissionsGranted: StateFlow<Boolean> get() = _hasReadStoragePermissionsGranted

    private val _hasCameraPermissionsGranted = MutableStateFlow(false)
    val hasCameraPermissionsGranted: StateFlow<Boolean> get() = _hasCameraPermissionsGranted

    /**
     * Method that receives changes related to read storage permissions
     *
     * @param hasPermissions If permission is granted. False, if not.
     */
    private fun updateReadStoragePermissions(hasPermissions: Boolean) {
        if (_hasReadStoragePermissionsGranted.value == hasPermissions) {
            return
        }

        _hasReadStoragePermissionsGranted.value = hasPermissions
        loadGallery()
    }

    private fun updateCameraPermissions(hasPermissions: Boolean) {
        if (_hasCameraPermissionsGranted.value == hasPermissions) {
            return
        }

        _hasCameraPermissionsGranted.value = hasPermissions

        _filesGallery.value = _filesGallery.value.map { file ->
            return@map when (file.isTakePicture) {
                true -> {
                    file.copy(hasCameraPermissions = true)
                }
                else -> file
            }
        }.toMutableList()
    }

    fun updatePermissionsGranted(typePermission: String) {
        when (typePermission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                updateReadStoragePermissions(true)
            }
            Manifest.permission.CAMERA -> {
                updateCameraPermissions(true)
            }
        }
    }

    /**
     * How to get images and videos from the gallery
     */
    private fun loadGallery() {
        if (_hasReadStoragePermissionsGranted.value && filesGallery.value.isEmpty()) {
            val files: MutableList<FileGalleryItem> = getGalleryFilesUseCase.get().blockingGet()
            files.add(0, createTakeAPictureOption())
            _filesGallery.value = files
        }
    }

    private fun createTakeAPictureOption(): FileGalleryItem {
        return FileGalleryItem(
                id = TAKE_PHOTO_OPTION_ID,
                isImage = false,
                isTakePicture = true,
                isSelected = false
        )
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

    companion object {
        const val TAKE_PHOTO_OPTION_ID: Long = -1
    }
}