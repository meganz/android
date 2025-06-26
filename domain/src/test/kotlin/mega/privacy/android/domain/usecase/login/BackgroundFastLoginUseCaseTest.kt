package mega.privacy.android.domain.usecase.login

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.SessionNotRetrievedException
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackgroundFastLoginUseCaseTest {

    private lateinit var underTest: BackgroundFastLoginUseCase

    private val loginRepository = mock<LoginRepository>()
    private val initialiseMegaChatUseCase = mock<InitialiseMegaChatUseCase>()
    private val getSessionUseCase = mock<GetSessionUseCase>()
    private val getRootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val loginMutex = mock<Mutex>()
    private val session = "User session"

    @BeforeEach
    fun setUp() {
        underTest = BackgroundFastLoginUseCase(
            loginRepository = loginRepository,
            initialiseMegaChatUseCase = initialiseMegaChatUseCase,
            getSessionUseCase = getSessionUseCase,
            getRootNodeExistsUseCase = getRootNodeExistsUseCase,
            loginMutex = loginMutex
        )
    }

    @AfterEach
    fun tearDown() {
        // Resetting the mocks after each test
        reset(
            loginRepository,
            initialiseMegaChatUseCase,
            getSessionUseCase,
            getRootNodeExistsUseCase,
            loginMutex,
        )
    }

    @Test
    fun `test that login is failed if get session null`() = runTest {
        whenever(getSessionUseCase()).thenReturn(null)
        assertThrows(SessionNotRetrievedException::class.java) {
            runBlocking { underTest() }
        }
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that login is success if get session differ null and root node exist`() = runTest {
        whenever(getSessionUseCase()).thenReturn(session)
        whenever(getRootNodeExistsUseCase()).thenReturn(true)
        val result = underTest()
        Truth.assertThat(result).isEqualTo(session)
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that login is failed if root node not exist and init mega chat throw exception`() =
        runTest {
            whenever(getSessionUseCase()).thenReturn(session)
            whenever(getRootNodeExistsUseCase()).thenReturn(false)
            whenever(initialiseMegaChatUseCase(session)).thenThrow(RuntimeException())
            assertThrows(RuntimeException::class.java) {
                runBlocking { underTest() }
            }
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that login is failed if root node not exist and fastLogin throw exception`() =
        runTest {
            whenever(getSessionUseCase()).thenReturn(session)
            whenever(getRootNodeExistsUseCase()).thenReturn(false)
            whenever(initialiseMegaChatUseCase(session)).thenReturn(Unit)
            whenever(loginRepository.fastLogin(session)).thenThrow(RuntimeException())
            assertThrows(RuntimeException::class.java) {
                runBlocking { underTest() }
            }
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that login is failed if root node not exist and fetchNodes throw exception`() =
        runTest {
            whenever(getSessionUseCase()).thenReturn(session)
            whenever(getRootNodeExistsUseCase()).thenReturn(false)
            whenever(initialiseMegaChatUseCase(session)).thenReturn(Unit)
            whenever(loginRepository.fastLogin(session)).thenReturn(Unit)
            whenever(loginRepository.fetchNodes()).thenThrow(RuntimeException())
            assertThrows(RuntimeException::class.java) {
                runBlocking { underTest() }
            }
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that login is success if root node not exist and init mega chat and fast login and fetch nodes success`() =
        runTest {
            whenever(getSessionUseCase()).thenReturn(session)
            whenever(getRootNodeExistsUseCase()).thenReturn(false)
            whenever(initialiseMegaChatUseCase(session)).thenReturn(Unit)
            whenever(loginRepository.fastLogin(session)).thenReturn(Unit)
            whenever(loginRepository.fetchNodes()).thenReturn(Unit)
            val result = underTest()
            Truth.assertThat(result).isEqualTo(session)
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }
}
