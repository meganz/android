package mega.privacy.android.feature.pdfviewer.presentation.components

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PdfViewerErrorContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that error content is displayed with correct test tag`() {
        initComposeRuleContent()

        composeRule.onNodeWithTag(PDF_VIEWER_ERROR_CONTENT_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that title and description are displayed`() {
        initComposeRuleContent()

        composeRule.onNodeWithText(
            composeRule.activity.getString(sharedR.string.file_preview_error_title)
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(sharedR.string.file_preview_error_description)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that clicking try again invokes onRetry`() {
        val onRetry = mock<() -> Unit>()
        initComposeRuleContent(onRetry = onRetry)

        composeRule.onNodeWithTag(PDF_VIEWER_ERROR_RETRY_BUTTON_TAG).performClick()

        verify(onRetry).invoke()
    }

    private fun initComposeRuleContent(onRetry: () -> Unit = {}) {
        composeRule.setContent {
            PdfViewerErrorContent(onRetry = onRetry)
        }
    }
}
