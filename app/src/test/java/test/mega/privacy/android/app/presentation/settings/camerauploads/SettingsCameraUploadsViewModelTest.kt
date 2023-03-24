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
import mega.privacy.android.domain.usecase.IsCameraUploadByWifi
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.RestorePrimaryTimestamps
import mega.privacy.android.domain.usecase.RestoreSecondaryTimestamps
import mega.privacy.android.domain.usecase.SetCameraUploadsByWifi
import mega.privacy.android.domain.usecase.SetupPrimaryFolder
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOption
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQuality
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimit
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompression
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompression
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabled
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOption
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQuality
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoSyncStatus
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimit
import org.junit.After
import org.junit.Before
import org.junit.Test
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

    private val areLocationTagsEnabled = mock<AreLocationTagsEnabled>()
    private val checkEnableCameraUploadsStatus = mock<CheckEnableCameraUploadsStatus>()
    private val clearCacheDirectory = mock<ClearCacheDirectory>()
    private val disableCameraUploadsInDatabase = mock<DisableCameraUploadsInDatabase>()
    private val disableMediaUploadSettings = mock<DisableMediaUploadSettings>()
    private val getUploadOption = mock<GetUploadOption>()
    private val getUploadVideoQuality = mock<GetUploadVideoQuality>()
    private val getVideoCompressionSizeLimit = mock<GetVideoCompressionSizeLimit>()
    private val isCameraUploadByWifi = mock<IsCameraUploadByWifi>()
    private val isChargingRequiredForVideoCompression =
        mock<IsChargingRequiredForVideoCompression>()
    private val resetCameraUploadTimeStamps = mock<ResetCameraUploadTimeStamps>()
    private val resetMediaUploadTimeStamps = mock<ResetMediaUploadTimeStamps>()
    private val restorePrimaryTimestamps = mock<RestorePrimaryTimestamps>()
    private val restoreSecondaryTimestamps = mock<RestoreSecondaryTimestamps>()
    private val setCameraUploadsByWifi = mock<SetCameraUploadsByWifi>()
    private val setChargingRequiredForVideoCompression =
        mock<SetChargingRequiredForVideoCompression>()
    private val setLocationTagsEnabled = mock<SetLocationTagsEnabled>()
    private val setUploadOption = mock<SetUploadOption>()
    private val setUploadVideoQuality = mock<SetUploadVideoQuality>()
    private val setUploadVideoSyncStatus = mock<SetUploadVideoSyncStatus>()
    private val setVideoCompressionSizeLimit = mock<SetVideoCompressionSizeLimit>()
    private val setupDefaultSecondaryFolder = mock<SetupDefaultSecondaryFolder>()
    private val setupPrimaryFolder = mock<SetupPrimaryFolder>()
    private val setupSecondaryFolder = mock<SetupSecondaryFolder>()

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
    private fun setupUnderTest() {
        underTest = SettingsCameraUploadsViewModel(
            areLocationTagsEnabled = areLocationTagsEnabled,
            checkEnableCameraUploadsStatus = checkEnableCameraUploadsStatus,
            clearCacheDirectory = clearCacheDirectory,
            disableCameraUploadsInDatabase = disableCameraUploadsInDatabase,
            disableMediaUploadSettings = disableMediaUploadSettings,
            getUploadOption = getUploadOption,
            getUploadVideoQuality = getUploadVideoQuality,
            getVideoCompressionSizeLimit = getVideoCompressionSizeLimit,
            isCameraUploadByWifi = isCameraUploadByWifi,
            isChargingRequiredForVideoCompression = isChargingRequiredForVideoCompression,
            monitorConnectivityUseCase = mock(),
            resetCameraUploadTimeStamps = resetCameraUploadTimeStamps,
            resetMediaUploadTimeStamps = resetMediaUploadTimeStamps,
            restorePrimaryTimestamps = restorePrimaryTimestamps,
            restoreSecondaryTimestamps = restoreSecondaryTimestamps,
            setCameraUploadsByWifi = setCameraUploadsByWifi,
            setChargingRequiredForVideoCompression = setChargingRequiredForVideoCompression,
            setLocationTagsEnabled = setLocationTagsEnabled,
            setUploadOption = setUploadOption,
            setUploadVideoQuality = setUploadVideoQuality,
            setUploadVideoSyncStatus = setUploadVideoSyncStatus,
            setVideoCompressionSizeLimit = setVideoCompressionSizeLimit,
            setupDefaultSecondaryFolder = setupDefaultSecondaryFolder,
            setupPrimaryFolder = setupPrimaryFolder,
            setupSecondaryFolder = setupSecondaryFolder,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        setupUnderTest()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.accessMediaLocationRationaleText).isNull()
            assertThat(state.areLocationTagsIncluded).isFalse()
            assertThat(state.isCameraUploadsRunning).isFalse()
            assertThat(state.isChargingRequiredForVideoCompression).isFalse()
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
    fun `test that uploadConnectionType is updated correctly when calling changeUploadConnectionType`() =
        runTest {
            setupUnderTest()

            whenever(isCameraUploadByWifi()).thenReturn(true)
            underTest.changeUploadConnectionType(wifiOnly = true)

            underTest.state.map { it.uploadConnectionType }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(UploadConnectionType.WIFI)

                whenever(isCameraUploadByWifi()).thenReturn(false)
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

        whenever(getUploadOption()).thenReturn(uploadOption)

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
            verifyNoInteractions(setUploadVideoQuality, getUploadVideoQuality)
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
        verify(setUploadVideoSyncStatus, times(1)).invoke(expectedVideoSyncStatus)
    }

    @Test
    fun `test that the device must now be charged in order to compress videos`() =
        testChargingRequiredForVideoCompression(true)

    @Test
    fun `test that the device no longer needs to be charged in order to compress videos`() =
        testChargingRequiredForVideoCompression(false)

    private fun testChargingRequiredForVideoCompression(expectedAnswer: Boolean) = runTest {
        whenever(isChargingRequiredForVideoCompression()).thenReturn(expectedAnswer)

        setupUnderTest()

        underTest.changeChargingRequiredForVideoCompression(expectedAnswer)

        verify(setChargingRequiredForVideoCompression).invoke(expectedAnswer)
        underTest.state.test {
            assertThat(awaitItem().isChargingRequiredForVideoCompression).isEqualTo(expectedAnswer)
        }
    }

    @Test
    fun `test that a new maximum video compression size limit is set`() = runTest {
        val newSize = 300
        whenever(getVideoCompressionSizeLimit()).thenReturn(newSize)

        setupUnderTest()

        underTest.changeVideoCompressionSizeLimit(newSize)

        verify(setVideoCompressionSizeLimit).invoke(newSize)
        underTest.state.test {
            assertThat(awaitItem().videoCompressionSizeLimit).isEqualTo(newSize)
        }
    }

    @Test
    fun `test that the value of areLocationTagsIncluded is updated when calling includeLocationTags`() =
        runTest {
            setupUnderTest()

            whenever(areLocationTagsEnabled()).thenReturn(true)
            underTest.includeLocationTags(true)

            underTest.state.map { it.areLocationTagsIncluded }.distinctUntilChanged().test {
                assertThat(awaitItem()).isTrue()

                whenever(areLocationTagsEnabled()).thenReturn(false)
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
}