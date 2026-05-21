package mega.privacy.android.app.myAccount.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.myAccount.MyAccountUsageUiState
import mega.privacy.android.app.myAccount.PaymentAlertType
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyAccountUsageScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // region: Loading / Shimmer

    @Test
    fun `test that storage progress bar is not shown when content is not ready`() {
        setContent(MyAccountUsageUiState(isUsageContentReady = false))
        composeTestRule.onNodeWithTag(STORAGE_PROGRESS_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that storage progress bar is shown when content is ready`() {
        setContent(readyFreeState())
        composeTestRule.onNodeWithTag(STORAGE_PROGRESS_BAR_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that transfer progress bar is shown for paid account when content is ready`() {
        setContent(readyFreeState().copy(accountType = AccountType.PRO_I))
        composeTestRule.onNodeWithTag(TRANSFER_PROGRESS_BAR_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that transfer progress bar is not shown for free account when content is ready`() {
        setContent(readyFreeState())
        composeTestRule.onNodeWithTag(TRANSFER_PROGRESS_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that upgrade button is shown for free account when content is ready`() {
        setContent(readyFreeState())
        composeTestRule.onNodeWithText(upgradeButtonText()).assertIsDisplayed()
    }

    @Test
    fun `test that upgrade button is not shown for business account`() {
        setContent(readyFreeState().copy(isBusinessAccount = true))
        composeTestRule.onNodeWithText(upgradeButtonText()).assertDoesNotExist()
    }

    @Test
    fun `test that upgrade button is not shown for pro flexi account`() {
        setContent(readyFreeState().copy(isProFlexiAccount = true))
        composeTestRule.onNodeWithText(upgradeButtonText()).assertDoesNotExist()
    }

    @Test
    fun `test that onUpgradeClick is invoked when upgrade button is clicked`() {
        var clicked = false
        setContent(state = readyFreeState(), onUpgradeClick = { clicked = true })
        composeTestRule.onNodeWithText(upgradeButtonText()).performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that error content is shown when usageLoadFailed is true`() {
        setContent(MyAccountUsageUiState(usageLoadFailed = true))
        val errorText = composeTestRule.activity.getString(sharedR.string.general_request_failed_message)
        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()
    }

    @Test
    fun `test that ok button is shown in error state`() {
        setContent(MyAccountUsageUiState(usageLoadFailed = true))
        val okText = composeTestRule.activity.getString(sharedR.string.general_ok)
        composeTestRule.onNodeWithText(okText).assertIsDisplayed()
    }

    @Test
    fun `test that onUsageLoadErrorDismiss is invoked when ok button is clicked`() {
        var dismissed = false
        setContent(
            state = MyAccountUsageUiState(usageLoadFailed = true),
            onUsageLoadErrorDismiss = { dismissed = true },
        )
        val okText = composeTestRule.activity.getString(sharedR.string.general_ok)
        composeTestRule.onNodeWithText(okText).performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun `test that storage details label is shown when content is ready`() {
        setContent(readyFreeState())
        val label = composeTestRule.activity.getString(R.string.usage_storage_details_label)
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }

    @Test
    fun `test that backup storage item is shown when backupStorage is not empty`() {
        setContent(readyFreeState().copy(backupStorage = "1 GB"))
        val label = composeTestRule.activity.getString(R.string.home_side_menu_backups_title)
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }

    @Test
    fun `test that backup storage item is not shown when backupStorage is empty`() {
        setContent(readyFreeState().copy(backupStorage = ""))
        val label = composeTestRule.activity.getString(R.string.home_side_menu_backups_title)
        composeTestRule.onNodeWithText(label).assertDoesNotExist()
    }

    @Test
    fun `test that previous versions item is shown when file versioning is enabled`() {
        setContent(readyFreeState().copy(isFileVersioningEnabled = true))
        val label = composeTestRule.activity.getString(R.string.file_properties_folder_previous_versions)
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }

    @Test
    fun `test that previous versions item is not shown when file versioning is disabled`() {
        setContent(readyFreeState().copy(isFileVersioningEnabled = false))
        val label = composeTestRule.activity.getString(R.string.file_properties_folder_previous_versions)
        composeTestRule.onNodeWithText(label).assertDoesNotExist()
    }

    @Test
    fun `test that storage progress bar is not shown for business account when content is ready`() {
        setContent(readyFreeState().copy(isBusinessAccount = true))
        composeTestRule.onNodeWithTag(STORAGE_PROGRESS_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that storage progress bar is not shown for pro flexi account when content is ready`() {
        setContent(readyFreeState().copy(isProFlexiAccount = true))
        composeTestRule.onNodeWithTag(STORAGE_PROGRESS_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that payment overdue alert is shown for business expired account when content is ready`() {
        setContent(
            readyFreeState().copy(
                isBusinessAccount = true,
                isMasterBusinessAccount = true,
                paymentAlertType = PaymentAlertType.BusinessExpired,
            )
        )
        val alertText = composeTestRule.activity.getString(R.string.payment_overdue_label)
        composeTestRule.onNodeWithText(alertText).assertIsDisplayed()
    }

    @Test
    fun `test that payment alert is not shown for business sub-account`() {
        setContent(
            readyFreeState().copy(
                isBusinessAccount = true,
                isMasterBusinessAccount = false,
                paymentAlertType = PaymentAlertType.None,
            )
        )
        val alertText = composeTestRule.activity.getString(R.string.payment_overdue_label)
        composeTestRule.onNodeWithText(alertText).assertDoesNotExist()
    }

    private fun setContent(
        state: MyAccountUsageUiState,
        onUpgradeClick: () -> Unit = {},
        onUsageLoadErrorDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MyAccountUsageScreen(
                uiState = state,
                onUpgradeClick = onUpgradeClick,
                onUsageLoadErrorDismiss = onUsageLoadErrorDismiss,
            )
        }
    }

    private fun readyFreeState() = MyAccountUsageUiState(
        isUsageContentReady = true,
        accountType = AccountType.FREE,
        isBusinessAccount = false,
        isProFlexiAccount = false,
        isMasterBusinessAccount = false,
        usedStorage = "5 GB",
        totalStorage = "20 GB",
        usedStoragePercentage = 25,
        usedTransfer = "1 GB",
        totalTransfer = "10 GB",
        usedTransferPercentage = 10,
        usedTransferStatus = UsedTransferStatus.NoTransferProblems,
        cloudStorage = "3 GB",
        incomingStorage = "1 GB",
        rubbishStorage = "1 GB",
        backupStorage = "",
        isFileVersioningEnabled = true,
        versionsInfo = "100 MB",
        paymentAlertType = PaymentAlertType.None,
    )

    private fun upgradeButtonText() =
        composeTestRule.activity.getString(R.string.account_my_account_usage_get_more_storage_button)
}
