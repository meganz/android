package mega.privacy.android.feature.photos.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.permisison.HasCameraUploadsPermissionUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.photos.MonitorCameraUploadShownUseCase
import mega.privacy.android.domain.usecase.photos.MonitorEnableCameraUploadBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.photos.ResetEnableCameraUploadBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.photos.SetCameraUploadShownUseCase
import mega.privacy.android.domain.usecase.photos.SetEnableCameraUploadBannerDismissedTimestampUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import mega.privacy.android.feature.photos.model.CameraUploadsStatus
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaCameraUploadViewModelTest {

    private lateinit var underTest: MediaCameraUploadViewModel

    private val monitorCameraUploadsStatusInfoUseCase: MonitorCameraUploadsStatusInfoUseCase =
        mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val startCameraUploadUseCase: StartCameraUploadUseCase = mock()
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase = mock()
    private val setInitialCUPreferences: SetInitialCUPreferences = mock()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase = mock()
    private val monitorCameraUploadShownUseCase: MonitorCameraUploadShownUseCase = mock()
    private val setCameraUploadShownUseCase: SetCameraUploadShownUseCase = mock()
    private val monitorEnableCameraUploadBannerVisibilityUseCase: MonitorEnableCameraUploadBannerVisibilityUseCase =
        mock()
    private val resetEnableCameraUploadBannerVisibilityUseCase: ResetEnableCameraUploadBannerVisibilityUseCase =
        mock()
    private val setEnableCameraUploadBannerDismissedTimestampUseCase: SetEnableCameraUploadBannerDismissedTimestampUseCase =
        mock()
    private val hasCameraUploadsPermissionUseCase: HasCameraUploadsPermissionUseCase = mock()

    private var cameraUploadsStatusFlow =
        MutableStateFlow<CameraUploadsStatusInfo>(CameraUploadsStatusInfo.Unknown)
    private var cameraUploadShownFlow = MutableStateFlow(false)

    @BeforeEach
    fun setup() = runTest {
        cameraUploadsStatusFlow = MutableStateFlow(CameraUploadsStatusInfo.Unknown)
        whenever(monitorCameraUploadsStatusInfoUseCase()) doReturn cameraUploadsStatusFlow
        cameraUploadShownFlow = MutableStateFlow(false)
        whenever(
            monitorCameraUploadShownUseCase.cameraUploadShownFlow
        ) doReturn cameraUploadShownFlow
        whenever(getFeatureFlagValueUseCase(any())) doReturn true
        whenever(hasMediaPermissionUseCase()) doReturn true
        whenever(isCameraUploadsEnabledUseCase()) doReturn true

        underTest = MediaCameraUploadViewModel(
            monitorCameraUploadsStatusInfoUseCase = monitorCameraUploadsStatusInfoUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            startCameraUploadUseCase = startCameraUploadUseCase,
            hasMediaPermissionUseCase = hasMediaPermissionUseCase,
            setInitialCUPreferences = setInitialCUPreferences,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            monitorCameraUploadShownUseCase = monitorCameraUploadShownUseCase,
            setCameraUploadShownUseCase = setCameraUploadShownUseCase,
            monitorEnableCameraUploadBannerVisibilityUseCase = monitorEnableCameraUploadBannerVisibilityUseCase,
            resetEnableCameraUploadBannerVisibilityUseCase = resetEnableCameraUploadBannerVisibilityUseCase,
            setEnableCameraUploadBannerDismissedTimestampUseCase = setEnableCameraUploadBannerDismissedTimestampUseCase,
            hasCameraUploadsPermissionUseCase = hasCameraUploadsPermissionUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorCameraUploadsStatusInfoUseCase,
            getFeatureFlagValueUseCase,
            startCameraUploadUseCase,
            hasMediaPermissionUseCase,
            setInitialCUPreferences,
            isCameraUploadsEnabledUseCase,
            stopCameraUploadsUseCase,
            monitorCameraUploadShownUseCase,
            setCameraUploadShownUseCase,
            monitorEnableCameraUploadBannerVisibilityUseCase,
            resetEnableCameraUploadBannerVisibilityUseCase,
            setEnableCameraUploadBannerDismissedTimestampUseCase,
            hasCameraUploadsPermissionUseCase
        )
    }

    @Test
    fun `test that the monitoring starts and feature flags are checked when uiState is collected`() =
        runTest {
            underTest.uiState.test { cancelAndConsumeRemainingEvents() }
            verify(monitorCameraUploadsStatusInfoUseCase).invoke()
            verify(monitorCameraUploadShownUseCase).cameraUploadShownFlow
            verify(startCameraUploadUseCase).invoke()
            verify(getFeatureFlagValueUseCase).invoke(AppFeatures.CameraUploadsTransferScreen)
            verify(getFeatureFlagValueUseCase).invoke(AppFeatures.CameraUploadsPausedWarningBanner)
        }

    @Test
    fun `test that setInitialPreferences and setCameraUploadShown are called when cameraUploadShownFlow emits true`() =
        runTest {
            cameraUploadShownFlow.value = true

            underTest.uiState.test { cancelAndConsumeRemainingEvents() }
            verify(setInitialCUPreferences).invoke()
            verify(setCameraUploadShownUseCase).invoke()
        }

    @Test
    fun `test that the UI state shows Sync FAB when camera upload status is CheckFilesForUpload`() =
        runTest {
            cameraUploadsStatusFlow.value = CameraUploadsStatusInfo.CheckFilesForUpload
            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.Sync)
                assertThat(item.showCameraUploadsPaused).isFalse()
                assertThat(item.showCameraUploadsComplete).isFalse()
                assertThat(item.showCameraUploadsWarning).isFalse()
            }
        }

    @Test
    fun `test that the UI state shows Uploading FAB with progress when status is UploadProgress`() =
        runTest {
            val progressInfo = CameraUploadsStatusInfo.UploadProgress(
                totalToUpload = 100,
                totalUploaded = 25,
                totalUploadedBytes = 0L,
                totalUploadBytes = 0L,
                progress = Progress(0.25F),
                areUploadsPaused = false
            )

            cameraUploadsStatusFlow.value = progressInfo

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.Uploading)
                assertThat(item.cameraUploadsProgress).isEqualTo(0.25f)
                assertThat(item.pending).isEqualTo(75)
                assertThat(item.cameraUploadsTotalUploaded).isEqualTo(100)
            }
        }

    @Test
    fun `test that UI shows complete fab then menu when status is Finished with COMPLETED`() =
        runTest {
            cameraUploadsStatusFlow.value = CameraUploadsStatusInfo.UploadProgress(
                totalUploaded = 10,
                totalToUpload = 5,
                totalUploadedBytes = 50,
                totalUploadBytes = 0L,
                progress = Progress(0.25F),
                areUploadsPaused = false
            )

            underTest.uiState.test {
                // Initial emission
                expectMostRecentItem()

                cameraUploadsStatusFlow.value = CameraUploadsStatusInfo.Finished(
                    reason = CameraUploadsFinishedReason.COMPLETED
                )

                val completeFabState = awaitItem()
                assertThat(completeFabState.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.Complete)
                assertThat(completeFabState.showCameraUploadsCompletedMessage).isTrue()
                val completeMenuState = awaitItem()
                assertThat(completeMenuState.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.None)
                assertThat(completeMenuState.showCameraUploadsCompletedMessage).isTrue()
            }
        }

    @Test
    fun `test that the warning banner is shown when status is Finished with network requirement`() =
        runTest {
            whenever(
                getFeatureFlagValueUseCase(
                    AppFeatures.CameraUploadsPausedWarningBanner
                )
            ) doReturn true

            cameraUploadsStatusFlow.value =
                CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET)

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.isWarningBannerShown).isTrue()
                assertThat(item.showCameraUploadsWarning).isTrue()
                assertThat(item.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.None)
            }
        }

    @Test
    fun `test that the limited access state is set when permissions are denied and handleCameraUploadsPermissionsResult called`() =
        runTest {
            whenever(hasMediaPermissionUseCase()) doReturn false

            underTest.handleCameraUploadsPermissionsResult()

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.isCameraUploadsLimitedAccess).isTrue()
                assertThat(item.isWarningBannerShown).isTrue()
            }
        }

    @Test
    fun `test that the camera upload is stopped successfully`() = runTest {
        underTest.stopCameraUploads()

        verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.StopAndDisable)
    }

    @Test
    fun `test that the enable button is hidden when CU is enabled and resetCUButtonAndProgress called`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()) doReturn true

            underTest.resetCUButtonAndProgress()

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.enableCameraUploadButtonShowing).isFalse()
                assertThat(item.enableCameraUploadPageShowing).isFalse()
            }
        }

    @Test
    fun `test that the enable button is shown when CU is disabled and resetCUButtonAndProgress called`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()) doReturn false

            underTest.resetCUButtonAndProgress()

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.enableCameraUploadButtonShowing).isTrue()
                assertThat(item.cameraUploadsStatus).isEqualTo(CameraUploadsStatus.None)
            }
        }

    @Test
    fun `test that CU page is not displayed when the photos are not empty`() = runTest {
        val photos = persistentListOf(
            PhotosNodeContentType.HeaderItem(
                time = LocalDateTime.now(),
                shouldShowGridSizeSettings = true
            )
        )
        whenever(isCameraUploadsEnabledUseCase()) doReturn false

        underTest.updateCUPageEnablementBasedOnDisplayedPhotos(photos)

        underTest.uiState.test {
            assertThat(expectMostRecentItem().enableCameraUploadPageShowing).isFalse()
        }
    }

    @Test
    fun `test that CU page is not displayed when the CU is enabled`() = runTest {
        whenever(isCameraUploadsEnabledUseCase()) doReturn true

        underTest.updateCUPageEnablementBasedOnDisplayedPhotos(persistentListOf())

        underTest.uiState.test {
            assertThat(expectMostRecentItem().enableCameraUploadPageShowing).isFalse()
        }
    }

    @Test
    fun `test that CU page is displayed when the photos are empty and CU is not enabled`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()) doReturn false

            underTest.updateCUPageEnablementBasedOnDisplayedPhotos(persistentListOf())

            underTest.uiState.test {
                assertThat(expectMostRecentItem().enableCameraUploadPageShowing).isTrue()
            }
        }

    @Test
    fun `test that enable CU banner is displayed when the visibility is visible and the CU is not enabled`() =
        runTest {
            whenever(
                monitorEnableCameraUploadBannerVisibilityUseCase.enableCameraUploadBannerVisibilityFlow
            ) doReturn flowOf(true)
            whenever(isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled) doReturn flowOf(
                false
            )

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldShowEnableCUBanner).isTrue()
            }
        }

    @Test
    fun `test that enable CU banner visibility is reset if visible`() =
        runTest {
            whenever(
                monitorEnableCameraUploadBannerVisibilityUseCase.enableCameraUploadBannerVisibilityFlow
            ) doReturn flowOf(true)
            whenever(isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled) doReturn flowOf(
                false
            )

            underTest.uiState.test { cancelAndConsumeRemainingEvents() }
            verify(resetEnableCameraUploadBannerVisibilityUseCase).invoke()
        }

    @Test
    fun `test that enable CU banner is not displayed when the visibility is visible but the CU is enabled`() =
        runTest {
            whenever(
                monitorEnableCameraUploadBannerVisibilityUseCase.enableCameraUploadBannerVisibilityFlow
            ) doReturn flowOf(true)
            whenever(isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled) doReturn flowOf(true)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldShowEnableCUBanner).isFalse()
            }
        }

    @Test
    fun `test that enable CU banner is not displayed when the CU is disabled but the visibility is not visible`() =
        runTest {
            whenever(
                monitorEnableCameraUploadBannerVisibilityUseCase.enableCameraUploadBannerVisibilityFlow
            ) doReturn flowOf(false)
            whenever(isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled) doReturn flowOf(
                false
            )

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldShowEnableCUBanner).isFalse()
            }
        }

    @Test
    fun `test that the enable CU banner is successfully dismissed`() = runTest {
        underTest.dismissEnableCUBanner()

        verify(setEnableCameraUploadBannerDismissedTimestampUseCase).invoke()
    }

    @Test
    fun `test that the CU access is successfully limited when CU permissions are not granted`() =
        runTest {
            whenever(hasCameraUploadsPermissionUseCase()) doReturn false

            underTest.checkCameraUploadsPermissions()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().isCameraUploadsLimitedAccess).isTrue()
            }
        }

    @Test
    fun `test that camera uploads warning menu is visible when CU permissions are not granted`() =
        runTest {
            whenever(hasCameraUploadsPermissionUseCase()) doReturn false
            cameraUploadsStatusFlow.emit(
                CameraUploadsStatusInfo.Finished(
                    reason = CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
                )
            )
            whenever(
                getFeatureFlagValueUseCase(
                    AppFeatures.CameraUploadsPausedWarningBanner
                )
            ) doReturn false

            underTest.uiState.test {
                underTest.checkCameraUploadsPermissions()

                assertThat(expectMostRecentItem().showCameraUploadsWarning).isTrue()
            }
        }

    @Test
    fun `test that camera uploads warning menu is visible when should show warning menu`() =
        runTest {
            whenever(hasCameraUploadsPermissionUseCase()) doReturn true
            cameraUploadsStatusFlow.emit(
                CameraUploadsStatusInfo.Finished(
                    reason = CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
                )
            )
            whenever(
                getFeatureFlagValueUseCase(
                    AppFeatures.CameraUploadsPausedWarningBanner
                )
            ) doReturn true

            underTest.checkCameraUploadsPermissions()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().showCameraUploadsWarning).isTrue()
            }
        }

    @Test
    fun `test that camera uploads warning banner is visible when CU permissions are not granted`() =
        runTest {
            whenever(hasCameraUploadsPermissionUseCase()) doReturn false
            cameraUploadsStatusFlow.emit(
                CameraUploadsStatusInfo.Finished(
                    reason = CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
                )
            )
            whenever(
                getFeatureFlagValueUseCase(
                    AppFeatures.CameraUploadsPausedWarningBanner
                )
            ) doReturn false

            underTest.uiState.test {
                underTest.checkCameraUploadsPermissions()

                assertThat(expectMostRecentItem().isWarningBannerShown).isTrue()
            }
        }

    @Test
    fun `test that camera uploads warning banner is visible when should show warning banner`() =
        runTest {
            whenever(hasCameraUploadsPermissionUseCase()) doReturn true
            cameraUploadsStatusFlow.emit(
                CameraUploadsStatusInfo.Finished(
                    reason = CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
                )
            )
            whenever(
                getFeatureFlagValueUseCase(
                    AppFeatures.CameraUploadsPausedWarningBanner
                )
            ) doReturn true

            underTest.checkCameraUploadsPermissions()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().isWarningBannerShown).isTrue()
            }
        }
}
