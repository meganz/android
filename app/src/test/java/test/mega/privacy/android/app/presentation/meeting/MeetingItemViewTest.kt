package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.list.view.MeetingItemView
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MeetingItemViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that onItemClick is called when a meeting is clicked`() {
        val meeting = MeetingRoomItem(1L, "Meeting 1")
        val onItemClick: (Long) -> Unit = Mockito.mock()

        composeRule.setContent {
            MeetingItemView(
                meeting = meeting,
                isSelected = false,
                isSelectionEnabled = false,
                timestampUpdate = null,
                onItemClick = onItemClick,
                onItemMoreClick = {},
                onItemSelected = {},
            )
        }

        composeRule.onNodeWithText(meeting.title).performClick()
        verify(onItemClick).invoke(meeting.chatId)
    }

    @Test
    fun `test that onItemMoreClick is called when more button is clicked`() {
        val meeting = MeetingRoomItem(1L, "Meeting 1")
        val onItemMoreClick: (Long) -> Unit = Mockito.mock()

        composeRule.setContent {
            MeetingItemView(
                meeting = meeting,
                isSelected = false,
                isSelectionEnabled = false,
                timestampUpdate = null,
                onItemClick = {},
                onItemMoreClick = onItemMoreClick,
                onItemSelected = {},
            )
        }

        composeRule.onNodeWithTag("onItemMore").performClick()
        verify(onItemMoreClick).invoke(meeting.chatId)
    }

    @Test
    fun `test that onItemSelected is called when meeting is clicked and selection is enabled`() {
        val meeting = MeetingRoomItem(1L, "Meeting 1")
        val onItemSelected: (Long) -> Unit = Mockito.mock()

        composeRule.setContent {
            MeetingItemView(
                meeting = meeting,
                isSelected = false,
                isSelectionEnabled = true,
                timestampUpdate = null,
                onItemClick = {},
                onItemMoreClick = {},
                onItemSelected = onItemSelected,
            )
        }

        composeRule.onNodeWithText(meeting.title).performClick()
        verify(onItemSelected).invoke(meeting.chatId)
    }

    @Test
    fun `test that selected image is shown when item is selected`() {
        val meeting = MeetingRoomItem(1L, "Meeting 1")

        composeRule.setContent {
            MeetingItemView(
                meeting = meeting,
                isSelected = true,
                isSelectionEnabled = true,
                timestampUpdate = null,
                onItemClick = {},
                onItemMoreClick = {},
                onItemSelected = {},
            )
        }

        composeRule.onNodeWithTag("selectedImage", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that the number of unread messages is shown`() {
        val meeting = MeetingRoomItem(1L, "Meeting 1", unreadCount = 7)

        composeRule.setContent {
            MeetingItemView(
                meeting = meeting,
                isSelected = false,
                isSelectionEnabled = false,
                onItemClick = {},
                onItemMoreClick = {},
                onItemSelected = {},
                timestampUpdate = null,
            )
        }

        composeRule.onNode(hasText(meeting.unreadCount.toString())).assertIsDisplayed()
    }
}
