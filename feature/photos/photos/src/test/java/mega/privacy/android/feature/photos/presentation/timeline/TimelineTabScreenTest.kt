package mega.privacy.android.feature.photos.presentation.timeline

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
import de.palm.composestateevents.triggered
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.CUStatusUiState
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.component.PHOTOS_NODE_BODY_IMAGE_NODE_TAG
import mega.privacy.android.feature.photos.presentation.timeline.component.ENABLE_CAMERA_UPLOADS_CONTENT_ENABLE_BUTTON_TAG
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.navigation.destination.LegacySettingsCameraUploadsActivityNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
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
    private val analyticsTracker = mock<AnalyticsTracker>()

    @Before
    fun setUp() {
        Analytics.initialise(analyticsTracker)
    }

    @After
    fun tearDown() {
        Analytics.initialise(null)
    }

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
            val selectedTimePeriod = MediaTimePeriod.All
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(
                        PhotosNodeContentItem.HeaderItem(
                            time = LocalDateTime.now()
                        )
                    )
                ),
                selectedPhotoIds = setOf(),
                selectedTimePeriod = selectedTimePeriod
            )

            onNodeWithTag(TIMELINE_TAB_CONTENT_GRID_VIEW_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the grid view is displayed when the selected time period is not All`() {
        composeRuleScope {
            val selectedTimePeriod = MediaTimePeriod.Years
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(
                        PhotosNodeContentItem.HeaderItem(
                            time = LocalDateTime.now()
                        )
                    )
                ),
                selectedPhotoIds = setOf(),
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
                        PhotosNodeContentItem.HeaderItem(
                            time = LocalDateTime.now()
                        )
                    )
                ),
                selectedPhotoIds = setOf(),
            )

            onNodeWithTag(TIMELINE_TAB_CONTENT_MEDIA_TIME_PERIOD_SELECTOR_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the photo modification time period selector is not displayed when we have selection`() {
        composeRuleScope {
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(
                        PhotosNodeContentItem.HeaderItem(
                            time = LocalDateTime.now(),
                        )
                    ),
                ),
                selectedPhotoIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L),
            )

            onNodeWithTag(TIMELINE_TAB_CONTENT_MEDIA_TIME_PERIOD_SELECTOR_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that the camera upload complete message is successfully cleared`() {
        composeRuleScope {
            val clearCameraUploadsCompletedMessage = mock<() -> Unit>()
            setScreen(
                mediaCameraUploadUiState = MediaCameraUploadUiState(
                    uploadComplete = triggered(content = 5)
                ),
                clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage
            )

            verify(clearCameraUploadsCompletedMessage).invoke()
        }
    }

    @Test
    fun `test that the photo is successfully clicked`() {
        val photoId = 1L
        val photoUiState = mock<PhotoUiState.Image> {
            on { id } doReturn photoId
            on { fileTypeInfo } doReturn StaticImageFileTypeInfo(
                mimeType = "",
                extension = "jpg"
            )
        }
        val node = PhotoNodeUiState(
            photo = photoUiState,
            isSensitive = false,
            defaultIcon = mega.privacy.android.icon.pack.R.drawable.ic_3d_medium_solid,
        )
        composeRuleScope {
            val onPhotoClick = mock<(node: PhotoNodeUiState) -> Unit>()
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(PhotosNodeContentItem.PhotoNodeItem(node = node))
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
            val onMediaTimePeriodSelected = mock<(MediaTimePeriod) -> Unit>()
            setScreen(
                uiState = TimelineTabUiState(
                    isLoading = false,
                    displayedPhotos = listOf(
                        PhotosNodeContentItem.HeaderItem(
                            time = LocalDateTime.now(),
                        )
                    ),
                ),
                selectedPhotoIds = setOf(),
                onMediaTimePeriodSelected = onMediaTimePeriodSelected
            )

            MediaTimePeriod.entries.forEach {
                val text = context.getString(it.stringResId)
                onNodeWithText(text, useUnmergedTree = true).performClick()

                verify(onMediaTimePeriodSelected).invoke(it)
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
        selectedPhotoIds: Set<Long> = setOf(),
        showTimelineSortDialog: Boolean = false,
        selectedTimePeriod: MediaTimePeriod = MediaTimePeriod.All,
        clearCameraUploadsCompletedMessage: () -> Unit = {},
        onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit = {},
        setEnableCUPage: (Boolean) -> Unit = {},
        onGridSizeChange: (value: TimelineGridSize) -> Unit = {},
        onSortDialogDismissed: () -> Unit = {},
        onSortOptionChange: (value: TimelineTabSortOptions) -> Unit = {},
        onPhotoClick: (node: PhotoNodeUiState) -> Unit = {},
        onPhotoSelected: (node: PhotoNodeUiState) -> Unit = {},
        handleCameraUploadsPermissionsResult: () -> Unit = {},
        handleNotificationPermissionResult: () -> Unit = {},
        onCUBannerDismissRequest: (status: CUStatusUiState) -> Unit = {},
        onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit = {},
        onMediaTimePeriodSelected: (MediaTimePeriod) -> Unit = {},
    ) {
        setContent {
            TimelineTabScreen(
                uiState = uiState,
                mediaCameraUploadUiState = mediaCameraUploadUiState,
                timelineFilterUiState = timelineFilterUiState,
                selectedPhotoIds = selectedPhotoIds,
                showTimelineSortDialog = showTimelineSortDialog,
                selectedTimePeriod = selectedTimePeriod,
                clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
                onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                setEnableCUPage = setEnableCUPage,
                onGridSizeChange = onGridSizeChange,
                onSortDialogDismissed = onSortDialogDismissed,
                onSortOptionChange = onSortOptionChange,
                onPhotoClick = onPhotoClick,
                onPhotoSelected = onPhotoSelected,
                handleCameraUploadsPermissionsResult = handleCameraUploadsPermissionsResult,
                handleNotificationPermissionResult = handleNotificationPermissionResult,
                onCUBannerDismissRequest = onCUBannerDismissRequest,
                onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
                onMediaTimePeriodSelected = onMediaTimePeriodSelected
            )
        }
    }
}
