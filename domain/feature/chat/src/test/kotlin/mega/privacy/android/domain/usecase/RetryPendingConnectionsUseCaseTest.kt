package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [RetryPendingConnectionsUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryPendingConnectionsUseCaseTest {

    private lateinit var underTest: RetryPendingConnectionsUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RetryPendingConnectionsUseCase(
            accountRepository = accountRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @ParameterizedTest(name = "with disconnect {0}")
    @ValueSource(booleans = [true, false])
    internal fun `test that  connections are retried when invoked`(disconnect: Boolean) =
        runTest {
            underTest(disconnect = disconnect)
            verify(accountRepository).retryPendingConnections()
            verify(accountRepository).retryChatPendingConnections(disconnect)
        }
}
