package mega.privacy.android.app.presentation.photos.albums

data class AlbumContentState(
    val isAddingPhotos: Boolean = false,
    val totalAddedPhotos: Int = 0,
    /** A temporary field to support compatibility between view and compose architecture. */
    val isDeleteAlbum: Boolean = false,
    val isRemovingPhotos: Boolean = false,
    val totalRemovedPhotos: Int = 0,
    val showRemoveLinkConfirmation: Boolean = false,
    val isLinkRemoved: Boolean = false,
) {
    val isAddingPhotosProgressCompleted: Boolean
        get() = !isAddingPhotos && totalAddedPhotos > 0
    val isRemovingPhotosProgressCompleted: Boolean
        get() = !isRemovingPhotos && totalRemovedPhotos > 0
}
