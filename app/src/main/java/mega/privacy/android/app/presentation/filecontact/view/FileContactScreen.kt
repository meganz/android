package mega.privacy.android.app.presentation.filecontact.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.button.PrimaryLargeIconButton
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.contract.AddContactsContract
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.app.presentation.filecontact.model.SelectionState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileContactScreen(
    state: FileContactListState.Data,
    onBackPressed: () -> Unit,
    removeContacts: (List<ShareRecipient>) -> Unit,
    shareFolder: (NodeId, List<String>, AccessPermission) -> Unit,
    updatePermissions: (NodeId, List<String>, AccessPermission) -> Unit,
    shareRemovedEventHandled: () -> Unit,
    shareCompletedEventHandled: () -> Unit,
    navigateToInfo: (ShareRecipient) -> Unit,
    modifier: Modifier = Modifier,
    addContactsContract: AddContactsContract = AddContactsContract(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    ) {
    var newShareRecipients: List<String>? by remember { mutableStateOf(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = addContactsContract
    ) { result ->
        coroutineScope.launch {
            result?.let {
                if (result.emails.isNotEmpty()) {
                    newShareRecipients = result.emails
                }
            }
        }
    }

    val onShareFolder = {
        launcher.launch(
            AddContactsContract.Input(
                contactType = AddContactsContract.ContactType.All,
                nodeHandle = listOf(state.folderId.longValue),
            )
        )
    }

    var selectedItems by remember { mutableStateOf(emptyList<ShareRecipient>()) }
    val selectionState = SelectionState(
        selectedCount = selectedItems.size,
        allSelected = selectedItems.size == state.recipients.size
    )
    val onRecipientClick = { recipient: ShareRecipient ->
        if (selectedItems.isNotEmpty()) {
            selectedItems = if (selectedItems.contains(recipient)) {
                selectedItems - recipient
            } else {
                selectedItems + recipient
            }
        } else {
            navigateToInfo(recipient)
        }
    }

    val onRecipientLongClick = { recipient: ShareRecipient ->
        selectedItems = if (selectedItems.isEmpty()) {
            listOf(recipient)
        } else {
            if (selectedItems.contains(recipient)) {
                selectedItems - recipient
            } else {
                selectedItems + recipient
            }
        }
    }

    var verifyRemoval: List<ShareRecipient>? by remember { mutableStateOf(null) }

    val selectAll = {
        selectedItems = state.recipients
    }
    val deselectAll = {
        selectedItems = emptyList()
    }
    var updatePermissions: List<ShareRecipient>? by remember { mutableStateOf(null) }
    val changePermissions = {
        updatePermissions = selectedItems
    }

    val removeShare = {
        verifyRemoval = selectedItems
    }

    var displayOptionsRecipient: ShareRecipient? by remember { mutableStateOf(null) }
    val onOptionsClick = { recipient: ShareRecipient ->
        displayOptionsRecipient = recipient
    }


    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            FileContactListTopBar(
                folderName = state.folderName,
                selectionState = selectionState,
                onBackPressed = onBackPressed,
                selectAll = selectAll,
                deselectAll = deselectAll,
                changePermissions = changePermissions,
                shareFolder = onShareFolder,
                removeShare = removeShare,
            )
        },
        snackbarHost = {
            MegaSnackbar(
                snackBarHostState = snackbarHostState,
            )
        },
        floatingActionButton = {
            PrimaryLargeIconButton(
                icon = painterResource(id = R.drawable.ic_add_white),
                onClick = onShareFolder,
            )
        }
    ) { paddingValues ->
        BoxSurface(
            surfaceColor = SurfaceColor.PageBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            ShareRecipientsListView(
                items = state.recipients,
                onRecipientClick = onRecipientClick,
                onRecipientLongClick = onRecipientLongClick,
                onOptionsClick = onOptionsClick,
                selectedItems = selectedItems
            )

            if (newShareRecipients != null) {
                SetNewSharePermissionBottomSheet(
                    onDismissSheet = {
                        newShareRecipients = null
                    },
                    shareWithPermission = { permission: AccessPermission ->
                        newShareRecipients?.let {
                            shareFolder(
                                state.folderId,
                                it,
                                permission
                            )
                        }
                    },
                    coroutineScope = coroutineScope
                )
            }

            updatePermissions?.let {
                SetNewSharePermissionBottomSheet(
                    onDismissSheet = {
                        updatePermissions = null
                        selectedItems = emptyList()
                    },
                    shareWithPermission = { permission: AccessPermission ->
                        updatePermissions(
                            state.folderId,
                            it.map { it.email },
                            permission
                        )
                    },
                    coroutineScope = coroutineScope
                )
            }

            displayOptionsRecipient?.let {
                FileContactListOptionsBottomSheet(
                    recipient = it,
                    onDismissSheet = { displayOptionsRecipient = null },
                    navigateToInfo = { navigateToInfo(it) },
                    updatePermissions = { updatePermissions = listOf(it) },
                    removeShare = {
                        verifyRemoval = listOf(it)
                    },
                    coroutineScope = coroutineScope
                )
            }

            EventEffect(
                event = state.shareRemovedEvent,
                onConsumed = shareRemovedEventHandled,
            ) { result ->
                snackbarHostState.showSnackbar(result)
            }

            EventEffect(
                event = state.sharingCompletedEvent,
                onConsumed = shareCompletedEventHandled,
            ) { result ->
                snackbarHostState.showSnackbar(result)
            }

            if (state.sharingInProgress) {
                ShareInProgressDialog()
            }

            verifyRemoval?.let {
                VerifyRemovalDialog(
                    selectedItems = it,
                    onDismiss = { verifyRemoval = null },
                    removeContacts = removeContacts
                )
            }
        }
    }

}

