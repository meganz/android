package mega.privacy.android.feature.photos.presentation.timeline.model

import mega.privacy.android.feature.photos.model.PhotoUiState

sealed class PhotosNodeListCard(
    open val date: String,
    open val photoItem: PhotoNodeListCardItem,
    open val key: String,
) {

    data class Years(
        override val date: String,
        override val photoItem: PhotoNodeListCardItem,
    ) : PhotosNodeListCard(date, photoItem, date) {
        override val key: String
            get() = date
    }

    data class Months(
        override val date: String,
        override val photoItem: PhotoNodeListCardItem,
    ) : PhotosNodeListCard(date, photoItem, date) {
        override val key: String
            get() = date
    }

    data class Days(
        override val date: String,
        override val photoItem: PhotoNodeListCardItem,
        val photosCount: Int,
    ) : PhotosNodeListCard(date, photoItem, date) {
        override val key: String
            get() = date
    }
}

data class PhotoNodeListCardItem(
    val photo: PhotoUiState,
    val isMarkedSensitive: Boolean,
)

enum class PhotosNodeListCardCount(val portrait: Int, val landscape: Int) {
    Grid(1, 2)
}
