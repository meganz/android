package mega.privacy.android.feature.contact.invite.view

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.contact.components.ContactListLoadingView
import mega.privacy.android.feature.contact.invite.model.EmailValidationMessage
import mega.privacy.android.feature.contact.invite.model.InviteContactUiState
import mega.privacy.android.feature.contact.picker.ContactPickMode
import mega.privacy.android.feature.contact.picker.PhoneContactsSection
import mega.privacy.android.feature.contact.picker.PickPhoneContactContract
import mega.privacy.android.feature.contact.picker.ScannedContactDialog
import mega.privacy.android.feature.contact.picker.ScannedContactInviteFeedback
import mega.privacy.android.feature.contact.picker.rememberContactSelectionState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.components.ScannedContactAlreadyAddedDialog
import mega.privacy.android.shared.contact.components.ScannedContactFoundDialog
import mega.privacy.android.shared.contact.components.ScannedContactInvalidCodeDialog
import mega.privacy.android.shared.contact.components.ScannerModuleNotInstalledDialog
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Invite contacts screen. Lets the user gather recipients to invite to MEGA from device contacts
 * (the OS picker or a bulk-loaded list), free-text emails and phone numbers, then send the
 * invitations. Which recipients are selected is owned locally via [rememberContactSelectionState] so
 * it survives search/configuration changes; the ViewModel only surfaces device contacts to render
 * and one-shot events the screen folds into the selection.
 *
 * @param state
 * @param onSearchQueryChange invoked with the new query text, or null when the search is cleared.
 * @param onInvite invoked with the selected email and phone-number recipients when the send FAB is
 * tapped.
 * @param onBack invoked when the user navigates back.
 * @param modifier
 * @param onReadContactsPermissionGranted invoked once READ_CONTACTS is granted (pre-picker path).
 * @param onContactsPicked invoked with the session Uri returned by the OS picker (picker path).
 * @param onPhoneContactsPickedConsumed invoked once the picked-contacts event has been folded in.
 * @param onSubmitManualInput invoked with the raw text typed in the recipient field for validation.
 * @param onManualEmailAcceptedConsumed invoked once an accepted manual email has been folded in.
 * @param onManualPhoneAcceptedConsumed invoked once an accepted manual phone has been folded in.
 * @param onEmailValidationConsumed invoked once an email-validation message has been surfaced.
 * @param onScanClick invoked when the scan-QR toolbar action is tapped.
 * @param onScanConfirmed invoked when the ongoing-call confirmation to open the camera is accepted.
 * @param onOngoingCallConfirmConsumed invoked once the ongoing-call confirmation has been surfaced.
 * @param onScannedContactDialogDismissed invoked when the shown scanned-contact dialog is dismissed.
 * @param onInviteScannedContactConfirmed invoked when the scanned-contact found dialog is confirmed.
 * @param onScannedContactSelectConsumed invoked once a scanned contact has been auto-selected.
 * @param onScannedContactInviteConsumed invoked once scanned-contact invite feedback has been shown.
 * @param onShareLink invoked when the invite-via-link toolbar action is tapped (wired later).
 * @param onOpenMyQr invoked when the my-QR-code toolbar action is tapped (wired later).
 * @param initialSelectedHandles handles to pre-select on first composition. Primarily a preview/test hook.
 * @param initialSelectedPhoneEmails phone emails to pre-select on first composition. Preview/test hook.
 * @param initialSelectedManualEmails manual emails to pre-select on first composition. Preview/test hook.
 * @param initialSelectedPhoneNumbers phone numbers to pre-select on first composition. Preview/test hook.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InviteContactsScreen(
    state: InviteContactUiState,
    onSearchQueryChange: (String?) -> Unit,
    onInvite: (emails: Set<String>, phones: Set<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onReadContactsPermissionGranted: () -> Unit = {},
    onContactsPicked: (UriPath) -> Unit = {},
    onPhoneContactsPickedConsumed: () -> Unit = {},
    onSubmitManualInput: (String) -> Unit = {},
    onManualEmailAcceptedConsumed: () -> Unit = {},
    onManualPhoneAcceptedConsumed: () -> Unit = {},
    onEmailValidationConsumed: () -> Unit = {},
    onScanClick: () -> Unit = {},
    onScanConfirmed: () -> Unit = {},
    onOngoingCallConfirmConsumed: () -> Unit = {},
    onScannedContactDialogDismissed: () -> Unit = {},
    onInviteScannedContactConfirmed: () -> Unit = {},
    onScannedContactSelectConsumed: () -> Unit = {},
    onScannedContactInviteConsumed: () -> Unit = {},
    onShareLink: () -> Unit = {},
    onOpenMyQr: () -> Unit = {},
    initialSelectedHandles: Set<Long> = emptySet(),
    initialSelectedPhoneEmails: Set<String> = emptySet(),
    initialSelectedManualEmails: Set<String> = emptySet(),
    initialSelectedPhoneNumbers: Set<String> = emptySet(),
) {
    val selectionState = rememberContactSelectionState(
        initialSelectedHandles = initialSelectedHandles,
        initialSelectedPhoneEmails = initialSelectedPhoneEmails,
        initialSelectedManualEmails = initialSelectedManualEmails,
        initialSelectedPhoneNumbers = initialSelectedPhoneNumbers,
    )
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var showOpenCameraConfirm by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) onReadContactsPermissionGranted()
    }
    val pickContactLauncher = rememberLauncherForActivityResult(PickPhoneContactContract()) { uri ->
        uri?.let { onContactsPicked(UriPath(it.toString())) }
    }

    if (state is InviteContactUiState.Data) {
        EventEffect(
            event = state.phoneContactsPickedEvent,
            onConsumed = onPhoneContactsPickedConsumed,
        ) { picked ->
            selectionState.selectPhoneEmails(picked.emails)
            selectionState.selectPhoneNumbers(picked.phoneNumbers)
        }
        EventEffect(
            event = state.manualEmailAcceptedEvent,
            onConsumed = onManualEmailAcceptedConsumed,
        ) { email ->
            selectionState.selectManualEmail(email)
        }
        EventEffect(
            event = state.manualPhoneAcceptedEvent,
            onConsumed = onManualPhoneAcceptedConsumed,
        ) { phone ->
            selectionState.selectPhoneNumbers(listOf(phone))
        }
        EventEffect(
            event = state.scannedContactSelectEvent,
            onConsumed = onScannedContactSelectConsumed,
        ) { handle ->
            selectionState.selectHandle(handle)
        }
        EventEffect(
            event = state.ongoingCallConfirmEvent,
            onConsumed = onOngoingCallConfirmConsumed,
        ) {
            showOpenCameraConfirm = true
        }
        ScannedContactDialogs(
            dialog = state.scannedContactDialog,
            onInviteConfirmed = onInviteScannedContactConfirmed,
            onDismiss = onScannedContactDialogDismissed,
        )
    }

    if (showOpenCameraConfirm) {
        BasicDialog(
            modifier = Modifier.testTag(INVITE_CONTACTS_OPEN_CAMERA_DIALOG_TAG),
            title = stringResource(sharedR.string.invite_contacts_open_camera_confirmation_title),
            description = stringResource(sharedR.string.invite_contacts_open_camera_confirmation_message),
            positiveButtonText = stringResource(sharedR.string.invite_contacts_open_camera_confirmation_confirm),
            onPositiveButtonClicked = {
                showOpenCameraConfirm = false
                onScanConfirmed()
            },
            negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
            onNegativeButtonClicked = { showOpenCameraConfirm = false },
            onDismiss = { showOpenCameraConfirm = false },
        )
    }

    LaunchedEffect(searchActive) {
        if (!searchActive && searchText.isNotEmpty()) {
            searchText = ""
            onSearchQueryChange(null)
        }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(INVITE_CONTACTS_SCREEN_TAG),
        topBar = {
            val title = if (selectionState.selectedItemsCount > 0) {
                pluralStringResource(
                    sharedR.plurals.general_selection_num_contacts,
                    selectionState.selectedItemsCount,
                    selectionState.selectedItemsCount,
                )
            } else {
                stringResource(sharedR.string.invite_contacts_action_label)
            }
            MegaSearchTopAppBar(
                title = title,
                navigationType = AppBarNavigationType.Back(onBack),
                query = searchText,
                isSearchingMode = searchActive,
                onQueryChanged = {
                    searchText = it
                    onSearchQueryChange(it.ifBlank { null })
                },
                onSearchingModeChanged = { searchActive = it },
                searchPlaceholder = stringResource(sharedR.string.contacts_search_hint),
                trailingIcons = {
                    IconButton(
                        modifier = Modifier.testTag(INVITE_CONTACTS_SCAN_QR_TAG),
                        onClick = onScanClick,
                    ) {
                        MegaIcon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(iconPackR.drawable.ic_qr_scan_medium_thin_outline),
                            contentDescription = stringResource(sharedR.string.contacts_qr_scan_action),
                            tint = IconColor.Primary,
                        )
                    }
                    IconButton(
                        modifier = Modifier.testTag(INVITE_CONTACTS_SHARE_LINK_TAG),
                        onClick = onShareLink,
                    ) {
                        MegaIcon(
                            modifier = Modifier.size(24.dp),
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Link01),
                            contentDescription = stringResource(sharedR.string.invite_contacts_share_link_action),
                            tint = IconColor.Primary,
                        )
                    }
                    IconButton(
                        modifier = Modifier.testTag(INVITE_CONTACTS_MY_QR_CODE_TAG),
                        onClick = onOpenMyQr,
                    ) {
                        MegaIcon(
                            modifier = Modifier.size(24.dp),
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Qr),
                            contentDescription = stringResource(sharedR.string.invite_contacts_my_qr_code_action),
                            tint = IconColor.Primary,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state is InviteContactUiState.Data && selectionState.selectedItemsCount > 0) {
                MegaFab(
                    modifier = Modifier
                        .testTag(INVITE_CONTACTS_FAB_TAG)
                        .applyScrollToHideFabBehavior(),
                    onClick = {
                        onInvite(
                            selectionState.selectedPhoneEmails + selectionState.selectedManualEmails,
                            selectionState.selectedPhoneNumbers,
                        )
                    },
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.SendHorizontal),
                )
            }
        },
    ) { padding ->
        when (state) {
            InviteContactUiState.Loading -> ContactListLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(INVITE_CONTACTS_LOADING_TAG),
            )

            is InviteContactUiState.Data -> {
                val snackbarHostState = LocalSnackBarHostState.current
                val ownEmailMessage = stringResource(sharedR.string.invite_contacts_own_email_error)
                val alreadyContactMessage =
                    stringResource(sharedR.string.invite_contacts_already_contact_error)
                val pendingMessage = stringResource(sharedR.string.invite_contacts_pending_error)
                val invalidInputMessage =
                    stringResource(sharedR.string.invite_contacts_invalid_input_error)
                val inviteSentMessage = stringResource(sharedR.string.contacts_invites_sent)
                val inviteFailedMessage = stringResource(sharedR.string.general_text_error)
                EventEffect(
                    event = state.emailValidationEvent,
                    onConsumed = onEmailValidationConsumed,
                ) { message ->
                    snackbarHostState?.showAutoDurationSnackbar(
                        when (message) {
                            EmailValidationMessage.MyOwnEmail -> ownEmailMessage
                            EmailValidationMessage.AlreadyInContacts -> alreadyContactMessage
                            EmailValidationMessage.Pending -> pendingMessage
                            EmailValidationMessage.InvalidInput -> invalidInputMessage
                        }
                    )
                }
                EventEffect(
                    event = state.scannedContactInviteEvent,
                    onConsumed = onScannedContactInviteConsumed,
                ) { feedback ->
                    snackbarHostState?.showAutoDurationSnackbar(
                        when (feedback) {
                            ScannedContactInviteFeedback.Sent -> inviteSentMessage
                            ScannedContactInviteFeedback.Failed -> inviteFailedMessage
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    InviteRecipientsSection(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        emails = selectionState.selectedPhoneEmails + selectionState.selectedManualEmails,
                        phoneNumbers = selectionState.selectedPhoneNumbers,
                        onSubmitInput = onSubmitManualInput,
                        onRemoveEmail = { email ->
                            if (email in selectionState.selectedPhoneEmails) {
                                selectionState.togglePhoneSelection(email)
                            }
                            if (email in selectionState.selectedManualEmails) {
                                selectionState.removeManualEmail(email)
                            }
                        },
                        onRemovePhoneNumber = selectionState::removePhoneNumber,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(INVITE_CONTACTS_LIST_TAG),
                    ) {
                        phoneSectionItems(
                            section = state.phoneContactsSection,
                            selectedEmails = selectionState.selectedPhoneEmails,
                            onAllowAccessClick = {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            onSelectPhoneContactsClick = {
                                pickContactLauncher.launch(ContactPickMode.EmailAndPhone)
                            },
                            onPhoneContactClick = selectionState::togglePhoneSelection,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedContactDialogs(
    dialog: ScannedContactDialog?,
    onInviteConfirmed: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit

        ScannedContactDialog.InvalidCode ->
            ScannedContactInvalidCodeDialog(onDismiss = onDismiss)

        ScannedContactDialog.ScannerNotInstalled ->
            ScannerModuleNotInstalledDialog(onDismiss = onDismiss)

        is ScannedContactDialog.AlreadyAdded ->
            ScannedContactAlreadyAddedDialog(
                contactEmail = dialog.email,
                onDismiss = onDismiss,
            )

        is ScannedContactDialog.Found ->
            ScannedContactFoundDialog(
                contactName = dialog.contactName,
                contactEmail = dialog.email,
                avatar = dialog.avatar,
                confirmActionText = stringResource(sharedR.string.invite_contacts_action_label),
                onConfirm = onInviteConfirmed,
                onDismiss = onDismiss,
            )
    }
}

private fun LazyListScope.phoneSectionItems(
    section: PhoneContactsSection,
    selectedEmails: Set<String>,
    onAllowAccessClick: () -> Unit,
    onSelectPhoneContactsClick: () -> Unit,
    onPhoneContactClick: (String) -> Unit,
) {
    when (section) {
        PhoneContactsSection.Hidden -> Unit

        PhoneContactsSection.PermissionRequired -> item(key = INVITE_ALLOW_ACCESS_KEY) {
            PhoneContactsCtaRow(
                text = stringResource(sharedR.string.add_contacts_phone_contacts_allow_access),
                testTagValue = INVITE_CONTACTS_ALLOW_ACCESS_TAG,
                onClick = onAllowAccessClick,
            )
        }

        is PhoneContactsSection.PickerAvailable -> {
            item(key = INVITE_SELECT_KEY) {
                PhoneContactsCtaRow(
                    text = stringResource(sharedR.string.add_contacts_phone_contacts_select),
                    testTagValue = INVITE_CONTACTS_SELECT_TAG,
                    onClick = onSelectPhoneContactsClick,
                )
            }
            pickedContactRows(section.picked)
        }

        is PhoneContactsSection.Loaded ->
            loadedContactRows(section.contacts, selectedEmails, onPhoneContactClick)
    }
}

private fun LazyListScope.pickedContactRows(
    contacts: ImmutableList<ContactItemUiState>,
) {
    itemsIndexed(contacts, key = { index, contact -> "$index:${contact.email}" }) { _, contact ->
        ContactItemView(
            contactItemUiState = contact,
            onClick = null,
        )
    }
}

private fun LazyListScope.loadedContactRows(
    contacts: ImmutableList<ContactItemUiState>,
    selectedEmails: Set<String>,
    onPhoneContactClick: (String) -> Unit,
) {
    itemsIndexed(contacts, key = { _, contact -> contact.email }) { _, contact ->
        ContactItemView(
            contactItemUiState = contact,
            onClick = { onPhoneContactClick(contact.email) },
            selected = contact.email in selectedEmails,
            inSelectionMode = true,
        )
    }
}

@Composable
private fun PhoneContactsCtaRow(
    text: String,
    testTagValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .testTag(testTagValue)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MegaIcon(
            modifier = Modifier.size(24.dp),
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus),
            contentDescription = null,
            tint = IconColor.Accent,
        )
        MegaText(
            text = text,
            modifier = Modifier.weight(1f),
            textColor = TextColor.Accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val INVITE_ALLOW_ACCESS_KEY = "invite_allow_access"
private const val INVITE_SELECT_KEY = "invite_select"

internal fun inviteDataState(
    phoneContactsSection: PhoneContactsSection = PhoneContactsSection.PickerAvailable(persistentListOf()),
): InviteContactUiState.Data = InviteContactUiState.Data(
    phoneContactsSection = phoneContactsSection,
    query = null,
    contactLink = null,
    invitationResultEvent = consumed(),
    emailValidationEvent = consumed(),
    sendSmsEvent = consumed(),
    ongoingCallConfirmEvent = consumed,
    phoneContactsPickedEvent = consumed(),
    manualEmailAcceptedEvent = consumed(),
    manualPhoneAcceptedEvent = consumed(),
    scannedContactDialog = null,
    scannedContactSelectEvent = consumed(),
    scannedContactInviteEvent = consumed(),
)

private class InviteContactUiStateProvider : PreviewParameterProvider<InviteContactUiState> {
    override val values: Sequence<InviteContactUiState> = sequenceOf(
        InviteContactUiState.Loading,
        inviteDataState(),
    )
}

@CombinedThemePreviews
@Composable
private fun InviteContactsScreenPreview(
    @PreviewParameter(InviteContactUiStateProvider::class) state: InviteContactUiState,
) {
    AndroidThemeForPreviews {
        InviteContactsScreen(
            state = state,
            onSearchQueryChange = {},
            onInvite = { _, _ -> },
            onBack = {},
        )
    }
}
