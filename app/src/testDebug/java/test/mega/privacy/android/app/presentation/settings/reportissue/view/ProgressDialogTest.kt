package test.mega.privacy.android.app.presentation.settings.reportissue.view

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.ProgressDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ProgressDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_that_progress_indicator_is_set_to_current_progress_value() {
        val expectedProgress = 0.33f
        composeTestRule.setContent {
            ProgressDialog(
                title = "Title",
                progress = expectedProgress,
                onCancel = {},
                cancelButtonText = "Cancel"
            )
        }

        val progressMatcher =
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        composeTestRule.onNode(progressMatcher)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(expectedProgress, 0f..1f))
    }

    @Test
    fun test_that_clicking_on_cancel_upload_calls_cancel_upload_function() {
        val onCancel = mock<() -> Unit>()
        val cancelButtonText = "Cancel"
        composeTestRule.setContent {
            ProgressDialog(
                title = stringResource(id = R.string.settings_help_report_issue_uploading_log_file),
                progress = 0.5f,
                onCancel = onCancel,
                cancelButtonText = cancelButtonText
            )
        }

        composeTestRule.onNodeWithText(
            cancelButtonText,
            ignoreCase = true
        ).performClick()

        verify(onCancel).invoke()
    }
}