package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.onNodeWithTag
import mega.privacy.android.app.presentation.meeting.dialog.view.CANCEL_OCCURRENCE_TAG
import mega.privacy.android.app.presentation.meeting.dialog.view.RecurringMeetingOccurrenceBottomSheetView
import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingInfoState
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.dialog.view.EDIT_OCCURRENCE_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class RecurringMeetingOccurrenceBottomSheetViewTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private val parentSchedId = 123456L
    private val schedId = 789123L

    private val sheetState = ModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        isSkipHalfExpanded = false,
    )

    @Test
    fun `test that Cancel button is shown`() {
        initComposeRuleContent(
            onCancelClick = {},
            onEditClick = {}
        )
        composeTestRule.onNodeWithTag(CANCEL_OCCURRENCE_TAG).assertExists()
    }

    @Test
    fun `test that verify Cancel button performs action`() {
        val onCancelClicked = mock<() -> Unit>()
        initComposeRuleContent(
            onCancelClick = onCancelClicked,
            onEditClick = {}
        )

        composeTestRule.onNodeWithTag(CANCEL_OCCURRENCE_TAG).performClick()
        verify(onCancelClicked).invoke()
    }

    @Test
    fun `test that Edit button is shown`() {
        initComposeRuleContent(
            onCancelClick = {},
            onEditClick = {}
        )
        composeTestRule.onNodeWithTag(EDIT_OCCURRENCE_TAG).assertExists()
    }

    @Test
    fun `test that verify Edit button performs action`() {
        val onEditClicked = mock<() -> Unit>()
        initComposeRuleContent(
            onCancelClick = { },
            onEditClick = onEditClicked
        )

        composeTestRule.onNodeWithTag(EDIT_OCCURRENCE_TAG).performClick()
        verify(onEditClicked).invoke()
    }

    private fun getOccurrence(): ChatScheduledMeetingOccurr =
        ChatScheduledMeetingOccurr(
            schedId = schedId,
            parentSchedId = parentSchedId,
            isCancelled = false,
            timezone = null,
            startDateTime = -1,
            endDateTime = -1,
            overrides = null,
        )

    private fun initComposeRuleContent(
        onCancelClick: () -> Unit,
        onEditClick: () -> Unit,
    ) {
        composeTestRule.setContent {
            val coroutineScope = rememberCoroutineScope()

            RecurringMeetingOccurrenceBottomSheetView(
                modalSheetState = sheetState,
                coroutineScope = coroutineScope,
                meetingState = RecurringMeetingInfoState(),
                occurrence = getOccurrence(),
                onCancelClick = onCancelClick,
                onEditClick = onEditClick
            )
        }
    }
}