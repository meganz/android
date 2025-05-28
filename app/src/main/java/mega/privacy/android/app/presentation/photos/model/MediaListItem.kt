package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.domain.entity.photos.Photo
import java.time.LocalDateTime

sealed interface MediaListItem {
    val key: String

    data class Separator(
        val modificationTime: LocalDateTime,
    ) : MediaListItem {
        override val key: String
            get() = modificationTime.toString()
    }

    data class PhotoItem(val photo: Photo.Image) : MediaListItem, MediaListMedia {
        override val mediaId: Long
            get() = photo.id
        override val key: String
            get() = photo.id.toString()
        override val isFavourite: Boolean
            get() = photo.isFavourite
        override val duration: String? = null
    }

    data class VideoItem(val video: Photo.Video, override val duration: String) : MediaListItem,
        MediaListMedia {
        override val mediaId: Long
            get() = video.id
        override val key: String
            get() = video.id.toString()
        override val isFavourite: Boolean
            get() = video.isFavourite
    }

}

interface MediaListMedia {
    val mediaId: Long
    val isFavourite: Boolean
    val duration: String?
}