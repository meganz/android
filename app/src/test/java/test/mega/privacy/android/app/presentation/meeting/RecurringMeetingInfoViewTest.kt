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
            RecurringMeetingInfoState(
                occurrencesList = occursList1,
            )
        )

        composeRule.onNodeWithTag("Occurrence_list_view").assertExists()
    }

    @Test
    fun `test that occurrences item view exists`() {
        initComposeRuleContent(
            RecurringMeetingInfoState(
                occurrencesList = occursList1,
            )
        )
        composeRule.onRoot(useUnmergedTree = true)
        composeRule.onAllNodesWithTag("Occurrence_item_view")[0].assertExists()
        composeRule.onAllNodesWithTag("Occurrence_item_view")[0].assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun `test that occurrences item view date formatted has padding`() {
        initComposeRuleContent(
            RecurringMeetingInfoState(
                occurrencesList = occursList2,
            )
        )
        composeRule.onRoot(useUnmergedTree = true)
        composeRule.onAllNodesWithTag("Occurrence_item_view_date_formatted")[0].assertExists()
    }

    @Test
    fun `test that occurrences item view has title`() {
        initComposeRuleContent(
            RecurringMeetingInfoState(
                occurrencesList = occursList2,
                schedTitle = "Title"
            )
        )
        composeRule.onRoot(useUnmergedTree = true)
        composeRule.onAllNodesWithTag("Occurrence_item_view_title")[0].assertExists()
    }

    @Test
    fun `test that occurrence frequency is daily`() {
        initComposeRuleContent(
            RecurringMeetingInfoState(
                typeOccurs = OccurrenceFrequencyType.Daily,
            )
        )
        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_occurs_daily_subtitle)
            .assertExists()
    }

    @Test
    fun `test that occurrence frequency is Weekly`() {
        initComposeRuleContent(
            RecurringMeetingInfoState(
                typeOccurs = OccurrenceFrequencyType.Weekly,
            )
        )

        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_occurs_weekly_subtitle)
            .assertExists()
    }

    @Test
    fun `test that occurrence frequency is Monthly`() {
        initComposeRuleContent(
            RecurringMeetingInfoState(
                typeOccurs = OccurrenceFrequencyType.Monthly,
            )
        )
        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_occurs_monthly_subtitle)
            .assertExists()
    }

    @Test
    fun `test that see more button is shown`() {
        initComposeRuleContent(
            RecurringMeetingInfoState(
                showSeeMoreButton = true,
            )
        )
        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_see_more_occurrences_button)
            .assertExists()
    }

    @Test
    fun `test that see more button performs action`() {
        val onSeeMoreClicked = mock<() -> Unit>()
        initComposeRuleContent(
            RecurringMeetingInfoState(
                showSeeMoreButton = true,
            ),
            onSeeMoreClicked = onSeeMoreClicked
        )

        composeRule.onNodeWithText(R.string.meetings_recurring_meeting_info_see_more_occurrences_button)
            .performClick()
        verify(onSeeMoreClicked).invoke()
    }

    private fun initComposeRuleContent(
        uiState: RecurringMeetingInfoState,
    ) {
        composeRule.setContent {
            RecurringMeetingInfoView(
                state = uiState,
                onScrollChange = { },
                onBackPressed = { },
                onOccurrenceClicked = { },
                onSeeMoreClicked = { },
            )
        }
    }


    private fun initComposeRuleContent(
        uiState: RecurringMeetingInfoState,
        onSeeMoreClicked: () -> Unit,
    ) {
        composeRule.setContent {
            RecurringMeetingInfoView(
                state = uiState,
                onScrollChange = { },
                onBackPressed = { },
                onOccurrenceClicked = { },
                onSeeMoreClicked = onSeeMoreClicked,
            )
        }
    }
}

