package test.mega.privacy.android.app.upgradeAccount.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.view.components.CURRENT_PLAN_TAG
import mega.privacy.android.app.upgradeAccount.view.components.ProPlanInfoCard
import mega.privacy.android.app.upgradeAccount.view.components.RECOMMENDED_PLAN_TAG
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProPlanInfoCardTest {
    @get:Rule
    var composeRule = createComposeRule()

    private val localisedPriceStringMapper = LocalisedPriceStringMapper()
    private val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    private val formattedSizeMapper = FormattedSizeMapper()

    private val subscriptionProI = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        storage = 2048,
        monthlyTransfer = 2048,
        yearlyTransfer = 24576,
        monthlyAmount = CurrencyAmount(9.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            99.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    @Test
    fun `test that Current plan label is displayed if showCurrentPlanLabel is true`() {
        composeRule.setContent {
            ProPlanInfoCard(
                proPlan = AccountType.PRO_I,
                subscription = subscriptionProI,
                isRecommended = false,
                onPlanClicked = {},
                isMonthly = true,
                isClicked = false,
                showCurrentPlanLabel = true,
                testTag = "test",
            )
        }
        composeRule.onNodeWithTag("test$CURRENT_PLAN_TAG", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that Recommended label is displayed if isRecommended is true`() {
        composeRule.setContent {
            ProPlanInfoCard(
                proPlan = AccountType.PRO_I,
                subscription = subscriptionProI,
                isRecommended = true,
                onPlanClicked = {},
                isMonthly = true,
                isClicked = false,
                showCurrentPlanLabel = false,
                testTag = "test",
            )
        }
        composeRule.onNodeWithTag("test$RECOMMENDED_PLAN_TAG", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}