package mega.privacy.android.app.presentation.chat.list.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MeetingBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that start meeting tile is shown`() {
        composeTestRule.setContent { MeetingBottomSheetContent() }

        composeTestRule.onNodeWithTag(TEST_TAG_START_MEETING).assertIsDisplayed()
    }

    @Test
    fun `test that join meeting tile is shown`() {
        composeTestRule.setContent { MeetingBottomSheetContent() }

        composeTestRule.onNodeWithTag(TEST_TAG_JOIN_MEETING).assertIsDisplayed()
    }

    @Test
    fun `test that schedule meeting tile is shown`() {
        composeTestRule.setContent { MeetingBottomSheetContent() }

        composeTestRule.onNodeWithTag(TEST_TAG_SCHEDULE_MEETING).assertIsDisplayed()
    }

    @Test
    fun `test that onStartMeetingClick is invoked when start meeting tile is clicked`() {
        val onStartMeetingClick = mock<() -> Unit>()
        composeTestRule.setContent {
            MeetingBottomSheetContent(onStartMeetingClick = onStartMeetingClick)
        }

        composeTestRule.onNodeWithTag(TEST_TAG_START_MEETING).performClick()
        verify(onStartMeetingClick).invoke()
    }

    @Test
    fun `test that onJoinMeetingClick is invoked when join meeting tile is clicked`() {
        val onJoinMeetingClick = mock<() -> Unit>()
        composeTestRule.setContent {
            MeetingBottomSheetContent(onJoinMeetingClick = onJoinMeetingClick)
        }

        composeTestRule.onNodeWithTag(TEST_TAG_JOIN_MEETING).performClick()
        verify(onJoinMeetingClick).invoke()
    }

    @Test
    fun `test that onScheduleMeetingClick is invoked when schedule meeting tile is clicked`() {
        val onScheduleMeetingClick = mock<() -> Unit>()
        composeTestRule.setContent {
            MeetingBottomSheetContent(onScheduleMeetingClick = onScheduleMeetingClick)
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SCHEDULE_MEETING).performClick()
        verify(onScheduleMeetingClick).invoke()
    }
}
