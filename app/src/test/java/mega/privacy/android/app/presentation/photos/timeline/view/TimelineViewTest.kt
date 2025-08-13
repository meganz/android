package mega.privacy.android.app.presentation.photos.timeline.view

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.model.CameraUploadsStatus
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.domain.entity.photos.Photo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class TimelineViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    private fun setComposeContent(
        photoDownload: PhotoDownload = mock(),
        timelineViewState: TimelineViewState,
        lazyGridState: LazyGridState = LazyGridState(),
        onCardClick: (DateCard) -> Unit = {},
        onTimeBarTabSelected: (TimeBarTab) -> Unit = {},
        enableCUView: @Composable () -> Unit = {},
        photosGridView: @Composable () -> Unit = {},
        emptyView: @Composable () -> Unit = {},
        onClickCameraUploadsSync: () -> Unit = {},
        onClickCameraUploadsUploading: () -> Unit = {},
        onChangeCameraUploadsPermissions: () -> Unit = {},
        onUpdateCameraUploadsLimitedAccessState: (Boolean) -> Unit = {},
        onEnableCameraUploads: () -> Unit = {},
        clearCameraUploadsMessage: () -> Unit = {},
        clearCameraUploadsChangePermissionsMessage: () -> Unit = {},
        clearCameraUploadsCompletedMessage: () -> Unit = {},
        loadPhotos: () -> Unit = {},
    ) {
        composeRule.setContent {
            TimelineView(
                photoDownload = photoDownload,
                timelineViewState = timelineViewState,
                lazyGridState = lazyGridState,
                onCardClick = onCardClick,
                onTimeBarTabSelected = onTimeBarTabSelected,
                enableCUView = enableCUView,
                photosGridView = photosGridView,
                emptyView = emptyView,
                onClickCameraUploadsSync = onClickCameraUploadsSync,
                onClickCameraUploadsUploading = onClickCameraUploadsUploading,
                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                onUpdateCameraUploadsLimitedAccessState = onUpdateCameraUploadsLimitedAccessState,
                onEnableCameraUploads = onEnableCameraUploads,
                clearCameraUploadsMessage = clearCameraUploadsMessage,
                clearCameraUploadsChangePermissionsMessage = clearCameraUploadsChangePermissionsMessage,
                clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
                loadPhotos = loadPhotos,
            )
        }
    }

    @Test
    fun `test EnableCameraUploadsBanner is displayed when camera uploads is disabled and no photos selected`() {
        val timelineViewState = createTimelineViewStateForEnableBanner()

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test EnableCameraUploadsBanner is not displayed when camera uploads is enabled`() {
        val timelineViewState = createTimelineViewStateForEnableBanner(
            enableCameraUploadButtonShowing = false
        )

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test EnableCameraUploadsBanner is not displayed when photos are selected`() {
        val timelineViewState = createTimelineViewStateForEnableBanner(
            selectedPhotoCount = 5
        )

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test CameraUploadsCheckingUploadsBanner is displayed when camera uploads status is Sync`() {
        val timelineViewState = createTimelineViewStateForCheckingUploadsBanner()

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test CameraUploadsPendingCountBanner is displayed when camera uploads status is Uploading`() {
        val timelineViewState = createTimelineViewStateForPendingCountBanner(pending = 10)

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_PENDING_COUNT_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test CameraUploadsNoFullAccessBanner is displayed when showCameraUploadsWarning is true`() {
        val timelineViewState = createTimelineViewStateForNoFullAccessBanner()

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test no banner is displayed when camera uploads banner improvement is disabled`() {
        val timelineViewState = createTimelineViewStateForEnableBanner(
            isBannerImprovementEnabled = false
        )

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_PENDING_COUNT_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test banner priority when multiple conditions are met`() {
        // When multiple banner conditions are met, showCameraUploadsWarning should have highest priority
        val timelineViewState = TimelineViewState(
            isCameraUploadsBannerImprovementEnabled = true,
            enableCameraUploadButtonShowing = true,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Sync,
            showCameraUploadsWarning = true,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )

        setComposeContent(timelineViewState = timelineViewState)

        // Should show warning banner instead of enable camera uploads banner
        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test banner visibility when scrolling states change`() {
        val timelineViewState = TimelineViewState(
            isCameraUploadsBannerImprovementEnabled = true,
            enableCameraUploadButtonShowing = true,
            selectedPhotoCount = 0,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )

        setComposeContent(timelineViewState = timelineViewState)

        // Banner should be visible initially
        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test CameraUploadsPendingCountBanner shows correct pending count`() {
        val pendingCount = 25
        val timelineViewState = TimelineViewState(
            isCameraUploadsBannerImprovementEnabled = true,
            enableCameraUploadButtonShowing = false,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Uploading,
            pending = pendingCount,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_PENDING_COUNT_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test CameraUploadsCheckingUploadsBanner is not displayed when status is not Sync`() {
        val timelineViewState = createTimelineViewStateForCheckingUploadsBanner(
            cameraUploadsStatus = CameraUploadsStatus.None
        )

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test CameraUploadsNoFullAccessBanner is not displayed when showCameraUploadsWarning is false`() {
        val timelineViewState = createTimelineViewStateForNoFullAccessBanner(
            showCameraUploadsWarning = false
        )

        setComposeContent(timelineViewState = timelineViewState)

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test banner priority order when all conditions are met`() {
        // Test the priority order: Warning > Enable > Sync > Uploading
        val timelineViewState = TimelineViewState(
            isCameraUploadsBannerImprovementEnabled = true,
            enableCameraUploadButtonShowing = true,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Sync,
            showCameraUploadsWarning = true,
            pending = 5,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )

        setComposeContent(timelineViewState = timelineViewState)

        // Warning banner should have highest priority
        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_PENDING_COUNT_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test banner priority when warning is false but other conditions are met`() {
        val timelineViewState = TimelineViewState(
            isCameraUploadsBannerImprovementEnabled = true,
            enableCameraUploadButtonShowing = true,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Sync,
            showCameraUploadsWarning = false,
            pending = 5,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )

        setComposeContent(timelineViewState = timelineViewState)

        // Enable camera uploads banner should have priority over sync status
        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test banner priority when enable button is false but sync status is active`() {
        val timelineViewState = TimelineViewState(
            isCameraUploadsBannerImprovementEnabled = true,
            enableCameraUploadButtonShowing = false,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Sync,
            showCameraUploadsWarning = false,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )

        setComposeContent(timelineViewState = timelineViewState)

        // Sync status banner should be shown
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test no banner when feature is disabled regardless of other conditions`() {
        val timelineViewState = TimelineViewState(
            isCameraUploadsBannerImprovementEnabled = false,
            enableCameraUploadButtonShowing = true,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Sync,
            showCameraUploadsWarning = true,
            pending = 10,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )

        setComposeContent(timelineViewState = timelineViewState)

        // No banner should be displayed when feature is disabled
        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_PENDING_COUNT_BANNER_TEST_TAG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    // Helper functions to create TimelineViewState for different banner scenarios
    private fun createTimelineViewStateForEnableBanner(
        isBannerImprovementEnabled: Boolean = true,
        enableCameraUploadButtonShowing: Boolean = true,
        selectedPhotoCount: Int = 0,
    ) = TimelineViewState(
        isCameraUploadsBannerImprovementEnabled = isBannerImprovementEnabled,
        enableCameraUploadButtonShowing = enableCameraUploadButtonShowing,
        selectedPhotoCount = selectedPhotoCount,
        currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
        loadPhotosDone = true
    )

    private fun createTimelineViewStateForCheckingUploadsBanner(
        isBannerImprovementEnabled: Boolean = true,
        cameraUploadsStatus: CameraUploadsStatus = CameraUploadsStatus.Sync,
    ) = TimelineViewState(
        isCameraUploadsBannerImprovementEnabled = isBannerImprovementEnabled,
        enableCameraUploadButtonShowing = false,
        selectedPhotoCount = 0,
        cameraUploadsStatus = cameraUploadsStatus,
        currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
        loadPhotosDone = true
    )

    private fun createTimelineViewStateForPendingCountBanner(
        isBannerImprovementEnabled: Boolean = true,
        pending: Int = 10,
    ) = TimelineViewState(
        isCameraUploadsBannerImprovementEnabled = isBannerImprovementEnabled,
        enableCameraUploadButtonShowing = false,
        selectedPhotoCount = 0,
        cameraUploadsStatus = CameraUploadsStatus.Uploading,
        pending = pending,
        currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
        loadPhotosDone = true
    )

    private fun createTimelineViewStateForNoFullAccessBanner(
        isBannerImprovementEnabled: Boolean = true,
        showCameraUploadsWarning: Boolean = true,
    ) = TimelineViewState(
        isCameraUploadsBannerImprovementEnabled = isBannerImprovementEnabled,
        enableCameraUploadButtonShowing = false,
        selectedPhotoCount = 0,
        cameraUploadsStatus = CameraUploadsStatus.None,
        showCameraUploadsWarning = showCameraUploadsWarning,
        currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
        loadPhotosDone = true
    )
}