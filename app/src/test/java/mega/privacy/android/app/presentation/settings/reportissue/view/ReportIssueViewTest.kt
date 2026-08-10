package mega.privacy.android.app.presentation.settings.reportissue.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.reportissue.model.ReportIssueUiState
import mega.privacy.android.app.presentation.settings.reportissue.view.ReportIssueView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.fromId

@RunWith(AndroidJUnit4::class)
class ReportIssueViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_that_label_is_shown() {
        composeTestRule.setContent {
            ReportIssueView(uiState = ReportIssueUiState())
        }

        composeTestRule.onNodeWithText(fromId(R.string.settings_help_report_issue_instructions))
            .assertExists()
    }

    @Test
    fun test_that_description_text_field_has_the_correct_hint_text() {
        composeTestRule.setContent {
            ReportIssueView(uiState = ReportIssueUiState())
        }

        composeTestRule.onNodeWithText(fromId(id = sharedR.string.report_issue_description_placeholder_message))
            .assertExists()
    }

    @Test
    fun test_that_description_text_is_shown_if_present() {
        val description = "A description!"
        composeTestRule.setContent {
            ReportIssueView(uiState = ReportIssueUiState(description = description))
        }

        composeTestRule.onNodeWithText(description)
            .assertExists()
    }

    @Test
    fun test_that_description_updates_call_description_update_function() {
        val onDescriptionChanged = mock<(String) -> Unit>()
        composeTestRule.setContent {
            ReportIssueView(
                uiState = ReportIssueUiState(),
                onDescriptionChanged = onDescriptionChanged,
            )
        }

        val expectedDescription = "expected description"

        composeTestRule.onNodeWithText(fromId(sharedR.string.report_issue_description_placeholder_message))
            .performTextInput(expectedDescription)

        verify(onDescriptionChanged).invoke(expectedDescription)
    }

    @Test
    fun test_that_toggle_is_displayed_if_include_logs_visible_is_true() {
        composeTestRule.setContent {
            ReportIssueView(
                uiState = ReportIssueUiState(includeLogsVisible = true),
            )
        }

        composeTestRule.onNodeWithText(fromId(R.string.settings_help_report_issue_attach_logs_label))
            .assertExists()
    }


    @Test
    fun test_that_toggle_is_checked_if_include_logs_is_set_to_true() {
        composeTestRule.setContent {
            ReportIssueView(
                uiState = ReportIssueUiState(includeLogsVisible = true, includeLogs = true),
            )
        }

        composeTestRule.onNodeWithText(fromId(R.string.settings_help_report_issue_attach_logs_label))
            .assertIsOn()
    }

    @Test
    fun test_that_toggle_is_not_checked_if_include_logs_is_set_to_false() {
        composeTestRule.setContent {
            ReportIssueView(
                uiState = ReportIssueUiState(includeLogsVisible = true, includeLogs = false),
            )
        }

        composeTestRule.onNodeWithText(fromId(R.string.settings_help_report_issue_attach_logs_label))
            .assertIsOff()
    }

    @Test
    fun test_that_include_logs_toggle_changes_call_include_logs_update_function() {
        val onIncludeLogsChanged = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            ReportIssueView(
                uiState = ReportIssueUiState(includeLogsVisible = true, includeLogs = true),
                onIncludeLogsChanged = onIncludeLogsChanged,
            )
        }

        composeTestRule.onNodeWithText(fromId(R.string.settings_help_report_issue_attach_logs_label))
            .assertIsOn()
            .performClick()

        verify(onIncludeLogsChanged).invoke(false)
    }


    @Test
    fun test_that_error_is_displayed_if_present() {
        val error = R.string.check_internet_connection_error
        composeTestRule.setContent {
            ReportIssueView(
                uiState = ReportIssueUiState(error = error),
            )
        }

        composeTestRule.onNodeWithText(fromId(error)).assertExists()
    }

    @Test
    fun test_that_upload_progress_dialog_is_displayed_if_file_is_being_uploaded() {
        composeTestRule.setContent {
            ReportIssueView(
                uiState = ReportIssueUiState(
                    includeLogs = true,
                    uploadProgress = 0.5f
                ),
            )
        }

        composeTestRule.onNode(isDialog())
            .assert(
                hasAnyDescendant(
                    hasText(
                        fromId(R.string.settings_help_report_issue_uploading_log_file)
                    )
                )
            )
            .assertIsDisplayed()
    }

}