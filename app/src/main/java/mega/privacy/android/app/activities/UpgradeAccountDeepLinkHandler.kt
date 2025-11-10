package mega.privacy.android.app.activities

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.navigation.contract.deeplinks.AbstractDeepLinkHandlerRegexPatternType
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import javax.inject.Inject

/**
 * Deep link handler for upgrade account link
 */
class UpgradeAccountDeepLinkHandler @Inject constructor(
    getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
) : AbstractDeepLinkHandlerRegexPatternType(getDecodedUrlRegexPatternTypeUseCase) {
    override suspend fun getNavKeysFromRegexPatternType(
        regexPatternType: RegexPatternType,
        uri: Uri,
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