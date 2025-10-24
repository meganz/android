package mega.privacy.android.feature.photos.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.photos.FilterFavouriteUseCase
import mega.privacy.android.feature.photos.R
import javax.inject.Inject

/**
 * System album type for favourite photos
 */
class FavouriteSystemAlbum @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filterFavouriteUseCase: FilterFavouriteUseCase,
) : SystemAlbum {

    override val albumName: String = context.getString(R.string.title_favourites_album)

    override suspend fun filter(photo: Photo): Boolean = filterFavouriteUseCase()(photo)
}