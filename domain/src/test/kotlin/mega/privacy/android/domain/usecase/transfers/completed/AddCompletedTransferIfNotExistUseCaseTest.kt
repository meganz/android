package mega.privacy.android.domain.usecase.transfers.completed

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddCompletedTransferIfNotExistUseCaseTest {

    private lateinit var underTest: AddCompletedTransferIfNotExistUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = AddCompletedTransferIfNotExistUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that use case invokes repository`() = runTest {
        val transfer = mock<Transfer>()
        val list = listOf(transfer)
        underTest(list)
        verify(transferRepository).addCompletedTransfersIfNotExist(eq(list))
    }
}