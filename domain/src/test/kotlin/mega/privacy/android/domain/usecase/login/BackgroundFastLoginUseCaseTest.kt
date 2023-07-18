package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.SessionNotRetrievedException
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundFastLoginUseCaseTest {

    private lateinit var underTest: BackgroundFastLoginUseCase

    private val loginRepository = mock<LoginRepository>()
    private val initialiseMegaChatUseCase = mock<InitialiseMegaChatUseCase>()
    private val getSessionUseCase = mock<GetSessionUseCase>()
    private val getRootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val session = "User session"

    @Before
    fun setUp() {
        underTest = BackgroundFastLoginUseCase(
            loginRepository = loginRepository,
            initialiseMegaChatUseCase = initialiseMegaChatUseCase,
            getSessionUseCase = getSessionUseCase,
            getRootNodeExistsUseCase = getRootNodeExistsUseCase,
            loginMutex = mock()
        )
    }

    @Test
    fun `test that login is failed if get session null`() = runTest {
        whenever(getSessionUseCase()).thenReturn(null)
        assertThrows(SessionNotRetrievedException::class.java) {
            runBlocking { underTest() }
        }
    }

    @Test
    fun `test that login is success if get session differ null and root node exist`() = runTest {
        whenever(getSessionUseCase()).thenReturn(session)
        whenever(getRootNodeExistsUseCase()).thenReturn(true)
        val result = underTest()
        assertEquals(session, result)
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
            assertEquals(session, result)
        }
}