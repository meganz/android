package mega.privacy.android.feature.photos.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.photos.FilterRAWUseCase
import javax.inject.Inject
import mega.privacy.android.feature.photos.R

/**
 * System album type for RAW photos
 */
class RawSystemAlbum @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filterRAWUseCase: FilterRAWUseCase,
) : SystemAlbum {

    override val albumName: String = context.getString(R.string.photos_album_title_raw)

    override suspend fun filter(photo: Photo): Boolean = filterRAWUseCase()(photo)
}