package mega.privacy.android.app.upgradeAccount

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.account.model.AccountStorageUIState
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.model.UIAccountType
import mega.privacy.android.app.upgradeAccount.view.ADDITIONAL_FEATURES_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.BACKUP_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.FILE_SHARING_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.GOOGLE_PLAY_STORE_SUBSCRIPTION_LINK_TAG
import mega.privacy.android.app.upgradeAccount.view.ONBOARDING_SCREEN_VARIANT_B
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_CARD_VARIANT_B
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TEXT
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TITLE
import mega.privacy.android.app.upgradeAccount.view.STORAGE_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.SUBSCRIPTION_DETAILS_DESCRIPTION_TAG
import mega.privacy.android.app.upgradeAccount.view.SUBSCRIPTION_DETAILS_TITLE_TAG
import mega.privacy.android.app.upgradeAccount.view.VariantBOnboardingDialogView
import mega.privacy.android.app.upgradeAccount.view.components.MONTHLY_CHECK_ICON_TAG
import mega.privacy.android.app.upgradeAccount.view.components.MONTHLY_TAB_TAG
import mega.privacy.android.app.upgradeAccount.view.components.RECOMMENDED_PLAN_TAG
import mega.privacy.android.app.upgradeAccount.view.components.YEARLY_CHECK_ICON_TAG
import mega.privacy.android.app.upgradeAccount.view.components.YEARLY_TAB_TAG
import mega.privacy.android.shared.resources.R.string.dialog_onboarding_feature_storage_description
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.app.fromId
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewTest.Companion.expectedLocalisedSubscriptionsList
import mega.privacy.android.app.upgradeAccount.VariantAOnboardingDialogViewTest.Companion.subscriptionProLite

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1980dp-xhdpi")
class VariantBOnboardingDialogViewTest {
    @get:Rule
    var composeRule = createComposeRule()

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
            fromId(
                dialog_onboarding_feature_storage_description,
                "400",
                "GB"
            )
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
    fun `test that choose plan title is displayed correctly`() {
        setContent()
        composeRule.onNodeWithTag("${ONBOARDING_SCREEN_VARIANT_B}title").assertExists()
        composeRule.onNodeWithText(fromId(R.string.account_upgrade_account_title_choose_right_plan))
            .assertExists()
    }

    @Test
    fun `test that if monthly tab is selected the monthly button displays correctly`() {
        setContent()
        composeRule.onNodeWithTag("$ONBOARDING_SCREEN_VARIANT_B$MONTHLY_TAB_TAG").performClick()
        composeRule.onNodeWithTag(
            "$ONBOARDING_SCREEN_VARIANT_B$MONTHLY_CHECK_ICON_TAG",
            useUnmergedTree = true
        )
            .assertIsDisplayed()
        composeRule.onNodeWithTag(
            "$ONBOARDING_SCREEN_VARIANT_B$YEARLY_CHECK_ICON_TAG",
            useUnmergedTree = true
        )
            .assertDoesNotExist()
    }

    @Test
    fun `test that if yearly tab is selected the yearly button displays correctly`() {
        setContent()
        composeRule.onNodeWithTag("$ONBOARDING_SCREEN_VARIANT_B$YEARLY_TAB_TAG").performClick()
        composeRule.onNodeWithTag(
            "$ONBOARDING_SCREEN_VARIANT_B$MONTHLY_CHECK_ICON_TAG",
            useUnmergedTree = true
        )
            .assertDoesNotExist()
        composeRule.onNodeWithTag(
            "$ONBOARDING_SCREEN_VARIANT_B$YEARLY_CHECK_ICON_TAG",
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that recommended label is shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(
            "$PRO_PLAN_CARD_VARIANT_B${UIAccountType.PRO_I.ordinal}",
            useUnmergedTree = true
        )
            .assert(hasAnyChild(hasTestTag("$PRO_PLAN_CARD_VARIANT_B$RECOMMENDED_PLAN_TAG")))
    }

    @Test
    fun `test that subscription details are shown correctly when pre-selected plan is Pro I`() {
        setContent()
        composeRule.onNodeWithTag(GOOGLE_PLAY_STORE_SUBSCRIPTION_LINK_TAG)
            .assertExists()
        composeRule.onNodeWithTag(SUBSCRIPTION_DETAILS_TITLE_TAG).assertExists()
        composeRule.onNodeWithTag("${SUBSCRIPTION_DETAILS_DESCRIPTION_TAG}_yearly_pro_i_with_price")
            .assertExists()
    }

    @Test
    fun `test that subscription details are shown correctly when user selects monthly tab`() {
        setContent()
        composeRule.onNodeWithTag("$ONBOARDING_SCREEN_VARIANT_B$MONTHLY_TAB_TAG").performClick()
        composeRule.onNodeWithTag("${SUBSCRIPTION_DETAILS_DESCRIPTION_TAG}_monthly_pro_i_with_price")
            .assertExists()
    }

    private fun setContent() = composeRule.setContent {
        VariantBOnboardingDialogView(
            state = getChooseAccountState(),
            accountUiState = AccountStorageUIState(),
            onBackPressed = {},
            onContinueClicked = {},
            onChoosingMonthlyYearlyPlan = {},
            onChoosingPlanType = {},
            onPlayStoreLinkClicked = {},
            onProIIIVisible = {},
        )
    }

    private fun setContentWithAdsEnabled() = composeRule.setContent {
        VariantBOnboardingDialogView(
            state = getChooseAccountStateWithAdsEnabled(),
            accountUiState = AccountStorageUIState(),
            onBackPressed = {},
            onContinueClicked = {},
            onChoosingMonthlyYearlyPlan = {},
            onChoosingPlanType = {},
            onPlayStoreLinkClicked = {},
            onProIIIVisible = {},
        )
    }

    private fun getChooseAccountState(): ChooseAccountState =
        ChooseAccountState(
            cheapestSubscriptionAvailable = subscriptionProLite,
            localisedSubscriptionsList = expectedLocalisedSubscriptionsList
        )

    private fun getChooseAccountStateWithAdsEnabled(): ChooseAccountState =
        ChooseAccountState(
            cheapestSubscriptionAvailable = subscriptionProLite,
            localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
            showAdsFeature = true,
        )
}