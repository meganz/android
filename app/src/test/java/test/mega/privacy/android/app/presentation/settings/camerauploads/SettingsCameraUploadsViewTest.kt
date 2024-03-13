package test.mega.privacy.android.app.presentation.settings.camerauploads

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.settings.camerauploads.SETTINGS_CAMERA_UPLOADS_TOOLBAR
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsView
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [SettingsCameraUploadsView]
 */
@RunWith(AndroidJUnit4::class)
internal class SettingsCameraUploadsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the toolbar is shown`() {
        composeTestRule.setContent {
            SettingsCameraUploadsView(
                uiState = SettingsCameraUploadsState(
                    isCameraUploadsEnabled = false,
                    isMediaUploadsEnabled = false,
                    requestPermissions = consumed,
                ),
                onBusinessAccountAdministratorSuspendedPromptAcknowledged = {},
                onBusinessAccountPromptAcknowledged = {},
                onBusinessAccountPromptDismissed = {},
                onBusinessAccountSubUserSuspendedPromptAcknowledged = {},
                onCameraUploadsStateChanged = {},
                onMediaPermissionsGranted = {},
                onMediaPermissionsRationaleStateChanged = {},
                onRequestPermissionsStateChanged = {},
                onSettingsScreenPaused = {},
            )
        }
        composeTestRule.onNodeWithTag(SETTINGS_CAMERA_UPLOADS_TOOLBAR).assertIsDisplayed()
    }
}