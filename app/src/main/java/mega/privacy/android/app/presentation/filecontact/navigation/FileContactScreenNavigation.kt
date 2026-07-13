package mega.privacy.android.app.presentation.filecontact.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.filecontact.ShareRecipientsViewModel
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.app.presentation.filecontact.view.FileContactHomeScreen
import mega.privacy.android.navigation.destination.AddContactToShareNavKey
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import mega.privacy.mobile.analytics.event.FileContactListScreenViewEvent

internal fun EntryProviderScope<NavKey>.fileContacts(
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    resultFlow: (String) -> Flow<List<String>?>,
    clearResults: (String) -> Unit,
) {
    entry<FileContactInfoNavKey> { key ->
        val viewModel = hiltViewModel<ShareRecipientsViewModel, ShareRecipientsViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(
                    ShareRecipientsViewModel.Args(
                        folderHandle = key.folderHandle,
                        folderName = key.folderName,
                    )
                )
            }
        )
        LaunchedEffect(Unit) {
            Analytics.tracker.trackEvent(FileContactListScreenViewEvent)
        }
        val result by resultFlow(AddContactToShareNavKey.KEY).collectAsStateWithLifecycle(
            null
        )

        var newShareRecipients: List<String>? by remember { mutableStateOf(null) }

        LaunchedEffect(result) {
            newShareRecipients = result
        }

        val state by viewModel.state.collectAsStateWithLifecycle()

        (state as? FileContactListState.Data)?.let { data ->
            EventEffect(
                event = data.navigateToAddContactEvent,
                onConsumed = viewModel::clearAddContactState,
            ) { handle ->
                onNavigate(
                    AddContactToShareNavKey(
                        contactType = AddContactToShareNavKey.ContactType.All,
                        nodeHandle = listOf(handle),
                    )
                )
            }
        }

        FileContactHomeScreen(
            state = state,
            newShareRecipients = newShareRecipients,
            clearNewShareRecipients = {
                newShareRecipients = null
                clearResults(AddContactToShareNavKey.KEY)
            },
            onBackPressed = onNavigateBack,
            removeContacts = viewModel::removeShare,
            shareFolder = viewModel::shareFolder,
            updatePermissions = viewModel::changePermissions,
            shareRemovedEventHandled = viewModel::onShareRemovedEventHandled,
            shareCompletedEventHandled = viewModel::onSharingCompletedEventHandled,
            navigateToInfo = { onNavigate(ContactInfoNavKey(it.email)) },
            addContact = { viewModel.onAddContactClicked() },
            onShareHiddenNodeWarningConfirmed = viewModel::onShareHiddenNodeWarningConfirmed,
            onShareHiddenNodeWarningDismissed = viewModel::clearAddContactState,
        )
    }
}