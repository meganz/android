package test.mega.privacy.android.app.presentation.photos.timeline.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.timeline.model.ApplyFilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.PhotoListItem
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.IsCameraSyncPreferenceEnabled
import mega.privacy.android.domain.usecase.MonitorCameraUploadProgress
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.photos.EnableCameraUploadsInPhotosUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadAndHeartbeatUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {
    private lateinit var underTest: TimelineViewModel

    private val isCameraSyncPreferenceEnabled =
        mock<IsCameraSyncPreferenceEnabled> { on { invoke() }.thenReturn(true) }

    private val getTimelinePhotosUseCase =
        mock<GetTimelinePhotosUseCase> { on { invoke() }.thenReturn(emptyFlow()) }

    private val filterCameraUploadPhotos =
        mock<FilterCameraUploadPhotos> { onBlocking { invoke(any()) }.thenAnswer { it.arguments[0] } }

    private val filterCloudDrivePhotos =
        mock<FilterCloudDrivePhotos> { onBlocking { invoke(any()) }.thenAnswer { it.arguments[0] } }

    private val setInitialCUPreferences = mock<SetInitialCUPreferences>()

    private val enableCameraUploadsInPhotosUseCase = mock<EnableCameraUploadsInPhotosUseCase>()

    private val getNodeListByIds = mock<GetNodeListByIds> {
        onBlocking { invoke(any()) }.thenReturn(
            emptyList()
        )
    }

    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()

    private val checkEnableCameraUploadsStatus = mock<CheckEnableCameraUploadsStatus>()

    private val monitorCameraUploadProgress = mock<MonitorCameraUploadProgress>()

    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()

    private val stopCameraUploadAndHeartbeatUseCase = mock<StopCameraUploadAndHeartbeatUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = TimelineViewModel(
            isCameraSyncPreferenceEnabled = isCameraSyncPreferenceEnabled,
            getTimelinePhotosUseCase = getTimelinePhotosUseCase,
            getCameraUploadPhotos = filterCameraUploadPhotos,
            getCloudDrivePhotos = filterCloudDrivePhotos,
            setInitialCUPreferences = setInitialCUPreferences,
            enableCameraUploadsInPhotosUseCase = enableCameraUploadsInPhotosUseCase,
            getNodeListByIds = getNodeListByIds,
            startCameraUploadUseCase = startCameraUploadUseCase,
            ioDispatcher = StandardTestDispatcher(),
            mainDispatcher = StandardTestDispatcher(),
            checkEnableCameraUploadsStatus = checkEnableCameraUploadsStatus,
            monitorCameraUploadProgress = monitorCameraUploadProgress,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            stopCameraUploadAndHeartbeatUseCase = stopCameraUploadAndHeartbeatUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state`() = runTest {
        underTest.state.test {
            val initialState = awaitItem()
            assertWithMessage("photos value is incorrect").that(initialState.photos).isEmpty()
            assertWithMessage("currentShowingPhotos value is incorrect").that(initialState.currentShowingPhotos)
                .isEmpty()
            assertWithMessage("photosListItems value is incorrect").that(initialState.photosListItems)
                .isEmpty()
            assertWithMessage("loadPhotosDone value is incorrect").that(initialState.loadPhotosDone)
                .isFalse()
            assertWithMessage("yearsCardPhotos value is incorrect").that(initialState.yearsCardPhotos)
                .isEmpty()
            assertWithMessage("monthsCardPhotos value is incorrect").that(initialState.monthsCardPhotos)
                .isEmpty()
            assertWithMessage("daysCardPhotos value is incorrect").that(initialState.daysCardPhotos)
                .isEmpty()
            assertWithMessage("timeBarTabs value is incorrect").that(initialState.timeBarTabs)
                .containsExactlyElementsIn(TimeBarTab.values())
            assertWithMessage("selectedTimeBarTab value is incorrect").that(initialState.selectedTimeBarTab)
                .isEqualTo(TimeBarTab.All)
            assertWithMessage("currentZoomLevel value is incorrect").that(initialState.currentZoomLevel)
                .isEqualTo(ZoomLevel.Grid_3)
            assertWithMessage("scrollStartIndex value is incorrect").that(initialState.scrollStartIndex)
                .isEqualTo(0)
            assertWithMessage("applyFilterMediaType value is incorrect").that(initialState.applyFilterMediaType)
                .isEqualTo(ApplyFilterMediaType.ALL_MEDIA_IN_CD_AND_CU)
            assertWithMessage("currentFilterMediaType value is incorrect").that(initialState.currentFilterMediaType)
                .isEqualTo(FilterMediaType.ALL_MEDIA)
            assertWithMessage("currentMediaSource value is incorrect").that(initialState.currentMediaSource)
                .isEqualTo(TimelinePhotosSource.ALL_PHOTOS)
            assertWithMessage("currentSort value is incorrect").that(initialState.currentSort)
                .isEqualTo(Sort.NEWEST)
            assertWithMessage("showingFilterPage value is incorrect").that(initialState.showingFilterPage)
                .isFalse()
            assertWithMessage("showingSortByDialog value is incorrect").that(initialState.showingSortByDialog)
                .isFalse()
            assertWithMessage("enableZoomIn value is incorrect").that(initialState.enableZoomIn)
                .isTrue()
            assertWithMessage("enableZoomOut value is incorrect").that(initialState.enableZoomOut)
                .isTrue()
            assertWithMessage("enableSortOption value is incorrect").that(initialState.enableSortOption)
                .isTrue()
            assertWithMessage("enableCameraUploadButtonShowing value is incorrect").that(
                initialState.enableCameraUploadButtonShowing
            ).isTrue()
            assertWithMessage("progressBarShowing value is incorrect").that(initialState.progressBarShowing)
                .isFalse()
            assertWithMessage("progress value is incorrect").that(initialState.progress)
                .isEqualTo(0f)
            assertWithMessage("pending value is incorrect").that(initialState.pending).isEqualTo(0)
            assertWithMessage("enableCameraUploadPageShowing value is incorrect").that(initialState.enableCameraUploadPageShowing)
                .isFalse()
            assertWithMessage("cuUploadsVideos value is incorrect").that(initialState.cuUploadsVideos)
                .isFalse()
            assertWithMessage("cuUseCellularConnection value is incorrect").that(initialState.cuUseCellularConnection)
                .isFalse()
            assertWithMessage("selectedPhotoCount value is incorrect").that(initialState.selectedPhotoCount)
                .isEqualTo(0)
            assertWithMessage("selectedPhoto value is incorrect").that(initialState.selectedPhoto)
                .isNull()
            assertWithMessage("shouldTriggerCameraUploads value is incorrect").that(initialState.shouldTriggerCameraUploads)
                .isFalse()
            assertWithMessage("shouldShowBusinessAccountPrompt value is incorrect").that(
                initialState.shouldShowBusinessAccountPrompt
            ).isFalse()
            assertWithMessage("shouldShowBusinessAccountSuspendedPrompt value is incorrect").that(
                initialState.shouldShowBusinessAccountSuspendedPrompt
            ).isFalse()
            assertWithMessage("shouldTriggerMediaPermissionsDeniedLogic value is incorrect").that(
                initialState.shouldTriggerMediaPermissionsDeniedLogic
            ).isFalse()
        }
    }

    @Test
    fun `test that a single photo returned is returned by the state`() = runTest {
        val expectedDate = LocalDateTime.now()
        val photo = mock<Photo> { on { modificationTime }.thenReturn(expectedDate) }
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf(photo)))

        underTest.state.drop(1).test {
            val initialisedState = awaitItem()
            assertWithMessage("Expected photos do not match").that(initialisedState.photos)
                .containsExactly(photo)
            assertWithMessage("Expected photosListItems do not match").that(initialisedState.photosListItems)
                .containsExactlyElementsIn(
                    listOf(
                        PhotoListItem.Separator(expectedDate),
                        PhotoListItem.PhotoGridItem(photo, false)
                    )
                )
            val hasPhoto =
                Correspondence.transforming<DateCard, Photo>({ it?.photo }, "contains photo")

            assertWithMessage("Day card photos do not match").that(initialisedState.daysCardPhotos)
                .comparingElementsUsing(hasPhoto)
                .contains(photo)

            assertWithMessage("Month card photos do not match").that(initialisedState.monthsCardPhotos)
                .comparingElementsUsing(hasPhoto)
                .contains(photo)

            assertWithMessage("Year card photos do not match").that(initialisedState.yearsCardPhotos)
                .comparingElementsUsing(hasPhoto)
                .contains(photo)

            assertWithMessage("Loading is not complete").that(initialisedState.loadPhotosDone)
                .isTrue()
        }
    }

    /**
     * Mocks the value of [checkEnableCameraUploadsStatus] and calls the ViewModel method
     *
     * @param status The [EnableCameraUploadsStatus] to mock the Use Case
     */
    private suspend fun handleEnableCameraUploads(status: EnableCameraUploadsStatus) {
        whenever(checkEnableCameraUploadsStatus()).thenReturn(status)
        underTest.handleEnableCameraUploads()
    }

    @Test
    fun `test that shouldShowBusinessAccountPrompt is true when checkEnableCameraUploadsStatus returns SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            handleEnableCameraUploads(status = EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT)
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldShowBusinessAccountPrompt).isTrue()
            }
        }

    @Test
    fun `test that shouldShowBusinessAccountSuspendedPrompt is true when checkEnableCameraUploadsStatus returns SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            handleEnableCameraUploads(status = EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT)
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldShowBusinessAccountSuspendedPrompt).isTrue()
            }
        }

    @Test
    fun `test that shouldTriggerCameraUploads is true when checkEnableCameraUploadsStatus returns CAN_ENABLE_CAMERA_UPLOADS`() =
        runTest {
            handleEnableCameraUploads(status = EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldTriggerCameraUploads).isTrue()
            }
        }

    @Test
    fun `test that when camera upload progress is received, then state is set properly`() =
        runTest {
            val expectedProgress = 50
            val expectedPending = 25
            val pair = Pair(expectedProgress, expectedPending)

            whenever(monitorCameraUploadProgress()).thenReturn(flowOf(pair))

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.pending).isEqualTo(expectedPending)
                assertThat(state.progressBarShowing).isEqualTo(true)
                assertThat(state.progress).isEqualTo(expectedProgress.toFloat() / 100)
            }
        }

    @Test
    fun `test that when camera upload progress is received with pending 0, then progressBarShowing state is set to false`() =
        runTest {
            val expectedProgress = 50
            val expectedPending = 0
            val pair = Pair(expectedProgress, expectedPending)

            whenever(monitorCameraUploadProgress()).thenReturn(flowOf(pair))

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.progressBarShowing).isEqualTo(false)
            }
        }

    @Test
    fun `test that when camera upload progress is received and some items are currently selected, the progressBarShowing state is set to false`() =
        runTest {
            val progress = flowOf(mock<Pair<Int, Int>>())

            underTest.setSelectedPhotos(listOf(mock()))
            whenever(monitorCameraUploadProgress()).thenReturn(progress)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.progressBarShowing).isEqualTo(false)
            }
        }

    @Test
    fun `test that when camera upload progress is received and current view is not TimeBar ALL, the progressBarShowing state is set to false`() =
        runTest {
            val progress = flowOf(mock<Pair<Int, Int>>())

            underTest.onTimeBarTabSelected(TimeBarTab.Years)
            whenever(monitorCameraUploadProgress()).thenReturn(progress)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.progressBarShowing).isEqualTo(false)
            }
        }

    @Test
    fun `test that when stopCameraUploadAndHeartbeat is called, stopCameraUploadAndHeartbeatUseCase is called`() =
        runTest {
            underTest.stopCameraUploadAndHeartbeat()

            advanceUntilIdle()

            verify(stopCameraUploadAndHeartbeatUseCase).invoke()
        }

    @Test
    fun `test that when enableCU is called, enableCameraUploadsInPhotosUseCase is called with the previously primary folder path set`() =
        runTest {
            val expected = "/previously/set/path"
            whenever(getPrimaryFolderPathUseCase()).thenReturn(expected)
            underTest.enableCU()
            advanceUntilIdle()
            verify(enableCameraUploadsInPhotosUseCase).invoke(
                primaryFolderLocalPath = eq(expected),
                shouldSyncVideos = any(),
                shouldUseWiFiOnly = any(),
                videoCompressionSizeLimit = any(),
                videoUploadQuality = any(),
            )
        }

    @Test
    fun `test that when enableCU is called, startCameraUploadUseCase is called`() = runTest {
        underTest.enableCU()
        advanceUntilIdle()
        verify(startCameraUploadUseCase).invoke()
    }
}
