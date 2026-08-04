package mega.privacy.android.feature.contact.requests.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.contact.requests.ContactRequestsViewModel
import mega.privacy.android.feature.contact.requests.model.ContactRequestTab
import mega.privacy.android.feature.contact.requests.view.ContactRequestsScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.ContactRequestsNavKey

/**
 * Contact requests entry. Renders the Compose contact requests screen, seeding the initially
 * selected tab from the [navType] the destination was opened on.
 *
 * Hosted by the app module's gated `ContactRequestsNavKey` destination (behind `ContactsComposeUI`).
 *
 * @param navigationHandler
 * @param navType the request type the screen opens on.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun ContactRequestsEntry(
    navigationHandler: NavigationHandler,
    navType: ContactRequestsNavKey.NavType,
) {
    val initialTab = when (navType) {
        ContactRequestsNavKey.NavType.ReceivedRequests -> ContactRequestTab.Received
        ContactRequestsNavKey.NavType.SentRequests -> ContactRequestTab.Sent
    }
    val viewModel =
        hiltViewModel<ContactRequestsViewModel, ContactRequestsViewModel.Factory> { factory ->
            factory.create(initialTab = initialTab)
        }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ContactRequestsScreen(
        state = state,
        onTabSelected = viewModel::selectTab,
        onItemAction = viewModel::handleAction,
        onBack = { navigationHandler.back() },
    )
}
