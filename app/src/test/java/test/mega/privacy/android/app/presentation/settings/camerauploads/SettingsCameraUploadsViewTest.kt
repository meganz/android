package test.mega.privacy.android.app.presentation.settings.camerauploads

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.settings.camerauploads.SETTINGS_CAMERA_UPLOADS_TOOLBAR
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsView
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.FILE_UPLOAD_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.HOW_TO_UPLOAD_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.VIDEO_QUALITY_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsUiState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CAMERA_UPLOADS_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.FILE_UPLOAD_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.HOW_TO_UPLOAD_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.INCLUDE_LOCATION_TAGS_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.KEEP_FILE_NAMES_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VIDEO_QUALITY_TILE
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
        initializeComposeContent(isCameraUploadsEnabled = false)

        composeTestRule.onNodeWithTag(SETTINGS_CAMERA_UPLOADS_TOOLBAR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAMERA_UPLOADS_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that the following tiles are always shown when camera uploads is enabled`() {
        initializeComposeContent(isCameraUploadsEnabled = true)

        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FILE_UPLOAD_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(KEEP_FILE_NAMES_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that the include location tags tile is hidden when the selected upload option is videos only`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.VideosOnly,
        )

        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE).assertDoesNotExist()
    }

    @Test
    fun `test that the include location tags tile is shown when the selected upload option is photos only`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
        )

        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that the include location tags tile is shown when the selected upload option is photos and videos`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
        )

        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that the video quality tile is hidden when the selected upload option is photos only`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
        )

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TILE).assertDoesNotExist()
    }

    @Test
    fun `test that the video quality tile is shown when the selected upload option is videos only`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.VideosOnly,
        )

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that the video quality tile is shown when the selected upload option is photos and videos`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.PhotosAndVideos,
        )

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that the how to upload prompt is shown when the user clicks the how to upload tile`() {
        initializeComposeContent(isCameraUploadsEnabled = true)

        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_TILE).apply {
            assertIsDisplayed()
            performClick()
        }

        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that the file upload prompt is shown when the user clicks the file upload tile`() {
        initializeComposeContent(isCameraUploadsEnabled = true)

        composeTestRule.onNodeWithTag(FILE_UPLOAD_TILE).apply {
            assertIsDisplayed()
            performClick()
        }

        composeTestRule.onNodeWithTag(FILE_UPLOAD_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that the video quality prompt is shown when the user clicks the video quality tile`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.VideosOnly,
        )

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TILE).apply {
            assertIsDisplayed()
            performClick()
        }

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_DIALOG).assertIsDisplayed()
    }

    private fun initializeComposeContent(
        isCameraUploadsEnabled: Boolean = false,
        uploadOptionUiItem: UploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
    ) {
        composeTestRule.setContent {
            SettingsCameraUploadsView(
                uiState = SettingsCameraUploadsUiState(
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                    uploadOptionUiItem = uploadOptionUiItem,
                ),
                onBusinessAccountPromptDismissed = {},
                onCameraUploadsStateChanged = {},
                onIncludeLocationTagsStateChanged = {},
                onHowToUploadPromptOptionSelected = {},
                onKeepFileNamesStateChanged = {},
                onMediaPermissionsGranted = {},
                onRegularBusinessAccountSubUserPromptAcknowledged = {},
                onRequestPermissionsStateChanged = {},
                onSettingsScreenPaused = {},
                onUploadOptionUiItemSelected = {},
                onVideoQualityUiItemSelected = {},
            )
        }
    }
}