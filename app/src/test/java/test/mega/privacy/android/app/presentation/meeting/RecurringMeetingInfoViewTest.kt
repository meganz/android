package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingInfoState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementState
import mega.privacy.android.app.presentation.meeting.view.RecurringMeetingInfoView
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class RecurringMeetingInfoViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    private val occursList1 = listOf(
        ChatScheduledMeetingOccurr(schedId = 123L),
        ChatScheduledMeetingOccurr(schedId = 234L),
        ChatScheduledMeetingOccurr(schedId = 345L),
        ChatScheduledMeetingOccurr(schedId = 456L),
        ChatScheduledMeetingOccurr(schedId = 567L)
    )

    private val occursList2 = listOf(
        ChatScheduledMeetingOccurr(schedId = 123L, startDateTime = 1679814000),
        ChatScheduledMeetingOccurr(schedId = 234L, startDateTime = 1679900400),
        ChatScheduledMeetingOccurr(schedId = 345L, startDateTime = 1679986800),
        ChatScheduledMeetingOccurr(schedId = 456L, startDateTime = 1680073200),
        ChatScheduledMeetingOccurr(schedId = 567L, startDateTime = 1680159600)
    )

    @Test
    fun `test that occurrences list exists`() {
        initComposeRuleContent(
            uiState = RecurringMeetingInfoState(
                occurrencesList = occursList1,
            ),
            managementState = ScheduledMeetingManagementState(),
        )

        composeRule.onNodeWithTag("Occurrence_list_view").assertExists()
    }

    @Test
    fun `test that occurrences item view exists`() {
        initComposeRuleContent(
            uiState = RecurringMeetingInfoState(
                occurrencesList = occursList1,
            ),
            managementState = ScheduledMeetingManagementState(),
        )
        composeRule.onRoot(useUnmergedTree = true)
        composeRule.onAllNodesWithTag("Occurrence_item_view")[0].assertExists()
        composeRule.onAllNodesWithTag("Occurrence_item_view")[0].assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun `test that occurrences item view date formatted has padding`() {
        initComposeRuleContent(
            uiState = RecurringMeetingInfoState(
                occurrencesList = occursList2,
            ),
            managementState = ScheduledMeetingManagementState(),
        )
        composeRule.onRoot(useUnmergedTree = true)
        composeRule.onAllNodesWithTag("Occurrence_item_view_date_formatted")[0].assertExists()
    }

    @Test
    fun `test that occurrences item view has title`() {
        initComposeRuleContent(
            uiState = RecurringMeetingInfoState(
                occurrencesList = occursList2, schedTitle = "Title"
            ),
            managementState = ScheduledMeetingManagementState(),
        )
        composeRule.onRoot(useUnmergedTree = true)
        composeRule.onAllNodesWithTag("Occurrence_item_view_title")[0].assertExists()
    }

    @Test
    fun `test that occurrence frequency is daily`() {
        initComposeRuleContent(
            uiState = RecurringMeetingInfoState(
                typeOccurs = OccurrenceFrequencyType.Daily,
            ),
            managementState = ScheduledMeetingManagementState(),
        )
        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_occurs_daily_subtitle)
            .assertExists()
    }

    @Test
    fun `test that occurrence frequency is Weekly`() {
        initComposeRuleContent(
            uiState = RecurringMeetingInfoState(
                typeOccurs = OccurrenceFrequencyType.Weekly,
            ),
            managementState = ScheduledMeetingManagementState(),
        )

        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_occurs_weekly_subtitle)
            .assertExists()
    }

    @Test
    fun `test that occurrence frequency is Monthly`() {
        initComposeRuleContent(
            uiState = RecurringMeetingInfoState(
                typeOccurs = OccurrenceFrequencyType.Monthly,
            ),
            managementState = ScheduledMeetingManagementState(),
        )
        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_occurs_monthly_subtitle)
            .assertExists()
    }

    @Test
    fun `test that see more button is shown`() {
        initComposeRuleContent(
            uiState = RecurringMeetingInfoState(
                showSeeMoreButton = true,
            ),
            managementState = ScheduledMeetingManagementState(),
        )
        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_see_more_occurrences_button)
            .assertExists()
    }

    @Test
    fun `test that see more button performs action`() {
        val onSeeMoreClicked = mock<() -> Unit>()
        initComposeRuleContent(
            uiState = RecurringMeetingInfoState(
                showSeeMoreButton = true,
            ),
            managementState = ScheduledMeetingManagementState(),
            onSeeMoreClicked = onSeeMoreClicked,
        )

        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_see_more_occurrences_button)
            .performClick()
        verify(onSeeMoreClicked).invoke()
    }

    private fun initComposeRuleContent(
        uiState: RecurringMeetingInfoState,
        managementState: ScheduledMeetingManagementState,
    ) {
        composeRule.setContent {
            RecurringMeetingInfoView(
                state = uiState,
                managementState = managementState,
                onScrollChange = { },
                onBackPressed = { },
                onOccurrenceClicked = { },
                onSeeMoreClicked = { },
                onCancelOccurrenceClicked = { },
                onEditOccurrenceClicked = { },
                onConsumeSelectOccurrenceEvent = { },
                onResetSnackbarMessage = { },
                onCancelOccurrence = { },
                onCancelOccurrenceAndMeeting = { },
                onEditOccurrence = { },
                onDismissDialog = { },
                onDateTap = { },
                onStartTimeTap = { },
                onEndTimeTap = { },
            )
        }
    }


    private fun initComposeRuleContent(
        uiState: RecurringMeetingInfoState,
        managementState: ScheduledMeetingManagementState,
        onSeeMoreClicked: () -> Unit,
    ) {
        composeRule.setContent {
            RecurringMeetingInfoView(
                state = uiState,
                managementState = managementState,
                onScrollChange = { },
                onBackPressed = { },
                onOccurrenceClicked = { },
                onSeeMoreClicked = onSeeMoreClicked,
                onCancelOccurrenceClicked = { },
                onEditOccurrenceClicked = { },
                onConsumeSelectOccurrenceEvent = { },
                onResetSnackbarMessage = { },
                onCancelOccurrence = { },
                onCancelOccurrenceAndMeeting = { },
                onEditOccurrence = { },
                onDismissDialog = { },
                onDateTap = { },
                onStartTimeTap = { },
                onEndTimeTap = { },
            )
        }
    }
}

