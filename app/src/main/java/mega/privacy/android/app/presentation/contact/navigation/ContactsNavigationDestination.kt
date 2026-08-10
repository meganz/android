package mega.privacy.android.app.presentation.contact.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.analytics.decorator.withScreenViewEvent
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.feature.contact.list.view.ContactListScreen
import mega.privacy.android.feature.contact.navigation.ContactsEntry
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ContactRequestsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.mobile.analytics.event.ContactListScreenEvent

/**
 * Navigation destination for the contacts list. Behind
 * [AppFeatures.ContactsComposeUI] either renders the Compose
 * [ContactListScreen] inline (flag on) or launches the legacy
 * [ContactsActivity] and pops the entry (flag off).
 *
 * TODO: Move this entry to the feature module once the feature flag is removed
 */
fun EntryProviderScope<NavKey>.contactsListDestination(
    navigationHandler: NavigationHandler,
) {
    entry<ContactsNavKey>(
        metadata = buildMetadata {
            withScreenViewEvent(ContactListScreenEvent)
        }
    ) { key ->
        FeatureFlagGate(
            feature = AppFeatures.ContactsComposeUI,
            disabled = {
                LegacyContactsEntry({ navigationHandler.remove(key) })
            },
            enabled = {
                ContactsEntry(
                    navigationHandler = navigationHandler,
                )
            },
        )
    }
}

/**
 * Legacy contacts entry
 *
 * @param removeDestination
 */
@Composable
private fun LegacyContactsEntry(removeDestination: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.startActivity(ContactsActivity.getListIntent(context))
        removeDestination()
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
