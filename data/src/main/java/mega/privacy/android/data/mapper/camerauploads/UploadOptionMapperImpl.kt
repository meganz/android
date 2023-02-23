package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.exception.mapper.UnknownMapperParameterException
import javax.inject.Inject

/**
 * Default implementation of [UploadOptionMapper]
 */
internal class UploadOptionMapperImpl @Inject constructor() : UploadOptionMapper {
    override fun invoke(state: String?) = when {
        state == null -> UploadOption.PHOTOS
        state.toInt() == MegaPreferences.ONLY_PHOTOS -> UploadOption.PHOTOS
        state.toInt() == MegaPreferences.ONLY_VIDEOS -> UploadOption.VIDEOS
        state.toInt() == MegaPreferences.PHOTOS_AND_VIDEOS -> UploadOption.PHOTOS_AND_VIDEOS
        else -> throw UnknownMapperParameterException(
            UploadOptionMapper::class.simpleName,
            state.toString()
        )
    }
}