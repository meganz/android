package mega.privacy.android.feature.photos.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.photos.FilterGIFUseCase
import mega.privacy.android.feature.photos.R
import javax.inject.Inject

/**
 * System album type for GIF photos
 */
class GifSystemAlbum @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filterGIFUseCase: FilterGIFUseCase,
) : SystemAlbum {

    override val albumName: String = context.getString(R.string.photos_album_title_gif)

    override suspend fun filter(photo: Photo): Boolean = filterGIFUseCase()(photo)
}