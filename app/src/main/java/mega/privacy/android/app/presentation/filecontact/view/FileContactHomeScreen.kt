package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileContactHomeScreen(
    state: FileContactListState,
    newShareRecipients: List<String>?,
    clearNewShareRecipients: () -> Unit,
    onBackPressed: () -> Unit,
    removeContacts: (List<ShareRecipient>) -> Unit,
    shareFolder: (List<String>, AccessPermission) -> Unit,
    updatePermissions: (List<ShareRecipient>, AccessPermission) -> Unit,
    shareRemovedEventHandled: () -> Unit,
    shareCompletedEventHandled: () -> Unit,
    navigateToInfo: (ShareRecipient) -> Unit,
    addContact: (Long) -> Unit,
    modifier: Modifier = Modifier,
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
                updatePermissions = updatePermissions,
                shareRemovedEventHandled = shareRemovedEventHandled,
                shareCompletedEventHandled = shareCompletedEventHandled,
                navigateToInfo = navigateToInfo,
                addContact = addContact,
                modifier = modifier,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
            )

            if (newShareRecipients != null) {
                if (state.accessPermissions.all { it == AccessPermission.READ }) {
                    val onPositiveButtonClicked: () -> Unit = {
                        shareFolder(
                            newShareRecipients,
                            AccessPermission.READ
                        )
                        clearNewShareRecipients()
                    }
                    BackupNodeShareWarningDialog(
                        onPositiveButtonClicked = onPositiveButtonClicked,
                        onDismiss = clearNewShareRecipients
                    )
                } else {
                    SetNewSharePermissionBottomSheet(
                        onDismissSheet = clearNewShareRecipients,
                        shareWithPermission = { permission: AccessPermission ->
                            shareFolder(
                                newShareRecipients,
                                permission
                            )
                        },
                        coroutineScope = coroutineScope
                    )
                }
            }
        }
    }
}