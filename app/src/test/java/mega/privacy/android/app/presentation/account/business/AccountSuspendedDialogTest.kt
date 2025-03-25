package mega.privacy.android.app.presentation.account.business

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.account.model.AccountDeactivatedStatus

/**
 * Test class for [AccountSuspendedDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class AccountSuspendedDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog with correct content is shown for suspended business administrators`() {
        composeTestRule.setContent {
            AccountSuspendedDialog(
                accountDeactivatedStatus = AccountDeactivatedStatus.MASTER_BUSINESS_ACCOUNT_DEACTIVATED,
                onAlertAcknowledged = {},
                onAlertDismissed = {},
            )
        }
        composeTestRule.onNodeWithTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.account_business_account_deactivated_dialog_title)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.account_business_account_deactivated_dialog_admin_body)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.account_business_account_deactivated_dialog_button)
            .assertExists()
    }

    @Test
    fun `test that the dialog with correct content is shown for suspended business sub-users`() {
        composeTestRule.setContent {
            AccountSuspendedDialog(
                accountDeactivatedStatus = AccountDeactivatedStatus.BUSINESS_ACCOUNT_DEACTIVATED,
                onAlertAcknowledged = {},
                onAlertDismissed = {},
            )
        }
        composeTestRule.onNodeWithTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.account_business_account_deactivated_dialog_title)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.account_business_account_deactivated_dialog_sub_user_body)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.account_business_account_deactivated_dialog_button)
            .assertExists()
    }

    @Test
    fun `test that clicking the positive button invokes the on alert acknowledged lambda`() {
        val onAlertAcknowledged = mock<() -> Unit>()
        composeTestRule.setContent {
            AccountSuspendedDialog(
                accountDeactivatedStatus = AccountDeactivatedStatus.MASTER_BUSINESS_ACCOUNT_DEACTIVATED,
                onAlertAcknowledged = onAlertAcknowledged,
                onAlertDismissed = {},
            )
        }
        composeTestRule.onNodeWithText(R.string.account_business_account_deactivated_dialog_button)
            .performClick()
        verify(onAlertAcknowledged).invoke()
    }
}