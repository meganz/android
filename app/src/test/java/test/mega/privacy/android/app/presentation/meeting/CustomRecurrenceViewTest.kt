package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.model.CustomRecurrenceState
import mega.privacy.android.app.presentation.meeting.view.CustomRecurrenceView
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_ACCEPT_ICON
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_BACK_ICON
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_OCCURS_DAILY
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class CustomRecurrenceViewTest {
    @get:Rule
    var composeTestRule = createComposeRule()


    @Test
    fun `test that occurs daily is shown`() {
        initComposeRuleContent(
            CustomRecurrenceState(
                rules = ChatScheduledRules(
                    freq = OccurrenceFrequencyType.Daily,
                    interval = 1,
                    until = 0,
                    weekDayList = null,
                    monthDayList = null,
                    monthWeekDayList = emptyList()
                ),
                dropdownOccurrenceType = DropdownOccurrenceType.Day,
                maxOccurrenceNumber = 99,
                isWeekdaysSelected = false,
                isValidRecurrence = false
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_OCCURS_DAILY).assertExists()
    }

    @Test
    fun `test that occurs daily is hidden`() {
        initComposeRuleContent(
            CustomRecurrenceState(
                rules = ChatScheduledRules(
                    freq = OccurrenceFrequencyType.Weekly,
                    interval = 1,
                    until = 0,
                    weekDayList = null,
                    monthDayList = null,
                    monthWeekDayList = emptyList()
                ),
                dropdownOccurrenceType = DropdownOccurrenceType.Week,
                maxOccurrenceNumber = 99,
                isWeekdaysSelected = false,
                isValidRecurrence = false
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_OCCURS_DAILY).assertDoesNotExist()
    }

    @Test
    fun `test that on click event is fired when accept icon is clicked`() {
        val mock = mock<() -> Unit>()
        composeTestRule.setContent {
            CustomRecurrenceView(
                state = CustomRecurrenceState(),
                onAcceptClicked = mock,
                onScrollChange = {},
                onBackPressed = {},
                onTypeClicked = {},
                onNumberClicked = {},
                onWeekdaysClicked = {},
                onFocusChanged = {}
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_ACCEPT_ICON).performClick()
        verify(mock).invoke()
    }

    @Test
    fun `test that on click event is fired when back icon is clicked`() {
        val mock = mock<() -> Unit>()
        composeTestRule.setContent {
            CustomRecurrenceView(
                state = CustomRecurrenceState(),
                onAcceptClicked = {},
                onScrollChange = {},
                onBackPressed = mock,
                onTypeClicked = {},
                onNumberClicked = {},
                onWeekdaysClicked = {},
                onFocusChanged = {}
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_BACK_ICON).performClick()
        verify(mock).invoke()
    }

    private fun initComposeRuleContent(
        state: CustomRecurrenceState,
    ) {
        composeTestRule.setContent {
            CustomRecurrenceView(
                state = state,
                onAcceptClicked = {},
                onScrollChange = {},
                onBackPressed = {},
                onTypeClicked = {},
                onNumberClicked = {},
                onWeekdaysClicked = {},
                onFocusChanged = {}
            )
        }
    }
}