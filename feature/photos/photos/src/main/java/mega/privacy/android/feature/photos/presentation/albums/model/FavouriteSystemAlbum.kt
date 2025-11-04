package mega.privacy.android.feature.photos.presentation.albums.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.photos.FilterFavouriteUseCase
import mega.privacy.android.shared.resources.R as sharedResR
import javax.inject.Inject

/**
 * System album type for favourite photos
 */
class FavouriteSystemAlbum @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filterFavouriteUseCase: FilterFavouriteUseCase,
) : SystemAlbum {

    override val albumName: String =
        context.getString(sharedResR.string.system_album_favourites_title)

    override suspend fun filter(photo: Photo): Boolean = filterFavouriteUseCase()(photo)
}