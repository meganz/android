package mega.privacy.android.navigation.contract.deeplinks

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType

/**
 * Deep link handler interface for cases where [GetUrlRegexPatternTypeUseCase] can be used to avoid calling it multiple times.
 */
interface DeepLinkHandlerRegexPatternType : DeepLinkHandler {
    /**
     * Get the NavKeys from the given RegexPatternType
     * @param regexPatternType The RegexPatternType to check
     * @param uri the Uri to be used to build the NavKeys
     *
     * @return The NavKeys if the Uri is valid, null otherwise
     */
    suspend fun getNavKeysFromRegexPatternType(
        regexPatternType: RegexPatternType,
        uri: Uri,
    ): List<NavKey>?
}