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
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatus
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.ClearCameraUploadsRecordUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsSettingsActionsUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetDefaultPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupDefaultSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [SettingsCameraUploadsViewModel]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SettingsCameraUploadsViewModelTest {
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
    private val setCameraUploadsByWifiUseCase = mock<SetCameraUploadsByWifiUseCase>()
    private val setChargingRequiredForVideoCompressionUseCase =
        mock<SetChargingRequiredForVideoCompressionUseCase>()
    private val setDefaultPrimaryFolderPathUseCase = mock<SetDefaultPrimaryFolderPathUseCase>()
    private val setLocationTagsEnabledUseCase = mock<SetLocationTagsEnabledUseCase>()
    private val setPrimaryFolderPathUseCase = mock<SetPrimaryFolderPathUseCase>()
    private val setUploadFileNamesKeptUseCase = mock<SetUploadFileNamesKeptUseCase>()
    private val setUploadOptionUseCase = mock<SetUploadOptionUseCase>()
    private val setUploadVideoQualityUseCase = mock<SetUploadVideoQualityUseCase>()
    private val setVideoCompressionSizeLimitUseCase = mock<SetVideoCompressionSizeLimitUseCase>()
    private val setupDefaultSecondaryFolderUseCase = mock<SetupDefaultSecondaryFolderUseCase>()
    private val setupPrimaryFolderUseCase = mock<SetupPrimaryFolderUseCase>()
    private val setupSecondaryFolderUseCase = mock<SetupSecondaryFolderUseCase>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val stopCameraUploadsUseCase = mock<StopCameraUploadsUseCase>()
    private val hasMediaPermissionUseCase = mock<HasMediaPermissionUseCase>()
    private val monitorCameraUploadsSettingsActionsUseCase =
        mock<MonitorCameraUploadsSettingsActionsUseCase>()
    private val setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase = mock()
    private val setupMediaUploadsSettingUseCase: SetupMediaUploadsSettingUseCase = mock()
    private val setupCameraUploadsSyncHandleUseCase: SetupCameraUploadsSyncHandleUseCase = mock()
    private val broadcastBusinessAccountExpiredUseCase =
        mock<BroadcastBusinessAccountExpiredUseCase>()
    private val getPrimarySyncHandleUseCase = mock<GetPrimarySyncHandleUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val isSecondaryFolderEnabledUseCase = mock<IsSecondaryFolderEnabled>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val getSecondarySyncHandleUseCase = mock<GetSecondarySyncHandleUseCase>()
    private val getSecondaryFolderPathUseCase = mock<GetSecondaryFolderPathUseCase>()
    private val setupMediaUploadsSyncHandleUseCase = mock<SetupMediaUploadsSyncHandleUseCase>()
    private val isSecondaryFolderPathValidUseCase = mock<IsSecondaryFolderPathValidUseCase>()
    private val setSecondaryFolderLocalPathUseCase = mock<SetSecondaryFolderLocalPathUseCase>()
    private val clearCameraUploadsRecordUseCase = mock<ClearCameraUploadsRecordUseCase>()
    private val listenToNewMediaUseCase = mock<ListenToNewMediaUseCase>()
    private val snackBarHandler = mock<SnackBarHandler>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isCameraUploadsEnabledUseCase,
            areLocationTagsEnabledUseCase,
            areUploadFileNamesKeptUseCase,
            checkEnableCameraUploadsStatus,
            clearCacheDirectory,
            disableMediaUploadSettings,
            getPrimaryFolderPathUseCase,
            getUploadOptionUseCase,
            getUploadVideoQualityUseCase,
            getVideoCompressionSizeLimitUseCase,
            isCameraUploadsByWifiUseCase,
            isChargingRequiredForVideoCompressionUseCase,
            isPrimaryFolderPathValidUseCase,
            preparePrimaryFolderPathUseCase,
            setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase,
            setDefaultPrimaryFolderPathUseCase,
            setLocationTagsEnabledUseCase,
            setPrimaryFolderPathUseCase,
            setUploadFileNamesKeptUseCase,
            setUploadOptionUseCase,
            setUploadVideoQualityUseCase,
            setVideoCompressionSizeLimitUseCase,
            setupDefaultSecondaryFolderUseCase,
            setupPrimaryFolderUseCase,
            setupSecondaryFolderUseCase,
            startCameraUploadUseCase,
            stopCameraUploadsUseCase,
            hasMediaPermissionUseCase,
            monitorCameraUploadsSettingsActionsUseCase,
            isConnectedToInternetUseCase,
            setupCameraUploadsSettingUseCase,
            setupMediaUploadsSettingUseCase,
            setupCameraUploadsSyncHandleUseCase,
            broadcastBusinessAccountExpiredUseCase,
            getPrimarySyncHandleUseCase,
            getNodeByIdUseCase,
            isSecondaryFolderEnabledUseCase,
            getSecondarySyncHandleUseCase,
            getSecondaryFolderPathUseCase,
            setupMediaUploadsSyncHandleUseCase,
            isSecondaryFolderPathValidUseCase,
            setSecondaryFolderLocalPathUseCase,
            clearCameraUploadsRecordUseCase,
            snackBarHandler,
        )
    }

    /**
     * Initializes [SettingsCameraUploadsViewModel] for testing
     */
    private suspend fun setupUnderTest() {
        stubCommon()
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
            isPrimaryFolderPathValidUseCase = isPrimaryFolderPathValidUseCase,
            monitorConnectivityUseCase = mock(),
            preparePrimaryFolderPathUseCase = preparePrimaryFolderPathUseCase,
            setCameraUploadsByWifiUseCase = setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase = setChargingRequiredForVideoCompressionUseCase,
            setDefaultPrimaryFolderPathUseCase = setDefaultPrimaryFolderPathUseCase,
            setLocationTagsEnabledUseCase = setLocationTagsEnabledUseCase,
            setPrimaryFolderPathUseCase = setPrimaryFolderPathUseCase,
            setUploadFileNamesKeptUseCase = setUploadFileNamesKeptUseCase,
            setUploadOptionUseCase = setUploadOptionUseCase,
            setUploadVideoQualityUseCase = setUploadVideoQualityUseCase,
            setVideoCompressionSizeLimitUseCase = setVideoCompressionSizeLimitUseCase,
            setupDefaultSecondaryFolderUseCase = setupDefaultSecondaryFolderUseCase,
            setupPrimaryFolderUseCase = setupPrimaryFolderUseCase,
            setupSecondaryFolderUseCase = setupSecondaryFolderUseCase,
            startCameraUploadUseCase = startCameraUploadUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            hasMediaPermissionUseCase = hasMediaPermissionUseCase,
            monitorCameraUploadsSettingsActionsUseCase = monitorCameraUploadsSettingsActionsUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            setupCameraUploadsSettingUseCase = setupCameraUploadsSettingUseCase,
            setupMediaUploadsSettingUseCase = setupMediaUploadsSettingUseCase,
            setupCameraUploadsSyncHandleUseCase = setupCameraUploadsSyncHandleUseCase,
            monitorBackupInfoTypeUseCase = mock(),
            broadcastBusinessAccountExpiredUseCase = broadcastBusinessAccountExpiredUseCase,
            monitorCameraUploadsFolderDestinationUseCase = mock(),
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            isSecondaryFolderEnabledUseCase = isSecondaryFolderEnabledUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
            setupMediaUploadsSyncHandleUseCase = setupMediaUploadsSyncHandleUseCase,
            isSecondaryFolderPathValidUseCase = isSecondaryFolderPathValidUseCase,
            setSecondaryFolderLocalPathUseCase = setSecondaryFolderLocalPathUseCase,
            clearCameraUploadsRecordUseCase = clearCameraUploadsRecordUseCase,
            listenToNewMediaUseCase = listenToNewMediaUseCase,
            snackBarHandler = snackBarHandler,
        )
    }

    private suspend fun stubCommon() {
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
        whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
        whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
        whenever(getPrimaryFolderPathUseCase()).thenReturn("/path/to/CU")
        whenever(getUploadOptionUseCase()).thenReturn(UploadOption.PHOTOS_AND_VIDEOS)
        whenever(getUploadVideoQualityUseCase()).thenReturn(VideoQuality.ORIGINAL)
        whenever(getVideoCompressionSizeLimitUseCase()).thenReturn(200)
        whenever(isCameraUploadsByWifiUseCase()).thenReturn(true)
        whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(true)
        whenever(isPrimaryFolderPathValidUseCase(any())).thenReturn(true)
        whenever(hasMediaPermissionUseCase()).thenReturn(true)
        whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
        whenever(isSecondaryFolderEnabledUseCase()).thenReturn(true)
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(getSecondarySyncHandleUseCase()).thenReturn(2L)
        whenever(getSecondaryFolderPathUseCase()).thenReturn("/path/to/MU")
        whenever(isSecondaryFolderPathValidUseCase(any())).thenReturn(true)
        val cuNode = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("Camera Uploads")
        }
        val muNode = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(2L))
            on { name }.thenReturn("Media Uploads")
        }
        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(cuNode)
        whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(muNode)
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        setupUnderTest()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.areLocationTagsIncluded).isTrue()
            assertThat(state.areUploadFileNamesKept).isTrue()
            assertThat(state.isCameraUploadsEnabled).isTrue()
            assertThat(state.isChargingRequiredForVideoCompression).isTrue()
            assertThat(state.primaryFolderPath).isEqualTo("/path/to/CU")
            assertThat(state.secondaryFolderPath).isEqualTo("/path/to/MU")
            assertThat(state.shouldShowBusinessAccountPrompt).isFalse()
            assertThat(state.shouldShowMediaPermissionsRationale).isFalse()
            assertThat(state.uploadConnectionType).isEqualTo(UploadConnectionType.WIFI)
            assertThat(state.uploadOption).isEqualTo(UploadOption.PHOTOS_AND_VIDEOS)
            assertThat(state.videoCompressionSizeLimit).isEqualTo(200)
            assertThat(state.showNewVideoCompressionSizePrompt).isFalse()
            assertThat(state.clearNewVideoCompressionSizeInput).isFalse()
            assertThat(state.videoQuality).isEqualTo(VideoQuality.ORIGINAL)
            assertThat(state.primaryUploadSyncHandle).isEqualTo(1L)
            assertThat(state.primaryFolderName).isEqualTo("Camera Uploads")
            assertThat(state.secondaryUploadSyncHandle).isEqualTo(2L)
            assertThat(state.secondaryFolderName).isEqualTo("Media Uploads")
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
    fun `test that a snackbar containing the access media location rationale is shown`() = runTest {
        setupUnderTest()
        underTest.showAccessMediaLocationRationale()
        verify(snackBarHandler).postSnackbarMessage(R.string.on_refuse_storage_permission)
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

    @ParameterizedTest(name = "invoked with {0}")
    @MethodSource("provideUploadOptionsParams")
    fun `test that the value of uploadOption changes when changeUploadOption`(uploadOption: UploadOption) =
        runTest {
            setupUnderTest()
            whenever(getUploadOptionUseCase()).thenReturn(uploadOption)

            underTest.changeUploadOption(uploadOption)

            verify(clearCacheDirectory).invoke()
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            underTest.state.test {
                assertThat(awaitItem().uploadOption).isEqualTo(uploadOption)
            }
        }

    private fun provideUploadOptionsParams() = Stream.of(
        UploadOption.PHOTOS,
        UploadOption.VIDEOS,
        UploadOption.PHOTOS_AND_VIDEOS
    )

    @ParameterizedTest(name = "with {1}, the videoQuality is {0}")
    @MethodSource("provideVideQualityParams")
    fun `test that  when calling changeUploadVideoQuality`(
        value: Int,
        expectedVideoQuality: VideoQuality,
    ) = runTest {
        setupUnderTest()

        whenever(getUploadVideoQualityUseCase()).thenReturn(expectedVideoQuality)

        underTest.changeUploadVideoQuality(value)
        underTest.state.test {
            assertThat(awaitItem().videoQuality).isEqualTo(expectedVideoQuality)
        }
    }

    private fun provideVideQualityParams() = Stream.of(
        Arguments.of(0, VideoQuality.LOW),
        Arguments.of(1, VideoQuality.MEDIUM),
        Arguments.of(2, VideoQuality.HIGH),
        Arguments.of(3, VideoQuality.ORIGINAL)
    )

    @Test
    fun `test that the value of videoQuality is not updated if its integer equivalent is invalid`() =
        runTest {
            setupUnderTest()
            underTest.changeUploadVideoQuality(4)
            verifyNoInteractions(
                setUploadVideoQualityUseCase,
            )
        }

    @ParameterizedTest(name = "invoked with {0}, device is needed to charge {0}")
    @ValueSource(booleans = [true, false])
    fun `test that when changeChargingRequiredForVideoCompression`(expectedAnswer: Boolean) =
        runTest {
            setupUnderTest()
            whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(expectedAnswer)
            underTest.changeChargingRequiredForVideoCompression(expectedAnswer)
            verify(setChargingRequiredForVideoCompressionUseCase).invoke(expectedAnswer)
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            underTest.state.test {
                assertThat(awaitItem().isChargingRequiredForVideoCompression).isEqualTo(
                    expectedAnswer
                )
            }
        }

    @ParameterizedTest(name = "with {0}, file names should be kept {0}")
    @ValueSource(booleans = [true, false])
    fun `test that when keepUploadFileNames is invoked`(expected: Boolean) = runTest {
        setupUnderTest()
        whenever(areUploadFileNamesKeptUseCase()).thenReturn(expected)
        underTest.keepUploadFileNames(expected)
        verify(setUploadFileNamesKeptUseCase).invoke(expected)
        verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
        underTest.state.test {
            assertThat(awaitItem().areUploadFileNamesKept).isEqualTo(expected)
        }
    }

    @Test
    fun `test that the new primary folder is set`() = runTest {
        val testPath = "test/new/folder/path"
        setupUnderTest()
        whenever(isPrimaryFolderPathValidUseCase(any())).thenReturn(true)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(testPath)
        underTest.setPrimaryFolder(newPath = testPath)

        verify(setPrimaryFolderPathUseCase).invoke(
            newFolderPath = testPath,
        )
        verify(clearCacheDirectory).invoke()
        verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
        underTest.state.test {
            assertThat(awaitItem().primaryFolderPath).isEqualTo(testPath)
        }
    }

    @Test
    fun `test that an error snackbar is shown if the new primary folder is invalid`() =
        runTest {
            val testPath = "test/invalid/folder/path"

            setupUnderTest()
            whenever(isPrimaryFolderPathValidUseCase(any())).thenReturn(false)

            underTest.setPrimaryFolder(testPath)
            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.error_invalid_folder_selected,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }

    @Test
    fun `test that the new cloud drive primary folder is set`() = runTest {
        val newMegaPrimaryFolderHandle = 123456L
        setupUnderTest()
        underTest.setPrimaryUploadNode(newMegaPrimaryFolderHandle)

        verify(setupPrimaryFolderUseCase).invoke(newMegaPrimaryFolderHandle)
        verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
    }

    @Test
    fun `test that an error snackbar is shown when setting the new cloud drive primary folder and the new setting is invalid`() =
        runTest {
            setupUnderTest()
            val newMegaPrimaryFolderHandle = 123456L

            // Set Cloud Drive Secondary Folder to be the same as the new Cloud Drive Primary Folder
            underTest.updateSecondaryUploadNode(newMegaPrimaryFolderHandle)
            underTest.setPrimaryUploadNode(newMegaPrimaryFolderHandle)

            underTest.state.test {
                assertThat(awaitItem().secondaryUploadSyncHandle).isEqualTo(
                    newMegaPrimaryFolderHandle
                )
            }
            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.error_invalid_folder_selected,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }

    @Test
    fun `test that an error snackbar is shown when an exception occurs from setting the new cloud drive primary folder`() =
        runTest {
            setupUnderTest()
            whenever(setupPrimaryFolderUseCase(any())).thenThrow(RuntimeException())
            underTest.setPrimaryUploadNode(123456L)

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.general_error,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }

    @Test
    fun `test that the value of areLocationTagsIncluded is updated when calling includeLocationTags`() =
        runTest {
            setupUnderTest()

            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            underTest.includeLocationTags(true)
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)

            underTest.state.map { it.areLocationTagsIncluded }.distinctUntilChanged().test {
                assertThat(awaitItem()).isTrue()

                whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
                underTest.includeLocationTags(false)
                assertThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `test that the new cloud drive secondary folder is set`() =
        runTest {
            setupUnderTest()

            val testHandle = 69L

            underTest.setSecondaryUploadNode(testHandle)

            verify(setupSecondaryFolderUseCase).invoke(testHandle)
        }

    @Test
    fun `test that media uploads are disabled when calling toggleMediaUploads`() =
        runTest {
            setupUnderTest()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            // enable media upload
            underTest.toggleMediaUploads()
            // disable media upload
            underTest.toggleMediaUploads()
            verify(disableMediaUploadSettings).invoke()
        }

    @Test
    fun `test that camera uploads is started when invoking startCameraUploads`() =
        runTest {
            setupUnderTest()

            underTest.startCameraUploads()

            verify(startCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that listening to new media is started when invoking startCameraUploads`() =
        runTest {
            setupUnderTest()

            underTest.startCameraUploads()

            verify(listenToNewMediaUseCase).invoke(forceEnqueue = false)
        }

    @Test
    fun `test that an error snackbar is shown when setting the new cloud drive secondary folder and the new setting is invalid`() =
        runTest {
            setupUnderTest()
            val newMegaSecondaryFolderHandle = 123456L

            // Set Cloud Drive Primary Folder to be the same as the new Cloud Drive Secondary Folder
            underTest.updatePrimaryUploadNode(newMegaSecondaryFolderHandle)
            underTest.setSecondaryUploadNode(newMegaSecondaryFolderHandle)

            underTest.state.test {
                assertThat(awaitItem().primaryUploadSyncHandle).isEqualTo(
                    newMegaSecondaryFolderHandle
                )
            }
            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.error_invalid_folder_selected,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }

    @Test
    fun `test that an error snackbar is shown when an exception occurs from setting the new cloud drive secondary folder`() =
        runTest {
            setupUnderTest()
            whenever(setupSecondaryFolderUseCase(any())).thenThrow(RuntimeException())
            underTest.setSecondaryUploadNode(any())

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.general_error,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }

    @Test
    fun `test that an error snackbar is shown when an exception occurs while setting up the default secondary folder`() =
        runTest {
            setupUnderTest()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            whenever(setupDefaultSecondaryFolderUseCase()).thenThrow(RuntimeException())
            // Disable Media Uploads
            underTest.toggleMediaUploads()
            // Enable Media Uploads
            underTest.toggleMediaUploads()

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.general_error,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }

    @Test
    fun `test that camera uploads is enabled when onCameraUploadsEnabled is invoked`() =
        runTest {
            setupUnderTest()
            underTest.onCameraUploadsEnabled()
            verify(setupCameraUploadsSettingUseCase).invoke(true)
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                assertThat(awaitItem().isCameraUploadsEnabled).isTrue()
            }
        }

    @Test
    fun `test that media uploads is enabled when toggleMediaUploads is invoked`() =
        runTest {
            setupUnderTest()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            //disable
            underTest.toggleMediaUploads()
            // enable
            underTest.toggleMediaUploads()
            verify(setupDefaultSecondaryFolderUseCase).invoke()
            verify(setupMediaUploadsSettingUseCase).invoke(true)
        }

    @Test
    fun `test that the new secondary folder is set`() =
        runTest {
            setupUnderTest()
            val mediaUploadsFolderPath = "/path/to/media uploads"
            whenever(isSecondaryFolderPathValidUseCase(mediaUploadsFolderPath)).thenReturn(true)
            underTest.setSecondaryFolder(mediaUploadsFolderPath)
            verify(setSecondaryFolderLocalPathUseCase).invoke(mediaUploadsFolderPath)
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
        }

    @Test
    fun `test that an error snackbar is shown when setting the new secondary folder and the new setting is invalid`() =
        runTest {
            setupUnderTest()
            // Simulate an invalid setting (e.g. the new secondary folder is the same as the primary
            // folder that was set)
            underTest.setSecondaryFolder("/path/to/CU")
            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.error_invalid_folder_selected,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
            verifyNoInteractions(
                isSecondaryFolderPathValidUseCase,
                clearCameraUploadsRecordUseCase,
                stopCameraUploadsUseCase,
            )
        }

    @Test
    fun `test that an error snackbar is shown when setting the new secondary folder with an invalid folder path`() =
        runTest {
            setupUnderTest()
            whenever(isSecondaryFolderPathValidUseCase(any())).thenReturn(false)
            underTest.setSecondaryFolder("new/MU/folder/path")
            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.error_invalid_folder_selected,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
            verifyNoInteractions(
                clearCameraUploadsRecordUseCase,
                stopCameraUploadsUseCase,
            )
        }

    @Test
    fun `test that camera upload node and name is updated when updatePrimaryUploadNode is invoked`() =
        runTest {
            setupUnderTest()
            val nodeId = NodeId(1L)
            val cameraUploadsNode = mock<TypedFolderNode> {
                on { id }.thenReturn(nodeId)
                on { name }.thenReturn("Camera Uploads")
            }
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(cameraUploadsNode)
            underTest.updatePrimaryUploadNode(nodeId.longValue)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.primaryUploadSyncHandle).isEqualTo(nodeId.longValue)
                assertThat(state.primaryFolderName).isEqualTo("Camera Uploads")
            }
        }

    @Test
    fun `test that media upload node and name is updated when updateSecondaryUploadNode is invoked`() =
        runTest {
            setupUnderTest()
            val nodeId = NodeId(1L)
            val cameraUploadsNode = mock<TypedFolderNode> {
                on { id }.thenReturn(nodeId)
                on { name }.thenReturn("Media Uploads")
            }
            whenever(getNodeByIdUseCase(nodeId)).thenReturn(cameraUploadsNode)
            underTest.updateSecondaryUploadNode(nodeId.longValue)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.secondaryUploadSyncHandle).isEqualTo(nodeId.longValue)
                assertThat(state.secondaryFolderName).isEqualTo("Media Uploads")
            }
        }

    @Test
    fun `test that the new settings are invalid when camera uploads is enabled but handle and path are not set`() =
        runTest {
            setupUnderTest()
            val actual = underTest.isNewSettingValid(primaryHandle = null, primaryPath = null)
            assertThat(actual).isFalse()
        }

    @Test
    fun `test that the new settings are invalid when camera uploads is enabled and the new cloud drive primary folder does not exist`() =
        runTest {
            setupUnderTest()
            val actual = underTest.isNewSettingValid(primaryHandle = -1L)
            assertThat(actual).isFalse()
        }

    @Test
    fun `test that the new settings are invalid when media uploads is enabled and the new cloud drive secondary folder does not exist`() =
        runTest {
            setupUnderTest()
            val actual = underTest.isNewSettingValid(secondaryHandle = -1L)
            assertThat(actual).isFalse()
        }

    @Test
    fun `test that the new settings are invalid when media uploads is enabled but handle and path are not set`() =
        runTest {
            setupUnderTest()
            val actual = underTest.isNewSettingValid(secondaryHandle = null, secondaryPath = null)
            assertThat(actual).isFalse()
        }

    @Test
    fun `test that the new settings are invalid when both camera uploads and media uploads handles are the same`() =
        runTest {
            setupUnderTest()
            val actual = underTest.isNewSettingValid(primaryHandle = 1L, secondaryHandle = 1L)
            assertThat(actual).isFalse()
        }

    @Test
    fun `test that the new settings are invalid when both camera uploads and media uploads local paths are the same`() =
        runTest {
            setupUnderTest()
            val actual = underTest.isNewSettingValid(
                primaryPath = "path/to/CU",
                secondaryPath = "path/to/CU/to/MU"
            )
            assertThat(actual).isFalse()
        }

    @Test
    fun `test that the primary folder records are cleared when the primary folder changes`() =
        runTest {
            val testPath = "test/new/folder/path"

            setupUnderTest()

            underTest.setPrimaryFolder(newPath = testPath)

            verify(clearCameraUploadsRecordUseCase).invoke(
                listOf(CameraUploadFolderType.Primary)
            )
        }

    @Test
    fun `test that the secondary folder records are cleared when the secondary folder changes`() =
        runTest {
            val mediaUploadsFolderPath = "/path/to/media uploads"
            setupUnderTest()
            whenever(isSecondaryFolderPathValidUseCase(mediaUploadsFolderPath)).thenReturn(true)
            underTest.setSecondaryFolder(mediaUploadsFolderPath)

            verify(clearCameraUploadsRecordUseCase).invoke(
                listOf(CameraUploadFolderType.Secondary)
            )
        }

    @ParameterizedTest(name = "when camera upload enabled status is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that camera uploads enabled status changes when refreshCameraUploadsSettings is invoked`(
        isEnabled: Boolean,
    ) =
        runTest {
            setupUnderTest()
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(isEnabled)
            whenever(hasMediaPermissionUseCase()).thenReturn(!isEnabled)
            whenever(checkEnableCameraUploadsStatus()).thenReturn(EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)
            underTest.toggleCameraUploadsSettings()
            if (isEnabled) {
                verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.StopAndDisable)
            } else {
                verify(checkEnableCameraUploadsStatus).invoke()
            }
            underTest.state.test {
                assertThat(awaitItem().isCameraUploadsEnabled).isEqualTo(!isEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "is new video compression size dialog shown: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the new video compression size dialog input has correct visibility`(showDialog: Boolean) =
        runTest {
            setupUnderTest()
            underTest.showNewVideoCompressionSizeDialog(showDialog)
            underTest.state.test {
                assertThat(awaitItem().showNewVideoCompressionSizePrompt).isEqualTo(showDialog)
            }
        }

    @Test
    fun `test that the prompt to clear the new video compression size dialog input has been acknowledged`() =
        runTest {
            setupUnderTest()
            underTest.onClearNewVideoCompressionSizeInputConsumed()
            underTest.state.test {
                assertThat(awaitItem().clearNewVideoCompressionSizeInput).isFalse()
            }
        }

    @ParameterizedTest(name = "inputted new video compression size in MB: \"{0}\"")
    @ValueSource(strings = ["", " "])
    fun `test that the new video compression size prompt is dismissed when the input is empty`(
        newVideoCompressionSize: String,
    ) = runTest {
        setupUnderTest()
        underTest.setNewVideoCompressionSize(newVideoCompressionSize)
        underTest.state.test {
            assertThat(awaitItem().showNewVideoCompressionSizePrompt).isFalse()
        }
    }

    @ParameterizedTest(name = "inputted new video compression size in MB: {0}")
    @ValueSource(strings = ["99", "1001", "0", "00"])
    fun `test that the new video compression size prompt is cleared when the input is invalid`(
        newVideoCompressionSize: String,
    ) = runTest {
        setupUnderTest()
        underTest.setNewVideoCompressionSize(newVideoCompressionSize)
        underTest.state.test {
            assertThat(awaitItem().clearNewVideoCompressionSizeInput).isTrue()
        }
    }

    @ParameterizedTest(name = "inputted new video compression size in MB: {0}")
    @ValueSource(strings = ["100", "500", "1000"])
    fun `test that the new video compression size is set and the prompt dismissed when the input is valid`(
        newVideoCompressionSizeString: String,
    ) = runTest {
        val newVideoCompressionSizeInt = newVideoCompressionSizeString.toInt()
        setupUnderTest()
        whenever(getVideoCompressionSizeLimitUseCase()).thenReturn(newVideoCompressionSizeInt)
        underTest.setNewVideoCompressionSize(newVideoCompressionSizeString)

        verify(setVideoCompressionSizeLimitUseCase).invoke(newVideoCompressionSizeInt)
        verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showNewVideoCompressionSizePrompt).isFalse()
            assertThat(state.videoCompressionSizeLimit).isEqualTo(newVideoCompressionSizeInt)
        }
    }
}
