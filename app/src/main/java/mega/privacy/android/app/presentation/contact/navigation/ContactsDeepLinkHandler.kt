package mega.privacy.android.app.presentation.contact.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.contact.link.dialog.ContactLinkDialogNavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CONTACT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.PENDING_CONTACTS_LINK
import mega.privacy.android.domain.usecase.contact.ContactLinkQueryFromLinkUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey.NavType
import mega.privacy.android.navigation.destination.WebSiteNavKey
import timber.log.Timber
import javax.inject.Inject

/**
 * Deep link handler for contacts
 */
class ContactsDeepLinkHandler @Inject constructor(
    private val contactLinkQueryFromLinkUseCase: ContactLinkQueryFromLinkUseCase,
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? = when (regexPatternType) {
        PENDING_CONTACTS_LINK -> listOf(ContactsNavKey(NavType.ReceivedRequests))
        CONTACT_LINK -> runCatching { contactLinkQueryFromLinkUseCase(uri.toString()) }
            .onFailure { Timber.e(it) }.getOrNull()?.let { contactLinkQueryResult ->
                listOf(ContactLinkDialogNavKey(contactLinkQueryResult))
            } ?: listOf(WebSiteNavKey(uri.toString()))

        else -> null
    }
}