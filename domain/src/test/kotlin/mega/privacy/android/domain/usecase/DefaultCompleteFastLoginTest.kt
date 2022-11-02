package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.ChatNotInitializedException
import mega.privacy.android.domain.exception.LoginAlreadyRunningException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.LoginRepository
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCompleteFastLoginTest {

    private lateinit var underTest: CompleteFastLogin

    private val loginRepository = mock<LoginRepository>()
    private val session = "User session"

    @Before
    fun setUp() {
        underTest = DefaultCompleteFastLogin(loginRepository)
    }

    @Test
    fun `test that LoginAlreadyRunningException is thrown if a login process is already running`() =
        runTest {
            whenever(loginRepository.isLoginAlreadyRunning()).thenReturn(true)
            assertThrows(LoginAlreadyRunningException::class.java) {
                runBlocking { underTest(session) }
            }
        }

    @Test
    fun `test that startLoginProcess is called if there is not a login process already running`() =
        runTest {
            whenever(!loginRepository.isLoginAlreadyRunning()).thenReturn(false)
            underTest(session)
            verify(loginRepository).startLoginProcess()
        }

    @Test
    fun `test that login is finished if initMegaChat request finish with an error`() = runTest {
        whenever(loginRepository.initMegaChat(session)).thenAnswer { throw ChatNotInitializedException() }
        assertThrows(ChatNotInitializedException::class.java) {
            runBlocking { underTest(session) }
        }
        verify(loginRepository).finishLoginProcess()
        assertThat(loginRepository.isLoginAlreadyRunning()).isFalse()
    }

    @Test
    fun `test that login is finished if fastLogin request finish with an error`() = runTest {
        whenever(loginRepository.fastLogin(session)).thenAnswer { throw MegaException(0, null) }
        assertThrows(MegaException::class.java) {
            runBlocking { underTest(session) }
        }
        verify(loginRepository).finishLoginProcess()
        assertThat(loginRepository.isLoginAlreadyRunning()).isFalse()
    }

    @Test
    fun `test that login is finished if fetchNodes request finish with an error`() = runTest {
        whenever(loginRepository.fetchNodes()).thenAnswer { throw MegaException(0, null) }
        assertThrows(MegaException::class.java) {
            runBlocking { underTest(session) }
        }
        verify(loginRepository).finishLoginProcess()
        assertThat(loginRepository.isLoginAlreadyRunning()).isFalse()
    }

    @Test
    fun `test that a new login is allowed if complete process finish with success`() = runTest {
        whenever(loginRepository.fetchNodes()).thenAnswer { Result.success(Unit) }
        underTest(session)
        verify(loginRepository).finishLoginProcess()
        assertThat(loginRepository.isLoginAlreadyRunning()).isFalse()
    }
}
