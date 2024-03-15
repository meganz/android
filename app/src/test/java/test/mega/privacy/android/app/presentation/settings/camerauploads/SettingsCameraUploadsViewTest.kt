package test.mega.privacy.android.app.presentation.settings.camerauploads

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.account.business.BUSINESS_ACCOUNT_SUSPENDED_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.SETTINGS_CAMERA_UPLOADS_TOOLBAR
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsView
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.HOW_TO_UPLOAD_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsUiState
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CAMERA_UPLOADS_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.HOW_TO_UPLOAD_TILE
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
    fun `test that only the toolbar and camera uploads switch is shown when the feature is disabled`() {
        initializeComposeContent()

        testInitialCameraUploadsConfiguration()
    }

    @Test
    fun `test that a prompt is shown when the business account sub user attempts to enable camera uploads`() {
        initializeComposeContent(showBusinessAccountPrompt = true)

        testInitialCameraUploadsConfiguration()
        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_BUSINESS_ACCOUNT_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that a prompt is shown when the suspended business account sub user attempts to enable camera uploads`() {
        initializeComposeContent(showBusinessAccountSubUserSuspendedPrompt = true)

        testInitialCameraUploadsConfiguration()
        composeTestRule.onNodeWithTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that a prompt is shown when the suspended business account administrator attempts to enable camera uploads`() {
        initializeComposeContent(showBusinessAccountAdministratorSuspendedPrompt = true)

        testInitialCameraUploadsConfiguration()
        composeTestRule.onNodeWithTag(BUSINESS_ACCOUNT_SUSPENDED_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that the how to upload prompt is shown when the user clicks the how to upload tile`() {
        initializeComposeContent(isCameraUploadsEnabled = true)
        testInitialCameraUploadsConfiguration()

        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_TILE).apply {
            assertIsDisplayed()
            performClick()
        }

        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that other options are shown when camera uploads is enabled`() {
        initializeComposeContent(isCameraUploadsEnabled = true)

        testInitialCameraUploadsConfiguration()
        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_TILE).assertIsDisplayed()
    }

    private fun initializeComposeContent(
        isCameraUploadsEnabled: Boolean = false,
        showBusinessAccountPrompt: Boolean = false,
        showBusinessAccountAdministratorSuspendedPrompt: Boolean = false,
        showBusinessAccountSubUserSuspendedPrompt: Boolean = false,
    ) {
        composeTestRule.setContent {
            SettingsCameraUploadsView(
                uiState = SettingsCameraUploadsUiState(
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                    showBusinessAccountPrompt = showBusinessAccountPrompt,
                    showBusinessAccountAdministratorSuspendedPrompt = showBusinessAccountAdministratorSuspendedPrompt,
                    showBusinessAccountSubUserSuspendedPrompt = showBusinessAccountSubUserSuspendedPrompt,
                ),
                onBusinessAccountAdministratorSuspendedPromptAcknowledged = {},
                onBusinessAccountPromptAcknowledged = {},
                onBusinessAccountPromptDismissed = {},
                onBusinessAccountSubUserSuspendedPromptAcknowledged = {},
                onCameraUploadsStateChanged = {},
                onHowToUploadPromptOptionSelected = {},
                onMediaPermissionsGranted = {},
                onMediaPermissionsRationaleStateChanged = {},
                onRequestPermissionsStateChanged = {},
                onSettingsScreenPaused = {},
            )
        }
    }

    private fun testInitialCameraUploadsConfiguration() {
        composeTestRule.onNodeWithTag(SETTINGS_CAMERA_UPLOADS_TOOLBAR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_TILE).assertIsDisplayed()
    }
}