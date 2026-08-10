package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AcknowledgeLastPurgeUseCaseTest {

    private lateinit var underTest: AcknowledgeLastPurgeUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeEach
    fun setUp() {
        underTest = AcknowledgeLastPurgeUseCase(accountRepository = accountRepository)
    }

    @Test
    fun `test that invoke calls setLastPurgeAcknowledged with the given timestamp`() = runTest {
        val purgeTimestamp = 1_700_000_000L

        underTest(purgeTimestamp)

        verify(accountRepository).setLastPurgeAcknowledged(purgeTimestamp)
    }
}
