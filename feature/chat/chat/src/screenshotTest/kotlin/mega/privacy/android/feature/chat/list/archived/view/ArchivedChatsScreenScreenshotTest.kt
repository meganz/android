package mega.privacy.android.feature.chat.list.archived.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.list.archived.model.ArchivedChatsUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem

/**
 * Screenshot tests for [ArchivedChatsScreen], covering the populated list, the empty state and the
 * loading skeleton.
 */
class ArchivedChatsScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ArchivedChatsScreenPopulated() {
        AndroidThemeForPreviews {
            ArchivedChatsScreen(
                uiState = ArchivedChatsUiState.Data(populatedItems()),
                onBackClick = {},
                onItemClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ArchivedChatsScreenEmpty() {
        AndroidThemeForPreviews {
            ArchivedChatsScreen(
                uiState = ArchivedChatsUiState.Empty,
                onBackClick = {},
                onItemClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ArchivedChatsScreenLoading() {
        AndroidThemeForPreviews {
            ArchivedChatsScreen(
                uiState = ArchivedChatsUiState.Loading,
                onBackClick = {},
                onItemClick = {},
            )
        }
    }

    private fun populatedItems() = persistentListOf(
        ChatRoomUiItem(
            chatId = 1L,
            title = "Mieko Kawakami",
            lastMessage = "See you tomorrow!",
            lastTimestampFormatted = "Today 14:25",
            header = null,
            pendingMeeting = null,
            showRecurringIcon = false,
            unreadCount = 0,
            isMuted = false,
            highlight = false,
            isNoteToSelf = false,
            avatar = ChatRoomUiItem.ChatRoomUiAvatar.Peer(
                placeholderText = "M",
                filePath = null,
                color = 0xFFFEBC00.toInt(),
            ),
            status = ContactItemStatus.Unknown,
            hasOngoingCall = false,
        ),
        ChatRoomUiItem(
            chatId = 2L,
            title = "Recipe test #14",
            lastMessage = "Anna: Seeya all soon!",
            lastTimestampFormatted = "1 May 2022 17:53",
            header = null,
            pendingMeeting = null,
            showRecurringIcon = false,
            unreadCount = 0,
            isMuted = false,
            highlight = false,
            isNoteToSelf = false,
            avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
            status = ContactItemStatus.Unknown,
            hasOngoingCall = false,
        ),
    )
}
