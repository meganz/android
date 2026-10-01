package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferState
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdatePendingTransferStateUseCaseTest {
    private lateinit var underTest: UpdatePendingTransferStateUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = UpdatePendingTransferStateUseCase(transferRepository)
    }

    @BeforeEach
    fun cleanUp() = reset(transferRepository)

    @ParameterizedTest
    @EnumSource(PendingTransferState::class)
    fun `test that repository method is called with the correct parameters`(state: PendingTransferState) =
        runTest {
            val ids = listOf(454L, 84543L, 54L)
            val pendingTransfers = ids.map {id->
                mock<PendingTransfer> {
                    on { this.pendingTransferId } doReturn id
                }
            }
            underTest.invoke(pendingTransfers = pendingTransfers, state)
            verify(transferRepository).updatePendingTransfers(
                ids.map { UpdatePendingTransferState(it, state) }
            )
        }
}