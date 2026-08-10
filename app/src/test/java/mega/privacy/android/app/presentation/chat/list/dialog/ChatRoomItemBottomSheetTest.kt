package mega.privacy.android.app.presentation.chat.list.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.meeting.ChatRoomItemStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ChatRoomItemBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val individualChatRoomItem = ChatRoomItem.IndividualChatRoomItem(
        chatId = 1L,
        title = "Mieko Kawakami",
        peerEmail = "mieko@miekokawakami.jp",
        avatar = ChatAvatarItem("M"),
        hasPermissions = true,
    )

    private val groupChatRoomItem = ChatRoomItem.GroupChatRoomItem(
        chatId = 2L,
        title = "Group Chat",
        avatars = listOf(ChatAvatarItem("L"), ChatAvatarItem("J")),
        hasPermissions = true,
        isActive = true,
    )

    private val pendingMeetingChatRoomItem = ChatRoomItem.MeetingChatRoomItem(
        chatId = 3L,
        schedId = 99L,
        title = "Photos Sprint #1",
        avatars = listOf(ChatAvatarItem("A")),
        hasPermissions = true,
        isPending = true,
        isActive = true,
    )

    private val noteToSelfChatRoomItem = ChatRoomItem.NoteToSelfChatRoomItem(
        chatId = 4L,
        title = "Note to self",
        hasPermissions = true,
    )

    @Test
    fun `test that error message is shown when item is null`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(item = null)
        }

        composeTestRule.onNodeWithText("Chat error").assertIsDisplayed()
    }

    @Test
    fun `test that info menu is shown for individual chat with permissions`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(item = individualChatRoomItem)
        }

        composeTestRule.onNodeWithTag("info").assertIsDisplayed()
    }

    @Test
    fun `test that mute menu is shown when item is not muted`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = individualChatRoomItem.copy(isMuted = false),
            )
        }

        composeTestRule.onNodeWithTag("mute").assertIsDisplayed()
    }

    @Test
    fun `test that unmute menu is shown when item is muted`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = individualChatRoomItem.copy(isMuted = true),
            )
        }

        composeTestRule.onNodeWithTag("unmute").assertIsDisplayed()
    }

    @Test
    fun `test that archive menu is shown for a non-archived item`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(item = individualChatRoomItem)
        }

        composeTestRule.onNodeWithTag("archive").assertIsDisplayed()
    }

    @Test
    fun `test that unarchive menu is shown for an archived item`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = individualChatRoomItem.copy(isArchived = true),
            )
        }

        composeTestRule.onNodeWithTag("unarchive").assertIsDisplayed()
    }

    @Test
    fun `test that leave menu is shown for an active group chat`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(item = groupChatRoomItem)
        }

        composeTestRule.onNodeWithTag("leave").assertIsDisplayed()
    }

    @Test
    fun `test that cancel menu is shown for a pending meeting with permissions`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(item = pendingMeetingChatRoomItem)
        }

        composeTestRule.onNodeWithTag("cancel").assertIsDisplayed()
    }

    @Test
    fun `test that start meeting menu is shown when meeting is not started`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = pendingMeetingChatRoomItem.copy(
                    currentCallStatus = ChatRoomItemStatus.NotStarted,
                ),
            )
        }

        composeTestRule.onNodeWithTag("start_meeting").assertIsDisplayed()
    }

    @Test
    fun `test that join meeting menu is shown when meeting is not joined`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = pendingMeetingChatRoomItem.copy(
                    currentCallStatus = ChatRoomItemStatus.NotJoined,
                ),
            )
        }

        composeTestRule.onNodeWithTag("join_meeting").assertIsDisplayed()
    }

    @Test
    fun `test that occurrences menu is shown for a recurring meeting`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = pendingMeetingChatRoomItem.copy(isRecurringWeekly = true),
            )
        }

        composeTestRule.onNodeWithTag("occurrences").assertIsDisplayed()
    }

    @Test
    fun `test that edit menu is shown for a meeting with schedId and permissions`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(item = pendingMeetingChatRoomItem)
        }

        composeTestRule.onNodeWithTag("edit").assertIsDisplayed()
    }

    @Test
    fun `test that clear chat history menu is shown for chats with permissions`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(item = individualChatRoomItem)
        }

        composeTestRule.onNodeWithTag("clear_chat_history").assertIsDisplayed()
    }

    @Test
    fun `test that onMuteClick is invoked when mute menu is clicked`() {
        val onMuteClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = individualChatRoomItem.copy(isMuted = false),
                onMuteClick = onMuteClick,
            )
        }

        composeTestRule.onNodeWithTag("mute").performClick()
        verify(onMuteClick).invoke()
    }

    @Test
    fun `test that onUnmuteClick is invoked when unmute menu is clicked`() {
        val onUnmuteClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = individualChatRoomItem.copy(isMuted = true),
                onUnmuteClick = onUnmuteClick,
            )
        }

        composeTestRule.onNodeWithTag("unmute").performClick()
        verify(onUnmuteClick).invoke()
    }

    @Test
    fun `test that onInfoClick is invoked when info menu is clicked`() {
        val onInfoClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = individualChatRoomItem,
                onInfoClick = onInfoClick,
            )
        }

        composeTestRule.onNodeWithTag("info").performClick()
        verify(onInfoClick).invoke()
    }

    @Test
    fun `test that onClearChatClick is invoked when clear chat history menu is clicked`() {
        val onClearChatClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = individualChatRoomItem,
                onClearChatClick = onClearChatClick,
            )
        }

        composeTestRule.onNodeWithTag("clear_chat_history").performClick()
        verify(onClearChatClick).invoke()
    }

    @Test
    fun `test that onArchiveClick is invoked with false when archiving a non note-to-self chat`() {
        val onArchiveClick = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = individualChatRoomItem,
                onArchiveClick = onArchiveClick,
            )
        }

        composeTestRule.onNodeWithTag("archive").performClick()
        verify(onArchiveClick).invoke(false)
    }

    @Test
    fun `test that onArchiveClick is invoked with true when archiving a note-to-self chat`() {
        val onArchiveClick = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = noteToSelfChatRoomItem,
                onArchiveClick = onArchiveClick,
            )
        }

        composeTestRule.onNodeWithTag("archive").performClick()
        verify(onArchiveClick).invoke(true)
    }

    @Test
    fun `test that onLeaveClick is invoked when leave menu is clicked`() {
        val onLeaveClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = groupChatRoomItem,
                onLeaveClick = onLeaveClick,
            )
        }

        composeTestRule.onNodeWithTag("leave").performClick()
        verify(onLeaveClick).invoke()
    }

    @Test
    fun `test that onCancelClick is invoked when cancel menu is clicked`() {
        val onCancelClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = pendingMeetingChatRoomItem,
                onCancelClick = onCancelClick,
            )
        }

        composeTestRule.onNodeWithTag("cancel").performClick()
        verify(onCancelClick).invoke()
    }

    @Test
    fun `test that onEditClick is invoked when edit menu is clicked`() {
        val onEditClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = pendingMeetingChatRoomItem,
                onEditClick = onEditClick,
            )
        }

        composeTestRule.onNodeWithTag("edit").performClick()
        verify(onEditClick).invoke()
    }

    @Test
    fun `test that onOccurrencesClick is invoked when occurrences menu is clicked`() {
        val onOccurrencesClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = pendingMeetingChatRoomItem.copy(isRecurringWeekly = true),
                onOccurrencesClick = onOccurrencesClick,
            )
        }

        composeTestRule.onNodeWithTag("occurrences").performClick()
        verify(onOccurrencesClick).invoke()
    }

    @Test
    fun `test that onStartMeetingClick is invoked when start meeting menu is clicked`() {
        val onStartMeetingClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = pendingMeetingChatRoomItem.copy(
                    currentCallStatus = ChatRoomItemStatus.NotStarted,
                ),
                onStartMeetingClick = onStartMeetingClick,
            )
        }

        composeTestRule.onNodeWithTag("start_meeting").performClick()
        verify(onStartMeetingClick).invoke()
    }

    @Test
    fun `test that mute menu is not shown for note-to-self chat`() {
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(item = noteToSelfChatRoomItem)
        }

        composeTestRule.onNodeWithTag("mute").assertDoesNotExist()
        composeTestRule.onNodeWithTag("unmute").assertDoesNotExist()
    }

    @Test
    fun `test that leave menu is not shown for individual chat`() {
        val onLeaveClick = mock<() -> Unit>()
        composeTestRule.setContent {
            ChatRoomItemBottomSheetContent(
                item = individualChatRoomItem,
                onLeaveClick = onLeaveClick,
            )
        }

        composeTestRule.onNodeWithTag("leave").assertDoesNotExist()
        verify(onLeaveClick, never()).invoke()
    }
}
