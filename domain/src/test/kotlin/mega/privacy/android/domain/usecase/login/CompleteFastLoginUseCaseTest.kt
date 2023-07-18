package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class CompleteFastLoginUseCaseTest {

    private lateinit var underTest: CompleteFastLoginUseCase

    private val loginRepository = mock<LoginRepository>()
    private val initialiseMegaChatUseCase = mock<InitialiseMegaChatUseCase>()
    private val session = "User session"

    @BeforeAll
    fun setUp() {
        underTest = CompleteFastLoginUseCase(
            loginRepository = loginRepository,
            initialiseMegaChatUseCase = initialiseMegaChatUseCase,
            loginMutex = mock()
        )
    }

    @Test
    fun `test that login is failed if init mega chat throw exception`() =
        runTest {
            whenever(initialiseMegaChatUseCase(session)).thenThrow(RuntimeException())
            assertThrows(RuntimeException::class.java) {
                runBlocking { underTest(session) }
            }
        }

    @Test
    fun `test that login is failed if fastLogin throw exception`() =
        runTest {
            whenever(initialiseMegaChatUseCase(session)).thenReturn(Unit)
            whenever(loginRepository.fastLogin(session)).thenThrow(RuntimeException())
            assertThrows(RuntimeException::class.java) {
                runBlocking { underTest(session) }
            }
        }

    @Test
    fun `test that login is failed if fetchNodes throw exception`() =
        runTest {
            whenever(initialiseMegaChatUseCase(session)).thenReturn(Unit)
            whenever(loginRepository.fastLogin(session)).thenReturn(Unit)
            whenever(loginRepository.fetchNodes()).thenThrow(RuntimeException())
            assertThrows(RuntimeException::class.java) {
                runBlocking { underTest(session) }
            }
        }

    @Test
    fun `test that login is success init mega chat, fast login and fetch nodes success`() =
        runTest {
            whenever(initialiseMegaChatUseCase(session)).thenReturn(Unit)
            whenever(loginRepository.fastLogin(session)).thenReturn(Unit)
            whenever(loginRepository.fetchNodes()).thenReturn(Unit)
        }
}