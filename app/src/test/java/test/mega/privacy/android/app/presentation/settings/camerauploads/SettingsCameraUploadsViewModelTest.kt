package test.mega.privacy.android.app.presentation.settings.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.UploadOptionUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.VideoQualityUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableMediaUploadsSettingsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupDefaultSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSettingUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [SettingsCameraUploadsViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SettingsCameraUploadsViewModelTest {

    private lateinit var underTest: SettingsCameraUploadsViewModel

    private val areLocationTagsEnabledUseCase = mock<AreLocationTagsEnabledUseCase>()
    private val areUploadFileNamesKeptUseCase = mock<AreUploadFileNamesKeptUseCase>()
    private val checkEnableCameraUploadsStatusUseCase =
        mock<CheckEnableCameraUploadsStatusUseCase>()
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase =
        mock<DeleteCameraUploadsTemporaryRootDirectoryUseCase>()
    private val disableMediaUploadsSettingsUseCase = mock<DisableMediaUploadsSettingsUseCase>()
    private val getSecondaryFolderPathUseCase = mock<GetSecondaryFolderPathUseCase>()
    private val getUploadOptionUseCase = mock<GetUploadOptionUseCase>()
    private val getUploadVideoQualityUseCase = mock<GetUploadVideoQualityUseCase>()
    private val getVideoCompressionSizeLimitUseCase = mock<GetVideoCompressionSizeLimitUseCase>()
    private val isCameraUploadsByWifiUseCase = mock<IsCameraUploadsByWifiUseCase>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val isChargingRequiredForVideoCompressionUseCase =
        mock<IsChargingRequiredForVideoCompressionUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()
    private val isSecondaryFolderPathValidUseCase = mock<IsSecondaryFolderPathValidUseCase>()
    private val listenToNewMediaUseCase = mock<ListenToNewMediaUseCase>()
    private val preparePrimaryFolderPathUseCase = mock<PreparePrimaryFolderPathUseCase>()
    private val setCameraUploadsByWifiUseCase = mock<SetCameraUploadsByWifiUseCase>()
    private val setChargingRequiredForVideoCompressionUseCase =
        mock<SetChargingRequiredForVideoCompressionUseCase>()
    private val setLocationTagsEnabledUseCase = mock<SetLocationTagsEnabledUseCase>()
    private val setSecondaryFolderLocalPathUseCase = mock<SetSecondaryFolderLocalPathUseCase>()
    private val setUploadFileNamesKeptUseCase = mock<SetUploadFileNamesKeptUseCase>()
    private val setUploadOptionUseCase = mock<SetUploadOptionUseCase>()
    private val setUploadVideoQualityUseCase = mock<SetUploadVideoQualityUseCase>()
    private val setVideoCompressionSizeLimitUseCase = mock<SetVideoCompressionSizeLimitUseCase>()
    private val setupCameraUploadsSettingUseCase = mock<SetupCameraUploadsSettingUseCase>()
    private val setupDefaultSecondaryFolderUseCase = mock<SetupDefaultSecondaryFolderUseCase>()
    private val setupMediaUploadsSettingUseCase = mock<SetupMediaUploadsSettingUseCase>()
    private val snackBarHandler = mock<SnackBarHandler>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val stopCameraUploadsUseCase = mock<StopCameraUploadsUseCase>()
    private val uploadOptionUiItemMapper = Mockito.spy(UploadOptionUiItemMapper())
    private val videoQualityUiItemMapper = Mockito.spy(VideoQualityUiItemMapper())

    @BeforeEach
    fun resetMocks() {
        reset(
            areLocationTagsEnabledUseCase,
            areUploadFileNamesKeptUseCase,
            checkEnableCameraUploadsStatusUseCase,
            deleteCameraUploadsTemporaryRootDirectoryUseCase,
            disableMediaUploadsSettingsUseCase,
            getSecondaryFolderPathUseCase,
            getUploadOptionUseCase,
            getUploadVideoQualityUseCase,
            getVideoCompressionSizeLimitUseCase,
            isCameraUploadsByWifiUseCase,
            isCameraUploadsEnabledUseCase,
            isChargingRequiredForVideoCompressionUseCase,
            isConnectedToInternetUseCase,
            isSecondaryFolderEnabled,
            isSecondaryFolderPathValidUseCase,
            listenToNewMediaUseCase,
            preparePrimaryFolderPathUseCase,
            setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase,
            setLocationTagsEnabledUseCase,
            setSecondaryFolderLocalPathUseCase,
            setUploadFileNamesKeptUseCase,
            setUploadOptionUseCase,
            setUploadVideoQualityUseCase,
            setVideoCompressionSizeLimitUseCase,
            setupCameraUploadsSettingUseCase,
            setupDefaultSecondaryFolderUseCase,
            setupMediaUploadsSettingUseCase,
            snackBarHandler,
            startCameraUploadUseCase,
            stopCameraUploadsUseCase,
        )
    }

    /**
     * Mocks some Use Cases and creates a new instance of [underTest]
     */
    private suspend fun initializeUnderTest(
        isCameraUploadsByWifi: Boolean = true,
        isCameraUploadsEnabled: Boolean = true,
        isMediaUploadsEnabled: Boolean = true,
        maximumNonChargingVideoCompressionSize: Int = 500,
        requireChargingDuringVideoCompression: Boolean = true,
        secondaryFolderPath: String = "secondary/folder/path",
        shouldIncludeLocationTags: Boolean = true,
        shouldKeepUploadFileNames: Boolean = true,
        uploadOption: UploadOption = UploadOption.PHOTOS,
        videoQuality: VideoQuality = VideoQuality.ORIGINAL,
    ) {
        whenever(areLocationTagsEnabledUseCase()).thenReturn(shouldIncludeLocationTags)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)
        whenever(areUploadFileNamesKeptUseCase()).thenReturn(shouldKeepUploadFileNames)
        whenever(getUploadOptionUseCase()).thenReturn(uploadOption)
        whenever(getUploadVideoQualityUseCase()).thenReturn(videoQuality)
        whenever(getVideoCompressionSizeLimitUseCase()).thenReturn(
            maximumNonChargingVideoCompressionSize
        )
        whenever(isCameraUploadsByWifiUseCase()).thenReturn(isCameraUploadsByWifi)
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(isCameraUploadsEnabled)
        whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(
            requireChargingDuringVideoCompression
        )
        whenever(isSecondaryFolderEnabled()).thenReturn(isMediaUploadsEnabled)

        underTest = SettingsCameraUploadsViewModel(
            areLocationTagsEnabledUseCase = areLocationTagsEnabledUseCase,
            areUploadFileNamesKeptUseCase = areUploadFileNamesKeptUseCase,
            checkEnableCameraUploadsStatusUseCase = checkEnableCameraUploadsStatusUseCase,
            deleteCameraUploadsTemporaryRootDirectoryUseCase = deleteCameraUploadsTemporaryRootDirectoryUseCase,
            disableMediaUploadsSettingsUseCase = disableMediaUploadsSettingsUseCase,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
            getUploadOptionUseCase = getUploadOptionUseCase,
            getUploadVideoQualityUseCase = getUploadVideoQualityUseCase,
            getVideoCompressionSizeLimitUseCase = getVideoCompressionSizeLimitUseCase,
            isCameraUploadsByWifiUseCase = isCameraUploadsByWifiUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isChargingRequiredForVideoCompressionUseCase = isChargingRequiredForVideoCompressionUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            isSecondaryFolderPathValidUseCase = isSecondaryFolderPathValidUseCase,
            listenToNewMediaUseCase = listenToNewMediaUseCase,
            preparePrimaryFolderPathUseCase = preparePrimaryFolderPathUseCase,
            setCameraUploadsByWifiUseCase = setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase = setChargingRequiredForVideoCompressionUseCase,
            setLocationTagsEnabledUseCase = setLocationTagsEnabledUseCase,
            setSecondaryFolderLocalPathUseCase = setSecondaryFolderLocalPathUseCase,
            setUploadFileNamesKeptUseCase = setUploadFileNamesKeptUseCase,
            setUploadOptionUseCase = setUploadOptionUseCase,
            setUploadVideoQualityUseCase = setUploadVideoQualityUseCase,
            setVideoCompressionSizeLimitUseCase = setVideoCompressionSizeLimitUseCase,
            setupCameraUploadsSettingUseCase = setupCameraUploadsSettingUseCase,
            setupDefaultSecondaryFolderUseCase = setupDefaultSecondaryFolderUseCase,
            setupMediaUploadsSettingUseCase = setupMediaUploadsSettingUseCase,
            snackBarHandler = snackBarHandler,
            startCameraUploadUseCase = startCameraUploadUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            uploadOptionUiItemMapper = uploadOptionUiItemMapper,
            videoQualityUiItemMapper = videoQualityUiItemMapper,
        )
    }

    /**
     * The Test Group that verifies behaviors when changing the Camera Uploads State. The
     * functionality is triggered when the User clicks the Camera Uploads Switch
     */
    @Nested
    @DisplayName("Camera Uploads")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class CameraUploadsTestGroup {

        @Test
        fun `test that an error snackbar is shown when the user disables camera uploads and is not connected to the internet`() =
            runTest {
                initializeUnderTest()
                whenever(isConnectedToInternetUseCase()).thenReturn(false)

                underTest.onCameraUploadsStateChanged(newState = false)

                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that an error snackbar is shown when disabling camera uploads throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(isConnectedToInternetUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onCameraUploadsStateChanged(newState = false) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that camera uploads is now disabled`() = runTest {
            initializeUnderTest()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)

            underTest.onCameraUploadsStateChanged(newState = false)

            // The Use Case is called two times: one during ViewModel initialization and another
            // when the ViewModel function is called
            verify(isCameraUploadsEnabledUseCase, times(2)).invoke()
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.StopAndDisable)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isCameraUploadsEnabled).isFalse()
                assertThat(state.isMediaUploadsEnabled).isFalse()
            }
        }

        @Test
        fun `test that an error snackbar is shown when the user enables camera uploads and is not connected to the internet`() =
            runTest {
                initializeUnderTest(isCameraUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenReturn(false)

                underTest.onCameraUploadsStateChanged(newState = true)

                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that an error snackbar is shown when enabling camera uploads throws an exception`() =
            runTest {
                initializeUnderTest(isCameraUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onCameraUploadsStateChanged(newState = true) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that enabling camera uploads will check if the media permissions are granted or not`() =
            runTest {
                initializeUnderTest(isCameraUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenReturn(true)

                underTest.onCameraUploadsStateChanged(newState = true)

                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.requestPermissions).isEqualTo(triggered)
                }
            }

        @ParameterizedTest(name = "new request permissions state event: {0}")
        @MethodSource("provideNewRequestPermissionsStateEventParams")
        fun `test that the request permissions state is updated`(newState: StateEvent) =
            runTest {
                initializeUnderTest()

                underTest.onRequestPermissionsStateChanged(newState = newState)

                underTest.uiState.test {
                    assertThat(awaitItem().requestPermissions).isEqualTo(newState)
                }
            }

        /**
         * Provides arguments for the Test that checks if the Request Permissions State is updated
         */
        private fun provideNewRequestPermissionsStateEventParams() = Stream.of(
            consumed, triggered,
        )

        @Test
        fun `test that an error snackbar is shown when checking the camera uploads status throws an exception`() =
            runTest {
                whenever(checkEnableCameraUploadsStatusUseCase()).thenThrow(
                    RuntimeException()
                )
                initializeUnderTest()

                assertDoesNotThrow { underTest.onMediaPermissionsGranted() }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that an error snackbar is shown when there are no account issues and enabling camera uploads throws an exception`() =
            runTest {
                whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(
                    EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
                )
                whenever(setupCameraUploadsSettingUseCase(any())).thenThrow(RuntimeException())
                initializeUnderTest()

                assertDoesNotThrow { underTest.onMediaPermissionsGranted() }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that camera uploads is immediately enabled when the user is on a regular or active business administrator account`() =
            runTest {
                whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(
                    EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
                )
                initializeUnderTest()

                underTest.onMediaPermissionsGranted()

                verify(setupCameraUploadsSettingUseCase).invoke(isEnabled = true)
                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.isCameraUploadsEnabled).isTrue()
                    assertThat(state.isMediaUploadsEnabled).isTrue()
                }
            }

        @Test
        fun `test that a business account prompt is shown when the business account sub user is active`() =
            runTest {
                whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(
                    EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT
                )
                initializeUnderTest()

                underTest.onMediaPermissionsGranted()

                underTest.uiState.test {
                    assertThat(awaitItem().businessAccountPromptType).isEqualTo(
                        EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT
                    )
                }
            }

        @Test
        fun `test that the business account prompt is dismissed`() = runTest {
            initializeUnderTest()

            underTest.onBusinessAccountPromptDismissed()

            underTest.uiState.test {
                assertThat(awaitItem().businessAccountPromptType).isNull()
            }
        }

        @Test
        fun `test that the business account prompt is dismissed and camera uploads is enabled when the active business account sub user acknowledges it`() =
            runTest {
                initializeUnderTest()

                underTest.onRegularBusinessAccountSubUserPromptAcknowledged()

                verify(setupCameraUploadsSettingUseCase).invoke(isEnabled = true)
                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.businessAccountPromptType).isNull()
                    assertThat(state.isCameraUploadsEnabled).isTrue()
                    assertThat(state.isMediaUploadsEnabled).isTrue()
                }
            }

        @Test
        fun `test that an error snackbar is displayed when acknowledging the business account prompt and enabling camera uploads throws an exception`() =
            runTest {
                whenever(setupCameraUploadsSettingUseCase(any())).thenThrow(RuntimeException())
                initializeUnderTest()

                assertDoesNotThrow { underTest.onRegularBusinessAccountSubUserPromptAcknowledged() }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
                underTest.uiState.test {
                    assertThat(awaitItem().businessAccountPromptType).isNull()
                }
            }

        @ParameterizedTest(name = "when the camera uploads status is {0}")
        @EnumSource(
            value = EnableCameraUploadsStatus::class,
            names = ["SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT", "SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT"]
        )
        fun `test that a suspended business account prompt is shown`(
            cameraUploadsStatus: EnableCameraUploadsStatus,
        ) = runTest {
            whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(cameraUploadsStatus)
            initializeUnderTest()

            underTest.onMediaPermissionsGranted()

            underTest.uiState.test {
                assertThat(awaitItem().businessAccountPromptType).isEqualTo(cameraUploadsStatus)
            }
        }

        @Test
        fun `test that camera uploads is not started when camera uploads is disabled and the user pauses the settings screen`() =
            runTest {
                initializeUnderTest()
                whenever(isConnectedToInternetUseCase()).thenReturn(true)

                // Disable Camera Uploads
                underTest.onCameraUploadsStateChanged(newState = false)
                // Exit the Settings Screen
                underTest.onSettingsScreenPaused()

                verifyNoInteractions(startCameraUploadUseCase, listenToNewMediaUseCase)
            }

        @Test
        fun `test that the exception is caught when attempting to start camera uploads upon pausing the settings screen`() =
            runTest {
                // This assumes that Camera Uploads is already enabled
                initializeUnderTest()
                whenever(startCameraUploadUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onSettingsScreenPaused() }
            }

        @Test
        fun `test that camera uploads is started when camera uploads is enabled and the user pauses the settings screen`() =
            runTest {
                // This assumes that Camera Uploads is already enabled
                initializeUnderTest()

                underTest.onSettingsScreenPaused()

                verify(startCameraUploadUseCase).invoke()
                verify(listenToNewMediaUseCase).invoke(forceEnqueue = false)
            }
    }

    /**
     * The Test Group that verifies behaviors when changing the Internet Connection Type used by Camera
     * Uploads to upload content. The functionality is triggered when the User clicks the How to
     * Upload Tile
     */
    @Nested
    @DisplayName("How to Upload")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class HowToUploadTestGroup {

        @Test
        fun `test that an error snackbar is displayed when changing the upload connection type throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setCameraUploadsByWifiUseCase(any())).thenThrow(RuntimeException())

                assertDoesNotThrow {
                    underTest.onHowToUploadPromptOptionSelected(UploadConnectionType.WIFI_OR_MOBILE_DATA)
                }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @ParameterizedTest(name = "new upload connection type: {0}")
        @EnumSource(UploadConnectionType::class)
        fun `test that changing the upload connection type stops camera uploads`(
            uploadConnectionType: UploadConnectionType,
        ) = runTest {
            initializeUnderTest()

            underTest.onHowToUploadPromptOptionSelected(uploadConnectionType)

            verify(setCameraUploadsByWifiUseCase).invoke(uploadConnectionType == UploadConnectionType.WIFI)
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.uploadConnectionType).isEqualTo(uploadConnectionType)
            }
        }
    }

    /**
     * The Test Group that verifies behaviors when changing the type of content being uploaded by
     * Camera Uploads. The functionality is triggered when the User clicks the File Upload Tile
     */
    @Nested
    @DisplayName("File Upload")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class FileUploadTestGroup {
        @Test
        fun `test that an error snackbar is displayed when changing the upload option throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setUploadOptionUseCase(any())).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onUploadOptionUiItemSelected(UploadOptionUiItem.PhotosOnly) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @ParameterizedTest(name = "new upload option ui item: {0}")
        @EnumSource(UploadOptionUiItem::class)
        fun `test that changing the upload option clears the internal cache and stops camera uploads`(
            uploadOptionUiItem: UploadOptionUiItem,
        ) = runTest {
            initializeUnderTest()

            underTest.onUploadOptionUiItemSelected(uploadOptionUiItem)

            verify(setUploadOptionUseCase).invoke(uploadOptionUiItem.uploadOption)
            verify(deleteCameraUploadsTemporaryRootDirectoryUseCase).invoke()
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            underTest.uiState.test {
                assertThat(awaitItem().uploadOptionUiItem).isEqualTo(uploadOptionUiItem)
            }
        }
    }

    /**
     * The Test Group that verifies behaviors when changing the Video Quality of Videos being
     * uploaded by Camera Uploads. The functionality is triggered when the User clicks the Video
     * Quality Tile
     */
    @Nested
    @DisplayName("Video Quality")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class VideoQualityTestGroup {
        @Test
        fun `test that an error snackbar is displayed when changing the video quality throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setUploadVideoQualityUseCase(any())).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onVideoQualityUiItemSelected(VideoQualityUiItem.High) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @ParameterizedTest(name = "new video quality ui item: {0}")
        @EnumSource(VideoQualityUiItem::class)
        fun `test that changing the video quality stops camera uploads`(
            videoQualityUiItem: VideoQualityUiItem,
        ) = runTest {
            initializeUnderTest()

            underTest.onVideoQualityUiItemSelected(videoQualityUiItem)

            verify(setUploadVideoQualityUseCase).invoke(videoQualityUiItem.videoQuality)
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            underTest.uiState.test {
                assertThat(awaitItem().videoQualityUiItem).isEqualTo(videoQualityUiItem)
            }
        }
    }

    /**
     * The Test Group that verifies behaviors when deciding whether or not the existing filenames are
     * used when uploading content
     */
    @Nested
    @DisplayName("Keep File Names As In The Device")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class KeepFileNamesTestGroup {
        @Test
        fun `test that an error snackbar is displayed when changing the keep file names state throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setUploadFileNamesKeptUseCase(any())).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onKeepFileNamesStateChanged(false) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @ParameterizedTest(name = "new keep upload file names state: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that changing the keep file names state stops camera uploads`(
            shouldKeepUploadFileNames: Boolean,
        ) = runTest {
            initializeUnderTest()

            underTest.onKeepFileNamesStateChanged(shouldKeepUploadFileNames)

            verify(setUploadFileNamesKeptUseCase).invoke(shouldKeepUploadFileNames)
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            underTest.uiState.test {
                assertThat(awaitItem().shouldKeepUploadFileNames).isEqualTo(
                    shouldKeepUploadFileNames
                )
            }
        }
    }

    /**
     * The Test Group that verifies behaviors when Location Tags are included / excluded when
     * uploading Photos
     */
    @Nested
    @DisplayName("Include Location Tags")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class IncludeLocationTagsTestGroup {
        @Test
        fun `test that an error snackbar is displayed when changing the include location tags state throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setLocationTagsEnabledUseCase(any())).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onIncludeLocationTagsStateChanged(false) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @ParameterizedTest(name = "new include location tags state: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that changing the include location tags state stops camera uploads`(
            shouldIncludeLocationTags: Boolean,
        ) = runTest {
            initializeUnderTest()

            underTest.onIncludeLocationTagsStateChanged(shouldIncludeLocationTags)

            verify(setLocationTagsEnabledUseCase).invoke(shouldIncludeLocationTags)
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            underTest.uiState.test {
                assertThat(awaitItem().shouldIncludeLocationTags).isEqualTo(
                    shouldIncludeLocationTags
                )
            }
        }
    }

    /**
     * The Test Group that verifies behaviors when Device Charging is enabled / disabled when
     * compressing Videos
     */
    @Nested
    @DisplayName("Require Charging During Video Compression")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class RequireChargingDuringVideoCompressionTestGroup {
        @Test
        fun `test that an error snackbar is displayed when changing the device charging state throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setChargingRequiredForVideoCompressionUseCase(any())).thenThrow(
                    RuntimeException()
                )

                assertDoesNotThrow { underTest.onChargingDuringVideoCompressionStateChanged(false) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @ParameterizedTest(name = "new device charging state during video compression: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that changing the device charging state during video compression stops camera uploads`(
            requireChargingDuringVideoCompression: Boolean,
        ) = runTest {
            initializeUnderTest()

            underTest.onChargingDuringVideoCompressionStateChanged(
                requireChargingDuringVideoCompression
            )

            verify(setChargingRequiredForVideoCompressionUseCase).invoke(
                requireChargingDuringVideoCompression
            )
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            underTest.uiState.test {
                assertThat(awaitItem().requireChargingDuringVideoCompression).isEqualTo(
                    requireChargingDuringVideoCompression
                )
            }
        }
    }

    /**
     * The Test Group that verifies behaviors when setting the new maximum aggregate Video Size that
     * can be compressed without having to charge the Device
     */
    @Nested
    @DisplayName("Video Compression")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class VideoCompressionTestGroup {
        @Test
        fun `test that an error snackbar is shown when changing the maximum video compression size throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setVideoCompressionSizeLimitUseCase(any())).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onNewVideoCompressionSizeLimitProvided(500) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that changing the maximum video compression size stops camera uploads`() =
            runTest {
                val expectedNewVideoCompressionSize = 500
                initializeUnderTest()

                underTest.onNewVideoCompressionSizeLimitProvided(expectedNewVideoCompressionSize)

                verify(setVideoCompressionSizeLimitUseCase).invoke(expectedNewVideoCompressionSize)
                verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
                underTest.uiState.test {
                    assertThat(awaitItem().maximumNonChargingVideoCompressionSize).isEqualTo(
                        expectedNewVideoCompressionSize
                    )
                }
            }
    }

    /**
     * The Test Group that verifies behaviors when changing the Media Uploads State
     */
    @Nested
    @DisplayName("Media Uploads")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class MediaUploadsTestGroup {
        @Test
        fun `test that an error snackbar is shown when the user disables media uploads and is not connected to the internet`() =
            runTest {
                initializeUnderTest()
                whenever(isConnectedToInternetUseCase()).thenReturn(false)

                underTest.onMediaUploadsStateChanged(enabled = false)

                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that an error snackbar is shown when disabling media uploads throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(isConnectedToInternetUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onMediaUploadsStateChanged(enabled = false) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that media uploads is now disabled`() = runTest {
            initializeUnderTest()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)

            underTest.onMediaUploadsStateChanged(enabled = false)

            verify(disableMediaUploadsSettingsUseCase).invoke()
            underTest.uiState.test {
                assertThat(awaitItem().isMediaUploadsEnabled).isFalse()
            }
        }

        @Test
        fun `test that an error snackbar is shown when the user enables media uploads and is not connected to the internet`() =
            runTest {
                initializeUnderTest(isMediaUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenReturn(false)

                underTest.onMediaUploadsStateChanged(enabled = true)

                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that an error snackbar is shown when enabling media uploads throws an exception`() =
            runTest {
                initializeUnderTest(isMediaUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onMediaUploadsStateChanged(enabled = true) }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }

        @Test
        fun `test that media uploads is now enabled with an empty secondary folder path if the existing path is invalid`() =
            runTest {
                initializeUnderTest(isMediaUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenReturn(true)
                whenever(isSecondaryFolderPathValidUseCase(any())).thenReturn(false)

                underTest.onMediaUploadsStateChanged(enabled = true)

                verify(setupDefaultSecondaryFolderUseCase).invoke()
                verify(setupMediaUploadsSettingUseCase).invoke(isEnabled = true)
                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.isMediaUploadsEnabled).isTrue()
                    assertThat(state.secondaryFolderPath).isEmpty()
                }
            }

        @Test
        fun `test that media uploads is now enabled using the existing secondary folder path if the existing path is valid`() =
            runTest {
                val expectedSecondaryFolderPath = "secondary/folder/path"
                initializeUnderTest(
                    isMediaUploadsEnabled = false,
                    secondaryFolderPath = expectedSecondaryFolderPath,
                )
                whenever(isConnectedToInternetUseCase()).thenReturn(true)
                whenever(isSecondaryFolderPathValidUseCase(any())).thenReturn(true)

                underTest.onMediaUploadsStateChanged(enabled = true)

                verify(setupDefaultSecondaryFolderUseCase).invoke()
                verify(setupMediaUploadsSettingUseCase).invoke(isEnabled = true)
                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.isMediaUploadsEnabled).isTrue()
                    assertThat(state.secondaryFolderPath).isEqualTo(expectedSecondaryFolderPath)
                }
            }

        @ParameterizedTest(name = "is media uploads enabled: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that enabling or disabling media uploads stops the ongoing camera uploads process`(
            isMediaUploadsEnabled: Boolean,
        ) = runTest {
            initializeUnderTest()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            whenever(isSecondaryFolderPathValidUseCase(any())).thenReturn(true)

            underTest.onMediaUploadsStateChanged(enabled = isMediaUploadsEnabled)

            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
        }
    }
}