package mega.privacy.android.app.presentation.login.createaccount

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import javax.inject.Inject

class CreateAccountDeepLinkHandler @Inject constructor(
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? =
        if (regexPatternType == RegexPatternType.REGISTRATION_LINK) {
            listOf(CreateAccountNavKey())
        } else {
            null
        }
}