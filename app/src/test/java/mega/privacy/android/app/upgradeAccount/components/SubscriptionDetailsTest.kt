package mega.privacy.android.app.upgradeAccount.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.upgradeAccount.view.GOOGLE_PLAY_STORE_SUBSCRIPTION_LINK_TAG
import mega.privacy.android.app.upgradeAccount.view.SUBSCRIPTION_DETAILS_DESCRIPTION_TAG
import mega.privacy.android.app.upgradeAccount.view.SUBSCRIPTION_DETAILS_TITLE_TAG
import mega.privacy.android.app.upgradeAccount.view.components.SubscriptionDetails
import mega.privacy.android.domain.entity.AccountType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewTest.Companion.expectedLocalisedSubscriptionsList

@RunWith(AndroidJUnit4::class)
internal class SubscriptionDetailsTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that subscription details are shown correctly when selected plan is monthly Pro Lite`() {
        composeRule.setContent {
            SubscriptionDetails(
                onLinkClick = {},
                chosenPlan = AccountType.PRO_LITE,
                subscriptionList = expectedLocalisedSubscriptionsList,
                isMonthly = true
            )
        }

        composeRule.onNodeWithTag(GOOGLE_PLAY_STORE_SUBSCRIPTION_LINK_TAG)
            .assertExists()
        composeRule.onNodeWithTag(SUBSCRIPTION_DETAILS_TITLE_TAG).assertExists()
        composeRule.onNodeWithTag("${SUBSCRIPTION_DETAILS_DESCRIPTION_TAG}_monthly_lite_with_price")
            .assertExists()
    }

    @Test
    fun `test that subscription details are shown correctly when selected plan is yearly Pro III`() {
        composeRule.setContent {
            SubscriptionDetails(
                onLinkClick = {},
                chosenPlan = AccountType.PRO_III,
                subscriptionList = expectedLocalisedSubscriptionsList,
                isMonthly = false
            )
        }

        composeRule.onNodeWithTag(GOOGLE_PLAY_STORE_SUBSCRIPTION_LINK_TAG)
            .assertExists()
        composeRule.onNodeWithTag(SUBSCRIPTION_DETAILS_TITLE_TAG).assertExists()
        composeRule.onNodeWithTag("${SUBSCRIPTION_DETAILS_DESCRIPTION_TAG}_yearly_pro_iii_with_price")
            .assertExists()
    }

    @Test
    fun `test that subscription details are shown correctly when no plan is selected and monthly tab is shown`() {
        composeRule.setContent {
            SubscriptionDetails(
                onLinkClick = {},
                chosenPlan = AccountType.FREE,
                subscriptionList = expectedLocalisedSubscriptionsList,
                isMonthly = true
            )
        }

        composeRule.onNodeWithTag(GOOGLE_PLAY_STORE_SUBSCRIPTION_LINK_TAG)
            .assertExists()
        composeRule.onNodeWithTag(SUBSCRIPTION_DETAILS_TITLE_TAG).assertExists()
        composeRule.onNodeWithTag("${SUBSCRIPTION_DETAILS_DESCRIPTION_TAG}_monthly_no_price")
            .assertExists()
    }
}