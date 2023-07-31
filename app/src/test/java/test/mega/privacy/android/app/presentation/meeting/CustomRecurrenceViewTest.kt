package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.meeting.model.CreateScheduledMeetingState
import mega.privacy.android.app.presentation.meeting.model.CustomRecurrenceState
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction
import mega.privacy.android.app.presentation.meeting.view.CustomRecurrenceView
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_ACCEPT_ICON
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_BACK_ICON
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_ENDS
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_OCCURS_DAILY
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_OCCURS_EVERY
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_OCCURS_MONTHLY
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_OCCURS_WEEKLY
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.Weekday
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
    fun `test that occurs every is shown`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                meetingTitle = "Title meeting",
                rulesSelected = getDailyRules(),
                customRecurrenceState = CustomRecurrenceState(newRules = getDailyRules()),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackbarMessageContent = consumed()
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_OCCURS_EVERY).assertExists()
    }

    @Test
    fun `test that occurs daily is shown`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                meetingTitle = "Title meeting",
                rulesSelected = getDailyRules(),
                customRecurrenceState = CustomRecurrenceState(newRules = getDailyRules()),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackbarMessageContent = consumed()
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_OCCURS_DAILY).assertExists()
    }

    @Test
    fun `test that occurs daily is hidden`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                meetingTitle = "Title meeting",
                rulesSelected = getWeeklyRules(),
                customRecurrenceState = CustomRecurrenceState(
                    newRules = getWeeklyRules(),
                ),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackbarMessageContent = consumed()
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_OCCURS_DAILY).assertDoesNotExist()
    }

    @Test
    fun `test that occurs weekly is shown`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                meetingTitle = "Title meeting",
                rulesSelected = getWeeklyRules(),
                customRecurrenceState = CustomRecurrenceState(
                    newRules = getWeeklyRules(),
                ),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackbarMessageContent = consumed()
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_OCCURS_WEEKLY).assertExists()
    }

    @Test
    fun `test that occurs weekly is hidden`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                meetingTitle = "Title meeting",
                rulesSelected = getDailyRules(),
                customRecurrenceState = CustomRecurrenceState(
                    newRules = getDailyRules(),
                ),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackbarMessageContent = consumed()
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_OCCURS_WEEKLY).assertDoesNotExist()
    }

    @Test
    fun `test that occurs monthly is shown`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                meetingTitle = "Title meeting",
                rulesSelected = getMonthlyRules(),
                customRecurrenceState = CustomRecurrenceState(
                    newRules = getMonthlyRules(),
                ),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackbarMessageContent = consumed()
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_OCCURS_MONTHLY).assertExists()
    }

    @Test
    fun `test that occurs monthly is hidden`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                meetingTitle = "Title meeting",
                rulesSelected = getDailyRules(),
                customRecurrenceState = CustomRecurrenceState(
                    newRules = getDailyRules(),
                ),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackbarMessageContent = consumed()
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_OCCURS_MONTHLY).assertDoesNotExist()
    }

    @Test
    fun `test that ends is shown`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                meetingTitle = "Title meeting",
                rulesSelected = getMonthlyRules(),
                customRecurrenceState = CustomRecurrenceState(
                    newRules = getMonthlyRules(),
                ),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackbarMessageContent = consumed()
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_ENDS).assertExists()
    }

    @Test
    fun `test that on click event is fired when accept icon is clicked`() {
        val mock = mock<() -> Unit>()
        composeTestRule.setContent {
            CustomRecurrenceView(
                state = CreateScheduledMeetingState(
                    meetingTitle = "Title meeting",
                    rulesSelected = getDailyRules(),
                    participantItemList = emptyList(),
                    buttons = ScheduleMeetingAction.values().asList(),
                    snackbarMessageContent = consumed()
                ),
                onAcceptClicked = mock,
                onScrollChange = {},
                onRejectClicked = {},
                onIntervalChanged = {},
                onFrequencyTypeChanged = {},
                onDayClicked = {},
                onWeekdaysClicked = {},
                onFocusChanged = {},
                onMonthlyRadioButtonClicked = {},
                onEndsRadioButtonClicked = {},
                onMonthDayChanged = {},
                onMonthWeekDayChanged = {},
                onWeekOfMonthChanged = {},
                onDateClicked = {},
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
                state = CreateScheduledMeetingState(
                    meetingTitle = "Title meeting",
                    rulesSelected = ChatScheduledRules(),
                    participantItemList = emptyList(),
                    buttons = ScheduleMeetingAction.values().asList(),
                    snackbarMessageContent = consumed()
                ),
                onRejectClicked = mock,
                onScrollChange = {},
                onAcceptClicked = {},
                onIntervalChanged = {},
                onFrequencyTypeChanged = {},
                onDayClicked = {},
                onWeekdaysClicked = {},
                onFocusChanged = {},
                onMonthlyRadioButtonClicked = {},
                onEndsRadioButtonClicked = {},
                onMonthDayChanged = {},
                onMonthWeekDayChanged = {},
                onWeekOfMonthChanged = {},
                onDateClicked = {},
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_BACK_ICON).performClick()
        verify(mock).invoke()
    }

    private fun getDailyRules() =
        ChatScheduledRules(
            freq = OccurrenceFrequencyType.Daily,
            interval = 12,
            until = 0,
            weekDayList = null,
            monthDayList = null,
            monthWeekDayList = emptyList()
        )

    private fun getWeeklyRules() =
        ChatScheduledRules(
            freq = OccurrenceFrequencyType.Weekly,
            interval = 2,
            until = 0,
            weekDayList = getWeeklyList(),
            monthDayList = null,
            monthWeekDayList = emptyList()
        )

    private fun getMonthlyRules() =
        ChatScheduledRules(
            freq = OccurrenceFrequencyType.Monthly,
            interval = 1,
            until = 0,
            weekDayList = null,
            monthDayList = null,
            monthWeekDayList = emptyList()
        )

    private fun getWeeklyList() = mutableListOf<Weekday>().apply {
        add(Weekday.Monday)
        add(Weekday.Friday)
    }

    private fun initComposeRuleContent(
        state: CreateScheduledMeetingState,
    ) {
        composeTestRule.setContent {
            CustomRecurrenceView(
                state = state,
                onScrollChange = {},
                onAcceptClicked = {},
                onRejectClicked = {},
                onIntervalChanged = {},
                onFrequencyTypeChanged = {},
                onDayClicked = {},
                onWeekdaysClicked = {},
                onFocusChanged = {},
                onMonthlyRadioButtonClicked = {},
                onEndsRadioButtonClicked = {},
                onMonthDayChanged = {},
                onMonthWeekDayChanged = {},
                onWeekOfMonthChanged = {},
                onDateClicked = {},
            )
        }
    }
}