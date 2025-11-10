package mega.privacy.android.feature.photos.presentation.timeline.model

import mega.privacy.android.domain.entity.photos.PhotoResult

sealed class PhotosNodeListCard(
    open val date: String,
    open val photo: PhotoResult,
    open val key: String,
) {

    data class Years(
        override val date: String,
        override val photo: PhotoResult,
    ) : PhotosNodeListCard(date, photo, date) {
        override val key: String
            get() = date
    }

    data class Months(
        override val date: String,
        override val photo: PhotoResult,
    ) : PhotosNodeListCard(date, photo, date) {
        override val key: String
            get() = date
    }

    data class Days(
        override val date: String,
        override val photo: PhotoResult,
        val photosCount: String,
    ) : PhotosNodeListCard(date, photo, date) {
        override val key: String
            get() = date
    }
}

enum class PhotosNodeListCardCount(val portrait: Int, val landscape: Int) {
    Grid(1, 2)
}
