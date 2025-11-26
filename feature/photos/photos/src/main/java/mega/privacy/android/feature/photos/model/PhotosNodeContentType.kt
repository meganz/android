package mega.privacy.android.feature.photos.model

import java.time.LocalDateTime

sealed interface PhotosNodeContentType {
    val key: Int

    data class HeaderItem(
        val time: LocalDateTime,
        val shouldShowGridSizeSettings: Boolean,
    ) : PhotosNodeContentType {
        override val key: Int = time.hashCode()
    }

    data class PhotoNodeItem(val node: PhotoNodeUiState) : PhotosNodeContentType {
        override val key: Int = node.hashCode()
    }
}
