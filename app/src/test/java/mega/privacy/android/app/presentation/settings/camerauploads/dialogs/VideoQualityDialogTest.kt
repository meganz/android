package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.VIDEO_QUALITY_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.VideoQualityDialog
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [VideoQualityDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class VideoQualityDialogTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog and the list of options are shown`() {
        initializeComposeContent()

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_video_upload_quality).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.general_cancel).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_video_quality_dialog_option_low)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_video_quality_dialog_option_medium)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_video_quality_dialog_option_high)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_video_quality_dialog_option_original)
            .assertIsDisplayed()
    }

    private fun initializeComposeContent() {
        composeTestRule.setContent {
            VideoQualityDialog(
                currentVideoQualityUiItem = VideoQualityUiItem.Low,
                onOptionSelected = {},
                onDismissRequest = {},
            )
        }
    }
}