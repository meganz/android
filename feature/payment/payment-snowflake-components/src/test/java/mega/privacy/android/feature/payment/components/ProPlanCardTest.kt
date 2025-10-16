package mega.privacy.android.feature.payment.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProPlanCardTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that ProPlanCard shows all key UI elements and texts`() {
        val planName = "Pro II"
        val storage = "8 TB storage"
        val transfer = "96 TB transfer"
        val price = "€16.67"
        val billingInfo = "€199.99 billed yearly"
        composeRule.setContent {
            ProPlanCard(
                modifier = Modifier.fillMaxWidth(),
                planName = planName,
                isRecommended = true,
                isSelected = true,
                storage = storage,
                transfer = transfer,
                price = price,
                billingInfo = billingInfo,
                isCurrentPlan = false,
                onSelected = {}
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_TITLE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_RECOMMENDED, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_RADIO).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_STORAGE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_TRANSFER, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_PRICE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_PRICE_UNIT, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_BILLING_INFO, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText(planName).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.account_upgrade_account_pro_plan_info_recommended_label))
            .assertExists()
        composeRule.onNodeWithText(storage).assertExists()
        composeRule.onNodeWithText(transfer).assertExists()
        composeRule.onNodeWithText(price).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.general_month))
            .assertExists()
        composeRule.onNodeWithText(billingInfo).assertExists()
    }

    @Test
    fun `test that ProPlanCard shows current plan badge when isCurrentPlan is true`() {
        val planName = "Pro II"
        val storage = "8 TB storage"
        val transfer = "96 TB transfer"
        val price = "€16.67"
        val billingInfo = "€199.99 billed yearly"
        composeRule.setContent {
            ProPlanCard(
                modifier = Modifier.fillMaxWidth(),
                planName = planName,
                isRecommended = false,
                isSelected = false,
                storage = storage,
                transfer = transfer,
                price = price,
                billingInfo = billingInfo,
                isCurrentPlan = true,
                onSelected = {}
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_CURRENT_PLAN, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.account_upgrade_account_pro_plan_info_current_plan_label)
        ).assertExists()
    }

    @Test
    fun `test that ProPlanCard does not billing info when billingInfo is null`() {
        val planName = "Pro II"
        val storage = "8 TB storage"
        val transfer = "96 TB transfer"
        val price = "€16.67"
        composeRule.setContent {
            ProPlanCard(
                modifier = Modifier.fillMaxWidth(),
                planName = planName,
                isRecommended = false,
                isSelected = false,
                storage = storage,
                transfer = transfer,
                price = price,
                billingInfo = null,
                isCurrentPlan = true,
                onSelected = {}
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_BILLING_INFO, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that ProPlanCard hides radio button when isCurrentPlan is true`() {
        val planName = "Pro II"
        val storage = "8 TB storage"
        val transfer = "96 TB transfer"
        val price = "€16.67"
        val billingInfo = "€199.99 billed yearly"
        composeRule.setContent {
            ProPlanCard(
                modifier = Modifier.fillMaxWidth(),
                planName = planName,
                isRecommended = false,
                isSelected = false,
                storage = storage,
                transfer = transfer,
                price = price,
                billingInfo = billingInfo,
                isCurrentPlan = true,
                onSelected = {}
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_RADIO).assertDoesNotExist()
    }

    @Test
    fun `test that ProPlanCard shows radio button when isCurrentPlan is false`() {
        val planName = "Pro II"
        val storage = "8 TB storage"
        val transfer = "96 TB transfer"
        val price = "€16.67"
        val billingInfo = "€199.99 billed yearly"
        composeRule.setContent {
            ProPlanCard(
                modifier = Modifier.fillMaxWidth(),
                planName = planName,
                isRecommended = false,
                isSelected = false,
                storage = storage,
                transfer = transfer,
                price = price,
                billingInfo = billingInfo,
                isCurrentPlan = false,
                onSelected = {}
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_RADIO).assertExists()
    }

    @Test
    fun `test that ProPlanCard is not clickable when isCurrentPlan is true`() {
        var clickCount = 0
        val planName = "Pro II"
        val storage = "8 TB storage"
        val transfer = "96 TB transfer"
        val price = "€16.67"
        val billingInfo = "€199.99 billed yearly"
        
        composeRule.setContent {
            ProPlanCard(
                modifier = Modifier.fillMaxWidth(),
                planName = planName,
                isRecommended = false,
                isSelected = false,
                storage = storage,
                transfer = transfer,
                price = price,
                billingInfo = billingInfo,
                isCurrentPlan = true,
                onSelected = { clickCount++ }
            )
        }
        
        // Try to click on the card
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD).performClick()
        
        // Verify that the click was not handled (clickCount should remain 0)
        assert(clickCount == 0) { "Card should not be clickable when isCurrentPlan is true" }
    }

    @Test
    fun `test that ProPlanCard is clickable when isCurrentPlan is false`() {
        var clickCount = 0
        val planName = "Pro II"
        val storage = "8 TB storage"
        val transfer = "96 TB transfer"
        val price = "€16.67"
        val billingInfo = "€199.99 billed yearly"
        
        composeRule.setContent {
            ProPlanCard(
                modifier = Modifier.fillMaxWidth(),
                planName = planName,
                isRecommended = false,
                isSelected = false,
                storage = storage,
                transfer = transfer,
                price = price,
                billingInfo = billingInfo,
                isCurrentPlan = false,
                onSelected = { clickCount++ }
            )
        }
        
        // Click on the card
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD).performClick()
        
        // Verify that the click was handled (clickCount should be 1)
        assert(clickCount == 1) { "Card should be clickable when isCurrentPlan is false" }
    }

    @Test
    fun `test that ProPlanCard radio button is clickable when isCurrentPlan is false`() {
        var clickCount = 0
        val planName = "Pro II"
        val storage = "8 TB storage"
        val transfer = "96 TB transfer"
        val price = "€16.67"
        val billingInfo = "€199.99 billed yearly"
        
        composeRule.setContent {
            ProPlanCard(
                modifier = Modifier.fillMaxWidth(),
                planName = planName,
                isRecommended = false,
                isSelected = false,
                storage = storage,
                transfer = transfer,
                price = price,
                billingInfo = billingInfo,
                isCurrentPlan = false,
                onSelected = { clickCount++ }
            )
        }
        
        // Click on the radio button
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_RADIO).performClick()
        
        // Verify that the click was handled (clickCount should be 1)
        assert(clickCount == 1) { "Radio button should be clickable when isCurrentPlan is false" }
    }
} 