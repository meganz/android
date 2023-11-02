package test.mega.privacy.android.app.upgradeAccount

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.view.FEATURE_TITLE
import mega.privacy.android.app.upgradeAccount.view.IMAGE_TAG
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TEXT
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TITLE
import mega.privacy.android.app.upgradeAccount.view.SECURITY_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.SKIP_BUTTON
import mega.privacy.android.app.upgradeAccount.view.STORAGE_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.TRANSFER_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.VIEW_PRO_PLAN_BUTTON
import mega.privacy.android.app.upgradeAccount.view.VariantAOnboardingDialogView
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class VariantAOnboardingDialogViewTest {
    private val localisedPriceStringMapper = LocalisedPriceStringMapper()
    private val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    private val formattedSizeMapper = FormattedSizeMapper()

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that image is shown`() {
        setContent()
        composeRule.onNodeWithTag(IMAGE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that pro plan row is shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(PRO_PLAN_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PRO_PLAN_TEXT).assertIsDisplayed()
    }

    @Test
    fun `test that title for features is shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(FEATURE_TITLE).assertIsDisplayed()
    }

    @Test
    fun `test that storage row is displayed`() {
        setContent()
        composeRule.onNodeWithTag(STORAGE_DESCRIPTION_ROW).assertIsDisplayed()
    }

    @Test
    fun `test that transfer row is displayed`() {
        setContent()
        composeRule.onNodeWithTag(TRANSFER_DESCRIPTION_ROW).assertIsDisplayed()
    }

    @Test
    fun `test that security row is displayed`() {
        setContent()
        composeRule.onNodeWithTag(SECURITY_DESCRIPTION_ROW).assertIsDisplayed()
    }

    @Test
    fun `test that skip button is shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(SKIP_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `test that view pro plan button is shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(VIEW_PRO_PLAN_BUTTON).assertIsDisplayed()
    }

    private fun setContent() = composeRule.setContent {
        VariantAOnboardingDialogView(
            state = getChooseAccountState(),
            onSkipPressed = {},
            onViewPlansPressed = {}
        )
    }

    private fun getChooseAccountState(): ChooseAccountState =
        ChooseAccountState(
            cheapestSubscriptionAvailable = subscriptionProLite,
        )

    private val subscriptionProLite = LocalisedSubscription(
        accountType = AccountType.PRO_LITE,
        storage = PRO_LITE_STORAGE,
        monthlyTransfer = PRO_LITE_TRANSFER_MONTHLY,
        yearlyTransfer = PRO_LITE_TRANSFER_YEARLY,
        monthlyAmount = CurrencyAmount(
            PRO_LITE_PRICE_MONTHLY,
            Currency("EUR")
        ),
        yearlyAmount = CurrencyAmount(
            PRO_LITE_PRICE_YEARLY,
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    companion object {
        const val PRO_LITE_STORAGE = 400
        const val PRO_LITE_TRANSFER_MONTHLY = 1024
        const val PRO_LITE_TRANSFER_YEARLY = 12288
        const val PRO_LITE_PRICE_MONTHLY = 4.99F
        const val PRO_LITE_PRICE_YEARLY = 49.99F
    }
}