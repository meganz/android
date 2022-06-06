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

    private val _checkReadStoragePermissions = MutableStateFlow(false)
    val checkReadStoragePermissions: StateFlow<Boolean> get() = _checkReadStoragePermissions

    private val _checkCameraPermissions = MutableStateFlow(false)
    val checkCameraPermissions: StateFlow<Boolean> get() = _checkCameraPermissions

    /**
     * Method that receives changes related to read storage permissions
     *
     * @param hasPermissions If permission is granted. False, if not.
     */
    private fun updateReadStoragePermissions(hasPermissions: Boolean) {
        _hasReadStoragePermissionsGranted.value = hasPermissions
        checkCameraPermission()
        if (!hasPermissions) {
            addTakePicture(mutableListOf())
            return
        }

        loadGallery()
    }

    fun checkCameraPermission() {
        _checkCameraPermissions.value = true
    }

    fun checkStoragePermission() {
        _checkReadStoragePermissions.value = true
    }

    /**
     * Update camera permissions
     *
     * @param hasPermissions If permission is granted
     */
    private fun updateCameraPermissions(isGranted: Boolean) {
        if (_hasCameraPermissionsGranted.value == isGranted || !isGranted) {
            return
        }

        _hasCameraPermissionsGranted.value = isGranted

        _filesGallery.value = _filesGallery.value.map { file ->
            return@map when (file.isTakePicture) {
                true -> {
                    file.copy(hasCameraPermissions = true)
                }
                else -> file
            }
        }.toMutableList()
    }

    /**
     * Update read storage permissions
     *
     * @param typePermission Type of permission
     * @param isGranted If permission is granted
     */
    fun updatePermissionsGranted(typePermission: String, isGranted: Boolean) {
        when (typePermission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                updateReadStoragePermissions(isGranted)
            }
            Manifest.permission.CAMERA -> {
                updateCameraPermissions(isGranted)
            }
        }
    }

    /**
     * Add take picture option in the gallery
     */
    private fun addTakePicture(files: MutableList<FileGalleryItem>) {
        files.add(0, createTakeAPictureOption())
        _filesGallery.value = files
    }

    /**
     * Get images and videos from the gallery
     */
    private fun loadGallery() {
        if (_hasReadStoragePermissionsGranted.value && filesGallery.value.isEmpty()) {
            val files: MutableList<FileGalleryItem> = getGalleryFilesUseCase.get().blockingGet()
            addTakePicture(files)
        }
    }

    private fun createTakeAPictureOption(): FileGalleryItem {
        return FileGalleryItem(
            id = TAKE_PHOTO_OPTION_ID,
            isImage = false,
            isTakePicture = true,
            hasCameraPermissions = hasCameraPermissionsGranted.value,
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