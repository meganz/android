package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test Class for [HandleLocalIpChangeUseCaseTest]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleLocalIpChangeUseCaseTest {

    private lateinit var underTest: HandleLocalIpChangeUseCase

    private val environmentRepository = mock<EnvironmentRepository>()
    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = HandleLocalIpChangeUseCase(
            accountRepository = accountRepository,
            environmentRepository = environmentRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(environmentRepository, accountRepository)
    }

    @Test
    fun `test that new ip address is set`() = runTest {
        val ipAddress = "192.168.0.1"
        whenever(environmentRepository.getLocalIpAddress()).thenReturn(ipAddress)
        underTest(shouldRetryChatConnections = false)
        verify(environmentRepository).setIpAddress(ipAddress)
    }

    @ParameterizedTest(name = "when shouldRetryChatConnections is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that api is reconnected when previous ip is null`(shouldRetryChatConnections: Boolean) =
        runTest {
            val ipAddress = "192.168.0.1"
            whenever(environmentRepository.getLocalIpAddress()).thenReturn(ipAddress)
            whenever(environmentRepository.getIpAddress()).thenReturn(null)
            underTest(shouldRetryChatConnections = shouldRetryChatConnections)
            verify(accountRepository).reconnect()
            if (shouldRetryChatConnections) {
                verify(accountRepository).retryChatPendingConnections(true)
            }
        }

    @ParameterizedTest(name = "when shouldRetryChatConnections is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that api is reconnected when previous ip and current ip are different`(
        shouldRetryChatConnections: Boolean,
    ) = runTest {
        val ipAddress = "192.168.0.1"
        val previousIpAddress = "192.168.0.2"
        whenever(environmentRepository.getLocalIpAddress()).thenReturn(ipAddress)
        whenever(environmentRepository.getIpAddress()).thenReturn(previousIpAddress)
        underTest(shouldRetryChatConnections = shouldRetryChatConnections)
        verify(accountRepository).reconnect()
        if (shouldRetryChatConnections) {
            verify(accountRepository).retryChatPendingConnections(true)
        }
    }

    @ParameterizedTest(name = "when shouldRetryChatConnections is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that pending connections are retried when previous ip is not null`(
        shouldRetryChatConnections: Boolean,
    ) = runTest {
        val ipAddress = "192.168.0.1"
        val previousIpAddress = "192.168.0.1"
        whenever(environmentRepository.getLocalIpAddress()).thenReturn(ipAddress)
        whenever(environmentRepository.getIpAddress()).thenReturn(previousIpAddress)
        underTest(shouldRetryChatConnections = shouldRetryChatConnections)
        verify(accountRepository).retryPendingConnections()
        if (shouldRetryChatConnections) {
            verify(accountRepository).retryChatPendingConnections(false)
        }
    }

    @ParameterizedTest(name = "when shouldRetryChatConnections is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that connections are not established or retried if current ip address is null`(
        shouldRetryChatConnections: Boolean,
    ) =
        runTest {
            val ipAddress = "127.0.0.1"
            val previousIpAddress = "192.168.0.1"
            whenever(environmentRepository.getLocalIpAddress()).thenReturn(ipAddress)
            whenever(environmentRepository.getIpAddress()).thenReturn(previousIpAddress)
            underTest(shouldRetryChatConnections = shouldRetryChatConnections)
            verifyNoInteractions(accountRepository)
        }

    @ParameterizedTest(name = "when shouldRetryChatConnections is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that connections are not established or retried if current ip address is equal to host address`(
        shouldRetryChatConnections: Boolean,
    ) =
        runTest {
            val previousIpAddress = "192.168.0.1"
            whenever(environmentRepository.getLocalIpAddress()).thenReturn(null)
            whenever(environmentRepository.getIpAddress()).thenReturn(previousIpAddress)
            underTest(shouldRetryChatConnections = shouldRetryChatConnections)
            verifyNoInteractions(accountRepository)
        }
}
