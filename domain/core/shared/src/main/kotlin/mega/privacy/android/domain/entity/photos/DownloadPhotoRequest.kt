package mega.privacy.android.domain.entity.photos

data class DownloadPhotoRequest(
    val isPreview: Boolean,
    val photo: Photo,
    val isPublicNode: Boolean = false,
)
