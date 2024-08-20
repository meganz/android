package test.mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.UPGRADE_IMAGE_TEST_TAG
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.UpgradeProPlanBottomSheet
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.mobile.analytics.event.UpgradeToProToGetUnlimitedCallsDialogEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class UpgradeProPlanBottomSheetTest {

    var composeRule = createComposeRule()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

    private val onUpgrade = mock<() -> Unit>()

    @Test
    fun `test that icon is shown`() {
        initComposeRule()
        composeRule.onNodeWithTag(UPGRADE_IMAGE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that title is shown`() {
        initComposeRule()
        composeRule.onNodeWithText(R.string.meetings_upgrade_pro_plan_title).assertIsDisplayed()
    }

    @Test
    fun `test that body is shown`() {
        initComposeRule()
        composeRule.onNodeWithText(R.string.meetings_upgrade_pro_plan_body).assertIsDisplayed()
    }

    @Test
    fun `test that button is shown`() {
        initComposeRule()
        composeRule.onNodeWithText(R.string.meetings_upgrade_pro_plan_button).apply {
            assertIsDisplayed()
        }
    }

    @Test
    fun `test that analytics tracker sends the right event when bottom sheet is shown`() {
        initComposeRule()
        assertThat(analyticsRule.events).contains(UpgradeToProToGetUnlimitedCallsDialogEvent)
    }


    private fun initComposeRule() {
        composeRule.setContent {
            UpgradeProPlanBottomSheet(
                hideSheet = onUpgrade
            )
        }
    }
}
