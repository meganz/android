package mega.privacy.android.app.activities

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import javax.inject.Inject

/**
 * Deep link handler for upgrade account link
 */
class UpgradeAccountDeepLinkHandler @Inject constructor() : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? =
        when (regexPatternType) {
            RegexPatternType.UPGRADE_PAGE_LINK, RegexPatternType.UPGRADE_LINK -> {
                listOf(UpgradeAccountNavKey())
            }

            else -> {
                null
            }
        }
}