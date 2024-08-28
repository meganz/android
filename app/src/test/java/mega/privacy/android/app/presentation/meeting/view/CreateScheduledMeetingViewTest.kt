package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.model.CreateScheduledMeetingState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementUiState
import mega.privacy.android.app.presentation.meeting.view.CreateScheduledMeetingView
import mega.privacy.android.app.presentation.meeting.view.UPGRADE_ACCOUNT_BUTTON_TAG
import mega.privacy.android.domain.entity.AccountType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class CreateScheduledMeetingViewTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that free plan limit warning is shown`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                startDate = ZonedDateTime.now(),
                endDate = ZonedDateTime.now().plus(80, ChronoUnit.MINUTES)
            ),
            ScheduledMeetingManagementUiState(
                isCallUnlimitedProPlanFeatureFlagEnabled = true,
                subscriptionPlan = AccountType.FREE
            )
        )
        composeRule.onNodeWithTag(UPGRADE_ACCOUNT_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that free plan limit warning is hidden`() {
        initComposeRuleContent(
            CreateScheduledMeetingState(
                startDate = ZonedDateTime.now(),
                endDate = ZonedDateTime.now().plus(10, ChronoUnit.MINUTES)
            ), ScheduledMeetingManagementUiState(isCallUnlimitedProPlanFeatureFlagEnabled = false)
        )
        composeRule.onNodeWithTag(UPGRADE_ACCOUNT_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that onUpgradeNowClicked is clicked`() {
        val mock = mock<() -> Unit>()
        composeRule.setContent {
            CreateScheduledMeetingView(
                state = CreateScheduledMeetingState(
                    startDate = ZonedDateTime.now(),
                    endDate = ZonedDateTime.now().plus(80, ChronoUnit.MINUTES)
                ),
                managementState = ScheduledMeetingManagementUiState(
                    isCallUnlimitedProPlanFeatureFlagEnabled = true,
                    subscriptionPlan = AccountType.FREE
                ),
                onDiscardClicked = { },
                onAcceptClicked = { },
                onStartTimeClicked = { },
                onStartDateClicked = { },
                onEndTimeClicked = { },
                onEndDateClicked = { },
                onDismiss = { },
                onResetSnackbarMessage = { },
                onDiscardMeetingDialog = { },
                onCloseWarningClicked = { },
                onUpgradeNowClicked = mock,
                onDescriptionValueChange = { },
                onTitleValueChange = { },
                onScrollChange = { },
                onRecurrenceDialogOptionClicked = { },
            )
        }
        composeRule.onNodeWithTag(UPGRADE_ACCOUNT_BUTTON_TAG).performClick()
        verify(mock).invoke()
    }

    private fun initComposeRuleContent(
        uiState: CreateScheduledMeetingState,
        managementState: ScheduledMeetingManagementUiState,
    ) {
        composeRule.setContent {
            CreateScheduledMeetingView(
                state = uiState,
                managementState = managementState,
                onDiscardClicked = { },
                onAcceptClicked = { },
                onStartTimeClicked = { },
                onStartDateClicked = { },
                onEndTimeClicked = { },
                onEndDateClicked = { },
                onDismiss = { },
                onResetSnackbarMessage = { },
                onDiscardMeetingDialog = { },
                onCloseWarningClicked = { },
                onUpgradeNowClicked = { },
                onDescriptionValueChange = { },
                onTitleValueChange = { },
                onScrollChange = { },
                onRecurrenceDialogOptionClicked = { },
            )
        }
    }
}
