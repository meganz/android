package mega.privacy.android.app.presentation.contact.link.dialog

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.contact.link.ContactLinkDialogViewModel
import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.ContactInfoNavKey

@Serializable
data class ContactLinkDialogNavKey(val contactLinkQueryResult: ContactLinkQueryResult) : NavKey

data object ContactLinkDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            contactLinkDialogDestination(
                navigateBack = navigationHandler::back,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<NavKey>.contactLinkDialogDestination(
    navigateBack: () -> Unit,
    navigate: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<ContactLinkDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        val viewModel =
            hiltViewModel<ContactLinkDialogViewModel, ContactLinkDialogViewModel.Factory>(
                creationCallback = { factory -> factory.create(key) }
            )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ContactLinkDialog(
            uiState = uiState,
            inviteContact = viewModel::inviteContact,
            viewContact = {
                key.contactLinkQueryResult.email?.let { email ->
                    navigate(ContactInfoNavKey(email))
                }
            },
            onDismiss = {
                onDialogHandled()
                navigateBack()
            },
            navigateToContactRequests = navigate,
        )
    }
}