package mega.privacy.android.domain.usecase.transfers.completed

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteFailedOrCancelledTransfersUseCaseTest {

    private lateinit var underTest: DeleteFailedOrCancelledTransfersUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = DeleteFailedOrCancelledTransfersUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that use case invokes repository`() = runTest {
        underTest()

        verify(transferRepository).deleteFailedOrCancelledTransfers()
    }
}