package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.CameraUploadsBusinessAccountDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [CameraUploadsBusinessAccountDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class CameraUploadsBusinessAccountDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog is shown`() {
        composeTestRule.setContent {
            CameraUploadsBusinessAccountDialog(
                onAlertAcknowledged = {},
                onAlertDismissed = {},
            )
        }
        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.section_photo_sync).assertExists()
        composeTestRule.onNodeWithText(R.string.camera_uploads_business_alert).assertExists()
        composeTestRule.onNodeWithText(R.string.general_enable).assertExists()
        composeTestRule.onNodeWithText(R.string.general_cancel).assertExists()
    }

    @Test
    fun `test that clicking the positive button invokes the on alert acknowledged lambda`() {
        val onAlertAcknowledged = mock<() -> Unit>()
        composeTestRule.setContent {
            CameraUploadsBusinessAccountDialog(
                onAlertAcknowledged = onAlertAcknowledged,
                onAlertDismissed = {},
            )
        }
        composeTestRule.onNodeWithText(R.string.general_enable).performClick()
        verify(onAlertAcknowledged).invoke()
    }

    @Test
    fun `test that clicking the negative button invokes the on alert dismissed lambda`() {
        val onAlertDismissed = mock<() -> Unit>()
        composeTestRule.setContent {
            CameraUploadsBusinessAccountDialog(
                onAlertAcknowledged = {},
                onAlertDismissed = onAlertDismissed,
            )
        }
        composeTestRule.onNodeWithText(R.string.general_cancel).performClick()
        verify(onAlertDismissed).invoke()
    }
}