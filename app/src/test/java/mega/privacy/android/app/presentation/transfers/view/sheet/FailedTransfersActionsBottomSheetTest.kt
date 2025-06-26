package mega.privacy.android.app.presentation.transfers.view.sheet

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.mobile.analytics.event.FailedTransfersClearAllMenuItemEvent
import mega.privacy.mobile.analytics.event.FailedTransfersRetryAllMenuItemEvent
import mega.privacy.mobile.analytics.event.FailedTransfersSelectMenuItemEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class FailedTransfersActionsBottomSheetTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val onRetryAllTransfers = mock<() -> Unit>()
    private val onClearAllTransfers = mock<() -> Unit>()
    private val onDismissSheet = mock<() -> Unit>()
    private val onSelectTransfers = mock<() -> Unit>()

    @Test
    fun `test that sheet shows correctly`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_FAILED_ACTIONS_PANEL).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_RETRY_ALL_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.option_to_retry_transfers).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CLEAR_ALL_FAILED_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.option_to_clear_transfers).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on retry all transfers option invokes onRetryAllTransfers and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_RETRY_ALL_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onRetryAllTransfers).invoke()
        verify(onDismissSheet).invoke()
    }

    @Test
    fun `test that clicking on clear all transfers option invokes onClearAllTransfers and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_CLEAR_ALL_FAILED_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onClearAllTransfers).invoke()
        verify(onDismissSheet).invoke()
    }

    @Test
    fun `test that clicking on select transfers option invokes onSelectTransfers and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_SELECT_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onSelectTransfers).invoke()
        verify(onDismissSheet).invoke()
    }

    @Test
    fun `test select menu event is tracked when select action si clicked`() {
        initComposeTestRule()
        composeTestRule.onNodeWithTag(TEST_TAG_SELECT_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertThat(analyticsRule.events).contains(FailedTransfersSelectMenuItemEvent)
    }

    @Test
    fun `test retry all menu event is tracked when retry all action si clicked`() {
        initComposeTestRule()
        composeTestRule.onNodeWithTag(TEST_TAG_RETRY_ALL_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertThat(analyticsRule.events).contains(FailedTransfersRetryAllMenuItemEvent)
    }

    @Test
    fun `test clear all menu event is tracked when clear all action si clicked`() {
        initComposeTestRule()
        composeTestRule.onNodeWithTag(TEST_TAG_CLEAR_ALL_FAILED_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertThat(analyticsRule.events).contains(FailedTransfersClearAllMenuItemEvent)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun initComposeTestRule() {
        composeTestRule.setContent {
            FailedTransfersActionsBottomSheet(
                onRetryAllTransfers = onRetryAllTransfers,
                onClearAllTransfers = onClearAllTransfers,
                onSelectTransfers = onSelectTransfers,
                onDismissSheet = onDismissSheet,
            )
        }
    }
}