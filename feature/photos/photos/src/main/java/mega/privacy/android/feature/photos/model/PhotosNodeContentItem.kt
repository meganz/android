package mega.privacy.android.feature.photos.model

import java.time.LocalDateTime

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

enum class PhotosNodeContentType {
    Header,
    PhotoNode
}
