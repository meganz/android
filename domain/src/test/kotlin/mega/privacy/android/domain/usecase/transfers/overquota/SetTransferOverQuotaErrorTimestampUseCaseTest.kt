package mega.privacy.android.domain.usecase.transfers.overquota

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetTransferOverQuotaErrorTimestampUseCaseTest {

    private lateinit var underTest: SetTransferOverQuotaErrorTimestampUseCase

    private val transfersRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetTransferOverQuotaErrorTimestampUseCase(transfersRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transfersRepository)
    }

    @Test
    fun `test that use case calls repository`() = runTest {
        underTest()

        verify(transfersRepository).setTransferOverQuotaErrorTimestamp()
        verifyNoMoreInteractions(transfersRepository)
    }
}