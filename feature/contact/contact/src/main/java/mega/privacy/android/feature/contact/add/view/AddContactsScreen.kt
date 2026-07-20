package mega.privacy.android.feature.contact.add.view

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import mega.privacy.android.feature.contact.add.model.PhoneContactsSection
import mega.privacy.android.feature.contact.add.model.ScannedContactDialog
import mega.privacy.android.feature.contact.add.model.ScannedContactInviteFeedback
import mega.privacy.android.feature.contact.components.ContactListLoadingView
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.components.ScannedContactAlreadyAddedDialog
import mega.privacy.android.shared.contact.components.ScannedContactFoundDialog
import mega.privacy.android.shared.contact.components.ScannedContactInvalidCodeDialog
import mega.privacy.android.shared.contact.components.ScannerModuleNotInstalledDialog
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Add contacts screen. A MEGA-contacts multi-select picker: search, toggle selection, and
 * confirm to publish the selected contacts. Selection is owned locally via [rememberContactSelectionState]
 * so it survives search/filter changes.
 *
 * When [state] carries a non-[PhoneContactsSection.Hidden] phone section, a collapsible "Phone contacts"
 * section is rendered above the MEGA list.
 *
 * @param state
 * @param onSearchQueryChange invoked with the new query text, or null when the search is cleared.
 * @param onConfirm invoked with the handles of the selected MEGA contacts and the emails of the
 * selected phone contacts plus any manually entered emails.
 * @param onBack invoked when the user navigates back without confirming.
 * @param modifier
 * @param onReadContactsPermissionGranted invoked once READ_CONTACTS is granted (pre-picker path).
 * @param onContactsPicked invoked with the session Uri returned by the OS picker (picker path).
 * @param onPhoneContactsPickedConsumed invoked once the picked-contacts event has been auto-selected.
 * @param onScanQrClick invoked when the scan-QR toolbar action is clicked.
 * @param onScannedContactDialogDismissed invoked when the shown scanned-contact dialog is dismissed.
 * @param onInviteScannedContactConfirmed invoked when the invite action of the scanned-contact
 * found dialog is confirmed.
 * @param onScannedContactSelectConsumed invoked once the scanned contact has been auto-selected.
 * @param onScannedContactInviteConsumed invoked once the invite feedback has been surfaced.
 * @param allowManualEmailEntry whether to surface the free-text email entry (share flow only).
 * @param isManualEmailValid returns whether a typed email is syntactically valid.
 * @param megaContactHandleForEmail resolves a typed email to the handle of a loaded MEGA contact
 * (case-insensitively), or null when no loaded contact has that email.
 * @param initialSelectedHandles handles to pre-select on first composition.
 * @param initialSelectedManualEmails manual emails to pre-select on first composition. Primarily a
 * hook for previews/tests.
 * @param titleRes toolbar title shown while nothing is selected; defaults to "Send contacts".
 * @param startPhoneSectionExpanded initial expanded state of the phone-contacts section; defaults
 * to collapsed. Primarily a hook for previews/tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddContactsScreen(
    state: AddContactUiState,
    onSearchQueryChange: (String?) -> Unit,
    onConfirm: (selectedHandles: Set<Long>, selectedEmails: Set<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onReadContactsPermissionGranted: () -> Unit = {},
    onContactsPicked: (UriPath) -> Unit = {},
    onPhoneContactsPickedConsumed: () -> Unit = {},
    onScanQrClick: () -> Unit = {},
    onScannedContactDialogDismissed: () -> Unit = {},
    onInviteScannedContactConfirmed: () -> Unit = {},
    onScannedContactSelectConsumed: () -> Unit = {},
    onScannedContactInviteConsumed: () -> Unit = {},
    allowManualEmailEntry: Boolean = false,
    isManualEmailValid: (String) -> Boolean = { false },
    megaContactHandleForEmail: (String) -> Long? = { null },
    initialSelectedHandles: Set<Long> = emptySet(),
    initialSelectedManualEmails: Set<String> = emptySet(),
    @StringRes titleRes: Int = sharedR.string.send_contacts,
    startPhoneSectionExpanded: Boolean = false,
) {
    val selectionState = rememberContactSelectionState(
        initialSelectedHandles = initialSelectedHandles,
        initialSelectedManualEmails = initialSelectedManualEmails,
    )
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var phoneSectionExpanded by rememberSaveable { mutableStateOf(startPhoneSectionExpanded) }

    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) onReadContactsPermissionGranted()
    }
    val pickContactLauncher = rememberLauncherForActivityResult(PickPhoneContactContract()) { uri ->
        uri?.let { onContactsPicked(UriPath(it.toString())) }
    }

    if (state is AddContactUiState.Data) {
        EventEffect(
            event = state.phoneContactsPickedEvent,
            onConsumed = onPhoneContactsPickedConsumed,
        ) { addedEmails ->
            selectionState.selectPhoneEmails(addedEmails)
        }
        EventEffect(
            event = state.scannedContactSelectEvent,
            onConsumed = onScannedContactSelectConsumed,
        ) { handle ->
            selectionState.selectHandle(handle)
        }
        ScannedContactDialogs(
            dialog = state.scannedContactDialog,
            onInviteConfirmed = onInviteScannedContactConfirmed,
            onDismiss = onScannedContactDialogDismissed,
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
            .testTag(ADD_CONTACTS_SCREEN_TAG),
        topBar = {
            val title = if (selectionState.selectedItemsCount > 0) {
                pluralStringResource(
                    sharedR.plurals.general_selection_num_selected,
                    selectionState.selectedItemsCount,
                    selectionState.selectedItemsCount,
                )
            } else {
                stringResource(titleRes)
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
                        modifier = Modifier.testTag(ADD_CONTACTS_SCAN_QR_TAG),
                        onClick = onScanQrClick,
                    ) {
                        MegaIcon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(iconPackR.drawable.ic_qr_scan_medium_thin_outline),
                            contentDescription = stringResource(sharedR.string.contacts_qr_scan_action),
                            tint = IconColor.Primary,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state is AddContactUiState.Data && selectionState.selectedItemsCount > 0) {
                MegaFab(
                    modifier = Modifier
                        .testTag(ADD_CONTACTS_FAB_TAG)
                        .applyScrollToHideFabBehavior(),
                    onClick = {
                        onConfirm(
                            selectionState.selectedHandles,
                            selectionState.selectedPhoneEmails + selectionState.selectedManualEmails,
                        )
                    },
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.SendHorizontal),
                )
            }
        },
    ) { padding ->
        if (state is AddContactUiState.Data) {
            val snackbarHostState = LocalSnackBarHostState.current
            val inviteSentMessage = stringResource(sharedR.string.contacts_invites_sent)
            val inviteFailedMessage = stringResource(sharedR.string.general_text_error)
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
        }
        when (state) {
            AddContactUiState.Loading -> ContactListLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(ADD_CONTACTS_LOADING_TAG),
            )

            is AddContactUiState.Data -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    if (state.showUserLimitWarning) {
                        TopWarningBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(ADD_CONTACTS_USER_LIMIT_WARNING_TAG),
                            body = stringResource(sharedR.string.meetings_add_participants_user_limit_warning),
                            showCancelButton = false,
                        )
                    }
                    if (allowManualEmailEntry) {
                        ManualEmailEntrySection(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            manualEmails = selectionState.selectedManualEmails,
                            onSubmitEmail = { email ->
                                submitManualEmail(
                                    email = email,
                                    selectionState = selectionState,
                                    isManualEmailValid = isManualEmailValid,
                                    megaContactHandleForEmail = megaContactHandleForEmail,
                                )
                            },
                            onRemoveEmail = selectionState::removeManualEmail,
                        )
                    }
                    val phoneSection = state.phoneContactsSection
                    val megaListEmpty = state.isEmpty
                    if (megaListEmpty && phoneSection is PhoneContactsSection.Hidden) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(ADD_CONTACTS_EMPTY_TAG),
                            contentAlignment = Alignment.Center,
                        ) {
                            MegaText(
                                text = stringResource(sharedR.string.contacts_empty_title),
                                textColor = TextColor.Secondary,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(ADD_CONTACTS_LIST_TAG),
                        ) {
                            if (phoneSection !is PhoneContactsSection.Hidden) {
                                item(key = PHONE_SECTION_HEADER_KEY) {
                                    PhoneContactsSectionHeader(
                                        expanded = phoneSectionExpanded,
                                        onToggle = { phoneSectionExpanded = !phoneSectionExpanded },
                                    )
                                }
                                if (phoneSectionExpanded) {
                                    phoneSectionItems(
                                        section = phoneSection,
                                        selectedEmails = selectionState.selectedPhoneEmails,
                                        onAllowAccessClick = {
                                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                        },
                                        onSelectPhoneContactsClick = {
                                            pickContactLauncher.launch(Unit)
                                        },
                                        onPhoneContactClick = selectionState::togglePhoneSelection,
                                    )
                                }
                            }
                            if (phoneSection !is PhoneContactsSection.Hidden && state.contacts.isNotEmpty()) {
                                item(key = MEGA_SECTION_HEADER_KEY) {
                                    ContactsSectionHeader(
                                        title = stringResource(sharedR.string.add_contacts_mega_contacts_section_title),
                                        testTagValue = MEGA_SECTION_HEADER_TAG,
                                    )
                                }
                            }
                            items(state.contacts, key = { it.handle }) { contact ->
                                ContactItemView(
                                    contactItemUiState = contact,
                                    onClick = { selectionState.toggleSelection(contact.handle) },
                                    selected = contact.handle in selectionState.selectedHandles,
                                    inSelectionMode = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Resolve a typed email into a selection update: auto-select the matching loaded MEGA contact when
 * there is one, otherwise keep it as a free-text manual entry, rejecting invalid emails and emails
 * that are already part of the selection in any form.
 */
private fun submitManualEmail(
    email: String,
    selectionState: ContactSelectionState,
    isManualEmailValid: (String) -> Boolean,
    megaContactHandleForEmail: (String) -> Long?,
): ManualEmailSubmitResult {
    if (!isManualEmailValid(email)) return ManualEmailSubmitResult.InvalidEmail
    val matchedHandle = megaContactHandleForEmail(email)
    return when {
        matchedHandle != null && matchedHandle in selectionState.selectedHandles ->
            ManualEmailSubmitResult.AlreadyAdded

        matchedHandle != null -> {
            selectionState.selectHandle(matchedHandle)
            ManualEmailSubmitResult.Accepted
        }

        selectionState.isEmailSelected(email) -> ManualEmailSubmitResult.AlreadyAdded

        else -> {
            selectionState.selectManualEmail(email)
            ManualEmailSubmitResult.Accepted
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

@Composable
private fun PhoneContactsSectionHeader(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onToggle)
            .testTag(PHONE_SECTION_HEADER_TAG)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MegaText(
            text = stringResource(sharedR.string.add_contacts_phone_contacts_section_title),
            modifier = Modifier.weight(1f),
            textColor = TextColor.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        MegaIcon(
            modifier = Modifier
                .size(24.dp)
                .testTag(PHONE_SECTION_CHEVRON_TAG),
            painter = rememberVectorPainter(
                if (expanded) IconPack.Small.Thin.Outline.ChevronUp
                else IconPack.Small.Thin.Outline.ChevronDown
            ),
            contentDescription = null,
            tint = IconColor.Secondary,
        )
    }
}

@Composable
private fun ContactsSectionHeader(
    title: String,
    testTagValue: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTagValue)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            text = title,
            modifier = Modifier.weight(1f),
            textColor = TextColor.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

        PhoneContactsSection.PermissionRequired -> item(key = PHONE_SECTION_ALLOW_ACCESS_KEY) {
            PhoneContactsCtaRow(
                text = stringResource(sharedR.string.add_contacts_phone_contacts_allow_access),
                testTagValue = PHONE_SECTION_ALLOW_ACCESS_TAG,
                onClick = onAllowAccessClick,
            )
        }

        is PhoneContactsSection.PickerAvailable -> {
            item(key = PHONE_SECTION_SELECT_KEY) {
                PhoneContactsCtaRow(
                    text = stringResource(sharedR.string.add_contacts_phone_contacts_select),
                    testTagValue = PHONE_SECTION_SELECT_TAG,
                    onClick = onSelectPhoneContactsClick,
                )
            }
            phoneContactRows(section.picked, selectedEmails, onPhoneContactClick)
        }

        is PhoneContactsSection.Loaded ->
            phoneContactRows(section.contacts, selectedEmails, onPhoneContactClick)
    }
}

private fun LazyListScope.phoneContactRows(
    contacts: List<ContactItemUiState>,
    selectedEmails: Set<String>,
    onPhoneContactClick: (String) -> Unit,
) {
    items(contacts, key = { it.email }) { contact ->
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

internal const val ADD_CONTACTS_SCREEN_TAG = "add_contacts_screen"
internal const val ADD_CONTACTS_LOADING_TAG = "add_contacts_screen:loading"
internal const val ADD_CONTACTS_LIST_TAG = "add_contacts_screen:list"
internal const val ADD_CONTACTS_EMPTY_TAG = "add_contacts_screen:empty"
internal const val ADD_CONTACTS_FAB_TAG = "add_contacts_screen:fab"
internal const val ADD_CONTACTS_USER_LIMIT_WARNING_TAG = "add_contacts_screen:user_limit_warning"
internal const val ADD_CONTACTS_SCAN_QR_TAG = "add_contacts_screen:scan_qr"
internal const val PHONE_SECTION_HEADER_TAG = "add_contacts_screen:phone_section_header"
internal const val PHONE_SECTION_CHEVRON_TAG = "add_contacts_screen:phone_section_chevron"
internal const val PHONE_SECTION_ALLOW_ACCESS_TAG = "add_contacts_screen:phone_section_allow_access"
internal const val PHONE_SECTION_SELECT_TAG = "add_contacts_screen:phone_section_select"
internal const val MEGA_SECTION_HEADER_TAG = "add_contacts_screen:mega_section_header"

private const val PHONE_SECTION_HEADER_KEY = "phone_section_header"
private const val MEGA_SECTION_HEADER_KEY = "mega_section_header"
private const val PHONE_SECTION_ALLOW_ACCESS_KEY = "phone_section_allow_access"
private const val PHONE_SECTION_SELECT_KEY = "phone_section_select"

private class AddContactUiStateProvider : PreviewParameterProvider<AddContactUiState> {
    override val values: Sequence<AddContactUiState> = sequenceOf(
        AddContactUiState.Loading,
        AddContactUiState.Data(
            contacts = persistentListOf(),
            query = null,
            showUserLimitWarning = false,
            phoneContactsSection = PhoneContactsSection.Hidden,
            phoneContactsPickedEvent = de.palm.composestateevents.consumed(),
            scannedContactDialog = null,
            scannedContactSelectEvent = de.palm.composestateevents.consumed(),
            scannedContactInviteEvent = de.palm.composestateevents.consumed(),
        ),
        AddContactUiState.Data(
            contacts = listOf(
                ContactItemUiState(
                    handle = 1L,
                    displayName = "Alice Anderson",
                    status = ContactItemStatus.Online,
                    lastSeen = null,
                    avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                    isVerified = true,
                    email = "alice@example.com",
                ),
                ContactItemUiState(
                    handle = 2L,
                    displayName = "Bob Brown",
                    status = ContactItemStatus.Away,
                    lastSeen = 45,
                    avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
                    isVerified = false,
                    email = "bob@example.com",
                ),
            ).toImmutableList(),
            query = null,
            showUserLimitWarning = false,
            phoneContactsSection = PhoneContactsSection.Hidden,
            phoneContactsPickedEvent = de.palm.composestateevents.consumed(),
            scannedContactDialog = null,
            scannedContactSelectEvent = de.palm.composestateevents.consumed(),
            scannedContactInviteEvent = de.palm.composestateevents.consumed(),
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun AddContactsScreenPreview(
    @PreviewParameter(AddContactUiStateProvider::class) state: AddContactUiState,
) {
    AndroidThemeForPreviews {
        AddContactsScreen(
            state = state,
            onSearchQueryChange = {},
            onConfirm = { _, _ -> },
            onBack = {},
        )
    }
}
