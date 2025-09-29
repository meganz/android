package mega.privacy.android.app.presentation.contact.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.navigation.destination.ContactsNavKey

fun NavGraphBuilder.contactsLegacyDestination(removeDestination: () -> Unit) {
    composable<ContactsNavKey> {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(ContactsActivity.getReceivedRequestsIntent(context))
            removeDestination()
        }
    }
}

