package mega.privacy.android.app.presentation.settings.exportrecoverykey

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.navigation.contract.deeplinks.AbstractDeepLinkHandlerRegexPatternType
import mega.privacy.android.navigation.destination.LegacyExportRecoveryKeyNavKey
import javax.inject.Inject

/**
 * DeepLinkHandler for export recovery key
 */
class ExportRecoveryKeyDeepLinkHandler @Inject constructor(
    getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
) : AbstractDeepLinkHandlerRegexPatternType(getDecodedUrlRegexPatternTypeUseCase) {
    override suspend fun getNavKeysFromRegexPatternType(
        regexPatternType: RegexPatternType,
        uri: Uri,
    ): List<NavKey>? {
        return if (regexPatternType == RegexPatternType.EXPORT_MASTER_KEY_LINK) {
            listOf(LegacyExportRecoveryKeyNavKey)
        } else {
            null
        }
    }

}