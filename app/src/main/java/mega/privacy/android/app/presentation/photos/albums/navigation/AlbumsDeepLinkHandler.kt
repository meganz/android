package mega.privacy.android.app.presentation.photos.albums.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.ALBUM_LINK
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.destination.LegacyAlbumImportNavKey
import javax.inject.Inject

class AlbumsDeepLinkHandler @Inject constructor() : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? = when (regexPatternType) {
        ALBUM_LINK -> {
            listOf(LegacyAlbumImportNavKey(uri.toString()))
        }

        else -> {
            null
        }
    }
}