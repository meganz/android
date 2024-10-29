package mega.privacy.android.app.presentation.photos.albums

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.presentation.photos.albums.model.AlbumTitle
import javax.inject.Inject

internal class AlbumTitleStringMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(albumTitle: AlbumTitle): String {
        return albumTitle.getTitleString(context)
    }
}
