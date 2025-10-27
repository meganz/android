package mega.privacy.android.app.presentation.logout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.app.R as sharedR
import mega.privacy.android.app.presentation.logout.model.LogoutState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class LogoutConfirmationDialogM3Test {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that dialog displays correctly with offline files and transfers`() {
        val onLogout = mock<() -> Unit>()
        val onDismissed = mock<() -> Unit>()
        val logoutState = LogoutState.Data(
            hasOfflineFiles = true,
            hasPendingTransfers = true
        )

        composeRule.setContent {
            LogoutConfirmationDialogM3(
                logoutState = logoutState,
                onLogout = onLogout,
                onDismissed = onDismissed
            )
        }

        // Verify dialog title is displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_title))
            .assertIsDisplayed()

        // Verify confirmation message is displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_offline_and_transfers_message))
            .assertIsDisplayed()

        // Verify buttons are displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_positive_button))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that dialog displays correctly with only offline files`() {
        val onLogout = mock<() -> Unit>()
        val onDismissed = mock<() -> Unit>()
        val logoutState = LogoutState.Data(
            hasOfflineFiles = true,
            hasPendingTransfers = false
        )

        composeRule.setContent {
            LogoutConfirmationDialogM3(
                logoutState = logoutState,
                onLogout = onLogout,
                onDismissed = onDismissed
            )
        }

        // Verify dialog title is displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_title))
            .assertIsDisplayed()

        // Verify confirmation message is displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_offline_message))
            .assertIsDisplayed()

        // Verify buttons are displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_positive_button))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that dialog displays correctly with only transfers`() {
        val onLogout = mock<() -> Unit>()
        val onDismissed = mock<() -> Unit>()
        val logoutState = LogoutState.Data(
            hasOfflineFiles = false,
            hasPendingTransfers = true
        )

        composeRule.setContent {
            LogoutConfirmationDialogM3(
                logoutState = logoutState,
                onLogout = onLogout,
                onDismissed = onDismissed
            )
        }

        // Verify dialog title is displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_title))
            .assertIsDisplayed()

        // Verify confirmation message is displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_transfers_message))
            .assertIsDisplayed()

        // Verify buttons are displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_positive_button))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that logout button click triggers onLogout callback`() {
        val onLogout = mock<() -> Unit>()
        val onDismissed = mock<() -> Unit>()
        val logoutState = LogoutState.Data(
            hasOfflineFiles = true,
            hasPendingTransfers = true
        )

        composeRule.setContent {
            LogoutConfirmationDialogM3(
                logoutState = logoutState,
                onLogout = onLogout,
                onDismissed = onDismissed
            )
        }

        // Click logout button
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_positive_button))
            .performClick()

        // Verify onLogout callback is called
        verify(onLogout).invoke()
        verifyNoInteractions(onDismissed)
    }

    @Test
    fun `test that cancel button click triggers onDismissed callback`() {
        val onLogout = mock<() -> Unit>()
        val onDismissed = mock<() -> Unit>()
        val logoutState = LogoutState.Data(
            hasOfflineFiles = true,
            hasPendingTransfers = true
        )

        composeRule.setContent {
            LogoutConfirmationDialogM3(
                logoutState = logoutState,
                onLogout = onLogout,
                onDismissed = onDismissed
            )
        }

        // Click cancel button
        composeRule.onNodeWithText(context.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button))
            .performClick()

        // Verify onDismissed callback is called
        verify(onDismissed).invoke()
        verifyNoInteractions(onLogout)
    }

    @Test
    fun `test that loading HUD is displayed when state is loading`() {
        val onLogout = mock<() -> Unit>()
        val onDismissed = mock<() -> Unit>()
        val logoutState = LogoutState.Loading

        composeRule.setContent {
            LogoutConfirmationDialogM3(
                logoutState = logoutState,
                onLogout = onLogout,
                onDismissed = onDismissed
            )
        }

        // Verify loading HUD is displayed
        composeRule.onNodeWithTag(LogoutConfirmationDialogM3TestTags.LOADING_HUD)
            .assertIsDisplayed()

        // Verify no dialog elements are displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_title))
            .assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_positive_button))
            .assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button))
            .assertDoesNotExist()
    }

    @Test
    fun `test that error dialog is displayed when state is error`() {
        val onLogout = mock<() -> Unit>()
        val onDismissed = mock<() -> Unit>()
        val logoutState = LogoutState.Error

        composeRule.setContent {
            LogoutConfirmationDialogM3(
                logoutState = logoutState,
                onLogout = onLogout,
                onDismissed = onDismissed
            )
        }

        // Verify error dialog title is displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.general_error_word))
            .assertIsDisplayed()

        // Verify error message is displayed
        composeRule.onNodeWithText(context.getString(mega.privacy.android.shared.resources.R.string.general_text_error))
            .assertIsDisplayed()

        // Verify OK button is displayed
        composeRule.onNodeWithText(context.getString(mega.privacy.android.shared.resources.R.string.general_ok))
            .assertIsDisplayed()

        // Verify no other dialog elements are displayed
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_title))
            .assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(sharedR.string.logout_warning_dialog_positive_button))
            .assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button))
            .assertDoesNotExist()
    }

    @Test
    fun `test that error dialog OK button click triggers onDismissed callback`() {
        val onLogout = mock<() -> Unit>()
        val onDismissed = mock<() -> Unit>()
        val logoutState = LogoutState.Error

        composeRule.setContent {
            LogoutConfirmationDialogM3(
                logoutState = logoutState,
                onLogout = onLogout,
                onDismissed = onDismissed
            )
        }

        // Click OK button
        composeRule.onNodeWithText(context.getString(mega.privacy.android.shared.resources.R.string.general_ok))
            .performClick()

        // Verify onDismissed callback is called
        verify(onDismissed).invoke()
        verifyNoInteractions(onLogout)
    }
}
