package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResetExtendedAccountDetailsTimestampUseCaseTest {
    private lateinit var underTest: ResetExtendedAccountDetailsTimestampUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ResetExtendedAccountDetailsTimestampUseCase(
            accountRepository = accountRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that invoke resets the extended account details timestamp in repository`() =
        runTest {
            underTest()

            verify(accountRepository).resetExtendedAccountDetailsTimestamp()
        }
}
