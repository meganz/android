package mega.privacy.android.domain.usecase.transfers.errorstatus

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
class ClearTransferErrorStatusUseCaseTest {
    private lateinit var underTest: ClearTransferErrorStatusUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = ClearTransferErrorStatusUseCase(
            transferRepository,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(transferRepository)
    }

    @Test
    fun `test that use case invokes repository clear method`() = runTest {
        underTest()

        verify(transferRepository).clearTransferErrorStatus()
    }
}