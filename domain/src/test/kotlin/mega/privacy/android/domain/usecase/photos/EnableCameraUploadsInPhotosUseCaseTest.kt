package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetDefaultPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset

/**
 * Test class for [EnableCameraUploadsInPhotosUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnableCameraUploadsInPhotosUseCaseTest {

    private lateinit var underTest: EnableCameraUploadsInPhotosUseCase

    private val listenToNewMediaUseCase = mock<ListenToNewMediaUseCase>()
    private val settingsRepository = mock<SettingsRepository>()
    private val setCameraUploadsByWifiUseCase = mock<SetCameraUploadsByWifiUseCase>()
    private val setChargingRequiredForVideoCompressionUseCase =
        mock<SetChargingRequiredForVideoCompressionUseCase>()
    private val setDefaultPrimaryFolderPathUseCase = mock<SetDefaultPrimaryFolderPathUseCase>()
    private val setUploadVideoQualityUseCase = mock<SetUploadVideoQualityUseCase>()
    private val setVideoCompressionSizeLimitUseCase = mock<SetVideoCompressionSizeLimitUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = EnableCameraUploadsInPhotosUseCase(
            listenToNewMediaUseCase = listenToNewMediaUseCase,
            settingsRepository = settingsRepository,
            setCameraUploadsByWifiUseCase = setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase = setChargingRequiredForVideoCompressionUseCase,
            setDefaultPrimaryFolderPathUseCase = setDefaultPrimaryFolderPathUseCase,
            setUploadVideoQualityUseCase = setUploadVideoQualityUseCase,
            setVideoCompressionSizeLimitUseCase = setVideoCompressionSizeLimitUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            listenToNewMediaUseCase,
            settingsRepository,
            setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase,
            setDefaultPrimaryFolderPathUseCase,
            setUploadVideoQualityUseCase,
            setVideoCompressionSizeLimitUseCase,
        )
    }

    @Test
    fun `test that the process of enabling camera uploads is done in order`() = runTest {
        val expectedShouldSyncVideos = true
        val expectedShouldUseWiFiOnly = true
        val expectedVideoCompressionSizeLimit = 200
        val expectedVideoUploadQuality = VideoQuality.HIGH

        underTest(
            shouldSyncVideos = expectedShouldSyncVideos,
            shouldUseWiFiOnly = expectedShouldUseWiFiOnly,
            videoCompressionSizeLimit = expectedVideoCompressionSizeLimit,
            videoUploadQuality = expectedVideoUploadQuality,
        )

        val inOrder = inOrder(
            listenToNewMediaUseCase,
            settingsRepository,
            setCameraUploadsByWifiUseCase,
            setChargingRequiredForVideoCompressionUseCase,
            setDefaultPrimaryFolderPathUseCase,
            setUploadVideoQualityUseCase,
            setVideoCompressionSizeLimitUseCase,
        )

        inOrder.verify(setDefaultPrimaryFolderPathUseCase).invoke()
        inOrder.verify(setCameraUploadsByWifiUseCase).invoke(expectedShouldUseWiFiOnly)
        inOrder.verify(settingsRepository).setCameraUploadFileType(expectedShouldSyncVideos)
        inOrder.verify(setUploadVideoQualityUseCase).invoke(expectedVideoUploadQuality)
        inOrder.verify(setChargingRequiredForVideoCompressionUseCase).invoke(true)
        inOrder.verify(setVideoCompressionSizeLimitUseCase)
            .invoke(expectedVideoCompressionSizeLimit)
        inOrder.verify(settingsRepository).setEnableCameraUpload(true)
        inOrder.verify(listenToNewMediaUseCase).invoke()

        inOrder.verifyNoMoreInteractions()
    }
}