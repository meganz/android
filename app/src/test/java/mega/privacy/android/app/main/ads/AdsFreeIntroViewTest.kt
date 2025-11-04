package mega.privacy.android.app.main.ads

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale


@RunWith(AndroidJUnit4::class)
class AdsFreeIntroViewTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val subscriptionProLiteMonthly = Subscription(
        sku = "pro_lite_monthly",
        accountType = AccountType.PRO_LITE,
        handle = -4226692769210777158,
        storage = 400,
        transfer = 1024,
        amount = CurrencyAmount(4.99F, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProLiteYearly = Subscription(
        sku = "pro_lite_yearly",
        accountType = AccountType.PRO_LITE,
        handle = -5517769810977460898,
        storage = 400,
        transfer = 12288,
        amount = CurrencyAmount(49.99F, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProLite = LocalisedSubscription(
        monthlySubscription = subscriptionProLiteMonthly,
        yearlySubscription = subscriptionProLiteYearly,
        localisedPriceCurrencyCode = LocalisedPriceCurrencyCodeStringMapper(),
        formattedSize = FormattedSizeMapper(),
    )

    @Test
    fun `test that ads free intro view shows correctly`() {
        val formattedPrice =
            subscriptionProLite.localisePriceCurrencyCode(Locale.getDefault(), true)
        val formattedStorage =
            subscriptionProLite.formatStorageSize(usePlaceholder = false)
        val minimalStorageValueAndUnit =
            formattedStorage.let { "${it.size} ${composeTestRule.activity.getString(it.unit)}" }
        initAdsFreeIntroView()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                sharedR.string.payment_ads_free_intro_description,
                formattedPrice.price
            )
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_generous_storage_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                sharedR.string.payment_ads_free_intro_generous_storage_description,
                minimalStorageValueAndUnit
            )
        )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_transfer_sharing_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_transfer_sharing_description))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_additional_security_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_additional_security_description))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(ADS_FREE_IMAGE_TEST_TAG)
            .assertExists()
    }

    private fun initAdsFreeIntroView() {
        composeTestRule.setContent {
            AdsFreeIntroContent(
                uiState = AdsFreeIntroUiState(
                    cheapestSubscriptionAvailable = subscriptionProLite
                ),
                onDismiss = {}
            )
        }
    }
}