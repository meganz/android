package mega.privacy.android.app.presentation.contact.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.app.contacts.ContactsActivity
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
 * - Navigate to received requests: navController.navigate(ContactsNavKey(ContactsNavKey.ContactsNavType.ReceivedRequests))
 */
fun NavGraphBuilder.contactsLegacyDestination(removeDestination: () -> Unit) {
    composable<ContactsNavKey> {
        val context = LocalContext.current
        val contactsNavKey = it.toRoute<ContactsNavKey>()

        LaunchedEffect(contactsNavKey.navType) {
            val intent = when (contactsNavKey.navType) {
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

