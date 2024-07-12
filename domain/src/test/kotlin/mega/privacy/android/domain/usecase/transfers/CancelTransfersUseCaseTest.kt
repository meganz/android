package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CancelTransfersUseCaseTest {
    private lateinit var underTest: CancelTransfersUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CancelTransfersUseCase(
            transferRepository = transferRepository,
        )
    }

    @Test
    fun `test that cancel in the repository is invoked when invoked`() =
        runTest {
            underTest()
            verify(transferRepository).cancelTransfers()
        }
}