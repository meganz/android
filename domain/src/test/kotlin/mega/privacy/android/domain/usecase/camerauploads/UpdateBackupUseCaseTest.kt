package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateBackupUseCaseTest {
    private lateinit var underTest: UpdateBackupUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateBackupUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    internal fun `test that both local and remote backup is done when invoked`() =
        runTest {
            val expected = 1L
            val backupId = 1L
            val backupName = "backup"
            val backupState = BackupState.ACTIVE
            whenever(cameraUploadRepository.getInvalidBackupType()).thenReturn(-1)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(
                cameraUploadRepository.updateBackup(
                    backupId,
                    cameraUploadRepository.getInvalidBackupType(),
                    cameraUploadRepository.getInvalidHandle(),
                    null,
                    backupName,
                    backupState,
                )
            ).thenReturn(expected)
            whenever(cameraUploadRepository.getBackupById(expected)).thenReturn(mock())
            val actual = underTest(backupId, null, backupName, backupState)
            assertThat(actual).isEqualTo(expected)
            verify(cameraUploadRepository).getBackupById(expected)
            verify(cameraUploadRepository).updateLocalBackup(any())
        }
}
