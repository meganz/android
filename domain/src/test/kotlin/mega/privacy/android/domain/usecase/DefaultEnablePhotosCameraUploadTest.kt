package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimit
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock

/**
 * Test class for [DefaultEnablePhotosCameraUpload]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultEnablePhotosCameraUploadTest {
    private lateinit var underTest: EnablePhotosCameraUpload

    private val settingsRepository = mock<SettingsRepository>()
    private val setCameraUploadsByWifiUseCase = mock<SetCameraUploadsByWifiUseCase>()
    private val setUploadVideoQualityUseCase = mock<SetUploadVideoQualityUseCase>()
    private val setChargingRequiredForVideoCompressionUseCase =
        mock<SetChargingRequiredForVideoCompressionUseCase>()
    private val setVideoCompressionSizeLimit = mock<SetVideoCompressionSizeLimit>()
    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = DefaultEnablePhotosCameraUpload(
            settingsRepository = settingsRepository,
            setCameraUploadsByWifiUseCase = setCameraUploadsByWifiUseCase,
            setUploadVideoQualityUseCase = setUploadVideoQualityUseCase,
            setChargingRequiredForVideoCompressionUseCase = setChargingRequiredForVideoCompressionUseCase,
            setVideoCompressionSizeLimit = setVideoCompressionSizeLimit,
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that initialisation functions are called in order`() = runTest {
        val expectedPath = "path"
        val expectedSyncVideo = true
        val expectedEnableCellularSync = true
        val expectedVideoQuality = VideoQuality.HIGH
        val expectedConversionChargingOnSize = 12

        underTest(
            path = expectedPath,
            syncVideo = expectedSyncVideo,
            enableCellularSync = expectedEnableCellularSync,
            videoQuality = expectedVideoQuality,
            conversionChargingOnSize = expectedConversionChargingOnSize,
        )

        val inOrder = inOrder(
            settingsRepository,
            setCameraUploadsByWifiUseCase,
            setUploadVideoQualityUseCase,
            setChargingRequiredForVideoCompressionUseCase,
            setVideoCompressionSizeLimit,
            cameraUploadRepository
        )

        inOrder.verify(settingsRepository).setCameraUploadLocalPath(expectedPath)
        inOrder.verify(setCameraUploadsByWifiUseCase).invoke(!expectedEnableCellularSync)
        inOrder.verify(settingsRepository).setCameraUploadFileType(expectedSyncVideo)
        inOrder.verify(settingsRepository).setCameraFolderExternalSDCard(false)
        inOrder.verify(setUploadVideoQualityUseCase).invoke(expectedVideoQuality)
        inOrder.verify(setChargingRequiredForVideoCompressionUseCase).invoke(true)
        inOrder.verify(setVideoCompressionSizeLimit).invoke(expectedConversionChargingOnSize)
        inOrder.verify(settingsRepository).setEnableCameraUpload(true)
        inOrder.verify(cameraUploadRepository).listenToNewMedia()

        inOrder.verifyNoMoreInteractions()
    }
}