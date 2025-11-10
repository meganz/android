package mega.privacy.android.app.presentation.settings

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.navigation.contract.deeplinks.AbstractDeepLinkHandlerRegexPatternType
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import javax.inject.Inject

/**
 * Deep Link Handler for Settings
 */
class SettingsDeepLinkHandler @Inject constructor(getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase) :
    AbstractDeepLinkHandlerRegexPatternType(getDecodedUrlRegexPatternTypeUseCase) {
    override suspend fun getNavKeysFromRegexPatternType(
        regexPatternType: RegexPatternType,
        uri: Uri,
    ): List<NavKey>? {
        return when (regexPatternType) {
            RegexPatternType.ENABLE_CAMERA_UPLOADS_LINK -> {
                listOf(SettingsCameraUploadsNavKey)
            }

            else -> null
        }
    }
}