package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.list.view.MeetingListView
import mega.privacy.android.app.presentation.meeting.model.MeetingListState
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeetingListViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that empty list view is shown when meetings are not present`() {
        val state = MeetingListState(emptyList())

        composeRule.setContent {
            MeetingListView(state = state)
        }

        composeRule.onNodeWithTag("EmptyView").assertIsDisplayed()
    }

    @Test
    fun `test that empty list view is shown when meetings are present`() {
        val meeting = MeetingRoomItem(1L, "Meeting 1")
        val state = MeetingListState(listOf(meeting))

        composeRule.setContent {
            MeetingListView(state = state)
        }

        composeRule.onNodeWithTag("ListView").assertIsDisplayed()
    }
}
