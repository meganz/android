package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.exception.mapper.UnknownMapperParameterException
import timber.log.Timber
import javax.inject.Inject

/**
 * Mapper that converts a specific [String] from MegaPreferences into [UploadOption]
 */
internal class UploadOptionMapper @Inject constructor() {
    operator fun invoke(state: String?) = when {
        state == null -> UploadOption.PHOTOS
        state.toInt() == MegaPreferences.ONLY_PHOTOS -> UploadOption.PHOTOS.also { Timber.d("Upload only Photos") }
        state.toInt() == MegaPreferences.ONLY_VIDEOS -> UploadOption.VIDEOS.also { Timber.d("Upload only Videos") }
        state.toInt() == MegaPreferences.PHOTOS_AND_VIDEOS -> UploadOption.PHOTOS_AND_VIDEOS.also {
            Timber.d(
                "Upload  Photos & Videos"
            )
        }

        else -> throw UnknownMapperParameterException(
            UploadOptionMapper::class.simpleName,
            state.toString()
        )
    }
}
