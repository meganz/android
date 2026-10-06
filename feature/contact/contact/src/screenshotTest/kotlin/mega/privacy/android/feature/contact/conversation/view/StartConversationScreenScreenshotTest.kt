package mega.privacy.android.feature.contact.conversation.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.contact.conversation.model.NoteToSelfUiState
import mega.privacy.android.feature.contact.group.create.model.CreateChatUiState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState

class StartConversationScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StartConversationScreenLoading() {
        AndroidThemeForPreviews {
            Screen(
                contactsState = CreateChatUiState.Loading,
                noteToSelfState = NoteToSelfUiState.Loading,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StartConversationScreenWithContactsNoteToSelfHint() {
        AndroidThemeForPreviews {
            Screen(
                contactsState = sampleData(),
                noteToSelfState = NoteToSelfUiState.Data(chatId = 10L, isHint = true),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StartConversationScreenWithContactsNoteToSelf() {
        AndroidThemeForPreviews {
            Screen(
                contactsState = sampleData(),
                noteToSelfState = NoteToSelfUiState.Data(chatId = 10L, isHint = false),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StartConversationScreenNoContacts() {
        AndroidThemeForPreviews {
            Screen(
                contactsState = CreateChatUiState.Data(
                    contacts = persistentListOf(),
                    query = null,
                    allowGroupImageSelection = false,
                ),
                noteToSelfState = NoteToSelfUiState.Data(chatId = 10L, isHint = true),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StartConversationScreenSearchNoResults() {
        AndroidThemeForPreviews {
            Screen(
                contactsState = CreateChatUiState.Data(
                    contacts = persistentListOf(),
                    query = "zed",
                    allowGroupImageSelection = false,
                ),
                noteToSelfState = NoteToSelfUiState.Data(chatId = 10L, isHint = true),
                searchActive = true,
                searchText = "zed",
            )
        }
    }

    @Composable
    private fun Screen(
        contactsState: CreateChatUiState,
        noteToSelfState: NoteToSelfUiState,
        searchActive: Boolean = false,
        searchText: String = "",
    ) {
        StartConversationScreenContent(
            contactsState = contactsState,
            noteToSelfState = noteToSelfState,
            searchActive = searchActive,
            searchText = searchText,
            onSearchActiveChange = {},
            onSearchTextChange = {},
            onContactClick = {},
            onNewGroupChatClick = {},
            onInviteClick = {},
            onNoteToSelfClick = {},
            onBack = {},
        )
    }

    private fun sampleData() = CreateChatUiState.Data(
        contacts = listOf(
            contact(1L, "Alice Anderson", "alice@example.com", Color(0xFF2E7D32)),
            contact(2L, "Amy Adams", "amy@example.com", Color(0xFFC62828)),
            contact(3L, "Bob Brown", "bob@example.com", Color(0xFF1565C0)),
            contact(4L, "Charlie Clark", "charlie@example.com", Color(0xFF6A1B9A)),
        ).toImmutableList(),
        query = null,
        allowGroupImageSelection = false,
    )

    private fun contact(
        handle: Long,
        displayName: String,
        email: String,
        avatarColor: Color,
    ) = ContactItemUiState(
        handle = handle,
        displayName = displayName,
        status = ContactItemStatus.Online,
        lastSeen = null,
        avatar = AvatarData.Initials(
            initials = displayName.first().toString(),
            avatarColor = avatarColor,
        ),
        isVerified = false,
        email = email,
    )
}
