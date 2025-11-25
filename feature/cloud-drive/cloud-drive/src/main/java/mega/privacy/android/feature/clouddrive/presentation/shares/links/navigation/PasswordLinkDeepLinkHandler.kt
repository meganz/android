package mega.privacy.android.feature.clouddrive.presentation.shares.links.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.feature.clouddrive.presentation.shares.links.OpenPasswordLinkDialogNavKey
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import javax.inject.Inject

/**
 * Password link deep link handler
 */
class PasswordLinkDeepLinkHandler @Inject constructor(
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? = when (regexPatternType) {
        RegexPatternType.PASSWORD_LINK -> {
            listOf(OpenPasswordLinkDialogNavKey(uri.toString()))
        }

        else -> null
    }
}
