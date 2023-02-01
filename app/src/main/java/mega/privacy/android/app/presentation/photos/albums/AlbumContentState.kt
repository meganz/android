package mega.privacy.android.app.presentation.photos.albums

data class AlbumContentState(
    val isAddingPhotos: Boolean = false,
    val totalAddedPhotos: Int = 0,
) {
    val isAddingPhotosProgressCompleted: Boolean
        get() = !isAddingPhotos && totalAddedPhotos > 0
}
