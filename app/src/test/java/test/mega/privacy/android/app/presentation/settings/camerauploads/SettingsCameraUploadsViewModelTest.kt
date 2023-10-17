package test.mega.privacy.android.app.presentation.settings.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateCameraUploadsBackupUseCase
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateMediaUploadsBackupUseCase
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsSettingsActionsUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetDefaultPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoSyncStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupDefaultSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.workers.RescheduleCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadAndHeartbeatUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [SettingsCameraUploadsViewModel]
 */
@ExperimentalCoroutinesApi
class SettingsCameraUploadsViewModelTest {

    private lateinit var underTest: SettingsCameraUploadsViewModel

    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val areLocationTagsEnabledUseCase = mock<AreLocationTagsEnabledUseCase>()
    private val areUploadFileNamesKeptUseCase = mock<AreUploadFileNamesKeptUseCase>()
    private val checkEnableCameraUploadsStatus = mock<CheckEnableCameraUploadsStatus>()
    private val clearCacheDirectory = mock<ClearCacheDirectory>()
    private val disableMediaUploadSettings = mock<DisableMediaUploadSettings>()
    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()
    private val getUploadOptionUseCase = mock<GetUploadOptionUseCase>()
    private val getUploadVideoQualityUseCase = mock<GetUploadVideoQualityUseCase>()
    private val getVideoCompressionSizeLimitUseCase = mock<GetVideoCompressionSizeLimitUseCase>()
    private val isCameraUploadsByWifiUseCase = mock<IsCameraUploadsByWifiUseCase>()
    private val isChargingRequiredForVideoCompressionUseCase =
        mock<IsChargingRequiredForVideoCompressionUseCase>()
    private val isPrimaryFolderPathValidUseCase = mock<IsPrimaryFolderPathValidUseCase>()
    private val preparePrimaryFolderPathUseCase = mock<PreparePrimaryFolderPathUseCase>()
    private val resetCameraUploadTimeStamps = mock<ResetCameraUploadTimeStamps>()
    private val resetMediaUploadTimeStamps = mock<ResetMediaUploadTimeStamps>()
    private val restorePrimaryTimestamps = mock<RestorePrimaryTimestamps>()
    private val restoreSecondaryTimestamps = mock<RestoreSecondaryTimestamps>()
    private val setCameraUploadsByWifiUseCase = mock<SetCameraUploadsByWifiUseCase>()
    private val setChargingRequiredForVideoCompressionUseCase =
        mock<SetChargingRequiredForVideoCompressionUseCase>()
    private val setDefaultPrimaryFolderPathUseCase = mock<SetDefaultPrimaryFolderPathUseCase>()
    private val setLocationTagsEnabledUseCase = mock<SetLocationTagsEnabledUseCase>()
    private val setPrimaryFolderPathUseCase = mock<SetPrimaryFolderPathUseCase>()
    private val setUploadFileNamesKeptUseCase = mock<SetUploadFileNamesKeptUseCase>()
    private val setUploadOptionUseCase = mock<SetUploadOptionUseCase>()
    private val setUploadVideoQualityUseCase = mock<SetUploadVideoQualityUseCase>()
    private val setUploadVideoSyncStatusUseCase = mock<SetUploadVideoSyncStatusUseCase>()
    private val setVideoCompressionSizeLimitUseCase = mock<SetVideoCompressionSizeLimitUseCase>()
    private val setupDefaultSecondaryFolderUseCase = mock<SetupDefaultSecondaryFolderUseCase>()
    private val setupPrimaryFolderUseCase = mock<SetupPrimaryFolderUseCase>()
    private val setupSecondaryFolderUseCase = mock<SetupSecondaryFolderUseCase>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val stopCameraUploadsUseCase = mock<StopCameraUploadsUseCase>()
    private val rescheduleCameraUploadUseCase = mock<RescheduleCameraUploadUseCase>()
    private val stopCameraUploadAndHeartbeatUseCase = mock<StopCameraUploadAndHeartbeatUseCase>()
    private val hasMediaPermissionUseCase = mock<HasMediaPermissionUseCase>()
    private val monitorCameraUploadsSettingsActionsUseCase =
        mock<MonitorCameraUploadsSettingsActionsUseCase>()
    private val setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase = mock()
    private val setupMediaUploadsSettingUseCase: SetupMediaUploadsSettingUseCase = mock()
    private val setupCameraUploadsSyncHandleUseCase: SetupCameraUploadsSyncHandleUseCase = mock()
    private val setupOrUpdateCameraUploadsBackupUseCase: SetupOrUpdateCameraUploadsBackupUseCase =
        mock()
    private val setupOrUpdateMediaUploadsBackupUseCase: SetupOrUpdateMediaUploadsBackupUseCase =
        mock()
    private val broadcastBusinessAccountExpiredUseCase =
        mock<BroadcastBusinessAccountExpiredUseCase>()
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val isSecondaryFolderEnabledUseCase: IsSecondaryFolderEnabled = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Initializes [SettingsCameraUploadsViewModel] for testing
     */
    private fun setupUnderTest(
        isPrimaryFolderPathValid: IsPrimaryFolderPathValidUseCase = isPrimaryFolderPathValidUseCase,
    ) {
        underTest = SettingsCameraUploadsViewModel(
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            areLocationTagsEnabledUseCase = areLocationTagsEnabledUseCase,
            areUploadFileNamesKeptUseCase = areUploadFileNamesKeptUseCase,
            checkEnableCameraUploadsStatus = checkEnableCameraUploadsStatus,
            clearCacheDirectory = clearCacheDirectory,
            disableMediaUploadSettings = disableMediaUploadSettings,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            getUploadOptionUseCase = getUploadOptionUseCase,
            getUploadVideoQualityUseCase = getUploadVideoQualityUseCase,
            getVideoCompressionSizeLimitUseCase = getVideoCompressionSizeLimitUseCase,
            isCameraUploadsByWifiUseCase = isCameraUploadsByWifiUseCase,
            isChargingRequiredForVideoCompressionUseCase = isChargingRequiredForVideoCompressionUseCase,
            isPrimaryFolderPathValidUseCase = isPrimaryFolderPathValid,
            monitorConnectivityUseCase = mock(),
            preparePrimaryFolderPathUseCase = preparePrimaryFolderPathUseCase,
            resetCameraUploadTimeStamps = resetCameraUploadTimeStamps,
            resetMediaUploadTimeStamps = resetMediaUploadTimeStamps,
            restorePrimaryTimestamps = restorePrimaryTimestamps,
            restoreSecondaryTimestamps = restoreSecondaryTimestamps,
            setCameraUploadsByWifiUseCase = setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase = setChargingRequiredForVideoCompressionUseCase,
            setDefaultPrimaryFolderPathUseCase = setDefaultPrimaryFolderPathUseCase,
            setLocationTagsEnabledUseCase = setLocationTagsEnabledUseCase,
            setPrimaryFolderPathUseCase = setPrimaryFolderPathUseCase,
            setUploadFileNamesKeptUseCase = setUploadFileNamesKeptUseCase,
            setUploadOptionUseCase = setUploadOptionUseCase,
            setUploadVideoQualityUseCase = setUploadVideoQualityUseCase,
            setUploadVideoSyncStatusUseCase = setUploadVideoSyncStatusUseCase,
            setVideoCompressionSizeLimitUseCase = setVideoCompressionSizeLimitUseCase,
            setupDefaultSecondaryFolderUseCase = setupDefaultSecondaryFolderUseCase,
            setupPrimaryFolderUseCase = setupPrimaryFolderUseCase,
            setupSecondaryFolderUseCase = setupSecondaryFolderUseCase,
            startCameraUploadUseCase = startCameraUploadUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            rescheduleCameraUploadUseCase = rescheduleCameraUploadUseCase,
            stopCameraUploadAndHeartbeatUseCase = stopCameraUploadAndHeartbeatUseCase,
            hasMediaPermissionUseCase = hasMediaPermissionUseCase,
            monitorCameraUploadsSettingsActionsUseCase = monitorCameraUploadsSettingsActionsUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            setupCameraUploadsSettingUseCase = setupCameraUploadsSettingUseCase,
            setupMediaUploadsSettingUseCase = setupMediaUploadsSettingUseCase,
            setupCameraUploadsSyncHandleUseCase = setupCameraUploadsSyncHandleUseCase,
            monitorBackupInfoTypeUseCase = mock(),
            setupOrUpdateCameraUploadsBackupUseCase = setupOrUpdateCameraUploadsBackupUseCase,
            setupOrUpdateMediaUploadsBackupUseCase = setupOrUpdateMediaUploadsBackupUseCase,
            broadcastBusinessAccountExpiredUseCase = broadcastBusinessAccountExpiredUseCase,
            monitorCameraUploadsFolderDestinationUseCase = mock(),
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            isSecondaryFolderEnabledUseCase = isSecondaryFolderEnabledUseCase,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        setupUnderTest()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.accessMediaLocationRationaleText).isNull()
            assertThat(state.areLocationTagsIncluded).isFalse()
            assertThat(state.areUploadFileNamesKept).isFalse()
            assertThat(state.isCameraUploadsEnabled).isFalse()
            assertThat(state.isChargingRequiredForVideoCompression).isFalse()
            assertThat(state.invalidFolderSelectedTextId).isNull()
            assertThat(state.primaryFolderPath).isEmpty()
            assertThat(state.shouldShowBusinessAccountPrompt).isFalse()
            assertThat(state.shouldTriggerCameraUploads).isFalse()
            assertThat(state.shouldShowMediaPermissionsRationale).isFalse()
            assertThat(state.shouldShowNotificationPermissionRationale).isFalse()
            assertThat(state.uploadConnectionType).isNull()
            assertThat(state.uploadOption).isNull()
            assertThat(state.videoCompressionSizeLimit).isEqualTo(0)
            assertThat(state.videoQuality).isNull()
            assertThat(state.shouldShowError).isFalse()
            assertThat(state.primaryUploadSyncHandle).isNull()
            assertThat(state.primaryFolderName).isEmpty()
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
            setupUnderTest()

            handleEnableCameraUploads(status = EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldShowBusinessAccountPrompt).isTrue()
            }
        }

    @Test
    fun `test that broadcastBusinessAccountExpiredUseCase is invoked when checkEnableCameraUploadsStatus returns SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            setupUnderTest()

            handleEnableCameraUploads(status = EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT)

            verify(broadcastBusinessAccountExpiredUseCase).invoke()
        }

    @Test
    fun `test that shouldTriggerCameraUploads is true when checkEnableCameraUploadsStatus returns CAN_ENABLE_CAMERA_UPLOADS`() =
        runTest {
            setupUnderTest()

            handleEnableCameraUploads(status = EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldTriggerCameraUploads).isTrue()
            }
        }

    @Test
    fun `test that shouldShowBusinessAccountPrompt is false when calling resetBusinessAccountPromptState`() =
        runTest {
            setupUnderTest()

            underTest.resetBusinessAccountPromptState()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldShowBusinessAccountPrompt).isFalse()
            }
        }

    @Test
    fun `test that isCameraUploadsEnabled is updated correctly`() = runTest {
        setupUnderTest()

        underTest.setCameraUploadsEnabled(true)

        underTest.state.map { it.isCameraUploadsEnabled }.distinctUntilChanged().test {
            assertThat(awaitItem()).isTrue()

            underTest.setCameraUploadsEnabled(false)
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that shouldTriggerCameraUploads is updated correctly`() = runTest {
        setupUnderTest()

        underTest.setTriggerCameraUploadsState(true)

        underTest.state.map { it.shouldTriggerCameraUploads }.distinctUntilChanged().test {
            assertThat(awaitItem()).isTrue()

            underTest.setTriggerCameraUploadsState(false)
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that shouldShowMediaPermissionsRationale is updated correctly`() = runTest {
        setupUnderTest()

        underTest.setMediaPermissionsRationaleState(true)

        underTest.state.map { it.shouldShowMediaPermissionsRationale }.distinctUntilChanged().test {
            assertThat(awaitItem()).isTrue()

            underTest.setMediaPermissionsRationaleState(false)
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that shouldShowNotificationPermissionRationale is updated correctly`() =
        runTest {
            setupUnderTest()

            underTest.setNotificationPermissionRationaleState(true)

            underTest.state.map { it.shouldShowNotificationPermissionRationale }
                .distinctUntilChanged().test {
                    assertThat(awaitItem()).isTrue()

                    underTest.setNotificationPermissionRationaleState(false)
                    assertThat(awaitItem()).isFalse()
                }
        }

    @Test
    fun `test that accessMediaLocationRationaleText is updated correctly`() = runTest {
        setupUnderTest()

        underTest.setAccessMediaLocationRationaleShown(true)

        underTest.state.map { it.accessMediaLocationRationaleText }.distinctUntilChanged().test {
            assertThat(awaitItem()).isEqualTo(R.string.on_refuse_storage_permission)

            underTest.setAccessMediaLocationRationaleShown(false)
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that invalidFolderSelectedTextId is updated correctly`() = runTest {
        setupUnderTest()

        underTest.setInvalidFolderSelectedPromptShown(true)

        underTest.state.map { it.invalidFolderSelectedTextId }.distinctUntilChanged().test {
            assertThat(awaitItem()).isEqualTo(R.string.error_invalid_folder_selected)

            underTest.setInvalidFolderSelectedPromptShown(false)
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that uploadConnectionType is updated correctly when calling changeUploadConnectionType`() =
        runTest {
            setupUnderTest()

            whenever(isCameraUploadsByWifiUseCase()).thenReturn(true)
            underTest.changeUploadConnectionType(wifiOnly = true)

            underTest.state.map { it.uploadConnectionType }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(UploadConnectionType.WIFI)

                whenever(isCameraUploadsByWifiUseCase()).thenReturn(false)
                underTest.changeUploadConnectionType(wifiOnly = false)
                assertThat(awaitItem()).isEqualTo(UploadConnectionType.WIFI_OR_MOBILE_DATA)
            }
        }

    @Test
    fun `test that the value of uploadOption is PHOTOS when calling changeUploadOption`() =
        testUploadOption(UploadOption.PHOTOS)

    @Test
    fun `test that the value of uploadOption is VIDEOS when calling changeUploadOption`() =
        testUploadOption(UploadOption.VIDEOS)

    @Test
    fun `test that the value of uploadOption is PHOTOS_AND_VIDEOS when calling changeUploadOption`() =
        testUploadOption(UploadOption.PHOTOS_AND_VIDEOS)

    private fun testUploadOption(uploadOption: UploadOption) = runTest {
        setupUnderTest()
        whenever(getUploadOptionUseCase()).thenReturn(uploadOption)

        underTest.changeUploadOption(uploadOption)

        verify(resetCameraUploadTimeStamps).invoke(clearCamSyncRecords = true)
        verify(clearCacheDirectory).invoke()
        verify(rescheduleCameraUploadUseCase).invoke()
        underTest.state.test {
            assertThat(awaitItem().uploadOption).isEqualTo(uploadOption)
        }
    }

    @Test
    fun `test that the value of videoQuality is LOW when calling changeUploadVideoQuality`() =
        testUploadVideoQuality(value = 0, expectedVideoQuality = VideoQuality.LOW)

    @Test
    fun `test that the value of videoQuality is MEDIUM when calling changeUploadVideoQuality`() =
        testUploadVideoQuality(value = 1, expectedVideoQuality = VideoQuality.MEDIUM)

    @Test
    fun `test that the value of videoQuality is HIGH when calling changeUploadVideoQuality`() =
        testUploadVideoQuality(value = 2, expectedVideoQuality = VideoQuality.HIGH)

    @Test
    fun `test that the value of videoQuality is ORIGINAL when calling changeUploadVideoQuality`() =
        testUploadVideoQuality(value = 3, expectedVideoQuality = VideoQuality.ORIGINAL)

    @Test
    fun `test that the value of videoQuality is not updated if its integer equivalent is invalid`() =
        runTest {
            setupUnderTest()

            underTest.changeUploadVideoQuality(4)
            verifyNoInteractions(setUploadVideoQualityUseCase, getUploadVideoQualityUseCase)
        }

    private fun testUploadVideoQuality(value: Int, expectedVideoQuality: VideoQuality) = runTest {
        setupUnderTest()

        whenever(getUploadVideoQualityUseCase()).thenReturn(expectedVideoQuality)

        underTest.changeUploadVideoQuality(value)
        underTest.state.test {
            assertThat(awaitItem().videoQuality).isEqualTo(expectedVideoQuality)
        }
    }

    @Test
    fun `test that the video sync status is set to STATUS_TO_COMPRESS if the videoQuality is LOW`() =
        testUploadSyncStatus(value = 0, expectedVideoSyncStatus = SyncStatus.STATUS_TO_COMPRESS)

    @Test
    fun `test that the video sync status is set to STATUS_TO_COMPRESS if the videoQuality is MEDIUM`() =
        testUploadSyncStatus(value = 1, expectedVideoSyncStatus = SyncStatus.STATUS_TO_COMPRESS)

    @Test
    fun `test that the video sync status is set to STATUS_TO_COMPRESS if the videoQuality is HIGH`() =
        testUploadSyncStatus(value = 2, expectedVideoSyncStatus = SyncStatus.STATUS_TO_COMPRESS)

    @Test
    fun `test that the video sync status is set to STATUS_PENDING if the videoQuality is ORIGINAL`() =
        testUploadSyncStatus(value = 3, expectedVideoSyncStatus = SyncStatus.STATUS_PENDING)

    private fun testUploadSyncStatus(value: Int, expectedVideoSyncStatus: SyncStatus) = runTest {
        setupUnderTest()

        underTest.changeUploadVideoQuality(value)
        verify(setUploadVideoSyncStatusUseCase).invoke(expectedVideoSyncStatus)
    }

    @Test
    fun `test that the device must now be charged in order to compress videos`() =
        testChargingRequiredForVideoCompression(true)

    @Test
    fun `test that the device no longer needs to be charged in order to compress videos`() =
        testChargingRequiredForVideoCompression(false)

    private fun testChargingRequiredForVideoCompression(expectedAnswer: Boolean) = runTest {
        whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(expectedAnswer)

        setupUnderTest()

        underTest.changeChargingRequiredForVideoCompression(expectedAnswer)

        verify(setChargingRequiredForVideoCompressionUseCase).invoke(expectedAnswer)
        underTest.state.test {
            assertThat(awaitItem().isChargingRequiredForVideoCompression).isEqualTo(expectedAnswer)
        }
    }

    @Test
    fun `test that a new maximum video compression size limit is set`() = runTest {
        val newSize = 300
        whenever(getVideoCompressionSizeLimitUseCase()).thenReturn(newSize)

        setupUnderTest()

        underTest.changeVideoCompressionSizeLimit(newSize)

        verify(setVideoCompressionSizeLimitUseCase).invoke(newSize)
        underTest.state.test {
            assertThat(awaitItem().videoCompressionSizeLimit).isEqualTo(newSize)
        }
    }

    @Test
    fun `test that the file names should now be kept when uploading content`() =
        testKeepUploadFileNames(true)

    @Test
    fun `test that the file names should no longer be kept when uploading content`() =
        testKeepUploadFileNames(false)

    private fun testKeepUploadFileNames(keepFileNames: Boolean) = runTest {
        whenever(areUploadFileNamesKeptUseCase()).thenReturn(keepFileNames)

        setupUnderTest()

        underTest.keepUploadFileNames(keepFileNames)

        verify(setUploadFileNamesKeptUseCase).invoke(keepFileNames)
        underTest.state.test {
            assertThat(awaitItem().areUploadFileNamesKept).isEqualTo(keepFileNames)
        }
    }

    @Test
    fun `test that the new primary folder path is set`() = runTest {
        val testPath = "test/new/folder/path"
        val isPrimaryFolderInSDCard = false

        whenever(isPrimaryFolderPathValidUseCase(any())).thenReturn(true)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(testPath)

        setupUnderTest()

        underTest.changePrimaryFolderPath(
            newPath = testPath,
            isFolderInSDCard = isPrimaryFolderInSDCard,
        )

        verify(setPrimaryFolderPathUseCase).invoke(
            newFolderPath = testPath,
            isPrimaryFolderInSDCard = isPrimaryFolderInSDCard,
        )
        verify(resetCameraUploadTimeStamps).invoke(clearCamSyncRecords = true)
        verify(clearCacheDirectory).invoke()
        verify(rescheduleCameraUploadUseCase).invoke()
        verify(setupOrUpdateCameraUploadsBackupUseCase).invoke(
            localFolder = testPath,
            targetNode = null
        )
        underTest.state.test {
            assertThat(awaitItem().primaryFolderPath).isEqualTo(testPath)
        }
    }

    @Test
    fun `test that the invalid folder selected prompt is shown if the new primary folder path is invalid`() =
        runTest {
            val testPath = "test/invalid/folder/path"
            val isPrimaryFolderInSDCard = false

            whenever(isPrimaryFolderPathValidUseCase(any())).thenReturn(false)
            setupUnderTest()

            underTest.changePrimaryFolderPath(
                testPath,
                isFolderInSDCard = isPrimaryFolderInSDCard,
            )
            underTest.state.test {
                assertThat(awaitItem().invalidFolderSelectedTextId).isEqualTo(R.string.error_invalid_folder_selected)
            }
        }

    @Test
    fun `test that the value of areLocationTagsIncluded is updated when calling includeLocationTags`() =
        runTest {
            setupUnderTest()

            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            underTest.includeLocationTags(true)

            underTest.state.map { it.areLocationTagsIncluded }.distinctUntilChanged().test {
                assertThat(awaitItem()).isTrue()

                whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
                underTest.includeLocationTags(false)
                assertThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `test that restorePrimaryTimestamps is invoked when calling restorePrimaryTimestampsAndSyncRecordProcess`() =
        runTest {
            setupUnderTest()

            underTest.restorePrimaryTimestampsAndSyncRecordProcess()

            verify(restorePrimaryTimestamps).invoke()
        }

    @Test
    fun `test that setupPrimaryFolder is invoked when calling setupPrimaryCameraUploadFolder`() =
        runTest {
            setupUnderTest()

            val testHandle = 69L

            underTest.setupPrimaryCameraUploadFolder(testHandle)

            verify(setupPrimaryFolderUseCase).invoke(testHandle)
        }

    @Test
    fun `test that setupSecondaryFolder is invoked when calling setupSecondaryCameraUploadFolder`() =
        runTest {
            setupUnderTest()

            val testHandle = 69L

            underTest.setupSecondaryCameraUploadFolder(testHandle)

            verify(setupSecondaryFolderUseCase).invoke(testHandle)
        }

    @Test
    fun `test that media uploads are disabled when calling onEnableOrDisableMediaUpload`() =
        runTest() {
            setupUnderTest()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            // enable media upload
            underTest.onEnableOrDisableMediaUpload()
            // disable media upload
            underTest.onEnableOrDisableMediaUpload()
            verify(resetMediaUploadTimeStamps).invoke()
            verify(disableMediaUploadSettings).invoke()
        }

    @Test
    fun `test that when startCameraUpload is called, startCameraUploadUseCase is called`() =
        runTest {
            setupUnderTest()

            underTest.startCameraUpload()

            verify(startCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that when stopCameraUpload is called, stopCameraUploadUseCase is called`() =
        runTest {
            setupUnderTest()

            underTest.stopCameraUploads()

            verify(stopCameraUploadsUseCase).invoke(shouldReschedule = false)
        }

    @Test
    fun `test that when rescheduleCameraUpload is called, rescheduleCameraUploadUseCase is called`() =
        runTest {
            setupUnderTest()

            underTest.rescheduleCameraUpload()

            verify(rescheduleCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that when stopCameraUpload is called, stopCameraUploadAndHeartbeatUseCase is called`() =
        runTest {
            setupUnderTest()

            underTest.stopCameraUploads()

            verify(stopCameraUploadAndHeartbeatUseCase).invoke()
        }

    @Test
    fun `test that shouldDisplayError is true when an exception occurs while setting up the primary folder`() =
        runTest {
            setupUnderTest()

            whenever(setupPrimaryFolderUseCase(any())).thenThrow(RuntimeException())

            underTest.state.map { it.shouldShowError }.distinctUntilChanged().test {
                assertThat(awaitItem()).isFalse()
                underTest.setupPrimaryCameraUploadFolder(any())
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `test that shouldDisplayError is true when an exception occurs while setting up the secondary folder`() =
        runTest {
            setupUnderTest()

            whenever(setupSecondaryFolderUseCase(any())).thenThrow(RuntimeException())

            underTest.state.map { it.shouldShowError }.distinctUntilChanged().test {
                assertThat(awaitItem()).isFalse()
                underTest.setupSecondaryCameraUploadFolder(any())
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `test that shouldDisplayError is true when an exception occurs while setting up the default secondary folder`() =
        runTest(StandardTestDispatcher()) {
            whenever(isSecondaryFolderEnabledUseCase()).thenReturn(true)
            setupUnderTest()
            testScheduler.advanceUntilIdle()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            whenever(setupDefaultSecondaryFolderUseCase()).thenThrow(RuntimeException())
            underTest.onEnableOrDisableMediaUpload()
            underTest.state.map { it.shouldShowError }.test {
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `test that camera uploads is enabled when onCameraUploadsEnabled is invoked`() =
        runTest {
            setupUnderTest()
            val shouldDisableMediaUploads = true
            underTest.onCameraUploadsEnabled(shouldDisableMediaUploads)
            verify(restorePrimaryTimestamps).invoke()
            verify(setupCameraUploadsSettingUseCase).invoke(true)
            verify(resetMediaUploadTimeStamps).invoke()
            verify(disableMediaUploadSettings).invoke()
        }

    @Test
    fun `test that media uploads is enabled when onEnableOrDisableMediaUpload is invoked`() =
        runTest(StandardTestDispatcher()) {
            setupUnderTest()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            underTest.onEnableOrDisableMediaUpload()
            verify(setupDefaultSecondaryFolderUseCase).invoke()
            verify(restoreSecondaryTimestamps).invoke()
            verify(setupMediaUploadsSettingUseCase).invoke(true)
        }

    @Test
    fun `test that media uploads backup is updated when updateMediaUploadsLocalFolder is invoked`() =
        runTest {
            setupUnderTest()
            val mediaUploadsFolderPath = "/path/to/media uploads"
            underTest.updateMediaUploadsLocalFolder(mediaUploadsFolderPath)
            verify(setupOrUpdateMediaUploadsBackupUseCase).invoke(
                localFolder = mediaUploadsFolderPath,
                targetNode = null
            )
            verify(restoreSecondaryTimestamps).invoke()
            verify(rescheduleCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that camera upload node and name is updated when updatePrimaryUploadNode is invoked`() =
        runTest(StandardTestDispatcher()) {
            setupUnderTest()
            val nodeId = NodeId(1L)
            testScheduler.advanceUntilIdle()
            val cameraUploadsNode = mock<TypedFolderNode>() {
                on { id }.thenReturn(nodeId)
                on { name }.thenReturn("Camera Uploads")
            }
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(cameraUploadsNode)
            underTest.updatePrimaryUploadNode(nodeId.longValue)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.primaryFolderName).isEqualTo("Camera Uploads")
            }
        }
}
