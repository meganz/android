package test.mega.privacy.android.app.presentation.settings.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.usecase.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
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

    private val checkEnableCameraUploadsStatusUseCase =
        mock<CheckEnableCameraUploadsStatusUseCase>()
    private val isCameraUploadsByWifiUseCase = mock<IsCameraUploadsByWifiUseCase>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()
    private val listenToNewMediaUseCase = mock<ListenToNewMediaUseCase>()
    private val preparePrimaryFolderPathUseCase = mock<PreparePrimaryFolderPathUseCase>()
    private val setCameraUploadsByWifiUseCase = mock<SetCameraUploadsByWifiUseCase>()
    private val setupCameraUploadsSettingUseCase = mock<SetupCameraUploadsSettingUseCase>()
    private val snackBarHandler = mock<SnackBarHandler>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val stopCameraUploadsUseCase = mock<StopCameraUploadsUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(
            checkEnableCameraUploadsStatusUseCase,
            isCameraUploadsByWifiUseCase,
            isCameraUploadsEnabledUseCase,
            isConnectedToInternetUseCase,
            isSecondaryFolderEnabled,
            listenToNewMediaUseCase,
            preparePrimaryFolderPathUseCase,
            setCameraUploadsByWifiUseCase,
            setupCameraUploadsSettingUseCase,
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
    ) {
        whenever(isCameraUploadsByWifiUseCase()).thenReturn(isCameraUploadsByWifi)
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(isCameraUploadsEnabled)
        whenever(isSecondaryFolderEnabled()).thenReturn(isMediaUploadsEnabled)

        underTest = SettingsCameraUploadsViewModel(
            checkEnableCameraUploadsStatusUseCase = checkEnableCameraUploadsStatusUseCase,
            isCameraUploadsByWifiUseCase = isCameraUploadsByWifiUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            listenToNewMediaUseCase = listenToNewMediaUseCase,
            preparePrimaryFolderPathUseCase = preparePrimaryFolderPathUseCase,
            setCameraUploadsByWifiUseCase = setCameraUploadsByWifiUseCase,
            setupCameraUploadsSettingUseCase = setupCameraUploadsSettingUseCase,
            snackBarHandler = snackBarHandler,
            startCameraUploadUseCase = startCameraUploadUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
        )
    }

    /**
     * The Test Group that verifies behaviors when changing the Camera Uploads State. The
     * functionality is triggered when the User clicks the Camera Uploads Switch
     */
    @Nested
    @DisplayName("Camera Uploads")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class CameraUploads {

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
            underTest.state.test {
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

                underTest.state.test {
                    val state = awaitItem()
                    assertThat(state.requestPermissions).isEqualTo(triggered)
                }
            }

        @ParameterizedTest(name = "show media permissions rationale: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the media permissions rationale is shown or hidden`(
            showRationale: Boolean,
        ) = runTest {
            initializeUnderTest()

            underTest.onMediaPermissionsRationaleStateChanged(showRationale = showRationale)

            underTest.state.test {
                assertThat(awaitItem().showMediaPermissionsRationale).isEqualTo(
                    showRationale
                )
            }
        }

        @ParameterizedTest(name = "new request permissions state event: {0}")
        @MethodSource("provideNewRequestPermissionsStateEventParams")
        fun `test that the request permissions state is updated`(newState: StateEvent) =
            runTest {
                initializeUnderTest()

                underTest.onRequestPermissionsStateChanged(newState = newState)

                underTest.state.test {
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
        fun `test that camera uploads is finally enabled when there are no account issues`() =
            runTest {
                whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(
                    EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
                )
                initializeUnderTest()

                underTest.onMediaPermissionsGranted()

                verify(setupCameraUploadsSettingUseCase).invoke(isEnabled = true)
                underTest.state.test {
                    val state = awaitItem()
                    assertThat(state.isCameraUploadsEnabled).isTrue()
                    assertThat(state.isMediaUploadsEnabled).isTrue()
                }
            }

        @Test
        fun `test that a business account prompt is shown when the user is on any active business account type`() =
            runTest {
                whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(
                    EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT
                )
                initializeUnderTest()

                underTest.onMediaPermissionsGranted()

                underTest.state.test {
                    assertThat(awaitItem().showBusinessAccountPrompt).isTrue()
                }
            }

        @Test
        fun `test that acknowledging the business account prompt dismisses it and finally enables camera uploads`() =
            runTest {
                initializeUnderTest()

                underTest.onBusinessAccountPromptAcknowledged()

                verify(setupCameraUploadsSettingUseCase).invoke(isEnabled = true)
                underTest.state.test {
                    val state = awaitItem()
                    assertThat(state.showBusinessAccountPrompt).isFalse()
                    assertThat(state.isCameraUploadsEnabled).isTrue()
                    assertThat(state.isMediaUploadsEnabled).isTrue()
                }
            }

        @Test
        fun `test that an error snackbar is displayed when acknowledging the business account prompt and enabling camera uploads throws an exception`() =
            runTest {
                whenever(setupCameraUploadsSettingUseCase(any())).thenThrow(RuntimeException())
                initializeUnderTest()

                assertDoesNotThrow { underTest.onBusinessAccountPromptAcknowledged() }
                verify(snackBarHandler).postSnackbarMessage(
                    resId = R.string.general_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
                underTest.state.test {
                    assertThat(awaitItem().showBusinessAccountPrompt).isFalse()
                }
            }

        @Test
        fun `test that the business account prompt is dismissed if the user does not acknowledge it`() =
            runTest {
                initializeUnderTest()

                underTest.onBusinessAccountPromptDismissed()

                underTest.state.test {
                    assertThat(awaitItem().showBusinessAccountPrompt).isFalse()
                }
            }

        @Test
        fun `test that a suspended business account sub user prompt is shown`() =
            runTest {
                whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(
                    EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT
                )
                initializeUnderTest()

                underTest.onMediaPermissionsGranted()

                underTest.state.test {
                    assertThat(awaitItem().showBusinessAccountSubUserSuspendedPrompt).isTrue()
                }
            }

        @Test
        fun `test that acknowledging the suspended business account sub user prompt dismisses it`() =
            runTest {
                initializeUnderTest()

                underTest.onBusinessAccountSubUserSuspendedPromptAcknowledged()

                underTest.state.test {
                    assertThat(awaitItem().showBusinessAccountSubUserSuspendedPrompt).isFalse()
                }
            }

        @Test
        fun `test that a suspended business account administrator prompt is shown`() =
            runTest {
                whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(
                    EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT
                )
                initializeUnderTest()

                underTest.onMediaPermissionsGranted()

                underTest.state.test {
                    assertThat(awaitItem().showBusinessAccountAdministratorSuspendedPrompt).isTrue()
                }
            }

        @Test
        fun `test that acknowledging the suspended business account administrator prompt dismisses it`() =
            runTest {
                initializeUnderTest()

                underTest.onBusinessAccountAdministratorSuspendedPromptAcknowledged()

                underTest.state.test {
                    assertThat(awaitItem().showBusinessAccountAdministratorSuspendedPrompt).isFalse()
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
     * The Test Group that verifies behaviors when changing how content is being uploaded in Camera
     * Uploads. The functionality is triggered when the User clicks the How to Upload Tile
     */
    @Nested
    @DisplayName("How to Upload")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class HowToUpload {

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
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.uploadConnectionType).isEqualTo(uploadConnectionType)
            }
        }
    }
}