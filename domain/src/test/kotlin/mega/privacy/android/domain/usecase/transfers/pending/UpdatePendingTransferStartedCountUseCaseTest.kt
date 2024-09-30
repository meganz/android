package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.UpdateAlreadyTransferredFilesCount
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdatePendingTransferStartedCountUseCaseTest {
    private lateinit var underTest: UpdatePendingTransferStartedCountUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = UpdatePendingTransferStartedCountUseCase(transferRepository)
    }

    @BeforeEach
    fun cleanUp() = reset(transferRepository)

    @Test
    fun `test that repository method is called with the correct parameters`() =
        runTest {
            val id = 345L
            val startedFiles = 454
            val alreadyTransferred = 3
            val pendingTransfer = mock<PendingTransfer> {
                on { this.pendingTransferId } doReturn id
            }
            underTest.invoke(pendingTransfer = pendingTransfer, startedFiles, alreadyTransferred)
            verify(transferRepository).updatePendingTransfer(
                UpdateAlreadyTransferredFilesCount(id, startedFiles, alreadyTransferred)
            )
        }
}