package mega.privacy.android.domain.usecase.login

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.coroutines.cancellation.CancellationException

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FastLoginUseCaseTest {

    private lateinit var underTest: FastLoginUseCase

    private val loginRepository = mock<LoginRepository>()
    private val initialiseMegaChatUseCase = mock<InitialiseMegaChatUseCase>()
    private val chatLogoutUseCase = mock<ChatLogoutUseCase> {
        onBlocking { invoke(disableChatApiUseCase) }.thenReturn(Unit)
    }
    private val resetChatSettingsUseCase = mock<ResetChatSettingsUseCase>()
    private val disableChatApiUseCase = mock<DisableChatApiUseCase>()
    private val saveAccountCredentialsUseCase = mock<SaveAccountCredentialsUseCase>()
    private val loginMutex = mock<Mutex>()

    private val session = "testSession"

    @BeforeEach
    fun setUp() {
        underTest = FastLoginUseCase(
            loginRepository = loginRepository,
            initialiseMegaChatUseCase = initialiseMegaChatUseCase,
            chatLogoutUseCase = chatLogoutUseCase,
            resetChatSettingsUseCase = resetChatSettingsUseCase,
            saveAccountCredentialsUseCase = saveAccountCredentialsUseCase,
            loginMutex = loginMutex
        )
    }

    @AfterEach
    fun tearDown() {
        // Resetting the mocks after each test
        reset(
            loginRepository,
            initialiseMegaChatUseCase,
            chatLogoutUseCase,
            resetChatSettingsUseCase,
            saveAccountCredentialsUseCase,
            loginMutex
        )
    }


    @Test
    fun `test that fast login invokes chatLogout if chat initialization fails with ErrorStatus and finish with success`() =
        runTest {
            whenever(initialiseMegaChatUseCase(session)).thenThrow(ChatNotInitializedErrorStatus())
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest.invoke(session, false, disableChatApiUseCase).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                cancelAndIgnoreRemainingEvents()
            }

            verify(initialiseMegaChatUseCase).invoke(session)
            verify(chatLogoutUseCase).invoke(disableChatApiUseCase)
            verify(saveAccountCredentialsUseCase).invoke()
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that fast login invokes chatLogout and resetChatSettings if throws RequireValidation`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flow { throw LoginRequireValidation() })

            underTest.invoke(session, false, disableChatApiUseCase).test {
                assertThat(awaitError()).isInstanceOf(LoginRequireValidation::class.java)
            }

            verify(initialiseMegaChatUseCase).invoke(session)
            verify(chatLogoutUseCase).invoke(disableChatApiUseCase)
            verify(resetChatSettingsUseCase).invoke()
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that fast login does not invoke chatLogout and resetChatSettings if throws LoggedOutFromOtherLocation`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flow { throw LoginLoggedOutFromOtherLocation() })

            underTest.invoke(session, false, disableChatApiUseCase).test {
                assertThat(awaitError()).isInstanceOf(LoginLoggedOutFromOtherLocation::class.java)
            }

            verify(initialiseMegaChatUseCase).invoke(session)
            verifyNoInteractions(chatLogoutUseCase)
            verifyNoInteractions(resetChatSettingsUseCase)
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that fast login success without error and invokes saveAccountCredentials`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest.invoke(session, true, disableChatApiUseCase).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                cancelAndIgnoreRemainingEvents()
            }

            verify(initialiseMegaChatUseCase).invoke(session)
            verify(saveAccountCredentialsUseCase).invoke()
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that while fast login, if refreshChatUrl is true, refreshMegaChatUrl is invoked`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest.invoke(session, true, disableChatApiUseCase).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                cancelAndIgnoreRemainingEvents()
            }

            verify(loginRepository).refreshMegaChatUrl()
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that while fast login, if refreshChatUrl is false, refreshMegaChatUrl is not invoked`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest.invoke(session, false, disableChatApiUseCase).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                cancelAndIgnoreRemainingEvents()
            }

            verify(loginRepository, never()).refreshMegaChatUrl()
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that loginMutex is released on successful login`() = runTest {
        whenever(loginRepository.fastLoginFlow(session))
            .thenReturn(flowOf(LoginStatus.LoginSucceed))

        underTest.invoke(session, false, disableChatApiUseCase).test {
            assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
            cancelAndIgnoreRemainingEvents()
        }
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that loginMutex is released on chat initialization failure`() = runTest {
        whenever(initialiseMegaChatUseCase(session)).thenThrow(ChatNotInitializedErrorStatus())
        whenever(loginRepository.fastLoginFlow(session))
            .thenReturn(flowOf(LoginStatus.LoginSucceed))

        underTest.invoke(session, false, disableChatApiUseCase).test {
            assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
            cancelAndIgnoreRemainingEvents()
        }
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that loginMutex is released when login throws an exception`() = runTest {

        whenever(loginRepository.fastLoginFlow(session))
            .thenReturn(flow { throw LoginRequireValidation() })

        underTest.invoke(session, false, disableChatApiUseCase).test {
            assertThat(awaitError()).isInstanceOf(LoginRequireValidation::class.java)
        }
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that loginMutex is released when coroutine scope is cancelled`() = runTest {
        whenever(initialiseMegaChatUseCase(session))
            .thenThrow(CancellationException("Coroutine scope cancelled"))

        underTest.invoke(session, false, disableChatApiUseCase).catch {
            assertThat(it).isEqualTo(CancellationException::class.java)
        }.test {
            cancelAndIgnoreRemainingEvents()
        }

        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that fast login closes with error when chatLogoutUseCase throws`() = runTest {
        val exception = RuntimeException("chatLogout failed")
        whenever(initialiseMegaChatUseCase(session)).thenThrow(ChatNotInitializedErrorStatus())
        whenever(chatLogoutUseCase.invoke(disableChatApiUseCase)).thenThrow(exception)

        underTest.invoke(session, false, disableChatApiUseCase).test {
            assertThat(awaitError()).isInstanceOf(RuntimeException::class.java)
        }

        verify(initialiseMegaChatUseCase).invoke(session)
        verifyNoInteractions(loginRepository)
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that fast login closes with error when resetChatSettingsUseCase throws `() = runTest {
        whenever(resetChatSettingsUseCase()).thenThrow(ChatNotInitializedErrorStatus())
        whenever(loginRepository.fastLoginFlow(session))
            .thenReturn(flow { throw LoginRequireValidation() })

        underTest.invoke(session, false, disableChatApiUseCase).test {
            assertThat(awaitError()).isInstanceOf(ChatNotInitializedErrorStatus::class.java)
        }

        verify(initialiseMegaChatUseCase).invoke(session)
        verify(chatLogoutUseCase).invoke(disableChatApiUseCase)
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that fast login closes with error when saveAccountCredentialsUseCase throws `() =
        runTest {
            val exception = RuntimeException("saveAccountCredentials failed")
            whenever(saveAccountCredentialsUseCase()).thenThrow(exception)
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest.invoke(session, false, disableChatApiUseCase).test {
                assertThat(awaitError()).isInstanceOf(RuntimeException::class.java)
            }

            verify(initialiseMegaChatUseCase).invoke(session)
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }
}
