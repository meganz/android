package mega.privacy.android.app.main.ads

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale


@RunWith(AndroidJUnit4::class)
class AdsFreeIntroViewTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val subscriptionProLite = LocalisedSubscription(
        accountType = AccountType.PRO_LITE,
        storage = 400,
        monthlyTransfer = 1024,
        yearlyTransfer = 12288,
        monthlyAmount = CurrencyAmount(4.99F, Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            49.99F,
            Currency("EUR")
        ),
        localisedPrice = LocalisedPriceStringMapper(),
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