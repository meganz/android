package mega.privacy.android.app.presentation.folderlink

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.NavOptions
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.navOptions
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.FolderLinkNavKey
import javax.inject.Inject

/**
 * DeepLinkHandler for folder links
 */
class FolderLinkDeepLinkHandler @Inject constructor(
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {

    override val navOptions: NavOptions = navOptions {
        popUpTo<FolderLinkNavKey> {
            inclusive = true
        }
    }

    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? {
        return if (regexPatternType == RegexPatternType.FOLDER_LINK) {
            listOf(FolderLinkNavKey(uri.toString()))
        } else {
            null
        }
    }
}