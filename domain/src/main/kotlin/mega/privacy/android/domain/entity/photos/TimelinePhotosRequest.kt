package mega.privacy.android.domain.entity.photos

data class TimelinePhotosRequest(
    val isPaginationEnabled: Boolean,
    val isHiddenNodesActive: Boolean,
)

data class TimelinePhotosResult(
    val allPhotos: List<PhotoResult>,
    val nonSensitivePhotos: List<PhotoResult>,
)

data class TimelineSortedPhotosResult(
    val sortedPhotos: List<PhotoResult>,
    val photosInDay: List<PhotoDateResult>,
    val photosInMonth: List<PhotoDateResult>,
    val photosInYear: List<PhotoDateResult>,
)

data class PhotoResult(
    val photo: Photo,
    val isMarkedSensitive: Boolean,
)

sealed class PhotoDateResult(
    open val date: String,
    open val photo: PhotoResult,
) {

    data class Day(
        override val date: String,
        override val photo: PhotoResult,
        val photosCount: Int,
    ) : PhotoDateResult(
        date = date,
        photo = photo
    )

    data class Month(
        override val date: String,
        override val photo: PhotoResult,
    ) : PhotoDateResult(
        date = date,
        photo = photo
    )

    data class Year(
        override val date: String,
        override val photo: PhotoResult,
    ) : PhotoDateResult(
        date = date,
        photo = photo
    )
}
