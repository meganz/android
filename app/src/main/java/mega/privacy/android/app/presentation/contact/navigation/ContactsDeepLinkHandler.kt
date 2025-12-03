package mega.privacy.android.app.presentation.contact.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.contact.link.dialog.ContactLinkDialogNavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CONTACT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.PENDING_CONTACTS_LINK
import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.contact.ContactLinkQueryFromLinkUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey.NavType
import mega.privacy.android.navigation.destination.LegacyOpenLinkAfterFetchNodes
import mega.privacy.android.navigation.destination.WebSiteNavKey
import javax.inject.Inject

/**
 * Deep link handler for contacts
 */
class ContactsDeepLinkHandler @Inject constructor(
    private val contactLinkQueryFromLinkUseCase: ContactLinkQueryFromLinkUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? = when (regexPatternType) {
        PENDING_CONTACTS_LINK -> listOf(ContactsNavKey(NavType.ReceivedRequests))

        else -> null
    }

    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        CONTACT_LINK -> catchWithEmptyListAndLog {
            when {
                !isLoggedIn -> listOf(ContactLinkDialogNavKey(ContactLinkQueryResult()))
                !rootNodeExistsUseCase() -> listOf(LegacyOpenLinkAfterFetchNodes(uri.toString()))
                else -> contactLinkQueryFromLinkUseCase(uri.toString())?.let {
                    listOf(ContactLinkDialogNavKey(it))
                } ?: listOf(WebSiteNavKey(uri.toString()))
            }
        }

        else -> null
    }
}