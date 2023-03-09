package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompression
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQuality
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
    private val setUploadVideoQuality = mock<SetUploadVideoQuality>()
    private val setChargingRequiredForVideoCompression =
        mock<SetChargingRequiredForVideoCompression>()
    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = DefaultEnablePhotosCameraUpload(
            settingsRepository = settingsRepository,
            setUploadVideoQuality = setUploadVideoQuality,
            setChargingRequiredForVideoCompression = setChargingRequiredForVideoCompression,
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
            setUploadVideoQuality,
            setChargingRequiredForVideoCompression,
            cameraUploadRepository
        )

        inOrder.verify(settingsRepository).setCameraUploadLocalPath(expectedPath)
        inOrder.verify(settingsRepository).setCamSyncWifi(!expectedEnableCellularSync)
        inOrder.verify(settingsRepository).setCameraUploadFileType(expectedSyncVideo)
        inOrder.verify(settingsRepository).setCameraFolderExternalSDCard(false)
        inOrder.verify(setUploadVideoQuality).invoke(expectedVideoQuality)
        inOrder.verify(setChargingRequiredForVideoCompression).invoke(true)
        inOrder.verify(settingsRepository).setChargingOnSize(expectedConversionChargingOnSize)
        inOrder.verify(settingsRepository).setEnableCameraUpload(true)
        inOrder.verify(cameraUploadRepository).listenToNewMedia()

        inOrder.verifyNoMoreInteractions()
    }
}