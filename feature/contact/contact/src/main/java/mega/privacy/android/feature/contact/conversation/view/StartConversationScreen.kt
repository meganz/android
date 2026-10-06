package mega.privacy.android.feature.contact.conversation.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.list.PrimaryHeaderListItem
import mega.android.core.ui.components.list.SecondaryHeaderListItem
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.contact.components.ContactListLoadingView
import mega.privacy.android.feature.contact.conversation.model.NoteToSelfUiState
import mega.privacy.android.feature.contact.group.create.model.CreateChatUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.chats.components.ChatExplorerListItemView
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.mobile.analytics.event.CreateNoteToSelfButtonPressedEvent
import mega.privacy.mobile.analytics.event.GroupChatPressedEvent
import mega.privacy.mobile.analytics.event.InviteContactsPressedEvent
import java.util.Locale
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Start conversation screen, opened from the chat list to begin a new chat. Offers a new group
 * chat, inviting contacts, the note-to-self chat and the user's contacts; tapping a contact starts
 * a 1:1 chat straight away.
 *
 * @param contactsState the searchable contacts state.
 * @param noteToSelfState the note-to-self row state.
 * @param onSearchQueryChange invoked with the new query text, or null when the search is cleared.
 * @param onContactClick invoked with the handle of the tapped contact.
 * @param onNewGroupChatClick invoked when "New group chat" is tapped.
 * @param onInviteClick invoked when an invite action is tapped.
 * @param onNoteToSelfClick invoked with the note-to-self chat id when its row is tapped.
 * @param onBack invoked when the user navigates back out of the screen.
 * @param modifier
 */
