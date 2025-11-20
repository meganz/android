package mega.privacy.android.app.presentation.filelink

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.destination.LegacyFileLinkNavKey
import javax.inject.Inject

/**
 * DeepLinkHandler for file links
 */
class FileLinkDeepLinkHandler @Inject constructor() : DeepLinkHandler {

    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = if (regexPatternType == RegexPatternType.FILE_LINK) {
        listOf(LegacyFileLinkNavKey(uri.toString()))
    } else {
        null
    }
}