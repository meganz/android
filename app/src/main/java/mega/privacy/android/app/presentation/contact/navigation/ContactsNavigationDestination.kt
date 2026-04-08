package mega.privacy.android.app.presentation.contact.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ContactRequestsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey

/**
 * Navigation destination for ContactsActivity that handles only the list entry point:
 */
fun EntryProviderScope<NavKey>.contactsLegacyDestination(removeDestination: () -> Unit) {
    entry<ContactsNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current

        LaunchedEffect(key) {
            val intent = ContactsActivity.getListIntent(context)
            context.startActivity(intent)
            removeDestination()
        }
    }
}

/**
 * Navigation destination for ContactsActivity that handles the requests entry points:
 * - SentRequests: Shows sent contact requests
 * - ReceivedRequests: Shows received contact requests
 *
 * Usage examples:
 * - Navigate to sent requests: navController.navigate(ContactsNavKey(ContactsNavKey.ContactsNavType.SentRequests))
 * - Navigate to received requests: navController.navigate(ContactsNavKey(ContactsNavKey.NavType.ReceivedRequests))
 */
fun EntryProviderScope<NavKey>.contactsRequestLegacyDestination(removeDestination: () -> Unit) {
    entry<ContactRequestsNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current

        LaunchedEffect(key.navType) {
            val intent = when (key.navType) {
                ContactRequestsNavKey.NavType.SentRequests -> ContactsActivity.getSentRequestsIntent(
                    context
                )

                ContactRequestsNavKey.NavType.ReceivedRequests -> ContactsActivity.getReceivedRequestsIntent(
                    context
                )
            }
            context.startActivity(intent)
            removeDestination()
        }
    }
}

