package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.contact.contract.AddContactsContract
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient

@Composable
internal fun FileContactHomeScreen(
    state: FileContactListState,
    onBackPressed: () -> Unit,
    removeContacts: (List<ShareRecipient>) -> Unit,
    shareFolder: (List<String>, AccessPermission) -> Unit,
    updatePermissions: (List<ShareRecipient>, AccessPermission) -> Unit,
    shareRemovedEventHandled: () -> Unit,
    shareCompletedEventHandled: () -> Unit,
    navigateToInfo: (ShareRecipient) -> Unit,
    modifier: Modifier = Modifier,
    addContactsContract: AddContactsContract = AddContactsContract(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    when (state) {
        is FileContactListState.Loading -> {
            FileContactLoadingScreen(
                state = state,
                onBackPressed = onBackPressed,
                modifier = modifier,
            )
        }

        is FileContactListState.Data -> {
            FileContactScreen(
                state = state,
                onBackPressed = onBackPressed,
                removeContacts = removeContacts,
                shareFolder = shareFolder,
                updatePermissions = updatePermissions,
                shareRemovedEventHandled = shareRemovedEventHandled,
                shareCompletedEventHandled = shareCompletedEventHandled,
                navigateToInfo = navigateToInfo,
                modifier = modifier,
                addContactsContract = addContactsContract,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}