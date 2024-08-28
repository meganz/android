package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.MEDIA_PERMISSIONS_RATIONALE_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.MediaPermissionsRationaleDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [MediaPermissionsRationaleDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class MediaPermissionsRationaleDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog is shown`() {
        composeTestRule.setContent {
            MediaPermissionsRationaleDialog(
                onMediaAccessGranted = {},
                onMediaAccessDenied = {},
            )
        }
        composeTestRule.onNodeWithTag(MEDIA_PERMISSIONS_RATIONALE_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_grant_media_permissions_body)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_grant_media_permissions_positive_button)
            .assertExists()
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_grant_media_permissions_negative_button)
            .assertExists()
    }

    @Test
    fun `test that clicking the positive button invokes the on media access granted lambda`() {
        val onMediaAccessGranted = mock<() -> Unit>()
        composeTestRule.setContent {
            MediaPermissionsRationaleDialog(
                onMediaAccessGranted = onMediaAccessGranted,
                onMediaAccessDenied = {},
            )
        }
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_grant_media_permissions_positive_button)
            .performClick()
        verify(onMediaAccessGranted).invoke()
    }

    @Test
    fun `test that clicking the negative button invokes the on media access denied lambda`() {
        val onMediaAccessDenied = mock<() -> Unit>()
        composeTestRule.setContent {
            MediaPermissionsRationaleDialog(
                onMediaAccessGranted = {},
                onMediaAccessDenied = onMediaAccessDenied,
            )
        }
        composeTestRule.onNodeWithText(R.string.settings_camera_uploads_grant_media_permissions_negative_button)
            .performClick()
        verify(onMediaAccessDenied).invoke()
    }
}