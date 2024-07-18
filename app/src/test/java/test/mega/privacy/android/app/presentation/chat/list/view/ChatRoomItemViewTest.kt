package test.mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.graphics.toColorInt
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.chat.list.view.ChatRoomItemView
import mega.privacy.android.app.presentation.chat.list.view.TEST_TAG_BOTTOM_TEXT
import mega.privacy.android.app.presentation.chat.list.view.TEST_TAG_BOTTOM_TEXT_CALL_CHRONOMETER
import mega.privacy.android.app.presentation.chat.list.view.TEST_TAG_MIDDLE_TEXT
import mega.privacy.android.app.presentation.chat.list.view.TEST_TAG_MIDDLE_TEXT_CALL_CHRONOMETER
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatRoomItemStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.time.Duration.Companion.seconds

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
        userChatStatus = UserChatStatus.Online,
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

        composeTestRule.onNodeWithTag("chat_room_item:avatar_image", useUnmergedTree = true)
            .performClick()

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

        composeTestRule.onNodeWithTag("chat_room_item:selected_image", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that duration text is not shown`() {
        val callInThisChat = mock<ChatCall> {
            on { status } doReturn ChatCallStatus.UserNoPresent
        }

        composeTestRule.setContent {
            ChatRoomItemView(
                item = ChatRoomItem.GroupChatRoomItem(
                    isPublic = false,
                    avatars = null,
                    call = callInThisChat,
                    chatId = 123L,
                    title = "Group Chat",
                    lastMessage = "Last message",
                    isLastMessageVoiceClip = false,
                    currentCallStatus = ChatRoomItemStatus.NotJoined,
                    unreadCount = 0,
                    hasPermissions = false,
                    isActive = true,
                    isMuted = false,
                    isArchived = false,
                    lastTimestamp = 0L,
                    lastTimestampFormatted = null,
                    highlight = true,
                    header = null,
                ),
                isSelected = true,
                isSelectionEnabled = true,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_MIDDLE_TEXT_CALL_CHRONOMETER).assertIsNotDisplayed()
    }

    @Test
    fun `test that middle text and the call duration is shown`() {
        val item = mock<ChatRoomItem.GroupChatRoomItem> {
            on { getDurationFromInitialTimestamp() } doReturn 27.seconds
            on { hasCallInProgress() } doReturn true
            on { title } doReturn "Title"
            on { call } doReturn mock()
            on { isPublic } doReturn false
            on { chatId } doReturn 123L
            on { currentCallStatus } doReturn ChatRoomItemStatus.Joined
            on { highlight } doReturn true
        }

        composeTestRule.setContent {
            ChatRoomItemView(
                item = item,
                isSelected = true,
                isSelectionEnabled = true,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_MIDDLE_TEXT, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_MIDDLE_TEXT_CALL_CHRONOMETER, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that bottom text and the call duration is shown`() {
        val item = mock<ChatRoomItem.MeetingChatRoomItem> {
            on { getDurationFromInitialTimestamp() } doReturn 27.seconds
            on { hasCallInProgress() } doReturn true
            on { isPending } doReturn true
            on { title } doReturn "Title"
            on { call } doReturn mock()
            on { isPublic } doReturn false
            on { chatId } doReturn 123L
            on { currentCallStatus } doReturn ChatRoomItemStatus.Joined
            on { highlight } doReturn true
        }

        composeTestRule.setContent {
            ChatRoomItemView(
                item = item,
                isSelected = true,
                isSelectionEnabled = true,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
                onItemSelected = onItemSelected,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_TEXT, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_BOTTOM_TEXT_CALL_CHRONOMETER, useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
