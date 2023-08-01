package test.mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.chat.list.view.ChatListView
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
            )
        }

        composeTestRule.onNodeWithTag("chat_room_list:list").assertIsDisplayed()
    }

    @Test
    fun `test that EmptyView is displayed when items are empty`() {
        composeTestRule.setContent {
            ChatListView(
                items = emptyList(),
                selectedIds = emptyList(),
                scrollToTop = false,
                isMeetingView = false,
            )
        }

        composeTestRule.onNodeWithTag("chat_room_list:empty").assertIsDisplayed()
    }

    @Test
    fun `test that ChatRoomItemHeaderView is displayed when item has a non-blank header`() {
        val items = listOf(individualChatRoomItem, groupChatRoomItem, meetingChatRoomItem)

        composeTestRule.setContent {
            ChatListView(
                items = items,
                selectedIds = emptyList(),
                scrollToTop = false,
                isMeetingView = false,
            )
        }

        composeTestRule.onNodeWithTag("chat_room_list:item_header").assertIsDisplayed()
    }
}
