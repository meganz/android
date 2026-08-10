package mega.privacy.android.feature.photos.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumTitle
import javax.inject.Inject

class AlbumTitleStringMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(albumTitle: AlbumTitle): String {
        return albumTitle.getTitleString(context)
    }
}