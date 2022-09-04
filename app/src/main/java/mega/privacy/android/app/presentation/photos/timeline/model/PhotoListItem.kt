package mega.privacy.android.app.presentation.photos.timeline.model

import mega.privacy.android.domain.entity.photos.Photo
import java.time.LocalDateTime

sealed interface PhotoListItem {
    val key: String

    data class Separator(
        val modificationTime: LocalDateTime,
    ) : PhotoListItem {
        override val key: String
            get() = modificationTime.toString()
    }

    data class PhotoGridItem(
        val photo: Photo,
        val isSelected: Boolean,
    ) : PhotoListItem {
        override val key: String
            get() = photo.id.toString()
    }

}


