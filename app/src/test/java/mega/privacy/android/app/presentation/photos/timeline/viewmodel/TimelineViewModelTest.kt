package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.mapper.TimelinePreferencesMapper
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.LocationPreference
import mega.privacy.android.app.presentation.photos.model.MediaTypePreference
import mega.privacy.android.app.presentation.photos.model.RememberPreferences
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.model.ApplyFilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.PhotoListItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.camerauploads.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.photos.EnableCameraUploadsInPhotosUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.photos.LoadNextPageOfPhotosUseCase
import mega.privacy.android.domain.usecase.photos.MonitorPaginatedTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import mega.privacy.android.feature.photos.domain.usecase.GetNodeListByIds
import mega.privacy.android.feature.photos.model.CameraUploadsStatus
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.Sort
import mega.privacy.android.feature.photos.model.TimelinePhotosSource
import mega.privacy.android.feature.photos.model.ZoomLevel
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

/**
 * Test class for [TimelineViewModel]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TimelineViewModelTest {
    private lateinit var underTest: TimelineViewModel

    private val isCameraUploadsEnabledUseCase =
        mock<IsCameraUploadsEnabledUseCase> { onBlocking { invoke() }.thenReturn(true) }

    private val getTimelinePhotosUseCase = mock<GetTimelinePhotosUseCase>()

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

    private val checkEnableCameraUploadsStatusUseCase =
        mock<CheckEnableCameraUploadsStatusUseCase>()

    private val cameraUploadsStatusInfoFlow = MutableSharedFlow<CameraUploadsStatusInfo>()
    private val monitorCameraUploadsStatusInfoUseCase =
        mock<MonitorCameraUploadsStatusInfoUseCase>()

    private val stopCameraUploadsUseCase = mock<StopCameraUploadsUseCase>()

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    private val getTimelineFilterPreferencesUseCase = mock<GetTimelineFilterPreferencesUseCase>()

    private val setTimelineFilterPreferencesUseCase = mock<SetTimelineFilterPreferencesUseCase>()

    private val hasMediaPermissionUseCase = mock<HasMediaPermissionUseCase>()

    private val timelinePreferencesMapper = mock<TimelinePreferencesMapper>()

    private val broadcastBusinessAccountExpiredUseCase =
        mock<BroadcastBusinessAccountExpiredUseCase>()

    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()

    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()

    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase>()

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()

    private val monitorPaginatedTimelinePhotosUseCase =
        mock<MonitorPaginatedTimelinePhotosUseCase>()

    private val loadNextPageOfPhotosUseCase = mock<LoadNextPageOfPhotosUseCase>()

    private val accountLevelDetail = mock<AccountLevelDetail> {
        on { accountType }.thenReturn(AccountType.PRO_III)
    }
    private val accountDetail = mock<AccountDetail> {
        on { levelDetail }.thenReturn(accountLevelDetail)
    }

    @BeforeEach
    fun setUp() {
        getTimelinePhotosUseCase.stub {
            on { invoke() }.thenReturn(emptyFlow())
        }
        monitorCameraUploadsStatusInfoUseCase.stub {
            onBlocking { invoke() }.thenReturn(cameraUploadsStatusInfoFlow)
        }
        monitorShowHiddenItemsUseCase.stub {
            onBlocking { invoke() }.thenReturn(flowOf(false))
        }
        monitorAccountDetailUseCase.stub {
            onBlocking { invoke() }.thenReturn(flowOf(accountDetail))
        }
        isHiddenNodesOnboardedUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
        }
        reset(
            enableCameraUploadsInPhotosUseCase
        )
        // Note: Feature flags and ViewModel initialization are now handled in each individual test
    }

    @AfterEach
    fun tearDownEach() {
        reset(startCameraUploadUseCase, getFeatureFlagValueUseCase)
    }

    fun initViewModel() {
        underTest = TimelineViewModel(
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            getTimelinePhotosUseCase = getTimelinePhotosUseCase,
            getCameraUploadPhotos = filterCameraUploadPhotos,
            getCloudDrivePhotos = filterCloudDrivePhotos,
            setInitialCUPreferences = setInitialCUPreferences,
            enableCameraUploadsInPhotosUseCase = enableCameraUploadsInPhotosUseCase,
            getNodeListByIds = getNodeListByIds,
            startCameraUploadUseCase = startCameraUploadUseCase,
            ioDispatcher = StandardTestDispatcher(),
            mainDispatcher = StandardTestDispatcher(),
            defaultDispatcher = UnconfinedTestDispatcher(),
            checkEnableCameraUploadsStatusUseCase = checkEnableCameraUploadsStatusUseCase,
            monitorCameraUploadsStatusInfoUseCase = monitorCameraUploadsStatusInfoUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getTimelineFilterPreferencesUseCase = getTimelineFilterPreferencesUseCase,
            setTimelineFilterPreferencesUseCase = setTimelineFilterPreferencesUseCase,
            hasMediaPermissionUseCase = hasMediaPermissionUseCase,
            timelinePreferencesMapper = timelinePreferencesMapper,
            broadcastBusinessAccountExpiredUseCase = broadcastBusinessAccountExpiredUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            monitorPaginatedTimelinePhotosUseCase = monitorPaginatedTimelinePhotosUseCase,
            loadNextPageOfPhotosUseCase = loadNextPageOfPhotosUseCase,
        )
    }

    private fun initViewModelWithDefaultFlags() {
        // Set up default feature flags (legacy behavior)
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.UIDrivenPhotoMonitoring) }.thenReturn(false)
        }
        initViewModel()
    }

    private fun setupUIDrivenPhotoMonitoring(enabled: Boolean) {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.UIDrivenPhotoMonitoring) }.thenReturn(enabled)
        }
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf()))
    }

    private fun setupUIDrivenPhotoMonitoringWithMockPhotos(enabled: Boolean) {
        val mockPhoto =
            mock<Photo.Image> { on { modificationTime }.thenReturn(LocalDateTime.now()) }
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.UIDrivenPhotoMonitoring) }.thenReturn(enabled)
        }
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf(mockPhoto)))
    }

    @Test
    fun `test initial state`() = runTest {
        initViewModelWithDefaultFlags()

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
                .containsExactlyElementsIn(TimeBarTab.entries.toTypedArray())
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
            assertWithMessage("enableFilterOption value is incorrect").that(initialState.enableFilterOption)
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
            assertWithMessage("shouldTriggerCameraUploads value is incorrect").that(initialState.shouldTriggerCameraUploads)
                .isFalse()
            assertWithMessage("shouldShowBusinessAccountPrompt value is incorrect").that(
                initialState.shouldShowBusinessAccountPrompt
            ).isFalse()
            assertWithMessage("shouldTriggerMediaPermissionsDeniedLogic value is incorrect").that(
                initialState.shouldTriggerMediaPermissionsDeniedLogic
            ).isFalse()
        }
    }

    @Test
    fun `test that a single photo returned is returned by the state`() = runTest {
        val expectedDate = LocalDateTime.now()
        val photo = mock<Photo.Image> { on { modificationTime }.thenReturn(expectedDate) }
        whenever(getTimelineFilterPreferencesUseCase()).thenReturn(null)
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf(photo)))

        // Set up default feature flags
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.UIDrivenPhotoMonitoring) }.thenReturn(false)
        }

        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
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
     * Mocks the value of [checkEnableCameraUploadsStatusUseCase] and calls the ViewModel method
     *
     * @param status The [EnableCameraUploadsStatus] to mock the Use Case
     */
    private suspend fun handleEnableCameraUploads(status: EnableCameraUploadsStatus) {
        whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(status)
        underTest.handleEnableCameraUploads()
    }

    @Test
    fun `test that shouldShowBusinessAccountPrompt is true when checkEnableCameraUploadsStatusUseCase returns SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            initViewModelWithDefaultFlags()

            handleEnableCameraUploads(status = EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT)
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldShowBusinessAccountPrompt).isTrue()
            }
        }

    @ParameterizedTest(name = "camera uploads status: {0}")
    @EnumSource(
        value = EnableCameraUploadsStatus::class,
        names = ["SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT", "SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `test that broadcastBusinessAccountExpiredUseCase is invoked when checkEnableCameraUploadsStatusUseCase returns specific camera uploads statuses`(
        cameraUploadsStatus: EnableCameraUploadsStatus,
    ) = runTest {
        handleEnableCameraUploads(status = cameraUploadsStatus)
        advanceUntilIdle()

        // Since the ViewModel is not reset on every test, use atLeast(1) just to confirm that
        // the Use Case has been triggered with a specific camera uploads status
        verify(broadcastBusinessAccountExpiredUseCase, atLeast(1)).invoke()
    }

    @Test
    fun `test that shouldTriggerCameraUploads is true when checkEnableCameraUploadsStatusUseCase returns CAN_ENABLE_CAMERA_UPLOADS`() =
        runTest {
            initViewModelWithDefaultFlags()

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
            initViewModelWithDefaultFlags()
            advanceUntilIdle() // Wait for init to complete

            val totalUploaded = 100
            val totalToUpload = 200
            val expectedProgress = 0.5f
            val expectedPending = totalToUpload - totalUploaded
            val cameraUploadsStatusInfo = mock<CameraUploadsStatusInfo.UploadProgress> {
                on { this.totalUploaded }.thenReturn(totalUploaded)
                on { this.totalToUpload }.thenReturn(totalToUpload)
                on { this.progress }.thenReturn(Progress(expectedProgress))
            }
            cameraUploadsStatusInfoFlow.emit(cameraUploadsStatusInfo)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.pending).isEqualTo(expectedPending)
                assertThat(state.progressBarShowing).isEqualTo(true)
                assertThat(state.progress).isEqualTo(expectedProgress)
            }
        }

    @Test
    fun `test that when camera upload progress is received with status finished, then progressBarShowing state is set to false`() =
        runTest {
            initViewModelWithDefaultFlags()
            val cameraUploadsStatusInfo =
                CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.COMPLETED)
            cameraUploadsStatusInfoFlow.emit(cameraUploadsStatusInfo)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.progressBarShowing).isEqualTo(false)
            }
        }

    @ParameterizedTest(name = "and isCUPausedWarningBannerEnabled is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the camera uploads toolbar menu icon is shown as expected when camera uploads finishes because the device is not charged`(
        isCUPausedWarningBannerEnabled: Boolean,
    ) =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.CameraUploadsPausedWarningBanner))
                .thenReturn(isCUPausedWarningBannerEnabled)
            initViewModelWithDefaultFlags()
            advanceUntilIdle() // Wait for init to complete

            val cameraUploadsStatusInfo =
                CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET)
            cameraUploadsStatusInfoFlow.emit(cameraUploadsStatusInfo)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.showCameraUploadsPaused).isEqualTo(!isCUPausedWarningBannerEnabled)
                assertThat(state.showCameraUploadsWarning).isEqualTo(isCUPausedWarningBannerEnabled)
                assertThat(state.showCameraUploadsComplete).isFalse()
                assertThat(state.isWarningBannerShown).isTrue()
            }
        }

    @Test
    fun `test that the fab icon is hidden when camera uploads finishes because the device is not charged`() =
        runTest {
            initViewModelWithDefaultFlags()
            val cameraUploadsStatusInfo =
                CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET)
            cameraUploadsStatusInfoFlow.emit(cameraUploadsStatusInfo)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.None)
            }
        }

    @ParameterizedTest(name = "and that specific reason is {0}")
    @EnumSource(
        value = CameraUploadsFinishedReason::class,
        names = ["UNKNOWN", "COMPLETED", "DEVICE_CHARGING_REQUIREMENT_NOT_MET"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that no toolbar menu icon is shown and a warning fab icon is shown when camera uploads finishes for any other reason`(
        cameraUploadsFinishedReason: CameraUploadsFinishedReason,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.CameraUploadsPausedWarningBanner))
            .thenReturn(true)
        initViewModelWithDefaultFlags()
        // Wait for init block to complete (monitoring starts automatically when feature flag is disabled)
        advanceUntilIdle()

        val cameraUploadsStatusInfo = CameraUploadsStatusInfo.Finished(cameraUploadsFinishedReason)
        cameraUploadsStatusInfoFlow.emit(cameraUploadsStatusInfo)

        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showCameraUploadsPaused).isFalse()
            assertThat(state.showCameraUploadsComplete).isFalse()
            assertThat(state.showCameraUploadsWarning).isEqualTo(
                when (cameraUploadsFinishedReason) {
                    CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW,
                    CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET,
                    CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET,
                        -> true

                    else -> false
                }
            )

            assertThat(state.isWarningBannerShown).isEqualTo(
                when (cameraUploadsFinishedReason) {
                    CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW,
                    CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET,
                    CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET,
                    CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA,
                        -> true

                    else -> false
                }
            )

            when (cameraUploadsFinishedReason) {
                CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW,
                CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA,
                    -> {
                    assertThat(state.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.None)
                    assertThat(state.cameraUploadsProgress).isEqualTo(0.0f)
                }

                else -> {
                    assertThat(state.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.Warning)
                    assertThat(state.cameraUploadsProgress).isEqualTo(0.5f)
                }
            }
        }
    }

    @Test
    fun `test that when camera upload progress is received with pending 0, then progressBarShowing state is set to false`() =
        runTest {
            initViewModelWithDefaultFlags()
            val totalUploaded = 200
            val cameraUploadsStatusInfo = CameraUploadsStatusInfo.UploadProgress(
                totalUploaded = totalUploaded,
                totalToUpload = totalUploaded,
                totalUploadedBytes = 2000,
                totalUploadBytes = 2000,
                progress = Progress(1f),
                areUploadsPaused = false,
            )
            cameraUploadsStatusInfoFlow.emit(cameraUploadsStatusInfo)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.progressBarShowing).isEqualTo(false)
            }
        }

    @Test
    fun `test that when camera upload progress is received and some items are currently selected, the progressBarShowing state is set to false`() =
        runTest {
            initViewModelWithDefaultFlags()
            val progress = mock<CameraUploadsStatusInfo.UploadProgress>()

            val selectedPhoto = mock<Photo.Image> { on { id }.thenReturn(1L) }
            underTest.setSelectedPhotos(listOf(mock<PhotoListItem.PhotoGridItem> {
                on { photo }.thenReturn(
                    selectedPhoto
                )
            }))
            cameraUploadsStatusInfoFlow.emit(progress)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.progressBarShowing).isEqualTo(false)
            }
        }

    @Test
    fun `test that when camera upload progress is received and current view is not TimeBar ALL, the progressBarShowing state is set to false`() =
        runTest {
            initViewModelWithDefaultFlags()
            val progress = mock<CameraUploadsStatusInfo.UploadProgress>()

            underTest.onTimeBarTabSelected(TimeBarTab.Years)
            cameraUploadsStatusInfoFlow.emit(progress)

            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.progressBarShowing).isEqualTo(false)
            }
        }

    @Test
    fun `test that when stopCameraUploadAndHeartbeat is called, stopCameraUploadAndHeartbeatUseCase is called`() =
        runTest {
            initViewModelWithDefaultFlags()

            underTest.stopCameraUploads()

            advanceUntilIdle()

            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.StopAndDisable)
        }

    @Test
    fun `test that when enableCU is called, enableCameraUploadsInPhotosUseCase is called`() =
        runTest {
            initViewModelWithDefaultFlags()

            underTest.enableCU()
            advanceUntilIdle()
            verify(enableCameraUploadsInPhotosUseCase).invoke(
                videoCompressionSizeLimit = any(),
                videoUploadQuality = any(),
                includeVideos = any(),
                wifiOnly = any(),
            )
        }

    @Test
    fun `test that when enableCU is called, startCameraUploadUseCase is called`() = runTest {
        initViewModelWithDefaultFlags()

        underTest.enableCU()
        advanceUntilIdle()
        verify(startCameraUploadUseCase).invoke()
    }

    @Test
    fun `test that first time sync CU call start process`() = runTest {
        initViewModelWithDefaultFlags()

        // when
        underTest.syncCameraUploadsStatus()
        advanceUntilIdle()

        // then
        verify(startCameraUploadUseCase).invoke()
    }

    @Test
    fun `test that CU status check files for upload is handled properly`() = runTest {
        initViewModelWithDefaultFlags()
        advanceUntilIdle() // Wait for init to complete

        // given
        cameraUploadsStatusInfoFlow.emit(CameraUploadsStatusInfo.CheckFilesForUpload)
        advanceUntilIdle()

        // then
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.Sync)
        }
    }

    @Test
    fun `test that CU status upload progress is handled properly`() = runTest {
        initViewModelWithDefaultFlags()
        advanceUntilIdle() // Wait for init to complete

        // given
        val progress = CameraUploadsStatusInfo.UploadProgress(
            totalToUpload = 0,
            totalUploaded = 1,
            totalUploadedBytes = 1L,
            progress = Progress(1f),
            totalUploadBytes = 1L,
            areUploadsPaused = false,
        )
        cameraUploadsStatusInfoFlow.emit(progress)
        advanceUntilIdle()

        // then
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.Uploading)
        }
    }

    @Test
    fun `test that CU status finished is handled properly`() = runTest {
        initViewModelWithDefaultFlags()
        advanceUntilIdle() // Wait for init to complete

        // given
        val info = CameraUploadsStatusInfo.Finished(
            reason = CameraUploadsFinishedReason.COMPLETED,
        )
        cameraUploadsStatusInfoFlow.emit(info)
        advanceUntilIdle()

        // then
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showCameraUploadsComplete).isTrue()
        }
    }

    @Test
    fun `test that CU completed message is set properly`() = runTest {
        initViewModelWithDefaultFlags()
        // when
        underTest.setCameraUploadsCompletedMessage(true)

        // then
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showCameraUploadsCompletedMessage).isTrue()
        }
    }

    @Test
    fun `test that if there is no preference set yet the saved timeline state is false`() =
        runTest {
            whenever(getTimelineFilterPreferencesUseCase()).thenReturn(null)
            initViewModel()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.rememberFilter).isFalse()
            }
        }

    @Test
    fun `test that if there is preference set then it is saved in the state`() = runTest {
        val expectedRememberPref = true
        val expectedLocation = TimelinePhotosSource.CLOUD_DRIVE

        val expectedMediaType = FilterMediaType.IMAGES
        val latestPref = mapOf(
            Pair(
                TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value,
                RememberPreferences(expectedRememberPref)
            ),
            Pair(
                TimelinePreferencesJSON.JSON_KEY_LOCATION.value,
                LocationPreference(expectedLocation)
            ),
            Pair(
                TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value,
                MediaTypePreference(expectedMediaType)
            ),
        )

        val expectedDate = LocalDateTime.now()
        val photo = mock<Photo.Image> { on { modificationTime }.thenReturn(expectedDate) }
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf(photo)))

        whenever(getTimelineFilterPreferencesUseCase()).thenReturn(mapOf())

        whenever(timelinePreferencesMapper(any())).thenReturn(latestPref)

        // Set up default feature flags
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.UIDrivenPhotoMonitoring) }.thenReturn(false)
        }

        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.rememberFilter).isTrue()
            assertThat(state.currentMediaSource).isEqualTo(expectedLocation)
            assertThat(state.currentFilterMediaType).isEqualTo(expectedMediaType)

            assertWithMessage("Expected photos do not match").that(state.photos)
                .containsExactly(photo)
            assertWithMessage("Expected photosListItems do not match").that(state.photosListItems)
                .containsExactlyElementsIn(
                    listOf(
                        PhotoListItem.Separator(expectedDate),
                        PhotoListItem.PhotoGridItem(photo, false)
                    )
                )
            val hasPhoto =
                Correspondence.transforming<DateCard, Photo>({ it?.photo }, "contains photo")

            assertWithMessage("Day card photos do not match").that(state.daysCardPhotos)
                .comparingElementsUsing(hasPhoto)
                .contains(photo)

            assertWithMessage("Month card photos do not match").that(state.monthsCardPhotos)
                .comparingElementsUsing(hasPhoto)
                .contains(photo)

            assertWithMessage("Year card photos do not match").that(state.yearsCardPhotos)
                .comparingElementsUsing(hasPhoto)
                .contains(photo)

            assertWithMessage("Loading is not complete").that(state.loadPhotosDone)
                .isTrue()
        }
    }

    @ParameterizedTest(name = "when isCameraUploadsTransferScreenEnabled is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isCameraUploadsTransferScreenEnabled is updated as expected`(
        isEnabled: Boolean,
    ) = runTest {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.UIDrivenPhotoMonitoring) }.thenReturn(false)
        }
        whenever(getFeatureFlagValueUseCase(AppFeatures.CameraUploadsTransferScreen))
            .thenReturn(isEnabled)

        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isCameraUploadsTransferScreenEnabled).isEqualTo(isEnabled)
        }
    }

    @ParameterizedTest(name = "when isCUPausedWarningBannerEnabled is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isCUPausedWarningBannerEnabled is updated as expected`(
        isEnabled: Boolean,
    ) = runTest {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.UIDrivenPhotoMonitoring) }.thenReturn(false)
        }
        whenever(getFeatureFlagValueUseCase(AppFeatures.CameraUploadsPausedWarningBanner))
            .thenReturn(isEnabled)

        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isCUPausedWarningBannerEnabled).isEqualTo(isEnabled)
        }
    }

    @Test
    fun `test that photo monitoring does not start automatically in init when UI-driven lifecycle is enabled`() =
        runTest {
            // Given: UI-driven lifecycle is enabled
            // Reset the use case to clear any previous setup from setUp()
            reset(getTimelinePhotosUseCase, monitorPaginatedTimelinePhotosUseCase)
            setupUIDrivenPhotoMonitoringWithMockPhotos(enabled = true)
            initViewModel()
            advanceUntilIdle()

            // Then: Verify that monitoring use cases are NOT called in init when UI-driven lifecycle is enabled
            verifyNoInteractions(getTimelinePhotosUseCase)
            verifyNoInteractions(monitorPaginatedTimelinePhotosUseCase)

            underTest.state.test {
                val initialState = awaitItem()
                assertWithMessage("Photos should not be loaded automatically in init when UI-driven lifecycle is enabled")
                    .that(initialState.photos).isEmpty()
                assertWithMessage("loadPhotosDone should be false initially")
                    .that(initialState.loadPhotosDone).isFalse()
            }
        }

    @Test
    fun `test that photo monitoring starts automatically in init when UI-driven lifecycle is disabled`() =
        runTest {
            // Given: UI-driven lifecycle is disabled (legacy behavior)
            setupUIDrivenPhotoMonitoringWithMockPhotos(enabled = false)
            initViewModel()
            advanceUntilIdle()

            // Then: Verify that monitoring use case IS called in init when UI-driven lifecycle is disabled
            verify(getTimelinePhotosUseCase, atLeast(1)).invoke()
            verifyNoInteractions(monitorPaginatedTimelinePhotosUseCase)

            underTest.state.test {
                val state = awaitItem()
                assertWithMessage("Photos should be loaded automatically when UI-driven lifecycle is disabled")
                    .that(state.photos).isNotEmpty()
                assertWithMessage("State should be properly initialized in legacy mode")
                    .that(state).isNotNull()
            }
        }

    @Test
    fun `test that startPhotoMonitoring starts photo monitoring when UI-driven lifecycle is enabled`() =
        runTest {
            // Given: UI-driven lifecycle is enabled
            setupUIDrivenPhotoMonitoring(enabled = true)
            initViewModel()

            // When: startPhotoMonitoring is called and camera uploads status is emitted
            underTest.startPhotoMonitoring()
            advanceUntilIdle()

            val cameraUploadsStatusInfo = CameraUploadsStatusInfo.CheckFilesForUpload
            cameraUploadsStatusInfoFlow.emit(cameraUploadsStatusInfo)
            advanceUntilIdle()

            // Then: Verify that monitoring use case IS called when startPhotoMonitoring is invoked
            verify(getTimelinePhotosUseCase, atLeast(1)).invoke()
            verifyNoInteractions(monitorPaginatedTimelinePhotosUseCase)

            underTest.state.test {
                val state = awaitItem()
                assertWithMessage("Camera uploads status should be processed after startPhotoMonitoring when UI-driven lifecycle is enabled")
                    .that(state.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.Sync)
            }
        }

    @Test
    fun `test that startPhotoMonitoring does nothing when UI-driven lifecycle is disabled`() =
        runTest {
            // Given: UI-driven lifecycle is disabled
            setupUIDrivenPhotoMonitoring(enabled = false)
            initViewModel()
            advanceUntilIdle() // Wait for init to complete

            // When: startPhotoMonitoring is called
            underTest.startPhotoMonitoring()

            // Then: Verify that monitoring use cases are called in init but NOT called again by startPhotoMonitoring in disabled mode
            verify(getTimelinePhotosUseCase, atLeast(1)).invoke() // Called in init
            verifyNoInteractions(monitorPaginatedTimelinePhotosUseCase)

            underTest.state.test {
                val state = awaitItem()
                assertWithMessage("Photos should remain empty when UI-driven lifecycle is disabled")
                    .that(state.photos).isEmpty()
                assertWithMessage("Camera uploads status should remain None when UI-driven lifecycle is disabled")
                    .that(state.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.None)
            }
        }

    @Test
    fun `test that startPhotoMonitoring prevents duplicate calls when UI-driven lifecycle is enabled`() =
        runTest {
            // Given: UI-driven lifecycle is enabled
            setupUIDrivenPhotoMonitoring(enabled = true)

            // Reset mocks to clear any previous calls from setUp()
            reset(monitorPaginatedTimelinePhotosUseCase, getTimelinePhotosUseCase)

            // Mock the use cases to return empty flows
            whenever(monitorPaginatedTimelinePhotosUseCase()).thenReturn(flowOf(listOf()))
            whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf()))

            // Create a fresh ViewModel with the correct flag values
            initViewModel()
            advanceUntilIdle() // Wait for init to complete

            // When: startPhotoMonitoring is called multiple times
            underTest.startPhotoMonitoring()
            underTest.startPhotoMonitoring()
            underTest.startPhotoMonitoring()
            advanceUntilIdle()

            // Then: Only one monitoring method should be called (duplicates prevented)
            // Since TimelinePhotosPagination is not set, it defaults to false, so getTimelinePhotosUseCase is called
            verify(getTimelinePhotosUseCase).invoke()
            verifyNoInteractions(monitorPaginatedTimelinePhotosUseCase)
        }

    @Test
    fun `test that startPhotoMonitoring allows multiple calls when UI-driven lifecycle is disabled`() =
        runTest {
            // Given: UI-driven lifecycle is disabled
            setupUIDrivenPhotoMonitoring(enabled = false)
            initViewModel()
            advanceUntilIdle() // Wait for init to complete

            // When: startPhotoMonitoring is called multiple times
            underTest.startPhotoMonitoring()
            underTest.startPhotoMonitoring()
            underTest.startPhotoMonitoring()

            // Then: Verify that monitoring use cases are called in init but NOT called again by multiple startPhotoMonitoring calls in disabled mode
            verify(getTimelinePhotosUseCase, atLeast(1)).invoke() // Called in init
            verifyNoInteractions(monitorPaginatedTimelinePhotosUseCase)

            underTest.state.test {
                val state = awaitItem()
                assertWithMessage("Photos should remain empty after multiple calls when UI-driven lifecycle is disabled")
                    .that(state.photos).isEmpty()
                assertWithMessage("Camera uploads status should remain None after multiple calls when UI-driven lifecycle is disabled")
                    .that(state.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.None)
            }
        }

    @Test
    fun `test that feature flag exception defaults to enabled behavior`() = runTest {
        // Given: Feature flag throws exception (simulating network/API error)
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.UIDrivenPhotoMonitoring) }.thenThrow(RuntimeException("Network error"))
        }
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf()))
        initViewModel()
        advanceUntilIdle() // Wait for init to complete

        // When: startPhotoMonitoring is called
        underTest.startPhotoMonitoring()

        // Then: Verify that monitoring use case IS called when feature flag throws exception (defaults to enabled)
        verify(getTimelinePhotosUseCase, atLeast(1)).invoke()
        verifyNoInteractions(monitorPaginatedTimelinePhotosUseCase)

        underTest.state.test {
            val state = awaitItem()
            assertWithMessage("State should be accessible when feature flag throws exception (defaults to enabled)")
                .that(state).isNotNull()
        }
    }

    @Test
    fun `test that feature flag null result defaults to enabled behavior`() = runTest {
        // Given: Feature flag returns null (simulating missing config)
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.UIDrivenPhotoMonitoring) }.thenReturn(null)
        }
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf()))
        initViewModel()
        advanceUntilIdle() // Wait for init to complete

        // When: startPhotoMonitoring is called
        underTest.startPhotoMonitoring()

        // Then: Verify that monitoring use case IS called when feature flag returns null (defaults to enabled)
        verify(getTimelinePhotosUseCase, atLeast(1)).invoke()
        verifyNoInteractions(monitorPaginatedTimelinePhotosUseCase)

        underTest.state.test {
            val state = awaitItem()
            assertWithMessage("State should be accessible when feature flag returns null (defaults to enabled)")
                .that(state).isNotNull()
        }
    }

    @Test
    fun `test that popBackFromCameraUploadsTransferScreenEvent is updated correctly`() = runTest {
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().popBackFromCameraUploadsTransferScreenEvent).isEqualTo(consumed)

            underTest.updatePopBackFromCameraUploadsTransferScreenEvent(triggered)
            assertThat(awaitItem().popBackFromCameraUploadsTransferScreenEvent).isEqualTo(triggered)

            underTest.updatePopBackFromCameraUploadsTransferScreenEvent(consumed)
            assertThat(awaitItem().popBackFromCameraUploadsTransferScreenEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that isWarningBannerShown is updated correctly`() = runTest {
        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().isWarningBannerShown).isFalse()

            underTest.updateIsWarningBannerShown(true)
            assertThat(awaitItem().isWarningBannerShown).isTrue()

            underTest.updateIsWarningBannerShown(false)
            assertThat(awaitItem().isWarningBannerShown).isFalse()
        }
    }

    @ParameterizedTest(name = "and finishReason is {0}")
    @EnumSource(
        value = CameraUploadsFinishedReason::class,
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that shouldShowWarningMenu returns true when isCUPausedWarningBannerEnabled is true`(
        finishReason: CameraUploadsFinishedReason,
    ) = runTest {
        initViewModel()
        val isWarningShown = underTest.shouldShowWarningMenu(
            finishReason = finishReason,
            isWarningBannerEnabled = true
        )

        if (
            finishReason in setOf(
                CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW,
                CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
            )
        ) {
            assertThat(isWarningShown).isTrue()
        } else {
            assertThat(isWarningShown).isFalse()
        }
    }

    @ParameterizedTest(name = "and finishReason is {0}")
    @EnumSource(
        value = CameraUploadsFinishedReason::class,
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that shouldShowWarningBanner returns true when isCUPausedWarningBannerEnabled is true`(
        finishReason: CameraUploadsFinishedReason,
    ) = runTest {
        initViewModel()
        val isWarningShown = underTest.shouldShowWarningBanner(
            finishReason = finishReason,
            isWarningBannerEnabled = true
        )

        if (
            finishReason in setOf(
                CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW,
                CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET,
                CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA
            )
        ) {
            assertThat(isWarningShown).isTrue()
        } else {
            assertThat(isWarningShown).isFalse()
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
