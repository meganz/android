package mega.privacy.android.app.presentation.startconversation.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.app.presentation.startconversation.model.StartConversationState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState

/**
 * Screen-level baselines for [StartConversationView]. Captures the layout with
 * the action buttons, "Note to self" row, alphabet headers and contact rows
 * after the migration to the `:shared:contact` `ContactItemView`.
 */
class StartConversationViewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StartConversationViewWithContacts() {
        AndroidThemeForPreviews {
            StartConversationView(
                state = StartConversationState(contactItemList = sampleContacts()),
                noteToSelfChatUIState = NoteToSelfChatUIState(),
                onContactClicked = {},
                onSearchTextChange = {},
                onCloseSearchClicked = {},
                onBackPressed = {},
                onSearchClicked = {},
                onInviteContactsClicked = {},
                onNoteToSelfClicked = {},
                onButtonClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun StartConversationViewEmptyContacts() {
        AndroidThemeForPreviews {
            StartConversationView(
                state = StartConversationState(),
                noteToSelfChatUIState = NoteToSelfChatUIState(),
                onContactClicked = {},
                onSearchTextChange = {},
                onCloseSearchClicked = {},
                onBackPressed = {},
                onSearchClicked = {},
                onInviteContactsClicked = {},
                onNoteToSelfClicked = {},
                onButtonClicked = {},
            )
        }
    }

    private fun sampleContacts(): List<ContactItemUiState> = listOf(
        ContactItemUiState(
            handle = 1L,
            displayName = "Alice Anderson",
            status = ContactItemStatus.Online,
            lastSeen = null,
            avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
            isVerified = true,
        ),
        ContactItemUiState(
            handle = 2L,
            displayName = "Bob Brown",
            status = ContactItemStatus.Away,
            lastSeen = 65535,
            avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
            isVerified = false,
        ),
        ContactItemUiState(
            handle = 3L,
            displayName = "Charlie Carter",
            status = ContactItemStatus.Offline,
            lastSeen = null,
            avatar = AvatarData.Initials(initials = "C", avatarColor = Color(0xFF6A1B9A)),
            isVerified = false,
        ),
        ContactItemUiState(
            handle = 4L,
            displayName = "Diana Davis",
            status = ContactItemStatus.Busy,
            lastSeen = null,
            avatar = AvatarData.Initials(initials = "D", avatarColor = Color(0xFFE65100)),
            isVerified = true,
        ),
    )
}
