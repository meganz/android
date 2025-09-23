package mega.privacy.android.app.presentation.filecontact.view

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
import androidx.compose.ui.res.stringResource
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContentConsumed
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.components.button.PrimaryLargeIconButton
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.filecontact.model.FileContactListState
import mega.privacy.android.app.presentation.filecontact.model.SelectionState
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileContactScreen(
    state: FileContactListState.Data,
    onBackPressed: () -> Unit,
    removeContacts: (List<ShareRecipient>) -> Unit,
    addContact: (Long) -> Unit,
    updatePermissions: (List<ShareRecipient>, AccessPermission) -> Unit,
    shareRemovedEventHandled: () -> Unit,
    shareCompletedEventHandled: () -> Unit,
    navigateToInfo: (ShareRecipient) -> Unit,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {

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
                shareFolder = { addContact(state.folderId.longValue) },
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
                onClick = { addContact(state.folderId.longValue) },
            )
        }
    ) { paddingValues ->
        ColumnSurface(
            surfaceColor = SurfaceColor.PageBackground,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            if (shouldDisplayVerificationBanner(state)) {
                TopWarningBanner(
                    modifier = Modifier,
                    body = stringResource(id = R.string.contact_share_file_to_unverified_contact_warning_message),
                    showCancelButton = false,
                )
            }

            ShareRecipientsListView(
                items = state.recipients,
                onRecipientClick = onRecipientClick,
                onRecipientLongClick = onRecipientLongClick,
                onOptionsClick = onOptionsClick,
                selectedItems = selectedItems
            )

            updatePermissions?.let { recipients ->
                val onDismiss = {
                    updatePermissions = null
                    selectedItems = emptyList()
                }
                if (state.accessPermissions.all { it == AccessPermission.READ }) {
                    BackupNodeShareWarningDialog(
                        onDismiss = onDismiss
                    )
                } else {
                    SetNewSharePermissionBottomSheet(
                        onDismissSheet = onDismiss,
                        shareWithPermission = { permission: AccessPermission ->
                            updatePermissions(
                                recipients,
                                permission
                            )
                        },
                        coroutineScope = coroutineScope
                    )
                }
            }

            displayOptionsRecipient?.let { recipient ->
                FileContactListOptionsBottomSheet(
                    recipient = recipient,
                    onDismissSheet = { displayOptionsRecipient = null },
                    navigateToInfo = { navigateToInfo(recipient) },
                    updatePermissions = { updatePermissions = listOf(recipient) },
                    removeShare = {
                        verifyRemoval = listOf(recipient)
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
                    onDismiss = {
                        verifyRemoval = null
                        selectedItems = emptyList()
                    },
                    removeContacts = removeContacts
                )
            }
        }
    }

}

private fun shouldDisplayVerificationBanner(state: FileContactListState.Data) =
    state.isContactVerificationWarningEnabled && state.recipients.any { it.isVerified.not() }

@CombinedThemePreviews
@Composable
private fun FileContactScreenPreview() {
    AndroidThemeForPreviews {
        FileContactScreen(
            state = FileContactListState.Data(
                folderId = NodeId(123456789L),
                folderName = "Folder name",
                recipients = listOf(
                    ShareRecipient.Contact(
                        handle = 123456789L,
                        email = "test@mega.com",
                        contactData = ContactData(
                            fullName = "Test",
                            alias = "User",
                            avatarUri = null,
                            userVisibility = UserVisibility.Visible,
                        ),
                        isVerified = true,
                        permission = AccessPermission.READ,
                        isPending = false,
                        status = UserChatStatus.Online,
                        defaultAvatarColor = 0xFF0000,
                    )
                ).toImmutableList(),
                accessPermissions = setOf(AccessPermission.READ).toImmutableSet(),
                sharingInProgress = false,
                shareRemovedEvent = StateEventWithContentConsumed,
                sharingCompletedEvent = StateEventWithContentConsumed,
                isContactVerificationWarningEnabled = true,
            ),
            onBackPressed = {},
            removeContacts = {},
            updatePermissions = { _, _ -> },
            shareRemovedEventHandled = {},
            shareCompletedEventHandled = {},
            navigateToInfo = {},
            addContact = {},
            coroutineScope = rememberCoroutineScope(),
            snackbarHostState = remember { SnackbarHostState() },
            modifier = Modifier,
        )
    }
}

