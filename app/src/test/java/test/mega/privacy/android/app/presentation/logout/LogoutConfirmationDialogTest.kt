package test.mega.privacy.android.app.presentation.logout

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.logout.LogoutConfirmationDialog
import mega.privacy.android.app.presentation.logout.LogoutViewModel
import mega.privacy.android.app.presentation.logout.model.LogoutState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class LogoutConfirmationDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val logoutViewModel = mock<LogoutViewModel>()

    @Test
    fun `test that correct message is displayed if both offline files and transfers exist`() {
        logoutViewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    LogoutState.Data(
                        hasOfflineFiles = true,
                        hasPendingTransfers = true,
                    )
                )
            )
        }

        composeRule.setContent {
            LogoutConfirmationDialog(
                onDismissed = {},
                logoutViewModel = logoutViewModel,
            )
        }

        composeRule.onNodeWithText(R.string.logout_warning_offline_and_transfers).assertExists()
    }

    @Test
    fun `test that correct message is shown if only offline files exist`() {
        logoutViewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    LogoutState.Data(
                        hasOfflineFiles = true,
                        hasPendingTransfers = false,
                    )
                )
            )
        }

        composeRule.setContent {
            LogoutConfirmationDialog(
                onDismissed = {},
                logoutViewModel = logoutViewModel,
            )
        }

        composeRule.onNodeWithText(R.string.logout_warning_offline).assertExists()
    }

    @Test
    fun `test that correct message is shown if only transfers exist`() {
        logoutViewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    LogoutState.Data(
                        hasOfflineFiles = false,
                        hasPendingTransfers = true,
                    )
                )
            )
        }

        composeRule.setContent {
            LogoutConfirmationDialog(
                onDismissed = {},
                logoutViewModel = logoutViewModel,
            )
        }

        composeRule.onNodeWithText(R.string.logout_warning_transfers).assertExists()
    }
}