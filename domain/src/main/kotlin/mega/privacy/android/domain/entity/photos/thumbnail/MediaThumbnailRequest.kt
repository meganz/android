package mega.privacy.android.domain.entity.photos.thumbnail

/**
 * Media thumbnail request
 *
 * @param isPublicNode True when loading from a public folder link
 * @param isPublicAlbumPhoto True when loading from a public album link
 */
data class MediaThumbnailRequest(
    val id: Long,
    val isPreview: Boolean,
    val thumbnailFilePath: String?,
    val previewFilePath: String?,
    val isPublicNode: Boolean,
    val fileExtension: String,
    val isPublicAlbumPhoto: Boolean = false,
)
