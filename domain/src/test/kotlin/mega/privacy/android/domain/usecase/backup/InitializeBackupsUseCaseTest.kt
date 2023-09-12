package mega.privacy.android.domain.usecase.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadBackupIDUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetMediaUploadBackupIDUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdatePrimaryFolderBackupNameUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateSecondaryFolderBackupNameUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [InitializeBackupsUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InitializeBackupsUseCaseTest {

    private lateinit var underTest: InitializeBackupsUseCase

    private val setupDeviceNameUseCase: SetupDeviceNameUseCase = mock()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val getCameraUploadBackupIDUseCase: GetCameraUploadBackupIDUseCase = mock()
    private val setupCameraUploadsBackupUseCase: SetupCameraUploadsBackupUseCase = mock()
    private val updatePrimaryFolderBackupNameUseCase: UpdatePrimaryFolderBackupNameUseCase = mock()
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled = mock()
    private val getMediaUploadBackupIDUseCase: GetMediaUploadBackupIDUseCase = mock()
    private val setupMediaUploadsBackupUseCase: SetupMediaUploadsBackupUseCase = mock()
    private val updateSecondaryFolderBackupNameUseCase: UpdateSecondaryFolderBackupNameUseCase =
        mock()

    @BeforeAll
    fun setUp() {
        underTest = InitializeBackupsUseCase(
            setupDeviceNameUseCase,
            isCameraUploadsEnabledUseCase,
            getCameraUploadBackupIDUseCase,
            setupCameraUploadsBackupUseCase,
            updatePrimaryFolderBackupNameUseCase,
            isSecondaryFolderEnabled,
            getMediaUploadBackupIDUseCase,
            setupMediaUploadsBackupUseCase,
            updateSecondaryFolderBackupNameUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setupDeviceNameUseCase,
            isCameraUploadsEnabledUseCase,
            getCameraUploadBackupIDUseCase,
            setupCameraUploadsBackupUseCase,
            updatePrimaryFolderBackupNameUseCase,
            isSecondaryFolderEnabled,
            getMediaUploadBackupIDUseCase,
            setupMediaUploadsBackupUseCase,
            updateSecondaryFolderBackupNameUseCase,
        )
    }

    @Test
    fun `test that device name is set up`() = runTest {
        val cameraUploadName = "Camera Uploads"
        val mediaUploadName = "Media Uploads"
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
        whenever(getCameraUploadBackupIDUseCase()).thenReturn(null)
        whenever(isSecondaryFolderEnabled()).thenReturn(false)
        whenever(getMediaUploadBackupIDUseCase()).thenReturn(null)
        underTest(
            cameraUploadName = cameraUploadName,
            mediaUploadName = mediaUploadName,
        )
        verify(setupDeviceNameUseCase).invoke()
    }

    @Test
    fun `test that camera uploads is setup when camera uploads in enabled`() = runTest {
        val cameraUploadName = "Camera Uploads"
        val mediaUploadName = "Media Uploads"
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
        whenever(getCameraUploadBackupIDUseCase()).thenReturn(null)
        whenever(isSecondaryFolderEnabled()).thenReturn(false)
        whenever(getMediaUploadBackupIDUseCase()).thenReturn(null)
        underTest(
            cameraUploadName = cameraUploadName,
            mediaUploadName = mediaUploadName,
        )
        verify(setupCameraUploadsBackupUseCase).invoke(cameraUploadName)
    }

    @Test
    fun `test that camera uploads name is updated when local camera upload backup is set`() =
        runTest {
            val cameraUploadName = "Camera Uploads"
            val mediaUploadName = "Media Uploads"
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(getCameraUploadBackupIDUseCase()).thenReturn(1L)
            whenever(isSecondaryFolderEnabled()).thenReturn(false)
            whenever(getMediaUploadBackupIDUseCase()).thenReturn(null)
            underTest(
                cameraUploadName = cameraUploadName,
                mediaUploadName = mediaUploadName,
            )
            verify(updatePrimaryFolderBackupNameUseCase).invoke(cameraUploadName)
        }

    @Test
    fun `test that media uploads is setup when media uploads in enabled`() = runTest {
        val cameraUploadName = "Camera Uploads"
        val mediaUploadName = "Media Uploads"
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
        whenever(getCameraUploadBackupIDUseCase()).thenReturn(null)
        whenever(isSecondaryFolderEnabled()).thenReturn(true)
        whenever(getMediaUploadBackupIDUseCase()).thenReturn(null)
        underTest(
            cameraUploadName = cameraUploadName,
            mediaUploadName = mediaUploadName,
        )
        verify(setupMediaUploadsBackupUseCase).invoke(mediaUploadName)
    }

    @Test
    fun `test that media uploads name is updated when local media uploads backup is set`() =
        runTest {
            val cameraUploadName = "Camera Uploads"
            val mediaUploadName = "Media Uploads"
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(getCameraUploadBackupIDUseCase()).thenReturn(null)
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(getMediaUploadBackupIDUseCase()).thenReturn(2L)
            underTest(
                cameraUploadName = cameraUploadName,
                mediaUploadName = mediaUploadName,
            )
            verify(updateSecondaryFolderBackupNameUseCase).invoke(mediaUploadName)
        }
}
