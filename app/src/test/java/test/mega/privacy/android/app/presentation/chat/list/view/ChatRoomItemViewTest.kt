package test.mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.graphics.toColorInt
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.chat.list.view.ChatRoomItemView
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ChatRoomItemViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val individualChatRoomItem = ChatRoomItem.IndividualChatRoomItem(
        chatId = 1L,
        title = "Individual Chat",
        peerEmail = "individual@chat.com",
        lastMessage = "Last message",
        lastTimestampFormatted = "Yesterday",
        avatar = ChatAvatarItem("I", color = "#FEBC00".toColorInt()),
        isMuted = true,
        userStatus = UserStatus.Online,
    )

    private val onItemClick: (Long) -> Unit = mock()
    private val onItemMoreClick: (ChatRoomItem) -> Unit = mock()
    private val onItemSelected: (Long) -> Unit = mock()

    @Test
    fun `test that onItemClick is called when item is clicked`() {
        composeTestRule.setContent {
            ChatRoomItemView(
                item = individualChatRoomItem,
                isSelected = false,
                isSelectionEnabled = false,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
            )
        }

        composeTestRule.onNodeWithTag("chat_room_item:avatar_image", useUnmergedTree = true).performClick()

        verify(onItemClick).invoke(individualChatRoomItem.chatId)
    }

    @Test
    fun `test that onItemMoreClick is called when more button is clicked`() {
        composeTestRule.setContent {
            ChatRoomItemView(
                item = individualChatRoomItem,
                isSelected = false,
                isSelectionEnabled = false,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
            )
        }

        composeTestRule.onNodeWithTag("chat_room_item:more_button").performClick()

        verify(onItemMoreClick).invoke(individualChatRoomItem)
    }

    @Test
    fun `test that selectedImage is shown when item is selected`() {
        composeTestRule.setContent {
            ChatRoomItemView(
                item = individualChatRoomItem,
                isSelected = true,
                isSelectionEnabled = true,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
            )
        }

        composeTestRule.onNodeWithTag("chat_room_item:selected_image", useUnmergedTree = true).assertIsDisplayed()
    }
}
