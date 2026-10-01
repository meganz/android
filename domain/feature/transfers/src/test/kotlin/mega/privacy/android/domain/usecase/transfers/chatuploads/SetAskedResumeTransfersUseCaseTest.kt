package mega.privacy.android.domain.usecase.transfers.chatuploads

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
class SetAskedResumeTransfersUseCaseTest {

    private lateinit var underTest: SetAskedResumeTransfersUseCase

    private val transfersRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = SetAskedResumeTransfersUseCase(transfersRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transfersRepository)
    }

    @Test
    fun `test that repository is correctly invoked`() = runTest {
        underTest()
        verify(transfersRepository).setAskedResumeTransfers()
    }
}