@Composable
internal fun StartConversationScreen(
    contactsState: CreateChatUiState,
    noteToSelfState: NoteToSelfUiState,
    onSearchQueryChange: (String?) -> Unit,
    onContactClick: (Long) -> Unit,
    onNewGroupChatClick: () -> Unit,
    onInviteClick: () -> Unit,
    onNoteToSelfClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }

    StartConversationScreenContent(
        contactsState = contactsState,
        noteToSelfState = noteToSelfState,
        searchActive = searchActive,
        searchText = searchText,
        onSearchActiveChange = { active ->
            searchActive = active
            if (!active && searchText.isNotEmpty()) {
                searchText = ""
                onSearchQueryChange(null)
            }
        },
        onSearchTextChange = { text ->
            searchText = text
            onSearchQueryChange(text.ifBlank { null })
        },
        onContactClick = onContactClick,
        onNewGroupChatClick = onNewGroupChatClick,
        onInviteClick = onInviteClick,
        onNoteToSelfClick = onNoteToSelfClick,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartConversationScreenContent(
    contactsState: CreateChatUiState,
    noteToSelfState: NoteToSelfUiState,
    searchActive: Boolean,
    searchText: String,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onContactClick: (Long) -> Unit,
    onNewGroupChatClick: () -> Unit,
    onInviteClick: () -> Unit,
    onNoteToSelfClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contacts = (contactsState as? CreateChatUiState.Data)?.contacts
    val title = stringResource(sharedR.string.new_chat_title)

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(START_CONVERSATION_SCREEN_TAG),
        topBar = {
            // Searching an empty contact list is pointless, so search is only offered with contacts.
            if (searchActive || !contacts.isNullOrEmpty()) {
                MegaSearchTopAppBar(
                    title = title,
                    navigationType = AppBarNavigationType.Back(onBack),
                    query = searchText,
                    isSearchingMode = searchActive,
                    onQueryChanged = onSearchTextChange,
                    onSearchingModeChanged = onSearchActiveChange,
                    searchPlaceholder = stringResource(sharedR.string.contacts_search_hint),
                )
            } else {
                MegaTopAppBar(
                    title = title,
                    navigationType = AppBarNavigationType.Back(onBack),
                )
            }
        },
    ) { padding ->
        if (contacts == null) {
            ContactListLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag(START_CONVERSATION_LOADING_TAG),
            )
        } else {
            val layoutDirection = LocalLayoutDirection.current
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(START_CONVERSATION_LIST_TAG),
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                ),
            ) {
                if (!searchActive) {
                    item(key = "new_group_chat") {
                        ActionItem(
                            icon = IconPack.Medium.Thin.Outline.MessageChatCircle,
                            text = stringResource(sharedR.string.general_new_group_chat),
                            testTag = START_CONVERSATION_NEW_GROUP_TAG,
                            onClick = {
                                Analytics.tracker.trackEvent(GroupChatPressedEvent)
                                onNewGroupChatClick()
                            },
                        )
                    }
                    if (contacts.isNotEmpty()) {
                        item(key = "invite") {
                            ActionItem(
                                icon = IconPack.Medium.Thin.Outline.PlusCircle,
                                text = stringResource(sharedR.string.invite_contacts),
                                testTag = START_CONVERSATION_INVITE_TAG,
                                onClick = {
                                    Analytics.tracker.trackEvent(InviteContactsPressedEvent)
                                    onInviteClick()
                                },
                            )
                        }
                    }
                }

                when {
                    contacts.isNotEmpty() -> {
                        contactsSectionHeader(noteToSelfState, onNoteToSelfClick)
                        contactItems(contacts, onContactClick)
                    }

                    searchActive -> item(key = "empty_search") {
                        EmptyStateView(
                            modifier = Modifier.testTag(START_CONVERSATION_EMPTY_SEARCH_TAG),
                            imagePainter = painterResource(iconPackR.drawable.ic_search_02),
                            title = stringResource(sharedR.string.contacts_empty_title),
                        )
                    }

                    else -> {
                        contactsSectionHeader(noteToSelfState, onNoteToSelfClick)
                        item(key = "empty_contacts") {
                            EmptyStateView(
                                modifier = Modifier.testTag(START_CONVERSATION_EMPTY_CONTACTS_TAG),
                                imagePainter = painterResource(iconPackR.drawable.ic_user_glass),
                                title = stringResource(sharedR.string.invite_contacts_to_start_chat_title),
                                description = SpannableText(
                                    stringResource(sharedR.string.invite_contacts_to_start_chat_subtitle)
                                ),
                                primaryAction = {
                                    PrimaryFilledButton(
                                        modifier = Modifier.testTag(START_CONVERSATION_EMPTY_INVITE_TAG),
                                        text = stringResource(sharedR.string.invite_contacts_action_label),
                                        onClick = {
                                            Analytics.tracker.trackEvent(InviteContactsPressedEvent)
                                            onInviteClick()
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.contactsSectionHeader(
    noteToSelfState: NoteToSelfUiState,
    onNoteToSelfClick: (Long) -> Unit,
) {
    item(key = "contacts_header") {
        PrimaryHeaderListItem(
            text = stringResource(sharedR.string.general_section_contacts),
            enableClick = false,
        )
    }
    if (noteToSelfState is NoteToSelfUiState.Data) {
        item(key = "note_to_self") {
            val noteToSelf = remember(noteToSelfState) {
                ChatExplorerUiItem.NoteToSelf(
                    id = noteToSelfState.chatId,
                    isHint = noteToSelfState.isHint,
                    isSelected = false,
                    isArchived = false,
                    lastTimestamp = 0L,
                )
            }
            ChatExplorerListItemView(
                modifier = Modifier.testTag(START_CONVERSATION_NOTE_TO_SELF_TAG),
                item = noteToSelf,
                isProcessingAction = false,
                showCheckbox = false,
                onItemClicked = {
                    Analytics.tracker.trackEvent(CreateNoteToSelfButtonPressedEvent)
                    onNoteToSelfClick(noteToSelfState.chatId)
                },
            )
        }
    }
}

private fun LazyListScope.contactItems(
    contacts: ImmutableList<ContactItemUiState>,
    onContactClick: (Long) -> Unit,
) {
    contacts.groupBy { it.headerLetter() }.forEach { (letter, group) ->
        item(key = "header_$letter") {
            SecondaryHeaderListItem(
                modifier = Modifier.testTag(startConversationHeaderTag(letter)),
                text = letter,
                enableClick = false,
            )
        }
        items(group, key = { it.handle }) { contact ->
            ContactItemView(
                contactItemUiState = contact,
                onClick = { onContactClick(contact.handle) },
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    text: String,
    testTag: String,
    onClick: () -> Unit,
) {
    OneLineListItem(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        text = text,
        leadingElement = {
            MegaIcon(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                tint = IconColor.Primary,
            )
        },
        onClickListener = onClick,
    )
}

private fun ContactItemUiState.headerLetter(): String =
    displayName.firstOrNull()?.toString()?.uppercase(Locale.getDefault()).orEmpty()

internal const val START_CONVERSATION_SCREEN_TAG = "start_conversation_screen"
internal const val START_CONVERSATION_LOADING_TAG = "start_conversation_screen:loading"
internal const val START_CONVERSATION_LIST_TAG = "start_conversation_screen:list"
internal const val START_CONVERSATION_NEW_GROUP_TAG = "start_conversation_screen:new_group_chat"
internal const val START_CONVERSATION_INVITE_TAG = "start_conversation_screen:invite"
internal const val START_CONVERSATION_NOTE_TO_SELF_TAG = "start_conversation_screen:note_to_self"
internal const val START_CONVERSATION_EMPTY_CONTACTS_TAG = "start_conversation_screen:empty_contacts"
internal const val START_CONVERSATION_EMPTY_INVITE_TAG = "start_conversation_screen:empty_invite"
internal const val START_CONVERSATION_EMPTY_SEARCH_TAG = "start_conversation_screen:empty_search"

internal fun startConversationHeaderTag(letter: String) = "start_conversation_screen:header_$letter"

private class StartConversationPreviewProvider : PreviewParameterProvider<CreateChatUiState> {
    override val values: Sequence<CreateChatUiState> = sequenceOf(
        CreateChatUiState.Loading,
        CreateChatUiState.Data(
            contacts = persistentListOf(),
            query = null,
            allowGroupImageSelection = false,
        ),
        CreateChatUiState.Data(
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
            allowGroupImageSelection = false,
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun StartConversationScreenPreview(
    @PreviewParameter(StartConversationPreviewProvider::class) state: CreateChatUiState,
) {
    AndroidThemeForPreviews {
        StartConversationScreen(
            contactsState = state,
            noteToSelfState = NoteToSelfUiState.Data(chatId = 1L, isHint = true),
            onSearchQueryChange = {},
            onContactClick = {},
            onNewGroupChatClick = {},
            onInviteClick = {},
            onNoteToSelfClick = {},
            onBack = {},
        )
    }
}
