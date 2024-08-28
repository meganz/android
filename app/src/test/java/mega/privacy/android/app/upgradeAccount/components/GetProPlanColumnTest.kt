package mega.privacy.android.app.upgradeAccount.components

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TEXT
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TITLE
import mega.privacy.android.app.upgradeAccount.view.components.GetProPlanColumn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.fromId
import mega.privacy.android.app.upgradeAccount.VariantAOnboardingDialogViewTest.Companion.subscriptionProLite

@RunWith(AndroidJUnit4::class)
internal class GetProPlanColumnTest {
    @get:Rule
    var composeRule = createComposeRule()

    private fun setContent() {
        composeRule.setContent {
            GetProPlanColumn(
                state = ChooseAccountState(
                    cheapestSubscriptionAvailable = subscriptionProLite
                ),
                isLoading = false,
                bodyTextStyle = MaterialTheme.typography.subtitle2,
            )
        }
    }

    @Test
    fun `test that pro plan row is shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(PRO_PLAN_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PRO_PLAN_TEXT).assertExists()
    }

    @Test
    fun `test that pro plan row text is shown correctly`() {
        setContent()
        composeRule.onNodeWithText(fromId(R.string.dialog_onboarding_get_pro_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(fromId(R.string.dialog_onboarding_get_pro_description, "€4.99"))
            .assertExists()
    }
}