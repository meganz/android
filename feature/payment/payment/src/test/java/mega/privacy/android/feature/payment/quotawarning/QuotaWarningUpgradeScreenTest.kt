package mega.privacy.android.feature.payment.quotawarning

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.feature.payment.components.TEST_TAG_QUOTA_CURRENT_PLAN_CARD
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningUpgradeScreen
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningUpgradeState
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_CONTACT_SUPPORT
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_LEARN_MORE
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_SKELETON
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_TITLE
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_VIEW_ALL_PLANS
import mega.privacy.android.navigation.payment.QuotaWarningTrigger
import mega.privacy.android.navigation.payment.QuotaWarningType
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuotaWarningUpgradeScreenTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(
        type: QuotaWarningType,
        trigger: QuotaWarningTrigger,
        state: QuotaWarningUpgradeState,
    ) {
        composeRule.setContent {
            QuotaWarningUpgradeScreen(
                type = type,
                trigger = trigger,
                uiState = state,
                onUpgradeClick = {},
                onViewAllPlansClick = {},
                onLearnMoreClick = {},
                onContactSupportClick = {},
                onManagePlanClick = {},
                onClose = {},
            )
        }
    }

    @Test
    fun `test that storage almost full scenario shows title, current plan card and view all plans`() {
        setScreen(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            state = QuotaWarningUpgradeState(
                currentPlan = AccountType.FREE,
                storageState = StorageState.Orange,
                storageUsedPercentage = 80,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(
                sharedR.string.subscription_quota_storage_almost_full_title,
                80,
            )
        ).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_CURRENT_PLAN_CARD).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_VIEW_ALL_PLANS).assertExists()
    }

    @Test
    fun `test that transfer scenario shows a learn more link`() {
        setScreen(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Download,
            state = QuotaWarningUpgradeState(
                currentPlan = AccountType.FREE,
                transferUsedPercentage = 90,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_LEARN_MORE).assertExists()
    }

    @Test
    fun `test that pro transfer running low scenario shows the percentage used title`() {
        setScreen(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Download,
            state = QuotaWarningUpgradeState(
                currentPlan = AccountType.PRO_I,
                transferUsedPercentage = 85,
                isTransferOverQuota = false,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText(
            composeRule.activity.getString(
                sharedR.string.subscription_quota_transfer_percentage_used_title,
                85,
            )
        ).assertExists()
    }

    @Test
    fun `test that transfer over quota scenario shows the exceeded title`() {
        setScreen(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Download,
            state = QuotaWarningUpgradeState(
                currentPlan = AccountType.PRO_I,
                isTransferOverQuota = true,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText(
            composeRule.activity.getString(sharedR.string.subscription_quota_transfer_over_title)
        ).assertExists()
    }

    @Test
    fun `test that highest plan scenario shows contact support and hides upgrade options`() {
        setScreen(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            state = QuotaWarningUpgradeState(
                currentPlan = AccountType.PRO_III,
                storageState = StorageState.Red,
                storageUsedPercentage = 98,
                isHighestPlan = true,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_CONTACT_SUPPORT).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_VIEW_ALL_PLANS).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_CURRENT_PLAN_CARD).assertExists()
    }

    @Test
    fun `test that loading state shows the skeleton`() {
        setScreen(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            state = QuotaWarningUpgradeState(isLoading = true),
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_SKELETON).assertExists()
    }
}
