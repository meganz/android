package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.FILE_UPLOAD_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.FileUploadDialog
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [FileUploadDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class FileUploadDialogTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog and the list of options are shown`() {
        initializeComposeContent()

        composeTestRule.onNodeWithTag(FILE_UPLOAD_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_upload_what_to_upload)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.general_cancel).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_upload_only_photos)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_upload_only_videos)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_upload_photos_and_videos)
            .assertIsDisplayed()
    }

    private fun initializeComposeContent() {
        composeTestRule.setContent {
            FileUploadDialog(
                currentUploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
                onOptionSelected = {},
                onDismissRequest = {},
            )
        }
    }
}