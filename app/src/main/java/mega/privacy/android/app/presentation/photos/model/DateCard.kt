package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.domain.entity.photos.Photo

sealed class DateCard(
    open val date: String,
    open val photo: Photo,
    open val key: String,
) {

    data class YearsCard(
        override val date: String,
        override val photo: Photo,
    ) : DateCard(date, photo, date) {
        override val key: String
            get() = date
    }

    data class MonthsCard(
        override val date: String,
        override val photo: Photo,
    ) : DateCard(date, photo, date) {
        override val key: String
            get() = date
    }

    data class DaysCard(
        override val date: String,
        override val photo: Photo,
        val photosCount: String,
    ) : DateCard(date, photo, date) {
        override val key: String
            get() = date
    }
}

enum class DateCardCount(val portrait: Int, val landscape: Int) {
    Grid(1, 2)
}
