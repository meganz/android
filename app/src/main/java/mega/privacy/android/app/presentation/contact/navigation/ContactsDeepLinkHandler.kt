package mega.privacy.android.app.presentation.contact.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.PENDING_CONTACTS_LINK
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey.NavType
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Deep link handler for contacts
 */
class ContactsDeepLinkHandler @Inject constructor(
    private val snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        PENDING_CONTACTS_LINK -> if (isLoggedIn) {
            listOf(ContactsNavKey(NavType.ReceivedRequests))
        } else {
            snackbarEventQueue.queueMessage(sharedR.string.general_alert_not_logged_in)
            emptyList()
        }

        else -> {
            null
        }
    }
}