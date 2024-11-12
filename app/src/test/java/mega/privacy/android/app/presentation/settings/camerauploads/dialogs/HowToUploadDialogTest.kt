package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        composeTestRule.onNodeWithText(sharedR.string.general_dialog_cancel_button)
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