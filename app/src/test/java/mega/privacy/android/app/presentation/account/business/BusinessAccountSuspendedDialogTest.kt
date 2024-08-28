package mega.privacy.android.app.presentation.account.business

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.account.business.BUSINESS_ACCOUNT_SUSPENDED_DIALOG
import mega.privacy.android.app.presentation.account.business.BusinessAccountSuspendedDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [BusinessAccountSuspendedDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class BusinessAccountSuspendedDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog with correct content is shown for suspended business administrators`() {
        composeTestRule.setContent {
            BusinessAccountSuspendedDialog(
                isBusinessAdministratorAccount = true,
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
            BusinessAccountSuspendedDialog(
                isBusinessAdministratorAccount = false,
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
            BusinessAccountSuspendedDialog(
                isBusinessAdministratorAccount = true,
                onAlertAcknowledged = onAlertAcknowledged,
                onAlertDismissed = {},
            )
        }
        composeTestRule.onNodeWithText(R.string.account_business_account_deactivated_dialog_button)
            .performClick()
        verify(onAlertAcknowledged).invoke()
    }
}