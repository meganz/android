package mega.privacy.android.feature.photos.presentation.timeline.mapper

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.domain.entity.photos.PhotoDateResult
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import javax.inject.Inject

class PhotosNodeListCardMapper @Inject constructor() {

    operator fun invoke(photosDateResults: List<PhotoDateResult>): ImmutableList<PhotosNodeListCard> =
        photosDateResults.map {
            when (it) {
                is PhotoDateResult.Day -> {
                    PhotosNodeListCard.Days(
                        date = it.date,
                        photo = it.photo,
                        photosCount = it.photosCount
                    )
                }

                is PhotoDateResult.Month -> {
                    PhotosNodeListCard.Months(
                        date = it.date,
                        photo = it.photo
                    )
                }

                is PhotoDateResult.Year -> {
                    PhotosNodeListCard.Years(
                        date = it.date,
                        photo = it.photo
                    )
                }
            }
        }.toImmutableList()
}
