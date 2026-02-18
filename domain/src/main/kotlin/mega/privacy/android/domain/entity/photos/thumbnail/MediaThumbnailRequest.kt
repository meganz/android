package mega.privacy.android.domain.entity.photos.thumbnail

/**
 * Media thumbnail request
 */
data class MediaThumbnailRequest(
    val id: Long,
    val isPreview: Boolean,
    val thumbnailFilePath: String?,
    val previewFilePath: String?,
    val isPublicNode: Boolean,
    val fileExtension: String,
)
