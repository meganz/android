package mega.privacy.android.feature.photos.model

import java.time.LocalDateTime

@Deprecated("Please use PhotosNodeContentItemV2.")
sealed interface PhotosNodeContentItem {
    val key: Int
    val type: PhotosNodeContentType

    data class HeaderItem(val time: LocalDateTime) : PhotosNodeContentItem {
        override val key: Int = time.hashCode()
        override val type: PhotosNodeContentType = PhotosNodeContentType.Header
    }

    data class PhotoNodeItem(val node: PhotoNodeUiState) : PhotosNodeContentItem {
        override val key: Int = node.photo.hashCode()
        override val type: PhotosNodeContentType = PhotosNodeContentType.PhotoNode
    }
}

data class PhotosNodeContentItemV2(
    val key: Long,
    val contentType: PhotosNodeContentType,
    val id: Long,
    val mediaType: MediaType,
    val day: Int,
    val month: Int,
    val year: Int,
    val fullModificationTime: Long,
    val thumbnailFilePath: String?,
    val previewFilePath: String?,
    val extension: String,
    val isFavourite: Boolean,
    val isSensitive: Boolean,
    val duration: String = "",
)

enum class PhotosNodeContentType {
    Header,
    PhotoNode
}

enum class MediaType {
    Image, Video
}
