package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.domain.entity.photos.Photo
import java.time.LocalDateTime

sealed interface UIPhoto {
    val key: String

    data class Separator(
        val modificationTime: LocalDateTime,
    ) : UIPhoto {
        override val key: String
            get() = modificationTime.toString()
    }

    data class PhotoItem(val photo: Photo) : UIPhoto {
        override val key: String
            get() = photo.id.toString()
    }

}
