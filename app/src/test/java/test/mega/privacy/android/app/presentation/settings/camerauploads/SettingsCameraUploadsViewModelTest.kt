package test.mega.privacy.android.app.presentation.settings.camerauploads

import mega.privacy.android.shared.resources.R as SharedR
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.UploadOptionUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.mapper.VideoQualityUiItemMapper
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.ClearCameraUploadsRecordUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableMediaUploadsSettingsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredToUploadContentUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsFolderPathExistingUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsNewFolderNodeValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsFolderDestinationUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsSettingsActionsUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.camerauploads.PreparePrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredToUploadContentUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadFileNamesKeptUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadOptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupDefaultSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupMediaUploadsSettingUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
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
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [SettingsCameraUploadsViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SettingsCameraUploadsViewModelTest {

    private lateinit var underTest: SettingsCameraUploadsViewModel

    private val areLocationTagsEnabledUseCase = mock<AreLocationTagsEnabledUseCase>()
    private val areUploadFileNamesKeptUseCase = mock<AreUploadFileNamesKeptUseCase>()
    private val checkEnableCameraUploadsStatusUseCase =
        mock<CheckEnableCameraUploadsStatusUseCase>()
    private val clearCameraUploadsRecordUseCase = mock<ClearCameraUploadsRecordUseCase>()
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase =
        mock<DeleteCameraUploadsTemporaryRootDirectoryUseCase>()
    private val disableMediaUploadsSettingsUseCase = mock<DisableMediaUploadsSettingsUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getPrimaryFolderNodeUseCase = mock<GetPrimaryFolderNodeUseCase>()
    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()
    private val getSecondaryFolderNodeUseCase = mock<GetSecondaryFolderNodeUseCase>()
    private val getSecondaryFolderPathUseCase = mock<GetSecondaryFolderPathUseCase>()
    private val getUploadOptionUseCase = mock<GetUploadOptionUseCase>()
    private val getUploadVideoQualityUseCase = mock<GetUploadVideoQualityUseCase>()
    private val getVideoCompressionSizeLimitUseCase = mock<GetVideoCompressionSizeLimitUseCase>()
    private val isCameraUploadsByWifiUseCase = mock<IsCameraUploadsByWifiUseCase>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val isChargingRequiredForVideoCompressionUseCase =
        mock<IsChargingRequiredForVideoCompressionUseCase>()
    private val isChargingRequiredToUploadContentUseCase =
        mock<IsChargingRequiredToUploadContentUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val isFolderPathExistingUseCase = mock<IsFolderPathExistingUseCase>()
    private val isMediaUploadsEnabledUseCase = mock<IsMediaUploadsEnabledUseCase>()
    private val isNewFolderNodeValidUseCase = mock<IsNewFolderNodeValidUseCase>()
    private val isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase =
        mock<IsPrimaryFolderPathUnrelatedToSecondaryFolderUseCase>()
    private val isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase =
        mock<IsSecondaryFolderPathUnrelatedToPrimaryFolderUseCase>()
    private val isSecondaryFolderPathValidUseCase = mock<IsSecondaryFolderPathValidUseCase>()
    private val listenToNewMediaUseCase = mock<ListenToNewMediaUseCase>()
    private val monitorCameraUploadsFolderDestinationUseCase =
        mock<MonitorCameraUploadsFolderDestinationUseCase>()
    private val monitorCameraUploadsSettingsActionsUseCase =
        mock<MonitorCameraUploadsSettingsActionsUseCase>()
    private val monitorCameraUploadsStatusInfoUseCase =
        mock<MonitorCameraUploadsStatusInfoUseCase>()
    private val preparePrimaryFolderPathUseCase = mock<PreparePrimaryFolderPathUseCase>()
    private val setCameraUploadsByWifiUseCase = mock<SetCameraUploadsByWifiUseCase>()
    private val setChargingRequiredForVideoCompressionUseCase =
        mock<SetChargingRequiredForVideoCompressionUseCase>()
    private val setChargingRequiredToUploadContentUseCase =
        mock<SetChargingRequiredToUploadContentUseCase>()
    private val setLocationTagsEnabledUseCase = mock<SetLocationTagsEnabledUseCase>()
    private val setPrimaryFolderPathUseCase = mock<SetPrimaryFolderPathUseCase>()
    private val setSecondaryFolderLocalPathUseCase = mock<SetSecondaryFolderLocalPathUseCase>()
    private val setUploadFileNamesKeptUseCase = mock<SetUploadFileNamesKeptUseCase>()
    private val setUploadOptionUseCase = mock<SetUploadOptionUseCase>()
    private val setUploadVideoQualityUseCase = mock<SetUploadVideoQualityUseCase>()
    private val setVideoCompressionSizeLimitUseCase = mock<SetVideoCompressionSizeLimitUseCase>()
    private val setupCameraUploadsSettingUseCase = mock<SetupCameraUploadsSettingUseCase>()
    private val setupDefaultSecondaryFolderUseCase = mock<SetupDefaultSecondaryFolderUseCase>()
    private val setupMediaUploadsSettingUseCase = mock<SetupMediaUploadsSettingUseCase>()
    private val setupPrimaryFolderUseCase = mock<SetupPrimaryFolderUseCase>()
    private val setupSecondaryFolderUseCase = mock<SetupSecondaryFolderUseCase>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val stopCameraUploadsUseCase = mock<StopCameraUploadsUseCase>()
    private val uploadOptionUiItemMapper = Mockito.spy(UploadOptionUiItemMapper())
    private val videoQualityUiItemMapper = Mockito.spy(VideoQualityUiItemMapper())

    private val fakeMonitorCameraUploadsSettingsActionsFlow =
        MutableSharedFlow<CameraUploadsSettingsAction>()
    private val fakeMonitorCameraUploadsStatusInfoFlow =
        MutableSharedFlow<CameraUploadsStatusInfo>()

    private val applicationScope = TestScope(UnconfinedTestDispatcher())

    @BeforeEach
    fun resetMocks() {
        reset(
            areLocationTagsEnabledUseCase,
            areUploadFileNamesKeptUseCase,
            checkEnableCameraUploadsStatusUseCase,
            clearCameraUploadsRecordUseCase,
            deleteCameraUploadsTemporaryRootDirectoryUseCase,
            disableMediaUploadsSettingsUseCase,
            getFeatureFlagValueUseCase,
            getPrimaryFolderNodeUseCase,
            getPrimaryFolderPathUseCase,
            getSecondaryFolderNodeUseCase,
            getSecondaryFolderPathUseCase,
            getUploadOptionUseCase,
            getUploadVideoQualityUseCase,
            getVideoCompressionSizeLimitUseCase,
            isCameraUploadsByWifiUseCase,
            isCameraUploadsEnabledUseCase,
            isChargingRequiredForVideoCompressionUseCase,
            isChargingRequiredToUploadContentUseCase,
            isConnectedToInternetUseCase,
            isFolderPathExistingUseCase,
            isMediaUploadsEnabledUseCase,
            isNewFolderNodeValidUseCase,
            isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase,
            isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase,
            isSecondaryFolderPathValidUseCase,
            listenToNewMediaUseCase,
            monitorCameraUploadsFolderDestinationUseCase,
            monitorCameraUploadsSettingsActionsUseCase,
            monitorCameraUploadsStatusInfoUseCase,
            preparePrimaryFolderPathUseCase,
            setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase,
            setChargingRequiredToUploadContentUseCase,
            setLocationTagsEnabledUseCase,
            setPrimaryFolderPathUseCase,
            setSecondaryFolderLocalPathUseCase,
            setUploadFileNamesKeptUseCase,
            setUploadOptionUseCase,
            setUploadVideoQualityUseCase,
            setVideoCompressionSizeLimitUseCase,
            setupCameraUploadsSettingUseCase,
            setupDefaultSecondaryFolderUseCase,
            setupMediaUploadsSettingUseCase,
            setupPrimaryFolderUseCase,
            setupSecondaryFolderUseCase,
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
        primaryFolderPath: String = "primary/folder/path",
        requireChargingDuringVideoCompression: Boolean = true,
        requireChargingWhenUploadingContent: Boolean = false,
        secondaryFolderPath: String = "secondary/folder/path",
        shouldIncludeLocationTags: Boolean = true,
        shouldKeepUploadFileNames: Boolean = true,
        uploadOption: UploadOption = UploadOption.PHOTOS,
        videoQuality: VideoQuality = VideoQuality.ORIGINAL,
    ) {
        val cameraUploadsFolderNode = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(123456L))
            on { name }.thenReturn("Camera Uploads")
        }
        val mediaUploadsFolderNode = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(789012L))
            on { name }.thenReturn("Media Uploads")
        }
        whenever(areLocationTagsEnabledUseCase()).thenReturn(shouldIncludeLocationTags)
        whenever(areUploadFileNamesKeptUseCase()).thenReturn(shouldKeepUploadFileNames)
        whenever(getPrimaryFolderNodeUseCase()).thenReturn(cameraUploadsFolderNode)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(primaryFolderPath)
        whenever(getSecondaryFolderNodeUseCase()).thenReturn(mediaUploadsFolderNode)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(secondaryFolderPath)
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
        whenever(isChargingRequiredToUploadContentUseCase()).thenReturn(
            requireChargingWhenUploadingContent
        )
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(isMediaUploadsEnabled)
        whenever(monitorCameraUploadsSettingsActionsUseCase()).thenReturn(
            fakeMonitorCameraUploadsSettingsActionsFlow
        )
        whenever(monitorCameraUploadsStatusInfoUseCase()).thenReturn(
            fakeMonitorCameraUploadsStatusInfoFlow
        )

        underTest = SettingsCameraUploadsViewModel(
            applicationScope = applicationScope,
            areLocationTagsEnabledUseCase = areLocationTagsEnabledUseCase,
            areUploadFileNamesKeptUseCase = areUploadFileNamesKeptUseCase,
            checkEnableCameraUploadsStatusUseCase = checkEnableCameraUploadsStatusUseCase,
            clearCameraUploadsRecordUseCase = clearCameraUploadsRecordUseCase,
            deleteCameraUploadsTemporaryRootDirectoryUseCase = deleteCameraUploadsTemporaryRootDirectoryUseCase,
            disableMediaUploadsSettingsUseCase = disableMediaUploadsSettingsUseCase,
            getPrimaryFolderNodeUseCase = getPrimaryFolderNodeUseCase,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            getSecondaryFolderNodeUseCase = getSecondaryFolderNodeUseCase,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
            getUploadOptionUseCase = getUploadOptionUseCase,
            getUploadVideoQualityUseCase = getUploadVideoQualityUseCase,
            getVideoCompressionSizeLimitUseCase = getVideoCompressionSizeLimitUseCase,
            isCameraUploadsByWifiUseCase = isCameraUploadsByWifiUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isChargingRequiredForVideoCompressionUseCase = isChargingRequiredForVideoCompressionUseCase,
            isChargingRequiredToUploadContentUseCase = isChargingRequiredToUploadContentUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            isFolderPathExistingUseCase = isFolderPathExistingUseCase,
            isMediaUploadsEnabledUseCase = isMediaUploadsEnabledUseCase,
            isNewFolderNodeValidUseCase = isNewFolderNodeValidUseCase,
            isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase = isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase,
            isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase = isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase,
            isSecondaryFolderPathValidUseCase = isSecondaryFolderPathValidUseCase,
            listenToNewMediaUseCase = listenToNewMediaUseCase,
            monitorCameraUploadsFolderDestinationUseCase = monitorCameraUploadsFolderDestinationUseCase,
            monitorCameraUploadsSettingsActionsUseCase = monitorCameraUploadsSettingsActionsUseCase,
            monitorCameraUploadsStatusInfoUseCase = monitorCameraUploadsStatusInfoUseCase,
            preparePrimaryFolderPathUseCase = preparePrimaryFolderPathUseCase,
            setCameraUploadsByWifiUseCase = setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase = setChargingRequiredForVideoCompressionUseCase,
            setChargingRequiredToUploadContentUseCase = setChargingRequiredToUploadContentUseCase,
            setLocationTagsEnabledUseCase = setLocationTagsEnabledUseCase,
            setPrimaryFolderPathUseCase = setPrimaryFolderPathUseCase,
            setupSecondaryFolderUseCase = setupSecondaryFolderUseCase,
            setSecondaryFolderLocalPathUseCase = setSecondaryFolderLocalPathUseCase,
            setUploadFileNamesKeptUseCase = setUploadFileNamesKeptUseCase,
            setUploadOptionUseCase = setUploadOptionUseCase,
            setUploadVideoQualityUseCase = setUploadVideoQualityUseCase,
            setVideoCompressionSizeLimitUseCase = setVideoCompressionSizeLimitUseCase,
            setupCameraUploadsSettingUseCase = setupCameraUploadsSettingUseCase,
            setupDefaultSecondaryFolderUseCase = setupDefaultSecondaryFolderUseCase,
            setupMediaUploadsSettingUseCase = setupMediaUploadsSettingUseCase,
            setupPrimaryFolderUseCase = setupPrimaryFolderUseCase,
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

                underTest.onCameraUploadsStateChanged(enabled = false)

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that an error snackbar is shown when disabling camera uploads throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(isConnectedToInternetUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onCameraUploadsStateChanged(enabled = false) }

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that camera uploads is now disabled`() = runTest {
            initializeUnderTest()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)

            underTest.onCameraUploadsStateChanged(enabled = false)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isCameraUploadsEnabled).isFalse()
                assertThat(state.isMediaUploadsEnabled).isFalse()
            }
        }

        @Test
        fun `test that disabling camera uploads stops and disables the ongoing camera uploads process`() =
            runTest {
                whenever(isConnectedToInternetUseCase()).thenReturn(true)
                initializeUnderTest()

                underTest.onCameraUploadsStateChanged(enabled = false)

                verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.StopAndDisable)
            }

        @Test
        fun `test that an error snackbar is shown when the user enables camera uploads and is not connected to the internet`() =
            runTest {
                initializeUnderTest(isCameraUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenReturn(false)

                underTest.onCameraUploadsStateChanged(enabled = true)

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that an error snackbar is shown when enabling camera uploads throws an exception`() =
            runTest {
                initializeUnderTest(isCameraUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onCameraUploadsStateChanged(enabled = true) }

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that enabling camera uploads will check if the media permissions are granted or not`() =
            runTest {
                initializeUnderTest(isCameraUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenReturn(true)

                underTest.onCameraUploadsStateChanged(enabled = true)

                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.requestMediaPermissions).isEqualTo(triggered)
                }
            }

        @ParameterizedTest(name = "is camera uploads enabled: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that camera uploads is disabled when it receives a monitor update to disable the feature`(
            isCameraUploadsEnabled: Boolean,
        ) =
            runTest {
                whenever(isConnectedToInternetUseCase()).thenReturn(true)
                initializeUnderTest(isCameraUploadsEnabled = isCameraUploadsEnabled)

                // Trigger an update to disable Camera Uploads
                fakeMonitorCameraUploadsSettingsActionsFlow.emit(CameraUploadsSettingsAction.DisableCameraUploads)

                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.isCameraUploadsEnabled).isFalse()
                    assertThat(state.isMediaUploadsEnabled).isFalse()
                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        fun `test that disabling camera uploads through a monitor update stops and disables the ongoing camera uploads process`() =
            runTest {
                whenever(isConnectedToInternetUseCase()).thenReturn(true)
                // Camera Uploads is initially enabled
                initializeUnderTest()

                // Trigger an update to disable Camera Uploads
                fakeMonitorCameraUploadsSettingsActionsFlow.emit(CameraUploadsSettingsAction.DisableCameraUploads)

                verify(
                    stopCameraUploadsUseCase,
                    atLeast(1)
                ).invoke(CameraUploadsRestartMode.StopAndDisable)
            }

        @ParameterizedTest(name = "new request media permissions state event: {0}")
        @MethodSource("provideNewRequestMediaPermissionsStateEventParams")
        fun `test that the request media permissions state is updated`(newState: StateEvent) =
            runTest {
                initializeUnderTest()

                underTest.onRequestMediaPermissionsStateChanged(newState = newState)

                underTest.uiState.test {
                    assertThat(awaitItem().requestMediaPermissions).isEqualTo(newState)
                }
            }

        /**
         * Provides arguments for the Test that checks if the Request Media Permissions State is
         * updated
         */
        private fun provideNewRequestMediaPermissionsStateEventParams() = Stream.of(
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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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
        fun `test that a snackbar is shown when enabling camera uploads and the user is on a regular or active business administrator account`() =
            runTest {
                whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(
                    EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
                )
                initializeUnderTest()

                underTest.onMediaPermissionsGranted()

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.settings_camera_notif_initializing_title))
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

                underTest.uiState.test {
                    val state = awaitItem()
                    assertThat(state.businessAccountPromptType).isNull()
                    assertThat(state.snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that the exception is caught when attempting to start the camera uploads process`() =
            runTest {
                initializeUnderTest()
                whenever(startCameraUploadUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onCameraUploadsProcessStarted() }
            }

        @Test
        fun `test that the camera uploads process is started`() =
            runTest {
                initializeUnderTest()

                underTest.onCameraUploadsProcessStarted()

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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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
     * The Test Group that verifies behaviors when Device Charging is enabled / disabled when
     * the active Camera Uploads begins uploading content
     */
    @Nested
    @DisplayName("Upload only while charging")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class UploadOnlyWhileChargingTestGroup {
        @Test
        fun `test that an error snackbar is displayed when changing the device charging state throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setChargingRequiredToUploadContentUseCase(any())).thenThrow(
                    RuntimeException()
                )

                assertDoesNotThrow { underTest.onChargingWhenUploadingContentStateChanged(false) }

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @ParameterizedTest(name = "new device charging state when uploading content: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that the device charging state is changed`(requireChargingWhenUploadingContent: Boolean) =
            runTest {
                initializeUnderTest()

                underTest.onChargingWhenUploadingContentStateChanged(
                    requireChargingWhenUploadingContent
                )

                verify(setChargingRequiredToUploadContentUseCase).invoke(
                    requireChargingWhenUploadingContent
                )
                underTest.uiState.test {
                    assertThat(awaitItem().requireChargingWhenUploadingContent).isEqualTo(
                        requireChargingWhenUploadingContent
                    )
                }
            }

        @Test
        fun `test that an error snackbar is shown when camera uploads finishes because the device is not charged`() =
            runTest {
                val cameraUploadsStatusInfo =
                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET)
                initializeUnderTest()

                fakeMonitorCameraUploadsStatusInfoFlow.emit(cameraUploadsStatusInfo)

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(SharedR.string.camera_uploads_phone_not_charging_message))
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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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
        fun `test that an error snackbar is displayed when disabling the option throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setLocationTagsEnabledUseCase(false)).thenThrow(RuntimeException())

                underTest.onIncludeLocationTagsStateChanged(false)

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that location tags are no longer included in uploaded photos when disabling the option`() =
            runTest {
                initializeUnderTest()

                underTest.onIncludeLocationTagsStateChanged(false)

                verify(setLocationTagsEnabledUseCase).invoke(false)
                underTest.uiState.test {
                    assertThat(awaitItem().shouldIncludeLocationTags).isFalse()
                }
            }

        @Test
        fun `test that disabling the option stops camera uploads`() = runTest {
            initializeUnderTest()

            underTest.onIncludeLocationTagsStateChanged(false)

            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
        }

        @Test
        fun `test that the media location permission is requested when enabling the option`() =
            runTest {
                initializeUnderTest()

                underTest.onIncludeLocationTagsStateChanged(true)

                underTest.uiState.test {
                    assertThat(awaitItem().requestLocationPermission).isEqualTo(triggered)
                }
            }

        @Test
        fun `test that an error snackbar is displayed when successfully enabling the option throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(setLocationTagsEnabledUseCase(true)).thenThrow(RuntimeException())

                underTest.onIncludeLocationTagsStateChanged(true)
                underTest.onLocationPermissionGranted()

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that location tags are now included in uploaded photos when the media location permission is granted`() =
            runTest {
                initializeUnderTest()

                underTest.onIncludeLocationTagsStateChanged(true)
                underTest.onLocationPermissionGranted()

                verify(setLocationTagsEnabledUseCase).invoke(true)
                underTest.uiState.test {
                    assertThat(awaitItem().shouldIncludeLocationTags).isTrue()
                }
            }

        @Test
        fun `test that successfully enabling the option stops camera uploads`() = runTest {
            initializeUnderTest()

            underTest.onIncludeLocationTagsStateChanged(true)
            underTest.onLocationPermissionGranted()

            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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
     * The Test Group that verifies behaviors for both Camera Uploads / Media Uploads Local Folders
     */
    @Nested
    @DisplayName("Local Folder Selection")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class LocalFolderSelectionTestGroup {
        @Test
        fun `test that the related new local folder warning prompt is dismissed`() = runTest {
            initializeUnderTest()

            underTest.onRelatedNewLocalFolderWarningDismissed()

            underTest.uiState.test {
                assertThat(awaitItem().showRelatedNewLocalFolderWarning).isFalse()
            }
        }
    }

    /**
     * The Test Group that verifies behaviors when changing the Camera Uploads Local Primary Folder
     */
    @Nested
    @DisplayName("Camera Uploads - Local Folder Selection")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class CameraUploadsLocalFolderSelectionTestGroup {
        @Test
        fun `test that an error snackbar is shown when changing the local primary folder throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(any())).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onLocalPrimaryFolderSelected("new/primary/folder/path") }

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that an error snackbar is shown when the new local primary folder is null`() =
            runTest {
                initializeUnderTest()

                underTest.onLocalPrimaryFolderSelected(null)

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.error_invalid_folder_selected))
                }
            }

        @Test
        fun `test that an error snackbar is shown when the new local primary folder does not exist`() =
            runTest {
                val newPath = "new/primary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenReturn(false)

                underTest.onLocalPrimaryFolderSelected(newPath)

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.error_invalid_folder_selected))
                }
            }

        @Test
        fun `test that a warning prompt is shown when the new local primary folder exists but is related to the local secondary folder`() =
            runTest {
                val newPath = "new/primary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenReturn(true)
                whenever(isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(newPath)).thenReturn(
                    false
                )

                underTest.onLocalPrimaryFolderSelected(newPath)

                underTest.uiState.test {
                    assertThat(awaitItem().showRelatedNewLocalFolderWarning).isTrue()
                }
            }

        @Test
        fun `test that the new local primary folder is set`() = runTest {
            val newPath = "new/primary/folder/path"
            initializeUnderTest()
            whenever(isFolderPathExistingUseCase(newPath)).thenReturn(true)
            whenever(isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(newPath)).thenReturn(true)

            underTest.onLocalPrimaryFolderSelected(newPath)

            verify(setPrimaryFolderPathUseCase).invoke(newPath)
            underTest.uiState.test {
                assertThat(awaitItem().primaryFolderPath).isEqualTo(newPath)
            }
        }

        @Test
        fun `test that setting the new local primary folder also deletes the camera uploads temporary root directory`() =
            runTest {
                val newPath = "new/primary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenReturn(true)
                whenever(isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(newPath)).thenReturn(
                    true
                )

                underTest.onLocalPrimaryFolderSelected(newPath)

                verify(deleteCameraUploadsTemporaryRootDirectoryUseCase).invoke()
            }

        @Test
        fun `test that setting the new local primary folder also clears the primary folder records`() =
            runTest {
                val newPath = "new/primary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenReturn(true)
                whenever(isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(newPath)).thenReturn(
                    true
                )

                underTest.onLocalPrimaryFolderSelected(newPath)

                verify(clearCameraUploadsRecordUseCase).invoke(listOf(CameraUploadFolderType.Primary))
            }

        @Test
        fun `test that setting the new local primary folder also stops the ongoing camera uploads process`() =
            runTest {
                val newPath = "new/primary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenReturn(true)
                whenever(isPrimaryFolderPathUnrelatedToSecondaryFolderUseCase(newPath)).thenReturn(
                    true
                )

                underTest.onLocalPrimaryFolderSelected(newPath)

                verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            }
    }

    /**
     * The Test Group that verifies behaviors when changing the Camera Uploads Primary Folder Node
     */
    @Nested
    @DisplayName("Camera Uploads - Folder Node Selection")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class CameraUploadsFolderNodeSelectionTestGroup {
        @Test
        fun `test that an error snackbar is shown when changing the primary folder node throws an exception`() =
            runTest {
                whenever(isNewFolderNodeValidUseCase(any())).thenThrow(RuntimeException())
                initializeUnderTest()

                assertDoesNotThrow { underTest.onPrimaryFolderNodeSelected(NodeId(123456L)) }

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that an error snackbar is shown when the new primary folder node is invalid`() =
            runTest {
                whenever(isNewFolderNodeValidUseCase(any())).thenReturn(false)
                initializeUnderTest()

                underTest.onPrimaryFolderNodeSelected(NodeId(123456L))

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.error_invalid_folder_selected))
                }
            }

        @Test
        fun `test that the new primary folder node is set`() = runTest {
            val primaryFolderNodeId = NodeId(123456L)
            whenever(isNewFolderNodeValidUseCase(any())).thenReturn(true)
            initializeUnderTest()

            underTest.onPrimaryFolderNodeSelected(primaryFolderNodeId)

            verify(setupPrimaryFolderUseCase).invoke(primaryFolderNodeId.longValue)
        }

        @Test
        fun `test that setting the new primary folder node also stops the ongoing camera uploads process`() =
            runTest {
                whenever(isNewFolderNodeValidUseCase(any())).thenReturn(true)
                initializeUnderTest()

                underTest.onPrimaryFolderNodeSelected(NodeId(123456L))

                verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            }

        @Test
        fun `test that the exception is caught when setting the primary folder name`() = runTest {
            val folderNodeId = NodeId(123456L)
            val folderDestinationUpdate = CameraUploadsFolderDestinationUpdate(
                nodeHandle = folderNodeId.longValue,
                cameraUploadFolderType = CameraUploadFolderType.Primary,
            )
            whenever(isNewFolderNodeValidUseCase(any())).thenReturn(true)
            whenever(getPrimaryFolderNodeUseCase(any())).thenThrow(RuntimeException())
            whenever(monitorCameraUploadsFolderDestinationUseCase()).thenReturn(
                flow {
                    emit(folderDestinationUpdate)
                    awaitCancellation()
                }
            )
            initializeUnderTest()

            assertDoesNotThrow { underTest.onPrimaryFolderNodeSelected(folderNodeId) }
        }

        @Test
        fun `test that the primary folder name is set`() =
            runTest {
                val folderName = "Camera Uploads"
                val folderNodeId = NodeId(123456L)
                val cameraUploadsNode = mock<TypedFolderNode> {
                    on { id }.thenReturn(folderNodeId)
                    on { name }.thenReturn(folderName)
                }
                val folderDestinationUpdate = CameraUploadsFolderDestinationUpdate(
                    nodeHandle = folderNodeId.longValue,
                    cameraUploadFolderType = CameraUploadFolderType.Primary,
                )

                whenever(isNewFolderNodeValidUseCase(any())).thenReturn(true)
                whenever(getPrimaryFolderNodeUseCase(any())).thenReturn(cameraUploadsNode)
                whenever(monitorCameraUploadsFolderDestinationUseCase()).thenReturn(
                    flow {
                        emit(folderDestinationUpdate)
                        awaitCancellation()
                    }
                )
                initializeUnderTest()

                underTest.onPrimaryFolderNodeSelected(folderNodeId)

                underTest.uiState.test {
                    assertThat(awaitItem().primaryFolderName).isEqualTo(folderName)
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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that an error snackbar is shown when disabling media uploads throws an exception`() =
            runTest {
                initializeUnderTest()
                whenever(isConnectedToInternetUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onMediaUploadsStateChanged(enabled = false) }

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that an error snackbar is shown when enabling media uploads throws an exception`() =
            runTest {
                initializeUnderTest(isMediaUploadsEnabled = false)
                whenever(isConnectedToInternetUseCase()).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onMediaUploadsStateChanged(enabled = true) }

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
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

        @ParameterizedTest(name = "is media uploads enabled: {0}")
        @ValueSource(booleans = [true, false])
        fun `test that media uploads is disabled when it receives a monitor update to disable the feature`(
            isMediaUploadsEnabled: Boolean,
        ) = runTest {
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            initializeUnderTest(isMediaUploadsEnabled = isMediaUploadsEnabled)

            // Trigger an update to disable Media Uploads
            fakeMonitorCameraUploadsSettingsActionsFlow.emit(CameraUploadsSettingsAction.DisableMediaUploads)

            verify(disableMediaUploadsSettingsUseCase, atLeast(1)).invoke()
            underTest.uiState.test {
                assertThat(awaitItem().isMediaUploadsEnabled).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `test that disabling media uploads through a monitor update stops the ongoing camera uploads process`() =
            runTest {
                whenever(isConnectedToInternetUseCase()).thenReturn(true)
                initializeUnderTest()

                // Trigger an update to disable Media Uploads
                fakeMonitorCameraUploadsSettingsActionsFlow.emit(CameraUploadsSettingsAction.DisableMediaUploads)

                verify(stopCameraUploadsUseCase, atLeast(1)).invoke(CameraUploadsRestartMode.Stop)
            }
    }

    /**
     * The Test Group that verifies behaviors when changing the Media Uploads Local Secondary Folder
     */
    @Nested
    @DisplayName("Media Uploads - Local Folder Selection")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class MediaUploadsLocalFolderSelectionTestGroup {
        @Test
        fun `test that an error snackbar is shown when changing the local secondary folder throws an exception`() =
            runTest {
                val newPath = "new/secondary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenThrow(RuntimeException())

                assertDoesNotThrow { underTest.onLocalSecondaryFolderSelected(newPath) }

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that an error snackbar is shown when the new local secondary folder is null`() =
            runTest {
                initializeUnderTest()

                underTest.onLocalSecondaryFolderSelected(null)

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.error_invalid_folder_selected))
                }
            }

        @Test
        fun `test that an error snackbar is shown when the new local secondary folder does not exist`() =
            runTest {
                val newPath = "new/secondary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenReturn(false)

                underTest.onLocalSecondaryFolderSelected(newPath)

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.error_invalid_folder_selected))
                }
            }

        @Test
        fun `test that a warning prompt is shown when the new local secondary folder exists but is related to the local primary folder`() =
            runTest {
                val newPath = "new/secondary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenReturn(true)
                whenever(isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase(newPath)).thenReturn(
                    false
                )

                underTest.onLocalSecondaryFolderSelected(newPath)

                underTest.uiState.test {
                    assertThat(awaitItem().showRelatedNewLocalFolderWarning).isTrue()
                }
            }

        @Test
        fun `test that the new local secondary folder is set`() = runTest {
            val newPath = "new/secondary/folder/path"
            initializeUnderTest()
            whenever(isFolderPathExistingUseCase(newPath)).thenReturn(true)
            whenever(isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase(newPath)).thenReturn(true)

            underTest.onLocalSecondaryFolderSelected(newPath)

            verify(setSecondaryFolderLocalPathUseCase).invoke(newPath)
            underTest.uiState.test {
                assertThat(awaitItem().secondaryFolderPath).isEqualTo(newPath)
            }
        }

        @Test
        fun `test that setting the new local secondary folder also clears the secondary folder records`() =
            runTest {
                val newPath = "new/secondary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenReturn(true)
                whenever(isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase(newPath)).thenReturn(
                    true
                )

                underTest.onLocalSecondaryFolderSelected(newPath)

                verify(clearCameraUploadsRecordUseCase).invoke(listOf(CameraUploadFolderType.Secondary))
            }

        @Test
        fun `test that setting the new local secondary folder also stops the ongoing camera uploads process`() =
            runTest {
                val newPath = "new/secondary/folder/path"
                initializeUnderTest()
                whenever(isFolderPathExistingUseCase(newPath)).thenReturn(true)
                whenever(isSecondaryFolderPathUnrelatedToPrimaryFolderUseCase(newPath)).thenReturn(
                    true
                )

                underTest.onLocalSecondaryFolderSelected(newPath)

                verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            }
    }

    /**
     * The Test Group that verifies behaviors when changing the Media Uploads Secondary Folder Node
     */
    @Nested
    @DisplayName("Media Uploads - Folder Node Selection")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    internal inner class MediaUploadsFolderNodeSelectionTestGroup {
        @Test
        fun `test that an error snackbar is shown when changing the secondary folder node throws an exception`() =
            runTest {
                whenever(isNewFolderNodeValidUseCase(any())).thenThrow(RuntimeException())
                initializeUnderTest()

                assertDoesNotThrow { underTest.onSecondaryFolderNodeSelected(NodeId(789012L)) }

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.general_error))
                }
            }

        @Test
        fun `test that an error snackbar is shown when the new secondary folder node is invalid`() =
            runTest {
                whenever(isNewFolderNodeValidUseCase(any())).thenReturn(false)
                initializeUnderTest()

                underTest.onSecondaryFolderNodeSelected(NodeId(789012L))

                underTest.uiState.test {
                    assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(R.string.error_invalid_folder_selected))
                }
            }

        @Test
        fun `test that the new secondary folder node is set`() = runTest {
            val secondaryFolderNodeId = NodeId(789012L)
            whenever(isNewFolderNodeValidUseCase(any())).thenReturn(true)
            initializeUnderTest()

            underTest.onSecondaryFolderNodeSelected(secondaryFolderNodeId)

            verify(setupSecondaryFolderUseCase).invoke(secondaryFolderNodeId.longValue)
        }

        @Test
        fun `test that setting the new secondary folder node also stops the ongoing camera uploads process`() =
            runTest {
                whenever(isNewFolderNodeValidUseCase(any())).thenReturn(true)
                initializeUnderTest()

                underTest.onSecondaryFolderNodeSelected(NodeId(789012L))

                verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.Stop)
            }

        @Test
        fun `test that the exception is caught when setting the secondary folder name`() = runTest {
            val folderNodeId = NodeId(789012L)
            val folderDestinationUpdate = CameraUploadsFolderDestinationUpdate(
                nodeHandle = folderNodeId.longValue,
                cameraUploadFolderType = CameraUploadFolderType.Secondary,
            )
            whenever(isNewFolderNodeValidUseCase(any())).thenReturn(true)
            whenever(getSecondaryFolderNodeUseCase(any())).thenThrow(RuntimeException())
            whenever(monitorCameraUploadsFolderDestinationUseCase()).thenReturn(
                flow {
                    emit(folderDestinationUpdate)
                    awaitCancellation()
                }
            )
            initializeUnderTest()

            assertDoesNotThrow { underTest.onSecondaryFolderNodeSelected(folderNodeId) }
        }

        @Test
        fun `test that the secondary folder name is set`() =
            runTest {
                val folderName = "Media Uploads"
                val folderNodeId = NodeId(789012L)
                val mediaUploadsNode = mock<TypedFolderNode> {
                    on { id }.thenReturn(folderNodeId)
                    on { name }.thenReturn(folderName)
                }
                val folderDestinationUpdate = CameraUploadsFolderDestinationUpdate(
                    nodeHandle = folderNodeId.longValue,
                    cameraUploadFolderType = CameraUploadFolderType.Secondary,
                )

                whenever(isNewFolderNodeValidUseCase(any())).thenReturn(true)
                whenever(getSecondaryFolderNodeUseCase(any())).thenReturn(mediaUploadsNode)
                whenever(monitorCameraUploadsFolderDestinationUseCase()).thenReturn(
                    flow {
                        emit(folderDestinationUpdate)
                        awaitCancellation()
                    }
                )
                initializeUnderTest()

                underTest.onSecondaryFolderNodeSelected(folderNodeId)

                underTest.uiState.test {
                    assertThat(awaitItem().secondaryFolderName).isEqualTo(folderName)
                }
            }
    }
}