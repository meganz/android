package mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatListViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val individualChatRoomItem = ChatRoomItem.IndividualChatRoomItem(
        chatId = 1L,
        title = "Individual Chat"
    )

    private val groupChatRoomItem = ChatRoomItem.GroupChatRoomItem(
        chatId = 2L,
        title = "Group Chat",
        header = "Header for Group Chat"
    )

    private val meetingChatRoomItem = ChatRoomItem.MeetingChatRoomItem(
        chatId = 3L,
        title = "Meeting Chat"
    )

    @Test
    fun `test that ListView is displayed when items are not empty`() {
        val items = listOf(individualChatRoomItem, groupChatRoomItem, meetingChatRoomItem)

        composeTestRule.setContent {
            ChatListView(
                items = items,
                selectedIds = emptyList(),
                scrollToTop = false,
                isMeetingView = false,
                isSearchMode = false,
                isLoading = false,
            )
        }

        composeTestRule.onNodeWithTag("chat_room_list:list").assertIsDisplayed()
    }

    @Test
    fun `test that EmptyView is not displayed when page is loading`() {
        composeTestRule.setContent {
            ChatListView(
                items = emptyList(),
                selectedIds = emptyList(),
                scrollToTop = false,
                isMeetingView = false,
                isLoading = true,
                isSearchMode = false
            )
        }

        composeTestRule.onNodeWithTag("chat_room_list:empty").assertIsNotDisplayed()
    }

    @Test
    fun `test that EmptyView is displayed when items are empty`() {
        composeTestRule.setContent {
            ChatListView(
                items = emptyList(),
                selectedIds = emptyList(),
                scrollToTop = false,
                isMeetingView = false,
                isLoading = false,
                isSearchMode = false
            )
        }

        composeTestRule.onNodeWithTag("chat_room_list:empty").assertIsDisplayed()
    }

    @Test
    fun `test that EmptyView is displayed when item is Note to self chat`() {
        val list = mutableListOf<ChatRoomItem>()
        list.add(ChatRoomItem.NoteToSelfChatRoomItem(chatId = 123L, title = "Note to self"))
        composeTestRule.setContent {
            ChatListView(
                items = list,
                selectedIds = emptyList(),
                scrollToTop = false,
                isSearchMode = false,
                isMeetingView = false,
                isLoading = false
            )
        }

        composeTestRule.onNodeWithTag("chat_room_list:empty").assertIsDisplayed()
    }

    @Test
    fun `test that EmptyView is hidden when item is Note to self chat and is search mode`() {
        val list = mutableListOf<ChatRoomItem>()
        list.add(ChatRoomItem.NoteToSelfChatRoomItem(chatId = 123L, title = "Note to self"))
        composeTestRule.setContent {
            ChatListView(
                items = list,
                selectedIds = emptyList(),
                scrollToTop = false,
                isSearchMode = true,
                isMeetingView = false,
                isLoading = false
            )
        }

        composeTestRule.onNodeWithTag("chat_room_list:empty").assertIsNotDisplayed()
    }

    @Test
    fun `test that ChatRoomItemHeaderView is displayed when item has a non-blank header`() {
        val items = listOf(individualChatRoomItem, groupChatRoomItem, meetingChatRoomItem)

        composeTestRule.setContent {
            ChatListView(
                items = items,
                selectedIds = emptyList(),
                scrollToTop = false,
                isSearchMode = false,
                isMeetingView = false,
            )
        }

        composeTestRule.onNodeWithTag("chat_room_list:item_header").assertIsDisplayed()
    }
}
