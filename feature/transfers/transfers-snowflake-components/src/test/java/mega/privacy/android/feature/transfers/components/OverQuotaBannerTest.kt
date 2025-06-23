package mega.privacy.android.feature.transfers.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class OverQuotaBannerTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that over quota banner is displayed correctly for transfer over quota`() {
        initComposeRuleContent(isTransferOverQuota = true)

        with(composeRule) {
            onNodeWithText(activity.getString(R.string.transfers_transfer_quota_banner_title))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.transfers_over_quota_banner_action_button))
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that over quota banner is displayed correctly for storage over quota`() {
        initComposeRuleContent(isStorageOverQuota = true)

        with(composeRule) {
            onNodeWithText(activity.getString(R.string.transfers_storage_quota_banner_title))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.transfers_over_quota_banner_action_button))
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that over quota banner is displayed correctly for both transfer and storage over quota`() {
        initComposeRuleContent(isTransferOverQuota = true, isStorageOverQuota = true)

        with(composeRule) {
            onNodeWithText(activity.getString(R.string.transfers_storage_and_transfer_quota_banner_title))
                .assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.transfers_over_quota_banner_action_button))
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that onUpgradeClick is called when upgrade button is clicked`() {
        val onUpgradeClick = mock<() -> Unit>()
        initComposeRuleContent(onUpgradeClick = onUpgradeClick)

        with(composeRule) {
            onNodeWithText(activity.getString(R.string.transfers_over_quota_banner_action_button))
                .performClick()
        }

        verify(onUpgradeClick).invoke()
    }

    @Test
    fun `test that onCancelButtonClick is called when cancel button is clicked`() {
        val onCancelButtonClick = mock<() -> Unit>()
        initComposeRuleContent(onCancelButtonClick = onCancelButtonClick)

        with(composeRule) {
            onNodeWithContentDescription(
                label = "cancel",
                substring = true,
                ignoreCase = true,
                useUnmergedTree = true,
            ).performClick()
        }

        verify(onCancelButtonClick).invoke()
    }

    private fun initComposeRuleContent(
        isTransferOverQuota: Boolean = false,
        isStorageOverQuota: Boolean = false,
        onUpgradeClick: () -> Unit = {},
        onCancelButtonClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            OverQuotaBanner(
                isTransferOverQuota = isTransferOverQuota,
                isStorageOverQuota = isStorageOverQuota,
                onUpgradeClick = onUpgradeClick,
                onCancelButtonClick = onCancelButtonClick
            )
        }
    }

}