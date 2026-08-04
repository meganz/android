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
import mega.privacy.android.feature.contact.requests.navigation.ContactRequestsEntry
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.metadata.buildMetadata
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
 * Navigation destination for the contact requests screen. Behind
 * [AppFeatures.ContactsComposeUI] either renders the Compose contact requests screen inline
 * (flag on) or launches the legacy [ContactsActivity] requests entry points and pops the entry
 * (flag off):
 * - SentRequests: Shows sent contact requests
 * - ReceivedRequests: Shows received contact requests
 */
fun EntryProviderScope<NavKey>.contactRequestsDestination(
    navigationHandler: NavigationHandler,
) {
    entry<ContactRequestsNavKey> { key ->
        FeatureFlagGate(
            feature = AppFeatures.ContactsComposeUI,
            disabled = {
                LegacyContactRequestsEntry(
                    navType = key.navType,
                    removeDestination = { navigationHandler.remove(key) },
                )
            },
            enabled = {
                ContactRequestsEntry(
                    navigationHandler = navigationHandler,
                    navType = key.navType,
                )
            },
        )
    }
}

/**
 * Legacy contact requests entry
 *
 * @param navType the request type to open the legacy screen on.
 * @param removeDestination
 */
@Composable
private fun LegacyContactRequestsEntry(
    navType: ContactRequestsNavKey.NavType,
    removeDestination: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(navType) {
        val intent = when (navType) {
            ContactRequestsNavKey.NavType.SentRequests ->
                ContactsActivity.getSentRequestsIntent(context)

            ContactRequestsNavKey.NavType.ReceivedRequests ->
                ContactsActivity.getReceivedRequestsIntent(context)
        }
        context.startActivity(intent)
        removeDestination()
    }
}
