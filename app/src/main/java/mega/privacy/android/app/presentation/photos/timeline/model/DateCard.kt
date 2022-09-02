package mega.privacy.android.app.presentation.photos.timeline.model

import mega.privacy.android.domain.entity.photos.Photo

interface DateCard {
    val date: String
    val photo: Photo
    val key: String

    data class YearsCard(
        override val date: String,
        override val photo: Photo,
    ) : DateCard {
        override val key: String
            get() = date
    }

    data class MonthsCard(
        override val date: String,
        override val photo: Photo,
    ) : DateCard {
        override val key: String
            get() = date
    }

    data class DaysCard(
        override val date: String,
        override val photo: Photo,
        val photosCount: String,
    ) : DateCard {
        override val key: String
            get() = date
    }


    enum class DateCardCount(val portrait: Int, val landscape: Int) {
        Grid(1, 2)
    }
}
