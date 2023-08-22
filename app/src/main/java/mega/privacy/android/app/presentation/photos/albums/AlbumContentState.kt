package mega.privacy.android.app.presentation.photos.albums

@Deprecated(message = "In favor of mega.privacy.android.app.presentation.photos.albums.albumcontent.AlbumContentState")
data class AlbumContentState(
    val isLoadingPhotos: Boolean = true,
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
