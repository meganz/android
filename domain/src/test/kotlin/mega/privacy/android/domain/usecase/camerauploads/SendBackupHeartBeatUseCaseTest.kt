package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SendBackupHeartBeatUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendBackupHeartBeatUseCaseTest {

    private lateinit var underTest: SendBackupHeartBeatUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SendBackupHeartBeatUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    internal fun `test that backup heart beat is send when invoked`() =
        runTest {
            val backupId = 1L
            val status = 1
            val progress = 20
            val ups = 10
            val downs = 20
            val ts = 10L
            val lastNode = 1L
            underTest(backupId, status, progress, ups, downs, ts, lastNode)
            verify(cameraUploadRepository).sendBackupHeartbeat(
                backupId,
                status,
                progress,
                ups,
                downs,
                ts,
                lastNode
            )
        }
}
