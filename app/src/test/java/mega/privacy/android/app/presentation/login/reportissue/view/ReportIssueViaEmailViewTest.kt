package mega.privacy.android.app.presentation.login.reportissue.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.reportissue.model.ReportIssueViaEmailUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ReportIssueViaEmailViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that views are are displayed correctly`() {
        composeTestRule.setContent {
            ReportIssueViaEmailView(
                uiState = ReportIssueViaEmailUiState(),
                onDescriptionChanged = {},
                onIncludeLogsChanged = {},
                onBackPress = {},
                onDiscard = {},
                onSubmit = {}
            )
        }

        composeTestRule.onNodeWithTag(NOTE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DESCRIPTION_FIELD_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(INCLUDE_LOGS_SWITCH_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ERROR_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that error text is displayed when error property in UI state is not null`() {
        composeTestRule.setContent {
            ReportIssueViaEmailView(
                uiState = ReportIssueViaEmailUiState(
                    error = R.string.settings_help_report_issue_description_label
                ),
                onDescriptionChanged = {},
                onIncludeLogsChanged = {},
                onBackPress = {},
                onDiscard = {},
                onSubmit = {}
            )
        }

        composeTestRule.onNodeWithTag(ERROR_TEST_TAG).assertIsDisplayed()
    }


}