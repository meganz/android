package mega.privacy.android.feature.payment.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
        val priceUnit = "month"
        val billingInfo = "€199.99 billed yearly"
        composeRule.setContent {
            ProPlanCard(
                planName = planName,
                isRecommended = true,
                isSelected = true,
                storage = storage,
                transfer = transfer,
                price = price,
                priceUnit = priceUnit,
                billingInfo = billingInfo,
                onSelected = {}
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_TITLE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_RECOMMENDED).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_RADIO).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_STORAGE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_TRANSFER).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_PRICE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_PRICE_UNIT).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_PRO_PLAN_CARD_BILLING_INFO).assertExists()
        composeRule.onNodeWithText(planName).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.account_upgrade_account_pro_plan_info_recommended_label))
            .assertExists()
        composeRule.onNodeWithText(storage).assertExists()
        composeRule.onNodeWithText(transfer).assertExists()
        composeRule.onNodeWithText(price).assertExists()
        composeRule.onNodeWithText("/$priceUnit").assertExists()
        composeRule.onNodeWithText(billingInfo).assertExists()
    }
} 