package mega.privacy.android.feature.chat.meeting.left.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import mega.android.core.ui.theme.AndroidThemeForPreviews
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.chat.meeting.left.model.LeftMeetingUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
class LeftMeetingScreenTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that the sign up invitation is shown`() {
        setContent()

        composeRule.onNodeWithTag(LEFT_MEETING_HEADLINE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LEFT_MEETING_ILLUSTRATION_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LEFT_MEETING_CREATE_ACCOUNT_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that onCloseClick is invoked when the close icon is clicked`() {
        val onCloseClick = mock<() -> Unit>()
        setContent(onCloseClick = onCloseClick)

        composeRule.onNodeWithContentDescription(
            "Navigation Icon",
            substring = true,
            ignoreCase = true,
            useUnmergedTree = true,
        ).performClick()

        verify(onCloseClick).invoke()
    }

    @Test
    fun `test that onCreateAccountClick is invoked when the sign up button is clicked`() {
        val onCreateAccountClick = mock<() -> Unit>()
        setContent(onCreateAccountClick = onCreateAccountClick)

        composeRule.onNodeWithTag(LEFT_MEETING_CREATE_ACCOUNT_TAG).performClick()

        verify(onCreateAccountClick).invoke()
    }

    @Test
    fun `test that the free plan limit dialog is not shown when there is no notice`() {
        setContent()

        composeRule.onNodeWithTag(LEFT_MEETING_FREE_PLAN_LIMIT_DIALOG_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the free plan limit dialog is shown when the call hit the free plan limit`() {
        setContent(uiState = LeftMeetingUiState.FreePlanParticipantsLimit)

        composeRule.onNodeWithTag(LEFT_MEETING_FREE_PLAN_LIMIT_DIALOG_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that onNoticeShown is invoked when the free plan limit dialog is acknowledged`() {
        val onNoticeShown = mock<() -> Unit>()
        setContent(
            uiState = LeftMeetingUiState.FreePlanParticipantsLimit,
            onNoticeShown = onNoticeShown,
        )

        composeRule.onNodeWithText(
            composeRule.activity.getString(
                sharedR.string.meetings_schedule_meeting_free_plan_100_participants_limit_dialog_button
            )
        ).performClick()

        verify(onNoticeShown).invoke()
    }

    @Test
    fun `test that the too many participants snackbar is shown when the call was full`() {
        setContent(uiState = LeftMeetingUiState.TooManyParticipants)

        composeRule.onNodeWithText(
            composeRule.activity.getString(sharedR.string.call_error_too_many_participants)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that the too many participants snackbar is not shown when there is no notice`() {
        val onNoticeShown = mock<() -> Unit>()
        setContent(onNoticeShown = onNoticeShown)

        composeRule.onNodeWithText(
            composeRule.activity.getString(sharedR.string.call_error_too_many_participants)
        ).assertDoesNotExist()
        verifyNoInteractions(onNoticeShown)
    }

    private fun setContent(
        uiState: LeftMeetingUiState = LeftMeetingUiState.None,
        onCloseClick: () -> Unit = {},
        onCreateAccountClick: () -> Unit = {},
        onNoticeShown: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                LeftMeetingScreen(
                    uiState = uiState,
                    onCloseClick = onCloseClick,
                    onCreateAccountClick = onCreateAccountClick,
                    onNoticeShown = onNoticeShown,
                )
            }
        }
    }
}
