package mega.privacy.android.app.upgradeAccount

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.view.ADDITIONAL_FEATURES_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.BACKUP_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.FILE_SHARING_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.IMAGE_TAG
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TEXT
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TITLE
import mega.privacy.android.app.upgradeAccount.view.SKIP_BUTTON
import mega.privacy.android.app.upgradeAccount.view.STORAGE_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.VIEW_PRO_PLAN_BUTTON
import mega.privacy.android.app.upgradeAccount.view.VariantAOnboardingDialogView
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.shared.resources.R.string.dialog_onboarding_feature_storage_description
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class VariantAOnboardingDialogViewTest {
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
    fun `test that pro plan row text is shown correctly`() {
        setContent()
        composeRule.onNodeWithText(fromId(R.string.dialog_onboarding_get_pro_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(fromId(R.string.dialog_onboarding_get_pro_description, "€4.99"))
            .assertExists()
    }

    @Test
    fun `test that storage row is displayed`() {
        setContent()
        composeRule.onNodeWithTag(STORAGE_DESCRIPTION_ROW).printToString()
        composeRule.onNodeWithTag(STORAGE_DESCRIPTION_ROW).assertIsDisplayed()
        composeRule.onNodeWithText(fromId(R.string.dialog_onboarding_feature_title_storage))
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            fromId(dialog_onboarding_feature_storage_description, "400", "GB")
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that file sharing row is displayed`() {
        setContent()
        composeRule.onNodeWithTag(FILE_SHARING_DESCRIPTION_ROW).assertIsDisplayed()
        composeRule.onNodeWithText(fromId(sharedR.string.dialog_onboarding_feature_title_file_sharing))
            .assertIsDisplayed()
        composeRule.onNodeWithText(fromId(sharedR.string.dialog_onboarding_feature_description_file_sharing))
            .assertIsDisplayed()
    }

    @Test
    fun `test that backup row is displayed`() {
        setContent()
        composeRule.onNodeWithTag(BACKUP_DESCRIPTION_ROW).assertIsDisplayed()
        composeRule.onNodeWithText(fromId(sharedR.string.dialog_onboarding_feature_title_backup_rewind))
            .assertIsDisplayed()
        composeRule.onNodeWithText(fromId(sharedR.string.dialog_onboarding_feature_description_backup_rewind))
            .assertIsDisplayed()
    }

    @Test
    fun `test that additional features row is displayed with Ad-free if Ads feature is enabled`() {
        setContentWithAdsEnabled()
        composeRule.onNodeWithTag(ADDITIONAL_FEATURES_DESCRIPTION_ROW).assertIsDisplayed()
        composeRule.onNodeWithText(fromId(sharedR.string.dialog_onboarding_feature_title_backup_rewind))
            .assertIsDisplayed()
        composeRule.onNodeWithText(fromId(sharedR.string.dialog_onboarding_feature_description_additional_features_with_ads))
            .assertIsDisplayed()
    }

    @Test
    fun `test that additional features row is displayed without Ad-free if Ads feature is disabled`() {
        setContent()
        composeRule.onNodeWithTag(ADDITIONAL_FEATURES_DESCRIPTION_ROW).assertIsDisplayed()
        composeRule.onNodeWithText(fromId(sharedR.string.dialog_onboarding_feature_title_additional_features))
            .assertIsDisplayed()
        composeRule.onNodeWithText(fromId(sharedR.string.dialog_onboarding_feature_description_additional_features_without_ads))
            .assertIsDisplayed()
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

    private fun setContent() = composeRule.setContent {
        VariantAOnboardingDialogView(
            state = getChooseAccountState(),
            onSkipPressed = {},
            onViewPlansPressed = {}
        )
    }

    private fun setContentWithAdsEnabled() = composeRule.setContent {
        VariantAOnboardingDialogView(
            state = getChooseAccountStateWithAdsEnabled(),
            onSkipPressed = {},
            onViewPlansPressed = {}
        )
    }

    private fun getChooseAccountState(): ChooseAccountState =
        ChooseAccountState(
            cheapestSubscriptionAvailable = subscriptionProLite,
        )

    private fun getChooseAccountStateWithAdsEnabled(): ChooseAccountState =
        ChooseAccountState(
            cheapestSubscriptionAvailable = subscriptionProLite,
            showAdsFeature = true,
        )

    companion object {
        const val PRO_LITE_STORAGE = 400
        const val PRO_LITE_TRANSFER_MONTHLY = 1024
        const val PRO_LITE_TRANSFER_YEARLY = 12288
        const val PRO_LITE_PRICE_MONTHLY = 4.99F
        const val PRO_LITE_PRICE_YEARLY = 49.99F

        private val localisedPriceStringMapper = LocalisedPriceStringMapper()
        private val localisedPriceCurrencyCodeStringMapper =
            LocalisedPriceCurrencyCodeStringMapper()
        private val formattedSizeMapper = FormattedSizeMapper()
        val subscriptionProLite = LocalisedSubscription(
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
    }
}