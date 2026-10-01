package mega.privacy.android.domain.usecase.transfers.paused

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupStatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PauseTransfersQueueUseCaseTest {

    private lateinit var underTest: PauseTransfersQueueUseCase

    private val transferRepository = mock<TransferRepository>()
    private val updateCameraUploadsBackupStatesUseCase =
        mock<UpdateCameraUploadsBackupStatesUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = PauseTransfersQueueUseCase(
            transferRepository = transferRepository,
            updateCameraUploadsBackupStatesUseCase = updateCameraUploadsBackupStatesUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository, updateCameraUploadsBackupStatesUseCase)
    }

    @ParameterizedTest(name = "when isPause = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invokes repository correctly`(
        isPause: Boolean,
    ) = runTest {
        whenever(transferRepository.pauseTransfers(isPause)).thenReturn(isPause)

        underTest(isPause)

        verify(transferRepository).pauseTransfers(isPause)
    }

    @ParameterizedTest(name = "when isPause = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invokes UpdateCameraUploadsBackupStatesUseCase correctly`(
        isPause: Boolean,
    ) = runTest {
        val newBackupState = if (isPause) BackupState.PAUSE_UPLOADS else BackupState.ACTIVE

        whenever(transferRepository.pauseTransfers(isPause)).thenReturn(isPause)

        underTest(isPause)

        verify(updateCameraUploadsBackupStatesUseCase).invoke(newBackupState)
    }

    @ParameterizedTest(name = "when isPause = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that returns correctly`(
        isPause: Boolean,
    ) = runTest {
        whenever(transferRepository.pauseTransfers(isPause)).thenReturn(isPause)

        assertThat(underTest(isPause)).isEqualTo(isPause)
    }
}