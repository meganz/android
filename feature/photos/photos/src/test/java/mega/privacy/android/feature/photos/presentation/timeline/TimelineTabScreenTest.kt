package mega.privacy.android.feature.photos.presentation.timeline

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.extensions.LocalDownloadPhotoResultMock
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.CUStatusUiState
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.component.PHOTOS_NODE_BODY_IMAGE_NODE_TAG
import mega.privacy.android.feature.photos.presentation.timeline.component.ENABLE_CAMERA_UPLOADS_CONTENT_ENABLE_BUTTON_TAG
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.navigation.destination.LegacySettingsCameraUploadsActivityNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TimelineTabScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that by default the sort dialog is not displayed`() {
        composeRuleScope {
            setScreen()

            onNodeWithTag(TIMELINE_TAB_SCREEN_SORT_DIALOG_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that the sort dialog is successfully displayed`() {
        composeRuleScope {
            setScreen(showTimelineSortDialog = true)

            // The internal implementation of BasicRadioDialog uses the parent modifier for two different
            // components. Therefore, in this test, we only need to verify the first node.
            onAllNodesWithTag(TIMELINE_TAB_SCREEN_SORT_DIALOG_TAG)
                .onFirst()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that the camera upload message is successfully cleared`() {
        composeRuleScope {
            val clearCameraUploadsMessage = mock<() -> Unit>()
            setScreen(
                mediaCameraUploadUiState = MediaCameraUploadUiState(
                    cameraUploadsMessage = "cameraUploadsMessage"
                ),
                clearCameraUploadsMessage = clearCameraUploadsMessage
            )

            verify(clearCameraUploadsMessage).invoke()
        }
    }

    @Test
    fun `test that the enable camera uploads content is successfully displayed when should enable the CU page`() {
        composeRuleScope {
            setScreen(
                mediaCameraUploadUiState = MediaCameraUploadUiState(
                    enableCameraUploadPageShowing = true,
                ),
                timelineFilterUiState = TimelineFilterUiState(
                    mediaSource = FilterMediaSource.CameraUpload
                )
            )

            onNodeWithTag(TIMELINE_TAB_SCREEN_ENABLE_CU_CONTENT_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the user is navigated to the settings CU when the enable button is clicked`() {
        composeRuleScope {
            val onNavigateToCameraUploadsSettings =
                mock<(key: LegacySettingsCameraUploadsActivityNavKey) -> Unit>()
            setScreen(
                mediaCameraUploadUiState = MediaCameraUploadUiState(
                    enableCameraUploadPageShowing = true,
                ),
                timelineFilterUiState = TimelineFilterUiState(
                    mediaSource = FilterMediaSource.CameraUpload
                ),
                onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings
            )

            onNodeWithTag(ENABLE_CAMERA_UPLOADS_CONTENT_ENABLE_BUTTON_TAG)
                .performScrollTo()
                .performClick()

            verify(onNavigateToCameraUploadsSettings).invoke(
                LegacySettingsCameraUploadsActivityNavKey()
            )
        }
    }

    @Test
    fun `test that the loading view is displayed`() {
        composeRuleScope {
            setScreen(
                uiState = TimelineTabUiState(isLoading = true)
            )

            onNodeWithTag(TIMELINE_TAB_SCREEN_LOADING_SKELETON_VIEW_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the empty view is displayed`() {
        composeRuleScope {
            setScreen(
                uiState = TimelineTabUiState(isLoading = false),
                mediaCameraUploadUiState = MediaCameraUploadUiState(
                    status = CUStatusUiState.None
                )
            )

            onNodeWithTag(TIMELINE_TAB_SCREEN_EMPTY_BODY_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the CU page enable is requested`() {
        composeRuleScope {
            val setEnableCUPage = mock<(Boolean) -> Unit>()
            setScreen(
                uiState = TimelineTabUiState(isLoading = false),
                mediaCameraUploadUiState = MediaCameraUploadUiState(
                    status = CUStatusUiState.Disabled()
                ),
                timelineFilterUiState = TimelineFilterUiState(
                    mediaSource = FilterMediaSource.CameraUpload
                ),
                setEnableCUPage = setEnableCUPage
            )

            verify(setEnableCUPage).invoke(true)
        }
    }

    @Test
    fun `test that the grid view is displayed when the selected time period is All`() {
        composeRuleScope {
            val selectedTimePeriod = PhotoModificationTimePeriod.All
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(
                        PhotosNodeContentType.HeaderItem(
                            time = LocalDateTime.now(),
                            shouldShowGridSizeSettings = true
                        )
                    )
                ),
                selectedTimePeriod = selectedTimePeriod
            )

            onNodeWithTag(TIMELINE_TAB_CONTENT_GRID_VIEW_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the grid view is displayed when the selected time period is not All`() {
        composeRuleScope {
            val selectedTimePeriod = PhotoModificationTimePeriod.Years
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(
                        PhotosNodeContentType.HeaderItem(
                            time = LocalDateTime.now(),
                            shouldShowGridSizeSettings = true
                        )
                    )
                ),
                selectedTimePeriod = selectedTimePeriod
            )

            onNodeWithTag(TIMELINE_TAB_CONTENT_LIST_VIEW_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the photo modification time period selector is displayed when no photos are selected`() {
        composeRuleScope {
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(
                        PhotosNodeContentType.HeaderItem(
                            time = LocalDateTime.now(),
                            shouldShowGridSizeSettings = true
                        )
                    )
                ),
            )

            onNodeWithTag(TIMELINE_TAB_CONTENT_PHOTO_MODIFICATION_TIME_PERIOD_SELECTOR_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the photo modification time period selector is not displayed when we have selection`() {
        composeRuleScope {
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(
                        PhotosNodeContentType.HeaderItem(
                            time = LocalDateTime.now(),
                            shouldShowGridSizeSettings = true
                        )
                    ),
                    selectedPhotoCount = 10
                ),
            )

            onNodeWithTag(TIMELINE_TAB_CONTENT_PHOTO_MODIFICATION_TIME_PERIOD_SELECTOR_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that the camera upload complete message is successfully cleared`() {
        composeRuleScope {
            val clearCameraUploadsCompletedMessage = mock<() -> Unit>()
            setScreen(
                mediaCameraUploadUiState = MediaCameraUploadUiState(
                    showCameraUploadsCompletedMessage = true
                ),
                clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage
            )

            verify(clearCameraUploadsCompletedMessage).invoke()
        }
    }

    @Test
    fun `test that the photo is successfully clicked`() {
        val photoUiState = mock<PhotoUiState.Image>()
        val node = PhotoNodeUiState(
            photo = photoUiState,
            isSensitive = false,
            isSelected = false,
            defaultIcon = mega.privacy.android.icon.pack.R.drawable.ic_3d_medium_solid,
        )
        composeRuleScope {
            val onPhotoClick = mock<(node: PhotoNodeUiState) -> Unit>()
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(PhotosNodeContentType.PhotoNodeItem(node = node))
                ),
                onPhotoClick = onPhotoClick,
            )

            onNodeWithTag(PHOTOS_NODE_BODY_IMAGE_NODE_TAG)
                .performScrollTo()
                .performClick()

            verify(onPhotoClick).invoke(node)
        }
    }

    @Test
    fun `test that the photo time period is successfully selected`() {
        composeRuleScope {
            val onPhotoTimePeriodSelected = mock<(PhotoModificationTimePeriod) -> Unit>()
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(
                        PhotosNodeContentType.HeaderItem(
                            time = LocalDateTime.now(),
                            shouldShowGridSizeSettings = true
                        )
                    ),
                ),
                onPhotoTimePeriodSelected = onPhotoTimePeriodSelected
            )

            PhotoModificationTimePeriod.entries.forEach {
                val text = context.getString(it.stringResId)
                onNodeWithText(text, useUnmergedTree = true).performClick()

                verify(onPhotoTimePeriodSelected).invoke(it)
            }
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setScreen(
        uiState: TimelineTabUiState = TimelineTabUiState(),
        mediaCameraUploadUiState: MediaCameraUploadUiState = MediaCameraUploadUiState(),
        timelineFilterUiState: TimelineFilterUiState = TimelineFilterUiState(),
        showTimelineSortDialog: Boolean = false,
        selectedTimePeriod: PhotoModificationTimePeriod = PhotoModificationTimePeriod.All,
        clearCameraUploadsMessage: () -> Unit = {},
        clearCameraUploadsCompletedMessage: () -> Unit = {},
        onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit = {},
        setEnableCUPage: (Boolean) -> Unit = {},
        onGridSizeChange: (value: TimelineGridSize) -> Unit = {},
        onSortDialogDismissed: () -> Unit = {},
        onSortOptionChange: (value: TimelineTabSortOptions) -> Unit = {},
        onPhotoClick: (node: PhotoNodeUiState) -> Unit = {},
        onPhotoSelected: (node: PhotoNodeUiState) -> Unit = {},
        handleCameraUploadsPermissionsResult: () -> Unit = {},
        onCUBannerDismissRequest: (status: CUStatusUiState) -> Unit = {},
        onTabsVisibilityChange: (shouldHide: Boolean) -> Unit = {},
        onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit = {},
        onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit = {},
    ) {
        setContent {
            CompositionLocalProvider(LocalDownloadPhotoResultMock provides DownloadPhotoResult.Idle) {
                TimelineTabScreen(
                    uiState = uiState,
                    mediaCameraUploadUiState = mediaCameraUploadUiState,
                    timelineFilterUiState = timelineFilterUiState,
                    showTimelineSortDialog = showTimelineSortDialog,
                    selectedTimePeriod = selectedTimePeriod,
                    clearCameraUploadsMessage = clearCameraUploadsMessage,
                    clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
                    onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                    setEnableCUPage = setEnableCUPage,
                    onGridSizeChange = onGridSizeChange,
                    onSortDialogDismissed = onSortDialogDismissed,
                    onSortOptionChange = onSortOptionChange,
                    onPhotoClick = onPhotoClick,
                    onPhotoSelected = onPhotoSelected,
                    handleCameraUploadsPermissionsResult = handleCameraUploadsPermissionsResult,
                    onCUBannerDismissRequest = onCUBannerDismissRequest,
                    onTabsVisibilityChange = onTabsVisibilityChange,
                    onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
                    onPhotoTimePeriodSelected = onPhotoTimePeriodSelected
                )
            }
        }
    }
}
