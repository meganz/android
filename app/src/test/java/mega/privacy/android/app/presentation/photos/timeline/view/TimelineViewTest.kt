package mega.privacy.android.app.presentation.photos.timeline.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.model.CameraUploadsStatus
import mega.privacy.android.feature.photos.presentation.timeline.model.CameraUploadsBannerType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class TimelineViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    private fun setComposeContent(
        timelineViewState: TimelineViewState,
        bannerType: CameraUploadsBannerType = CameraUploadsBannerType.NONE,
        isWarningBannerShown: Boolean = false,
        isBannerShown: Boolean = false,
        onChangeCameraUploadsPermissions: () -> Unit = {},
        onEnableCameraUploads: () -> Unit = {},
        onNavigateToCameraUploadsTransferScreen: () -> Unit = {},
        onNavigateToCameraUploadsSettings: () -> Unit = {},
        onWarningBannerDismissed: () -> Unit = {},
        onNavigateMobileDataSetting: () -> Unit = {},
        onNavigateUpgradeScreen: () -> Unit = {},
    ) {
        composeRule.setContent {
            CameraUploadsBanners(
                timelineViewState = timelineViewState,
                bannerType = bannerType,
                isWarningBannerShown = isWarningBannerShown,
                isBannerShown = isBannerShown,
                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                onEnableCameraUploads = onEnableCameraUploads,
                onNavigateToCameraUploadsTransferScreen = onNavigateToCameraUploadsTransferScreen,
                onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                onWarningBannerDismissed = onWarningBannerDismissed,
                onNavigateMobileDataSetting = onNavigateMobileDataSetting,
                onNavigateUpgradeScreen = onNavigateUpgradeScreen,
            )
        }
    }

    @Test
    fun `test EnableCameraUploadsBanner is displayed when camera uploads is disabled and no photos selected`() {
        val timelineViewState = createTimelineViewStateForEnableBanner()
        val onEnableCameraUploads = mock<() -> Unit>()
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true,
            onEnableCameraUploads = onEnableCameraUploads
        )

        val banner = composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
        banner.assertIsDisplayed()
        banner.performClick()
        verify(onEnableCameraUploads).invoke()
    }

    @Test
    fun `test EnableCameraUploadsBanner is not displayed when camera uploads is enabled`() {
        val timelineViewState = createTimelineViewStateForEnableBanner(
            enableCameraUploadButtonShowing = false
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true
        )

        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test EnableCameraUploadsBanner is not displayed when photos are selected`() {
        val timelineViewState = createTimelineViewStateForEnableBanner(
            selectedPhotoCount = 5
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true
        )

        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test CameraUploadsCheckingUploadsBanner is displayed when camera uploads status is Sync`() {
        val timelineViewState = createTimelineViewStateForCheckingUploadsBanner()
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true
        )

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test CameraUploadsPendingCountBanner is displayed when camera uploads status is Uploading`() {
        val timelineViewState = createTimelineViewStateForPendingCountBanner()
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true
        )

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_PENDING_COUNT_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test CameraUploadsNoFullAccessBanner is displayed when isWarningBannerShown is true`() {
        val timelineViewState = createTimelineViewStateForNoFullAccessBanner()
        val onChangeCameraUploadsPermissions = mock<() -> Unit>()
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isWarningBannerShown = true,
            onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions
        )

        val banner =
            composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG)
        banner.assertIsDisplayed()
        banner.performClick()
        verify(onChangeCameraUploadsPermissions).invoke()
    }

    @Test
    fun `test banner priority when multiple conditions are met`() {
        // When multiple banner conditions are met, showCameraUploadsWarning should have highest priority
        val timelineViewState = TimelineViewState(
            enableCameraUploadButtonShowing = true,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Sync,
            showCameraUploadsWarning = true,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true
        )

        // Should show warning banner instead of enable camera uploads banner
        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test CameraUploadsPendingCountBanner shows correct pending count`() {
        val pendingCount = 25
        val timelineViewState = TimelineViewState(
            enableCameraUploadButtonShowing = false,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Uploading,
            pending = pendingCount,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true
        )

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_PENDING_COUNT_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test CameraUploadsCheckingUploadsBanner is not displayed when status is not Sync`() {
        val timelineViewState = createTimelineViewStateForCheckingUploadsBanner(
            cameraUploadsStatus = CameraUploadsStatus.None
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true,
            isWarningBannerShown = true
        )

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test CameraUploadsNoFullAccessBanner is not displayed when showCameraUploadsWarning is false`() {
        val timelineViewState = createTimelineViewStateForNoFullAccessBanner(
            showCameraUploadsWarning = false
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(timelineViewState = timelineViewState, bannerType = bannerType)

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test DeviceChargingNotMetPausedBanner is displayed as expected`() {
        val timelineViewState = createTimelineViewStateForPausedWarningBanners(
            cameraUploadsFinishedReason = CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isWarningBannerShown = true
        )

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_DEVICE_CHARGING_NOT_MET_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test LowBatteryPausedBanner is displayed as expected`() {
        val timelineViewState = createTimelineViewStateForPausedWarningBanners(
            cameraUploadsFinishedReason = CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isWarningBannerShown = true
        )

        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_LOW_BATTERY_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test network requirement not met banner is displayed as expected`() {
        val timelineViewState = createTimelineViewStateForPausedWarningBanners(
            cameraUploadsFinishedReason = CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isWarningBannerShown = true
        )

        composeRule.onNodeWithTag(
            TIMELINE_CAMERA_UPLOADS_NETWORK_REQUIREMENT_NOT_MET_BANNER_TEST_TAG
        ).assertIsDisplayed()
    }

    @Test
    fun `test banner priority order when all conditions are met`() {
        // Test the priority order: Warning > Enable > Sync > Uploading
        val timelineViewState = TimelineViewState(
            enableCameraUploadButtonShowing = true,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Sync,
            showCameraUploadsWarning = true,
            pending = 5,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true
        )

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
            enableCameraUploadButtonShowing = true,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Sync,
            showCameraUploadsWarning = false,
            pending = 5,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true
        )

        // Enable camera uploads banner should have priority over sync status
        composeRule.onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test banner priority when enable button is false but sync status is active`() {
        val timelineViewState = TimelineViewState(
            enableCameraUploadButtonShowing = false,
            selectedPhotoCount = 0,
            cameraUploadsStatus = CameraUploadsStatus.Sync,
            showCameraUploadsWarning = false,
            currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
            loadPhotosDone = true
        )
        val bannerType = getCameraUploadsBannerType(timelineViewState)

        setComposeContent(
            timelineViewState = timelineViewState,
            bannerType = bannerType,
            isBannerShown = true
        )

        // Sync status banner should be shown
        composeRule.onNodeWithTag(TIMELINE_CAMERA_UPLOADS_CHECKING_UPLOADS_BANNER_TEST_TAG)
            .assertIsDisplayed()
    }

    // Helper functions to create TimelineViewState for different banner scenarios
    private fun createTimelineViewStateForEnableBanner(
        enableCameraUploadButtonShowing: Boolean = true,
        selectedPhotoCount: Int = 0,
    ) = TimelineViewState(
        enableCameraUploadButtonShowing = enableCameraUploadButtonShowing,
        selectedPhotoCount = selectedPhotoCount,
        currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
        loadPhotosDone = true
    )

    private fun createTimelineViewStateForCheckingUploadsBanner(
        cameraUploadsStatus: CameraUploadsStatus = CameraUploadsStatus.Sync,
    ) = TimelineViewState(
        enableCameraUploadButtonShowing = false,
        selectedPhotoCount = 0,
        cameraUploadsStatus = cameraUploadsStatus,
        currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
        loadPhotosDone = true
    )

    private fun createTimelineViewStateForPendingCountBanner(
    ) = TimelineViewState(
        enableCameraUploadButtonShowing = false,
        selectedPhotoCount = 0,
        cameraUploadsStatus = CameraUploadsStatus.Uploading,
        pending = 10,
        currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
        loadPhotosDone = true
    )

    private fun createTimelineViewStateForNoFullAccessBanner(
        showCameraUploadsWarning: Boolean = true,
    ) = TimelineViewState(
        isCameraUploadsLimitedAccess = true,
        isWarningBannerShown = true,
        enableCameraUploadButtonShowing = false,
        selectedPhotoCount = 0,
        cameraUploadsStatus = CameraUploadsStatus.None,
        showCameraUploadsWarning = showCameraUploadsWarning,
        currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
        loadPhotosDone = true
    )

    private fun createTimelineViewStateForPausedWarningBanners(
        cameraUploadsFinishedReason: CameraUploadsFinishedReason,
    ) = TimelineViewState(
        isCameraUploadsLimitedAccess = false,
        isWarningBannerShown = true,
        enableCameraUploadButtonShowing = false,
        selectedPhotoCount = 0,
        cameraUploadsStatus = CameraUploadsStatus.None,
        isCUPausedWarningBannerEnabled = true,
        showCameraUploadsWarning = true,
        cameraUploadsFinishedReason = cameraUploadsFinishedReason,
        currentShowingPhotos = listOf<Photo.Video>(mock(), mock()),
        loadPhotosDone = true
    )
}