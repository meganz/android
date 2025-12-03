package mega.privacy.android.domain.entity.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * A model to request timeline photos.
 *
 * @property isPaginationEnabled
 * @property selectedFilterFlow Used to retrigger the timeline photos result.
 */
data class TimelinePhotosRequest(
    val isPaginationEnabled: Boolean,
    val selectedFilterFlow: Flow<Map<String, String?>?> = flowOf(null),
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
    val inTypedNode: TypedNode?,
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
