package mega.privacy.android.app.presentation.contact.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ContactsNavKey

/**
 * Navigation destination for ContactsActivity that handles all three entry points:
 * - List: Shows the contact list (default)
 * - SentRequests: Shows sent contact requests
 * - ReceivedRequests: Shows received contact requests
 * 
 * Usage examples:
 * - Navigate to contact list: navController.navigate(ContactsNavKey())
 * - Navigate to sent requests: navController.navigate(ContactsNavKey(ContactsNavKey.ContactsNavType.SentRequests))
 * - Navigate to received requests: navController.navigate(ContactsNavKey(ContactsNavKey.NavType.ReceivedRequests))
 */
fun EntryProviderScope<NavKey>.contactsLegacyDestination(removeDestination: () -> Unit) {
    entry<ContactsNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current

        LaunchedEffect(key.navType) {
            val intent = when (key.navType) {
                ContactsNavKey.NavType.List -> ContactsActivity.getListIntent(context)
                ContactsNavKey.NavType.SentRequests -> ContactsActivity.getSentRequestsIntent(
                    context
                )
                ContactsNavKey.NavType.ReceivedRequests -> ContactsActivity.getReceivedRequestsIntent(
                    context
                )
            }
            context.startActivity(intent)
            removeDestination()
        }
    }
}

