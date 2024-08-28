package mega.privacy.android.app.presentation.meeting.view.sheet

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.app.presentation.meeting.view.sheet.BOTTOM_SHEET_ASSIGN_AND_LEAVE_BUTTON
import mega.privacy.android.app.presentation.meeting.view.sheet.BOTTOM_SHEET_CONTAINER
import mega.privacy.android.app.presentation.meeting.view.sheet.BOTTOM_SHEET_END_FOR_ALL_BUTTON
import mega.privacy.android.app.presentation.meeting.view.sheet.BOTTOM_SHEET_HEADER
import mega.privacy.android.app.presentation.meeting.view.sheet.BOTTOM_SHEET_LEAVE_ANYWAY_BUTTON
import mega.privacy.android.app.presentation.meeting.view.sheet.LeaveMeetingBottomSheetView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class LeaveMeetingBottomSheetViewTest {
    @get:Rule
    var composeTestRule = createComposeRule()


    @Test
    fun `test that Container is shown`() {
        initComposeRuleContent(
            onAssignAndLeaveClick = {},
            onLeaveAnywayClick = {},
            onEndForAllClick = {},
        )
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_CONTAINER).assertExists()
    }

    @Test
    fun `test that Header is shown`() {
        initComposeRuleContent(
            onAssignAndLeaveClick = {},
            onLeaveAnywayClick = {},
            onEndForAllClick = {}
        )
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_HEADER).assertExists()
    }

    @Test
    fun `test that Assign and leave button is shown`() {
        initComposeRuleContent(
            onAssignAndLeaveClick = {},
            onLeaveAnywayClick = {},
            onEndForAllClick = {}
        )
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_ASSIGN_AND_LEAVE_BUTTON).assertExists()
    }

    @Test
    fun `test that verify Assign and leave button performs action`() {
        val onAssignAndLeaveClick = mock<() -> Unit>()
        initComposeRuleContent(
            onAssignAndLeaveClick = onAssignAndLeaveClick,
            onLeaveAnywayClick = {},
            onEndForAllClick = {}
        )

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_ASSIGN_AND_LEAVE_BUTTON).performClick()
        verify(onAssignAndLeaveClick).invoke()
    }

    @Test
    fun `test that Leave anyway button is shown`() {
        initComposeRuleContent(
            onAssignAndLeaveClick = {},
            onLeaveAnywayClick = {},
            onEndForAllClick = {}
        )
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_LEAVE_ANYWAY_BUTTON).assertExists()
    }

    @Test
    fun `test that verify Leave anyway button performs action`() {
        val onLeaveAnywayClick = mock<() -> Unit>()
        initComposeRuleContent(
            onAssignAndLeaveClick = {},
            onLeaveAnywayClick = onLeaveAnywayClick,
            onEndForAllClick = {}
        )

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_LEAVE_ANYWAY_BUTTON).performClick()
        verify(onLeaveAnywayClick).invoke()
    }

    @Test
    fun `test that End for all button is shown`() {
        initComposeRuleContent(
            onAssignAndLeaveClick = {},
            onLeaveAnywayClick = {},
            onEndForAllClick = {}
        )
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_END_FOR_ALL_BUTTON).assertExists()
    }

    @Test
    fun `test that verify End for all button performs action`() {
        val onEndForAllClick = mock<() -> Unit>()
        initComposeRuleContent(
            onAssignAndLeaveClick = {},
            onLeaveAnywayClick = {},
            onEndForAllClick = onEndForAllClick
        )

        composeTestRule.onNodeWithTag(BOTTOM_SHEET_END_FOR_ALL_BUTTON).performClick()
        composeTestRule.waitForIdle()
        verify(onEndForAllClick).invoke()
    }

    private fun initComposeRuleContent(
        onAssignAndLeaveClick: () -> Unit,
        onLeaveAnywayClick: () -> Unit,
        onEndForAllClick: () -> Unit,
    ) {
        composeTestRule.setContent {
            val coroutineScope = rememberCoroutineScope()

            LeaveMeetingBottomSheetView(
                state = InMeetingUiState(showEndMeetingAsOnlyHostBottomPanel = true),
                onAssignAndLeaveClick = onAssignAndLeaveClick,
                onLeaveAnywayClick = onLeaveAnywayClick,
                onEndForAllClick = onEndForAllClick,
                onDismiss = {}
            )
        }
    }
}