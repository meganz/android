package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldAskForResumeTransfersUseCaseTest {

    private lateinit var underTest: ShouldAskForResumeTransfersUseCase

    private val transfersRepository = mock<TransferRepository>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()

    @BeforeAll
    fun setup() {
        underTest = ShouldAskForResumeTransfersUseCase(
            transfersRepository = transfersRepository,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transfersRepository, areTransfersPausedUseCase)
    }

    @Test
    fun `test that use case returns true when monitorAskedResumeTransfers value is false and areTransfersPausedUseCase is true`() =
        runTest {
            whenever(transfersRepository.monitorAskedResumeTransfers())
                .thenReturn(MutableStateFlow(false))
            whenever(areTransfersPausedUseCase()).thenReturn(true)

            assertThat(underTest()).isTrue()
        }
}