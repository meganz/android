package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import timber.log.Timber
import javax.inject.Inject

/**
 * Mapper that converts a specific [String] from MegaPreferences into [UploadOption]
 */
internal class UploadOptionMapper @Inject constructor() {
    operator fun invoke(state: Int?) = when (state) {
        1001 -> UploadOption.PHOTOS.also { Timber.d("Upload only Photos") }
        1002 -> UploadOption.VIDEOS.also { Timber.d("Upload only Videos") }
        1003 -> UploadOption.PHOTOS_AND_VIDEOS.also {
            Timber.d(
                "Upload  Photos & Videos"
            )
        }

        else -> null
    }
}
