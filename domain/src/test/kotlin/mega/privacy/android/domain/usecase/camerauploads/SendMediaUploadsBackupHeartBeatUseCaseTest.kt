package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [SendMediaUploadsBackupHeartBeatUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendMediaUploadsBackupHeartBeatUseCaseTest {

    private lateinit var underTest: SendMediaUploadsBackupHeartBeatUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SendMediaUploadsBackupHeartBeatUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(cameraUploadsRepository)
    }

    @Test
    internal fun `test that media uploads backup heart beat is sent when sync is enabled and backup id exists`() =
        runTest {
            whenever(cameraUploadsRepository.isSecondaryMediaFolderEnabled()).thenReturn(true)
            whenever(cameraUploadsRepository.getMuBackUpId()).thenReturn(69L)
            underTest(HeartbeatStatus.values().random(), 1L)
            verify(cameraUploadsRepository, times(1)).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }

    @Test
    internal fun `test that media uploads backup heart beat is not sent when sync is enabled and backup id does not exist`() =
        runTest {
            whenever(cameraUploadsRepository.isSecondaryMediaFolderEnabled()).thenReturn(true)
            whenever(cameraUploadsRepository.getMuBackUpId()).thenReturn(null)
            underTest(HeartbeatStatus.values().random(), 1L)
            verify(cameraUploadsRepository, times(0)).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }

    @Test
    internal fun `test that media uploads backup heart beat is not sent when sync is not enabled`() =
        runTest {
            whenever(cameraUploadsRepository.isSecondaryMediaFolderEnabled()).thenReturn(false)
            whenever(cameraUploadsRepository.getMuBackUpId()).thenReturn(69L)
            underTest(HeartbeatStatus.values().random(), 1L)
            verify(cameraUploadsRepository, times(0)).sendBackupHeartbeat(
                any(), any(), any(),
                any(), any(), any()
            )
        }
}
