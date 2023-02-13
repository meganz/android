package mega.privacy.android.app.data.facade

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.data.facade.AlbumStringResourceGateway
import javax.inject.Inject

/**
 * Album string resource facade
 */
class AlbumStringResourceFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlbumStringResourceGateway {
    override fun getSystemAlbumNames(): List<String> = with(context) {
        listOf(
            getString(R.string.title_favourites_album),
            getString(R.string.photos_album_title_raw),
            getString(R.string.photos_album_title_gif),
        )
    }

    override fun getProscribedStrings(): List<String> = with(context) {
        listOf(
            getString(R.string.photos_album_subsection_my_albums),
            getString(R.string.photos_album_subsection_shared_albums),
        )
    }
}