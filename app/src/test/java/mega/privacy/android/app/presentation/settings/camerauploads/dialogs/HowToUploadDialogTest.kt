package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.HOW_TO_UPLOAD_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.HowToUploadDialog
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [HowToUploadDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class HowToUploadDialogTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog is shown`() {
        initializeComposeContent()
        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_upload_how_to_upload)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.general_cancel)
            .assertIsDisplayed()
    }

    @Test
    fun `test that wifi only is displayed`() {
        initializeComposeContent()
        composeTestRule.onNodeWithText(R.string.cam_sync_wifi).assertIsDisplayed()
    }

    @Test
    fun `test that wifi or mobile data is displayed`() {
        initializeComposeContent()
        composeTestRule.onNodeWithText(R.string.cam_sync_data).assertIsDisplayed()
    }

    private fun initializeComposeContent() {
        composeTestRule.setContent {
            HowToUploadDialog(
                currentUploadConnectionType = UploadConnectionType.WIFI,
                onOptionSelected = {},
                onDismissRequest = {},
            )
        }
    }
}