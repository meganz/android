package mega.privacy.android.feature.payment.quotawarning

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.feature.payment.components.TEST_TAG_QUOTA_CURRENT_PLAN_CARD
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningUpgradeScreen
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningUpgradeState
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_CONTACT_SUPPORT
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_ERROR
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_RETRY
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_SKELETON
import mega.privacy.android.feature.payment.presentation.quotawarning.TEST_TAG_QUOTA_WARNING_SUBTITLE
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
        onRetryClick: () -> Unit = {},
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
                onRetryClick = onRetryClick,
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
                currentPlan = AccountType.PRO_I,
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
    fun `test that free user sees the current plan card on storage quota`() {
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
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_CURRENT_PLAN_CARD).assertExists()
    }

    @Test
    fun `test that free user does not see the current plan card on transfer quota`() {
        setScreen(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Download,
            state = QuotaWarningUpgradeState(
                currentPlan = AccountType.FREE,
                isTransferOverQuota = true,
                transferUsedPercentage = 100,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_CURRENT_PLAN_CARD).assertDoesNotExist()
    }

    @Test
    fun `test that logged out user does not see the current plan card`() {
        setScreen(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Streaming,
            state = QuotaWarningUpgradeState(
                isLoggedIn = false,
                isTransferOverQuota = true,
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText(
            composeRule.activity.getString(sharedR.string.subscription_quota_transfer_over_title)
        ).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_CURRENT_PLAN_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_VIEW_ALL_PLANS).assertExists()
    }

    @Test
    fun `test that transfer scenario appends the learn more link to the subtitle`() {
        setScreen(
            type = QuotaWarningType.Transfer,
            trigger = QuotaWarningTrigger.Download,
            state = QuotaWarningUpgradeState(
                currentPlan = AccountType.FREE,
                transferUsedPercentage = 90,
                isLoading = false,
            ),
        )

        val subtitle = composeRule.activity.getString(
            sharedR.string.subscription_quota_transfer_low_download_subtitle
        )
        val learnMore = composeRule.activity.getString(sharedR.string.general_learn_more)
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_SUBTITLE)
            .assertTextEquals("$subtitle $learnMore")
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

    @Test
    fun `test that no connection state shows the error message and hides the plan content`() {
        setScreen(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            state = QuotaWarningUpgradeState(
                currentPlan = AccountType.FREE,
                storageState = StorageState.Orange,
                storageUsedPercentage = 80,
                isLoading = false,
                isConnected = false,
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_ERROR).assertExists()
        composeRule.onNodeWithText(
            composeRule.activity.getString(sharedR.string.subscription_quota_no_connection_title)
        ).assertExists()
        composeRule.onNodeWithText(
            composeRule.activity.getString(
                sharedR.string.subscription_quota_no_connection_description
            )
        ).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_CURRENT_PLAN_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_VIEW_ALL_PLANS).assertDoesNotExist()
    }

    @Test
    fun `test that no connection state takes precedence over the loading state`() {
        setScreen(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            state = QuotaWarningUpgradeState(isLoading = true, isConnected = false),
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_ERROR).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_SKELETON).assertDoesNotExist()
    }

    @Test
    fun `test that load error state shows the error message while connected`() {
        setScreen(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            state = QuotaWarningUpgradeState(
                currentPlan = AccountType.FREE,
                storageState = StorageState.Orange,
                storageUsedPercentage = 80,
                isLoading = false,
                isConnected = true,
                hasLoadError = true,
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_ERROR).assertExists()
        composeRule.onNodeWithText(
            composeRule.activity.getString(sharedR.string.subscription_quota_no_connection_title)
        ).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_CURRENT_PLAN_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_VIEW_ALL_PLANS).assertDoesNotExist()
    }

    @Test
    fun `test that a load still in flight shows the skeleton and not the error state`() {
        setScreen(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            state = QuotaWarningUpgradeState(
                isLoading = true,
                isConnected = true,
                hasLoadError = false,
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_SKELETON).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_ERROR).assertDoesNotExist()
    }

    @Test
    fun `test that try again click invokes the retry callback`() {
        var retried = false
        setScreen(
            type = QuotaWarningType.Storage,
            trigger = QuotaWarningTrigger.Upload,
            state = QuotaWarningUpgradeState(isLoading = false, isConnected = false),
            onRetryClick = { retried = true },
        )

        composeRule.onNodeWithTag(TEST_TAG_QUOTA_WARNING_RETRY).performClick()

        assertThat(retried).isTrue()
    }
}
