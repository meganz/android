package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteAllPendingTransfersUseCaseTest {
    private lateinit var underTest: DeleteAllPendingTransfersUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = DeleteAllPendingTransfersUseCase(transferRepository)
    }

    @BeforeEach
    fun cleanUp() = reset(transferRepository)

    @ParameterizedTest
    @EnumSource(PendingTransferState::class)
    fun `test that repository method is called when the use case is invoked`() =
        runTest {
            underTest.invoke()
            verify(transferRepository).deleteAllPendingTransfers()
        }
}