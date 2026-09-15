package mega.privacy.android.feature.contact.attachment.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentItem
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentMessage
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentPrimaryAction
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentUiState
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Contact attachment screen: lists the contacts attached to a chat message, offers the primary
 * invite/start-conversation action and, per row, a bottom sheet with the contextual actions.
 *
 * @param state
 * @param onNavigateBack invoked when the user navigates back.
 * @param onPrimaryActionClick invoked when the bottom primary action is selected.
 * @param onContactClicked invoked with the tapped row's handle so the view model can route to the
 * contact info screen or surface the "not a contact" feedback message.
 * @param onViewContactInfo invoked with a contact's email to open its contact info screen.
 * @param onStartConversation invoked with a contact's handle to start a one-to-one conversation.
 * @param onInviteContact invoked with a non-contact's email to send a contact invitation.
 * @param onMessageEventConsumed invoked once the feedback message event has been consumed.
 * @param modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactAttachmentScreen(
    state: ContactAttachmentUiState,
    onNavigateBack: () -> Unit,
    onPrimaryActionClick: () -> Unit,
    onContactClicked: (handle: Long) -> Unit,
    onViewContactInfo: (email: String) -> Unit,
    onStartConversation: (handle: Long) -> Unit,
    onInviteContact: (email: String) -> Unit,
    onMessageEventConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = LocalSnackBarHostState.current
    val notAContactMessage = stringResource(sharedR.string.alert_user_is_not_contact)
    val invitesSentMessage = stringResource(sharedR.string.contacts_invites_sent)
    val startConversationErrorMessage =
        stringResource(sharedR.string.contact_info_create_chat_error)

    if (state is ContactAttachmentUiState.Data) {
        EventEffect(
            event = state.messageEvent,
            onConsumed = onMessageEventConsumed,
        ) { message ->
            snackbarHostState?.showAutoDurationSnackbar(
                when (message) {
                    ContactAttachmentMessage.ContactsInvited -> invitesSentMessage
                    ContactAttachmentMessage.StartConversationError -> startConversationErrorMessage
                    ContactAttachmentMessage.UserNotAContact -> notAContactMessage
                }
            )
        }
    }

    var selectedContactHandle by rememberSaveable { mutableStateOf<Long?>(null) }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(CONTACT_ATTACHMENT_SCREEN_TAG),
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.activity_title_contacts_attached),
                navigationType = AppBarNavigationType.Back(onNavigateBack),
            )
        },
    ) { padding ->
        when (state) {
            ContactAttachmentUiState.Loading -> Unit

            is ContactAttachmentUiState.Data -> {
                val onRowClick: (ContactAttachmentItem) -> Unit = { contact ->
                    onContactClicked(contact.handle)
                }
                if (state.contacts.isEmpty()) {
                    EmptyStateView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .testTag(CONTACT_ATTACHMENT_EMPTY_TAG),
                        imagePainter = painterResource(id = iconPackR.drawable.ic_user_glass),
                        title = stringResource(sharedR.string.contact_attachment_empty_state),
                    )
                } else {
                    ContactAttachmentContent(
                        contacts = state.contacts,
                        primaryAction = state.primaryAction,
                        contentPadding = padding,
                        onRowClick = onRowClick,
                        onRowMore = { selectedContactHandle = it.handle },
                        onPrimaryActionClick = onPrimaryActionClick,
                    )
                }
            }
        }
    }

    val selectedContact = selectedContactHandle?.let { handle ->
        (state as? ContactAttachmentUiState.Data)?.contacts?.firstOrNull { it.handle == handle }
    }
    selectedContact?.let { contact ->
        ContactAttachmentActionsBottomSheet(
            contact = contact,
            onDismiss = { selectedContactHandle = null },
            onViewContactInfo = {
                selectedContactHandle = null
                if (contact.email.isNotBlank()) onViewContactInfo(contact.email)
            },
            onStartConversation = {
                selectedContactHandle = null
                onStartConversation(contact.handle)
            },
            onInviteContact = {
                selectedContactHandle = null
                if (contact.email.isNotBlank()) onInviteContact(contact.email)
            },
        )
    }
}

@Composable
private fun ContactAttachmentContent(
    contacts: List<ContactAttachmentItem>,
    primaryAction: ContactAttachmentPrimaryAction?,
    contentPadding: PaddingValues,
    onRowClick: (ContactAttachmentItem) -> Unit,
    onRowMore: (ContactAttachmentItem) -> Unit,
    onPrimaryActionClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(contentPadding)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(CONTACT_ATTACHMENT_LIST_TAG),
        ) {
            items(items = contacts, key = { it.handle }) { contact ->
                ContactItemView(
                    displayName = contact.displayName,
                    statusText = null,
                    status = ContactItemStatus.Unknown,
                    avatar = contact.avatar(),
                    isVerified = contact.isVerified,
                    onClick = { onRowClick(contact) },
                    onMoreClicked = { onRowMore(contact) },
                )
            }
        }
        primaryAction?.let { action ->
            PrimaryFilledButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag(CONTACT_ATTACHMENT_PRIMARY_ACTION_TAG),
                text = stringResource(action.labelRes()),
                onClick = onPrimaryActionClick,
            )
        }
    }
}

private fun ContactAttachmentPrimaryAction.labelRes(): Int = when (this) {
    ContactAttachmentPrimaryAction.Invite -> sharedR.string.menu_add_contact
    ContactAttachmentPrimaryAction.StartConversation ->
        sharedR.string.group_chat_start_conversation_label
}

private fun ContactAttachmentItem.avatar(): AvatarData = AvatarData.Initials(
    initials = displayName.trim().firstOrNull()?.uppercase().orEmpty(),
    avatarColor = Color(avatarColor),
)

internal const val CONTACT_ATTACHMENT_SCREEN_TAG = "contact_attachment_screen"
internal const val CONTACT_ATTACHMENT_LIST_TAG = "contact_attachment_screen:list"
internal const val CONTACT_ATTACHMENT_EMPTY_TAG = "contact_attachment_screen:empty"
internal const val CONTACT_ATTACHMENT_PRIMARY_ACTION_TAG =
    "contact_attachment_screen:primary_action"
