package mega.privacy.android.app.presentation.contact.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.PENDING_CONTACTS_LINK
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.navigation.contract.deeplinks.AbstractDeepLinkHandlerRegexPatternType
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey.NavType
import javax.inject.Inject

/**
 * Deep link handler for contacts
 */
class ContactsDeepLinkHandler @Inject constructor(
    getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
) : AbstractDeepLinkHandlerRegexPatternType(getDecodedUrlRegexPatternTypeUseCase) {
    override suspend fun getNavKeysFromRegexPatternType(
        regexPatternType: RegexPatternType,
        uri: Uri,
    ): List<NavKey>? {
        return when (regexPatternType) {
            PENDING_CONTACTS_LINK -> {
                listOf(ContactsNavKey(NavType.ReceivedRequests))
            }

            else -> {
                null
            }
        }
    }

}