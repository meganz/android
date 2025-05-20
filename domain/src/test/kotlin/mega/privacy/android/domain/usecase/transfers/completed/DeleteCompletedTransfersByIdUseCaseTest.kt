package mega.privacy.android.domain.usecase.transfers.completed

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteCompletedTransfersByIdUseCaseTest {

    private lateinit var underTest: DeleteCompletedTransfersByIdUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = DeleteCompletedTransfersByIdUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that use case invokes repository`() = runTest {
        val ids = listOf(1, 2, 3)

        underTest(ids)

        verify(transferRepository).deleteCompletedTransfersById(ids)
    }
}