package mega.privacy.android.app.presentation.transfers.view.dialog

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class LargeDownloadConfirmationDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val onAlwaysAllow = mock<() -> Unit>()
    private val onAllow = mock<() -> Unit>()
    private val onDismiss = mock<() -> Unit>()
    private val sizeString = "100 MB"

    @Test
    fun `test that dialog shows correctly for large download confirmation`() {
        val message = composeTestRule.activity
            .getString(R.string.alert_larger_file, sizeString)

        initComposeTestRule(isPreviewDownload = false)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_LARGE_DOWNLOAD_CONFIRMATION_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.transfers_confirm_large_download_title).assertIsDisplayed()
            onNodeWithText(message).assertIsDisplayed()
            onNodeWithText(R.string.transfers_confirm_large_download_button_start).assertIsDisplayed()
            onNodeWithText(R.string.transfers_confirm_large_download_button_start_always).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that dialog shows correctly for large preview confirmation`() {
        val message = composeTestRule.activity
            .getString(sharedR.string.alert_larger_file_preview, sizeString)

        initComposeTestRule(isPreviewDownload = true)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_LARGE_DOWNLOAD_CONFIRMATION_DIALOG).assertIsDisplayed()
            onNodeWithText(sharedR.string.alert_larger_file_preview_title).assertIsDisplayed()
            onNodeWithText(message).assertIsDisplayed()
            onNodeWithText(sharedR.string.alert_larger_file_preview_confirm_button).assertIsDisplayed()
            onNodeWithText(sharedR.string.alert_larger_file_preview_always_allow_button).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on allow button for downloads calls onAllow`() {
        initComposeTestRule(isPreviewDownload = false)

        composeTestRule.onNodeWithText(R.string.transfers_confirm_large_download_button_start)
            .performClick()

        verify(onAllow).invoke()
    }

    @Test
    fun `test that clicking on allow button for preview calls onAllow`() {
        initComposeTestRule(isPreviewDownload = true)

        composeTestRule.onNodeWithText(sharedR.string.alert_larger_file_preview_confirm_button)
            .performClick()

        verify(onAllow).invoke()
    }

    @Test
    fun `test that clicking on always allow button for downloads calls onAlwaysAllow`() {
        initComposeTestRule(isPreviewDownload = false)

        composeTestRule.onNodeWithText(R.string.transfers_confirm_large_download_button_start_always)
            .performClick()

        verify(onAlwaysAllow).invoke()
    }

    @Test
    fun `test that clicking on always allow button for previews calls onAlwaysAllow`() {
        initComposeTestRule(isPreviewDownload = true)

        composeTestRule.onNodeWithText(sharedR.string.alert_larger_file_preview_always_allow_button)
            .performClick()

        verify(onAlwaysAllow).invoke()
    }

    @Test
    fun `test that clicking on dismiss button for downloads calls onDismiss`() {
        initComposeTestRule(isPreviewDownload = false)

        composeTestRule.onNodeWithText(sharedR.string.general_dialog_cancel_button).performClick()

        verify(onDismiss).invoke()
    }

    @Test
    fun `test that clicking on dismiss button for previews calls onDismiss`() {
        initComposeTestRule(isPreviewDownload = false)

        composeTestRule.onNodeWithText(sharedR.string.general_dialog_cancel_button).performClick()

        verify(onDismiss).invoke()
    }

    private fun initComposeTestRule(isPreviewDownload: Boolean) {
        composeTestRule.setContent {
            LargeDownloadConfirmationDialog(
                isPreviewDownload = isPreviewDownload,
                sizeString = sizeString,
                onAllow = onAllow,
                onAlwaysAllow = onAlwaysAllow,
                onDismiss = onDismiss
            )
        }
    }
}