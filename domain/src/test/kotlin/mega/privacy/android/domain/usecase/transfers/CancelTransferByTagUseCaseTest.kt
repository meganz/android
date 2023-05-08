package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfer.CancelTransferByTagUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [CancelTransferByTagUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CancelTransferByTagUseCaseTest {

    private lateinit var underTest: CancelTransferByTagUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CancelTransferByTagUseCase(
            transferRepository = transferRepository,
        )
    }

    @Test
    fun `test that transfer is cancelled when invoked`() =
        runTest {
            val tag = 123
            underTest(tag)
            verify(transferRepository).cancelTransferByTag(tag)
        }
}
