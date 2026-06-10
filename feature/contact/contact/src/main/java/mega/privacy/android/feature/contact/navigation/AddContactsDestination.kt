package mega.privacy.android.feature.contact.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.contact.add.AddContactViewModel
import mega.privacy.android.feature.contact.add.view.AddContactsScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.AddContactsNavKey

/**
 * Add contacts entry. Renders the Compose MEGA-contacts multi-select picker and publishes the
 * selected contact emails as a `List<String>` under [AddContactsNavKey.KEY] when confirmed.
 *
 * Hosted by the app module's gated `AddContactsNavKey` destination (behind `ContactsComposeUI`).
 *
 * @param navigationHandler
 * @param viewModel
 */
@Composable
fun AddContactsEntry(
    navigationHandler: NavigationHandler,
    viewModel: AddContactViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AddContactsScreen(
        state = state,
        onSearchQueryChange = viewModel::setQuery,
        onConfirm = { handles ->
            navigationHandler.returnResult(
                AddContactsNavKey.KEY,
                viewModel.emailsForSelected(handles),
            )
        },
        onBack = { navigationHandler.remove(AddContactsNavKey) },
    )
}
