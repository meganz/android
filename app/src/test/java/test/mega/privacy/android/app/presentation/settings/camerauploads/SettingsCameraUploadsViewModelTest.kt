package test.mega.privacy.android.app.presentation.settings.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.SetupDefaultSecondaryFolder
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.DisableCameraUploadsInDatabase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.SetupPrimaryFolder
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsNewPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoSyncStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.workers.RescheduleCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [SettingsCameraUploadsViewModel]
 */
@ExperimentalCoroutinesApi
class SettingsCameraUploadsViewModelTest {

    private lateinit var underTest: SettingsCameraUploadsViewModel

    private val areLocationTagsEnabledUseCase = mock<AreLocationTagsEnabledUseCase>()
    private val areUploadFileNamesKeptUseCase = mock<AreUploadFileNamesKeptUseCase>()
    private val checkEnableCameraUploadsStatus = mock<CheckEnableCameraUploadsStatus>()
    private val clearCacheDirectory = mock<ClearCacheDirectory>()
    private val disableCameraUploadsInDatabase = mock<DisableCameraUploadsInDatabase>()
    private val disableMediaUploadSettings = mock<DisableMediaUploadSettings>()
    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()
    private val getUploadOptionUseCase = mock<GetUploadOptionUseCase>()
    private val getUploadVideoQuality = mock<GetUploadVideoQualityUseCase>()
    private val getVideoCompressionSizeLimitUseCase = mock<GetVideoCompressionSizeLimitUseCase>()
    private val isCameraUploadsByWifiUseCase = mock<IsCameraUploadsByWifiUseCase>()
    private val isChargingRequiredForVideoCompressionUseCase =
        mock<IsChargingRequiredForVideoCompressionUseCase>()
    private val isNewPrimaryFolderPathValidUseCase = mock<IsNewPrimaryFolderPathValidUseCase>()
    private val resetCameraUploadTimeStamps = mock<ResetCameraUploadTimeStamps>()
    private val resetMediaUploadTimeStamps = mock<ResetMediaUploadTimeStamps>()
    private val restorePrimaryTimestamps = mock<RestorePrimaryTimestamps>()
    private val restoreSecondaryTimestamps = mock<RestoreSecondaryTimestamps>()
    private val setCameraUploadsByWifiUseCase = mock<SetCameraUploadsByWifiUseCase>()
    private val setChargingRequiredForVideoCompressionUseCase =
        mock<SetChargingRequiredForVideoCompressionUseCase>()
    private val setLocationTagsEnabledUseCase = mock<SetLocationTagsEnabledUseCase>()
    private val setPrimaryFolderPathUseCase = mock<SetPrimaryFolderPathUseCase>()
    private val setUploadFileNamesKeptUseCase = mock<SetUploadFileNamesKeptUseCase>()
    private val setUploadOptionUseCase = mock<SetUploadOptionUseCase>()
    private val setUploadVideoQualityUseCase = mock<SetUploadVideoQualityUseCase>()
    private val setUploadVideoSyncStatusUseCase = mock<SetUploadVideoSyncStatusUseCase>()
    private val setVideoCompressionSizeLimitUseCase = mock<SetVideoCompressionSizeLimitUseCase>()
    private val setupDefaultSecondaryFolder = mock<SetupDefaultSecondaryFolder>()
    private val setupPrimaryFolder = mock<SetupPrimaryFolder>()
    private val setupSecondaryFolder = mock<SetupSecondaryFolder>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val stopCameraUploadUseCase = mock<StopCameraUploadUseCase>()
    private val rescheduleCameraUploadUseCase = mock<RescheduleCameraUploadUseCase>()

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
        isNewPrimaryFolderPathValid: IsNewPrimaryFolderPathValidUseCase = isNewPrimaryFolderPathValidUseCase,
    ) {
        underTest = SettingsCameraUploadsViewModel(
            areLocationTagsEnabledUseCase = areLocationTagsEnabledUseCase,
            areUploadFileNamesKeptUseCase = areUploadFileNamesKeptUseCase,
            checkEnableCameraUploadsStatus = checkEnableCameraUploadsStatus,
            clearCacheDirectory = clearCacheDirectory,
            disableCameraUploadsInDatabase = disableCameraUploadsInDatabase,
            disableMediaUploadSettings = disableMediaUploadSettings,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            getUploadOptionUseCase = getUploadOptionUseCase,
            getUploadVideoQualityUseCase = getUploadVideoQuality,
            getVideoCompressionSizeLimitUseCase = getVideoCompressionSizeLimitUseCase,
            isCameraUploadsByWifiUseCase = isCameraUploadsByWifiUseCase,
            isChargingRequiredForVideoCompressionUseCase = isChargingRequiredForVideoCompressionUseCase,
            isNewPrimaryFolderPathValidUseCase = isNewPrimaryFolderPathValid,
            monitorConnectivityUseCase = mock(),
            resetCameraUploadTimeStamps = resetCameraUploadTimeStamps,
            resetMediaUploadTimeStamps = resetMediaUploadTimeStamps,
            restorePrimaryTimestamps = restorePrimaryTimestamps,
            restoreSecondaryTimestamps = restoreSecondaryTimestamps,
            setCameraUploadsByWifiUseCase = setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase = setChargingRequiredForVideoCompressionUseCase,
            setLocationTagsEnabledUseCase = setLocationTagsEnabledUseCase,
            setPrimaryFolderPathUseCase = setPrimaryFolderPathUseCase,
            setUploadFileNamesKeptUseCase = setUploadFileNamesKeptUseCase,
            setUploadOptionUseCase = setUploadOptionUseCase,
            setUploadVideoQualityUseCase = setUploadVideoQualityUseCase,
            setUploadVideoSyncStatusUseCase = setUploadVideoSyncStatusUseCase,
            setVideoCompressionSizeLimitUseCase = setVideoCompressionSizeLimitUseCase,
            setupDefaultSecondaryFolder = setupDefaultSecondaryFolder,
            setupPrimaryFolder = setupPrimaryFolder,
            setupSecondaryFolder = setupSecondaryFolder,
            startCameraUploadUseCase = startCameraUploadUseCase,
            stopCameraUploadUseCase = stopCameraUploadUseCase,
            rescheduleCameraUploadUseCase = rescheduleCameraUploadUseCase,
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
            assertThat(state.isCameraUploadsRunning).isFalse()
            assertThat(state.isChargingRequiredForVideoCompression).isFalse()
            assertThat(state.invalidFolderSelectedTextId).isNull()
            assertThat(state.primaryFolderPath).isEmpty()
            assertThat(state.shouldShowBusinessAccountPrompt).isFalse()
            assertThat(state.shouldShowBusinessAccountSuspendedPrompt).isFalse()
            assertThat(state.shouldTriggerCameraUploads).isFalse()
            assertThat(state.shouldShowMediaPermissionsRationale).isFalse()
            assertThat(state.shouldShowNotificationPermissionRationale).isFalse()
            assertThat(state.uploadConnectionType).isNull()
            assertThat(state.uploadOption).isNull()
            assertThat(state.videoCompressionSizeLimit).isEqualTo(0)
            assertThat(state.videoQuality).isNull()
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
    fun `test that shouldShowBusinessAccountSuspendedPrompt is true when checkEnableCameraUploadsStatus returns SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT`() =
        runTest {
            setupUnderTest()

            handleEnableCameraUploads(status = EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldShowBusinessAccountSuspendedPrompt).isTrue()
            }
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
    fun `test that shouldShowBusinessAccountSuspendedPrompt is false when calling resetBusinessAccountSuspendedPromptState`() =
        runTest {
            setupUnderTest()

            underTest.resetBusinessAccountSuspendedPromptState()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.shouldShowBusinessAccountSuspendedPrompt).isFalse()
            }
        }

    @Test
    fun `test that isCameraUploadsRunning is updated correctly`() = runTest {
        setupUnderTest()

        underTest.setCameraUploadsRunning(true)

        underTest.state.map { it.isCameraUploadsRunning }.distinctUntilChanged().test {
            assertThat(awaitItem()).isTrue()

            underTest.setCameraUploadsRunning(false)
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
            verifyNoInteractions(setUploadVideoQualityUseCase, getUploadVideoQuality)
        }

    private fun testUploadVideoQuality(value: Int, expectedVideoQuality: VideoQuality) = runTest {
        setupUnderTest()

        whenever(getUploadVideoQuality()).thenReturn(expectedVideoQuality)

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
        verify(setUploadVideoSyncStatusUseCase, times(1)).invoke(expectedVideoSyncStatus)
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

        verify(setUploadFileNamesKeptUseCase, times(1)).invoke(keepFileNames)
        underTest.state.test {
            assertThat(awaitItem().areUploadFileNamesKept).isEqualTo(keepFileNames)
        }
    }

    @Test
    fun `test that the new primary folder path is set`() = runTest {
        val testPath = "test/new/folder/path"

        whenever(isNewPrimaryFolderPathValidUseCase(any())).thenReturn(true)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(testPath)

        setupUnderTest()

        underTest.changePrimaryFolderPath(testPath)

        verify(setPrimaryFolderPathUseCase, times(1)).invoke(testPath)
        underTest.state.test {
            assertThat(awaitItem().primaryFolderPath).isEqualTo(testPath)
        }
    }

    @Test
    fun `test that the invalid folder selected prompt is shown if the new primary folder path is invalid`() =
        runTest {
            whenever(isNewPrimaryFolderPathValidUseCase(any())).thenReturn(false)
            setupUnderTest()

            underTest.changePrimaryFolderPath("test/invalid/folder/path")
            underTest.state.test {
                assertThat(awaitItem().invalidFolderSelectedTextId).isEqualTo(R.string.error_invalid_folder_selected)
            }
        }

    @Test
    fun `test that the invalid folder selected prompt is shown if the new primary folder path is null`() =
        runTest {
            val isNewPrimaryFolderPathValidSpy = Mockito.spy(isNewPrimaryFolderPathValidUseCase)
            setupUnderTest(isNewPrimaryFolderPathValid = isNewPrimaryFolderPathValidSpy)

            underTest.changePrimaryFolderPath(null)
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

            verify(restorePrimaryTimestamps, times(1)).invoke()
        }

    @Test
    fun `test that restoreSecondaryTimestamps is invoked when calling restoreSecondaryTimestampsAndSyncRecordProcess`() =
        runTest {
            setupUnderTest()

            underTest.restoreSecondaryTimestampsAndSyncRecordProcess()

            verify(restoreSecondaryTimestamps, times(1)).invoke()
        }

    @Test
    fun `test that setupDefaultSecondaryFolder is invoked when calling setupDefaultSecondaryCameraUploadFolder`() =
        runTest {
            setupUnderTest()

            val testName = "Media Uploads"

            underTest.setupDefaultSecondaryCameraUploadFolder(testName)

            verify(setupDefaultSecondaryFolder, times(1)).invoke(testName)
        }

    @Test
    fun `test that setupPrimaryFolder is invoked when calling setupPrimaryCameraUploadFolder`() =
        runTest {
            setupUnderTest()

            val testHandle = 69L

            underTest.setupPrimaryCameraUploadFolder(testHandle)

            verify(setupPrimaryFolder, times(1)).invoke(testHandle)
        }

    @Test
    fun `test that setupSecondaryFolder is invoked when calling setupSecondaryCameraUploadFolder`() =
        runTest {
            setupUnderTest()

            val testHandle = 69L

            underTest.setupSecondaryCameraUploadFolder(testHandle)

            verify(setupSecondaryFolder, times(1)).invoke(testHandle)
        }

    @Test
    fun `test that resetCameraUploadTimeStamps and clearCacheDirectory are invoked in order when calling resetTimestampsAndCacheDirectory`() =
        runTest {
            setupUnderTest()

            underTest.resetTimestampsAndCacheDirectory()

            with(inOrder(clearCacheDirectory, resetCameraUploadTimeStamps)) {
                verify(resetCameraUploadTimeStamps, times(1)).invoke(clearCamSyncRecords = true)
                verify(clearCacheDirectory, times(1)).invoke()
            }
        }

    @Test
    fun `test that disableCameraUploadsInDatabase is invoked when calling disableCameraUploadsInDB`() =
        runTest {
            setupUnderTest()

            underTest.disableCameraUploadsInDB()

            verify(disableCameraUploadsInDatabase, times(1)).invoke()
        }

    @Test
    fun `test that media uploads are disabled when calling disableMediaUploads`() = runTest {
        setupUnderTest()

        underTest.disableMediaUploads()

        verify(resetMediaUploadTimeStamps, times(1)).invoke()
        verify(disableMediaUploadSettings, times(1)).invoke()
    }

    @Test
    fun `test that when startCameraUpload is called, startCameraUploadUseCase is called`() =
        runTest {
            setupUnderTest()

            underTest.startCameraUpload()

            verify(startCameraUploadUseCase, times(1)).invoke()
        }

    @Test
    fun `test that when stopCameraUpload is called, stopCameraUploadUseCase is called`() =
        runTest {
            setupUnderTest()

            underTest.stopCameraUpload()

            verify(stopCameraUploadUseCase, times(1)).invoke()
        }

    @Test
    fun `test that when rescheduleCameraUpload is called, rescheduleCameraUploadUseCase is called`() =
        runTest {
            setupUnderTest()

            underTest.rescheduleCameraUpload()

            verify(rescheduleCameraUploadUseCase, times(1)).invoke()
        }
}
