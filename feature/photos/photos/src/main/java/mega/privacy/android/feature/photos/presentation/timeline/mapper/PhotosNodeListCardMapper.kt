package mega.privacy.android.feature.photos.presentation.timeline.mapper

import mega.privacy.android.domain.entity.photos.PhotoDateResult
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoNodeListCardItem
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import javax.inject.Inject

class PhotosNodeListCardMapper @Inject constructor(
    private val photoUiStateMapper: PhotoUiStateMapper,
) {

    operator fun invoke(photosDateResults: List<PhotoDateResult>): List<PhotosNodeListCard> =
        photosDateResults.map {
            when (it) {
                is PhotoDateResult.Day -> {
                    PhotosNodeListCard.Days(
                        date = it.date,
                        photoItem = PhotoNodeListCardItem(
                            photo = photoUiStateMapper(photo = it.photo.photo),
                            isMarkedSensitive = it.photo.isMarkedSensitive
                        ),
                        photosCount = it.photosCount
                    )
                }

                is PhotoDateResult.Month -> {
                    PhotosNodeListCard.Months(
                        date = it.date,
                        photoItem = PhotoNodeListCardItem(
                            photo = photoUiStateMapper(photo = it.photo.photo),
                            isMarkedSensitive = it.photo.isMarkedSensitive
                        ),
                    )
                }

                is PhotoDateResult.Year -> {
                    PhotosNodeListCard.Years(
                        date = it.date,
                        photoItem = PhotoNodeListCardItem(
                            photo = photoUiStateMapper(photo = it.photo.photo),
                            isMarkedSensitive = it.photo.isMarkedSensitive
                        ),
                    )
                }
            }
        }
}
