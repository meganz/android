package mega.privacy.android.app.upgradeAccount.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.view.SKIP_BUTTON
import mega.privacy.android.app.upgradeAccount.view.VIEW_PRO_PLAN_BUTTON
import mega.privacy.android.app.upgradeAccount.view.components.ButtonsRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.fromId

@RunWith(AndroidJUnit4::class)
internal class ButtonsRowTest {
    @get:Rule
    var composeRule = createComposeRule()

    private fun setContent() {
        composeRule.setContent {
            ButtonsRow(
                onSkipPressed = {},
                onViewPlansPressed = {},
                isLoading = false
            )
        }
    }

    @Test
    fun `test that skip button is shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(SKIP_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText(fromId(R.string.general_skip)).assertIsDisplayed()
    }

    @Test
    fun `test that upgrade to pro button is shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(VIEW_PRO_PLAN_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText(fromId(R.string.dialog_onboarding_button_view_pro_plan))
            .assertIsDisplayed()
    }
}