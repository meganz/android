package mega.privacy.android.app.presentation.filecontact.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.filecontact.ShareRecipientsViewModel
import mega.privacy.android.app.presentation.filecontact.view.FileContactHomeScreen
import mega.privacy.android.navigation.destination.AddContactToShareNavKey
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import mega.privacy.mobile.analytics.event.FileContactListScreenViewEvent

internal fun NavGraphBuilder.fileContacts(
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    resultFlow: (String) -> Flow<List<String>?>,
) {
    composable<FileContactInfoNavKey> {
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

        val onShareFolder = { handle: Long ->
            onNavigate(
                AddContactToShareNavKey(
                    contactType = AddContactToShareNavKey.ContactType.All,
                    nodeHandle = listOf(handle),
                )
            )
        }

        val viewModel = hiltViewModel<ShareRecipientsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        FileContactHomeScreen(
            state = state,
            newShareRecipients = newShareRecipients,
            clearNewShareRecipients = { newShareRecipients = null },
            onBackPressed = onNavigateBack,
            removeContacts = viewModel::removeShare,
            shareFolder = viewModel::shareFolder,
            updatePermissions = viewModel::changePermissions,
            shareRemovedEventHandled = viewModel::onShareRemovedEventHandled,
            shareCompletedEventHandled = viewModel::onSharingCompletedEventHandled,
            navigateToInfo = { onNavigate(ContactInfoNavKey(it.email)) },
            addContact = onShareFolder,
        )
    }
}