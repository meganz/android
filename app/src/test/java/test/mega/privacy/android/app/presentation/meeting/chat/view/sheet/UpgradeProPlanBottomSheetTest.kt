package test.mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.UPGRADE_IMAGE_TEST_TAG
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.UpgradeProPlanBottomSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class UpgradeProPlanBottomSheetTest {

    @get:Rule
    var composeRule = createComposeRule()

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
            performClick()
        }
        verify(onUpgrade).invoke()
    }


    private fun initComposeRule() {
        composeRule.setContent {
            UpgradeProPlanBottomSheet(
                onUpgrade = onUpgrade
            )
        }
    }
}
