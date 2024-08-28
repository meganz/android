package mega.privacy.android.app.presentation.settings.camerauploads

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.SETTINGS_CAMERA_UPLOADS_TOOLBAR
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsView
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.FILE_UPLOAD_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.HOW_TO_UPLOAD_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.RELATED_NEW_LOCAL_FOLDER_WARNING_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.VIDEO_COMPRESSION_SIZE_INPUT_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.VIDEO_QUALITY_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsUiState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CAMERA_UPLOADS_FOLDER_NODE_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CAMERA_UPLOADS_LOCAL_FOLDER_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CAMERA_UPLOADS_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.FILE_UPLOAD_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.HOW_TO_UPLOAD_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.INCLUDE_LOCATION_TAGS_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.KEEP_FILE_NAMES_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MEDIA_UPLOADS_FOLDER_NODE_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MEDIA_UPLOADS_LOCAL_FOLDER_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.MEDIA_UPLOADS_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.REQUIRE_CHARGING_DURING_VIDEO_COMPRESSION_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.UPLOAD_ONLY_WHILE_CHARGING_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VIDEO_COMPRESSION_TILE
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.VIDEO_QUALITY_TILE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.fromId

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

        listOf(
            HOW_TO_UPLOAD_TILE,
            UPLOAD_ONLY_WHILE_CHARGING_TILE,
            FILE_UPLOAD_TILE,
            KEEP_FILE_NAMES_TILE,
            CAMERA_UPLOADS_LOCAL_FOLDER_TILE,
            CAMERA_UPLOADS_FOLDER_NODE_TILE,
            MEDIA_UPLOADS_TILE,
        ).forEach { tag ->
            composeTestRule.onNodeWithTag(tag).apply {
                performScrollTo()
                assertIsDisplayed()
            }
        }
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

        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
        }
    }

    @Test
    fun `test that the include location tags tile is shown when the selected upload option is photos and videos`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
        )

        composeTestRule.onNodeWithTag(INCLUDE_LOCATION_TAGS_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
        }
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

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
        }
    }

    @Test
    fun `test that the video quality tile is shown when the selected upload option is photos and videos`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.PhotosAndVideos,
        )

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
        }
    }

    @Test
    fun `test that the require charging during video compression tile is shown`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            uploadOptionUiItem = UploadOptionUiItem.VideosOnly,
            videoQualityUiItem = VideoQualityUiItem.Medium,
        )

        composeTestRule.onNodeWithTag(REQUIRE_CHARGING_DURING_VIDEO_COMPRESSION_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
        }
    }

    @Test
    fun `test that the video compression tile is hidden when charging is not required during video compression`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            requireChargingDuringVideoCompression = false,
            uploadOptionUiItem = UploadOptionUiItem.VideosOnly,
            videoQualityUiItem = VideoQualityUiItem.Medium,
        )

        composeTestRule.onNodeWithTag(VIDEO_COMPRESSION_TILE).assertDoesNotExist()
    }

    @Test
    fun `test that the video compression tile is shown when charging is required during video compression`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            requireChargingDuringVideoCompression = true,
            uploadOptionUiItem = UploadOptionUiItem.VideosOnly,
            videoQualityUiItem = VideoQualityUiItem.Medium,
        )

        composeTestRule.onNodeWithTag(VIDEO_COMPRESSION_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
        }
    }

    @Test
    fun `test that the how to upload prompt is shown when the user clicks the how to upload tile`() {
        initializeComposeContent(isCameraUploadsEnabled = true)

        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
            performClick()
        }

        composeTestRule.onNodeWithTag(HOW_TO_UPLOAD_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that the file upload prompt is shown when the user clicks the file upload tile`() {
        initializeComposeContent(isCameraUploadsEnabled = true)

        composeTestRule.onNodeWithTag(FILE_UPLOAD_TILE).apply {
            performScrollTo()
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
            performScrollTo()
            assertIsDisplayed()
            performClick()
        }

        composeTestRule.onNodeWithTag(VIDEO_QUALITY_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that the video compression size input prompt is shown when the user clicks the video compression tile`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            requireChargingDuringVideoCompression = true,
            uploadOptionUiItem = UploadOptionUiItem.VideosOnly,
            videoQualityUiItem = VideoQualityUiItem.Medium,
        )

        composeTestRule.onNodeWithTag(VIDEO_COMPRESSION_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
            performClick()
        }

        composeTestRule.onNodeWithTag(VIDEO_COMPRESSION_SIZE_INPUT_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that the related new local folder warning prompt is shown`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            showRelatedNewLocalFolderWarning = true,
        )

        composeTestRule.onNodeWithTag(RELATED_NEW_LOCAL_FOLDER_WARNING_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that the camera uploads folder node tile shows a default name when the primary folder name is empty`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            primaryFolderName = "",
        )

        with(composeTestRule) {
            onNodeWithTag(CAMERA_UPLOADS_FOLDER_NODE_TILE).apply {
                performScrollTo()
                assertIsDisplayed()
                assertTextContains(fromId(R.string.section_photo_sync))
            }
        }
    }

    @Test
    fun `test that the camera uploads folder node tile shows a default name when the primary folder name is null`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            primaryFolderName = null,
        )

        with(composeTestRule) {
            onNodeWithTag(CAMERA_UPLOADS_FOLDER_NODE_TILE).apply {
                performScrollTo()
                assertIsDisplayed()
                assertTextContains(fromId(R.string.section_photo_sync))
            }
        }
    }

    @Test
    fun `test that the following tiles are always shown when media uploads is enabled`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            isMediaUploadsEnabled = true,
        )

        listOf(
            MEDIA_UPLOADS_LOCAL_FOLDER_TILE,
            MEDIA_UPLOADS_FOLDER_NODE_TILE,
        ).forEach { tag ->
            composeTestRule.onNodeWithTag(tag).apply {
                performScrollTo()
                assertIsDisplayed()
            }
        }
    }

    @Test
    fun `test that the media uploads folder node tile shows a default name when the secondary folder name is empty`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            isMediaUploadsEnabled = true,
            secondaryFolderName = "",
        )

        composeTestRule.onNodeWithTag(MEDIA_UPLOADS_FOLDER_NODE_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(fromId(R.string.section_secondary_media_uploads))
        }
    }

    @Test
    fun `test that the media uploads folder node tile shows a default name when the secondary folder name is null`() {
        initializeComposeContent(
            isCameraUploadsEnabled = true,
            isMediaUploadsEnabled = true,
            secondaryFolderName = null,
        )

        composeTestRule.onNodeWithTag(MEDIA_UPLOADS_FOLDER_NODE_TILE).apply {
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(fromId(R.string.section_secondary_media_uploads))
        }
    }

    private fun initializeComposeContent(
        isCameraUploadsEnabled: Boolean = false,
        isMediaUploadsEnabled: Boolean = false,
        primaryFolderName: String? = "Primary Folder Name",
        requireChargingDuringVideoCompression: Boolean = true,
        secondaryFolderName: String? = "Secondary Folder Name",
        secondaryFolderPath: String = "secondary/folder/path",
        showRelatedNewLocalFolderWarning: Boolean = false,
        uploadOptionUiItem: UploadOptionUiItem = UploadOptionUiItem.PhotosOnly,
        videoQualityUiItem: VideoQualityUiItem = VideoQualityUiItem.Original,
    ) {
        composeTestRule.setContent {
            SettingsCameraUploadsView(
                uiState = SettingsCameraUploadsUiState(
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                    isMediaUploadsEnabled = isMediaUploadsEnabled,
                    primaryFolderName = primaryFolderName,
                    requireChargingDuringVideoCompression = requireChargingDuringVideoCompression,
                    secondaryFolderName = secondaryFolderName,
                    secondaryFolderPath = secondaryFolderPath,
                    showRelatedNewLocalFolderWarning = showRelatedNewLocalFolderWarning,
                    uploadOptionUiItem = uploadOptionUiItem,
                    videoQualityUiItem = videoQualityUiItem,
                ),
                onBusinessAccountPromptDismissed = {},
                onCameraUploadsProcessStarted = {},
                onCameraUploadsStateChanged = {},
                onChargingDuringVideoCompressionStateChanged = {},
                onChargingWhenUploadingContentStateChanged = {},
                onHowToUploadPromptOptionSelected = {},
                onIncludeLocationTagsStateChanged = {},
                onKeepFileNamesStateChanged = {},
                onLocalPrimaryFolderSelected = {},
                onLocalSecondaryFolderSelected = {},
                onLocationPermissionGranted = {},
                onMediaPermissionsGranted = {},
                onMediaUploadsStateChanged = {},
                onNewVideoCompressionSizeLimitProvided = {},
                onPrimaryFolderNodeSelected = {},
                onRegularBusinessAccountSubUserPromptAcknowledged = {},
                onRelatedNewLocalFolderWarningDismissed = {},
                onRequestLocationPermissionStateChanged = {},
                onRequestMediaPermissionsStateChanged = {},
                onSecondaryFolderNodeSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadOptionUiItemSelected = {},
                onVideoQualityUiItemSelected = {},
            )
        }
    }
}