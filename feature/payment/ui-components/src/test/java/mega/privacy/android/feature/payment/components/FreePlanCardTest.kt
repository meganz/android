package mega.privacy.android.feature.payment.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.resources.R as shareR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreePlanCardTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that FreePlanCard shows all key UI elements and texts`() {
        composeRule.setContent {
            FreePlanCard(onContinue = {})
        }
        composeRule.onNodeWithTag(TEST_TAG_FREE_PLAN_CARD).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_FREE_PLAN_CARD_TITLE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_FREE_PLAN_CARD_DESCRIPTION).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_FREE_PLAN_CARD_BUTTON).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(shareR.string.free_plan_card_title))
            .assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(shareR.string.free_plan_card_description))
            .assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(shareR.string.free_plan_card_storage_feature))
            .assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(shareR.string.free_plan_card_transfer_feature))
            .assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(shareR.string.free_plan_card_button))
            .assertExists()
    }
}