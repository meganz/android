package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SuppressPurgeTimestampUseCaseTest {

    private lateinit var underTest: SuppressPurgeTimestampUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeEach
    fun setUp() {
        underTest = SuppressPurgeTimestampUseCase(accountRepository = accountRepository)
    }

    @Test
    fun `test that invoke sets the suppressed purge timestamp on the repository`() {
        val purgeTimestamp = 1_000_000L

        underTest(purgeTimestamp)

        verify(accountRepository).setSuppressedPurgeTimestamp(purgeTimestamp)
    }
}
