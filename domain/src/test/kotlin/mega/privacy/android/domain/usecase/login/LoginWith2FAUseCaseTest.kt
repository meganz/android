package mega.privacy.android.domain.usecase.login

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.exception.LoginWrongMultiFactorAuth
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginWith2FAUseCaseTest {

    private lateinit var underTest: LoginWith2FAUseCase

    private val loginRepository = mock<LoginRepository>()
    private val chatLogoutUseCase = mock<ChatLogoutUseCase> {
        onBlocking { invoke(disableChatApiUseCase) }.thenReturn(Unit)
    }
    private val resetChatSettingsUseCase = mock<ResetChatSettingsUseCase>()
    private val disableChatApiUseCase = mock<DisableChatApiUseCase>()
    private val saveAccountCredentialsUseCase = mock<SaveAccountCredentialsUseCase>()
    private val loginMutex = mock<Mutex>()

    private val email = "test@email.com"
    private val password = "testPassword"
    private val pin2FA = "123456"

    @BeforeEach
    fun setUp() {
        underTest = LoginWith2FAUseCase(
            loginRepository = loginRepository,
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
            resetChatSettingsUseCase,
            chatLogoutUseCase,
            saveAccountCredentialsUseCase,
            loginMutex,
        )
    }

    @Test
    fun `test that login invokes chatLogout and resetChatSettings if throws LoginRequireValidation`() =
        runTest {
            whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
                .thenReturn(flow { throw LoginRequireValidation() })

            underTest.invoke(email, password, pin2FA, disableChatApiUseCase).test {
                assertThat(awaitError()).isInstanceOf(LoginRequireValidation::class.java)
            }

            verify(chatLogoutUseCase).invoke(disableChatApiUseCase)
            verify(resetChatSettingsUseCase).invoke()
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that login does not invoke chatLogout and resetChatSettings if throws LoginWrongMultiFactorAuth`() =
        runTest {
            whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
                .thenReturn(flow { throw LoginWrongMultiFactorAuth() })

            underTest.invoke(email, password, pin2FA, disableChatApiUseCase).test {
                assertThat(awaitError()).isInstanceOf(LoginWrongMultiFactorAuth::class.java)
            }

            verifyNoInteractions(chatLogoutUseCase)
            verifyNoInteractions(resetChatSettingsUseCase)
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that login does not invoke chatLogout and resetChatSettings if throws LoggedOutFromOtherLocation`() =
        runTest {
            whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
                .thenReturn(flow { throw LoginLoggedOutFromOtherLocation() })

            underTest.invoke(email, password, pin2FA, disableChatApiUseCase).test {
                assertThat(awaitError()).isInstanceOf(LoginLoggedOutFromOtherLocation::class.java)
            }

            verifyNoInteractions(chatLogoutUseCase)
            verifyNoInteractions(resetChatSettingsUseCase)
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that login success without error and invokes saveAccountCredentials`() = runTest {
        whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
            .thenReturn(flowOf(LoginStatus.LoginSucceed))

        underTest.invoke(email, password, pin2FA, disableChatApiUseCase).test {
            assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
            cancelAndIgnoreRemainingEvents()
        }

        verify(saveAccountCredentialsUseCase).invoke()
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that exception thrown by chatLogoutUseCase is propagated`() = runTest {
        whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
            .thenReturn(flow { throw LoginRequireValidation() })
        whenever(chatLogoutUseCase.invoke(disableChatApiUseCase))
            .thenThrow(RuntimeException("chatLogout error"))

        underTest.invoke(email, password, pin2FA, disableChatApiUseCase).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(RuntimeException::class.java)
            assertThat(error).hasMessageThat().contains("chatLogout error")
        }
        verify(chatLogoutUseCase).invoke(disableChatApiUseCase)
        verifyNoInteractions(resetChatSettingsUseCase)
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that exception thrown by resetChatSettingsUseCase is propagated`() = runTest {
        whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
            .thenReturn(flow { throw LoginRequireValidation() })
        whenever(resetChatSettingsUseCase.invoke())
            .thenThrow(RuntimeException("resetChatSettings error"))

        underTest.invoke(email, password, pin2FA, disableChatApiUseCase).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(RuntimeException::class.java)
            assertThat(error).hasMessageThat().contains("resetChatSettings error")
        }
        verify(chatLogoutUseCase).invoke(disableChatApiUseCase)
        verify(resetChatSettingsUseCase).invoke()
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }

    @Test
    fun `test that exception thrown by saveAccountCredentialsUseCase is propagated`() = runTest {
        whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
            .thenReturn(flowOf(LoginStatus.LoginSucceed))
        whenever(saveAccountCredentialsUseCase.invoke())
            .thenThrow(RuntimeException("saveAccountCredentials error"))

        underTest.invoke(email, password, pin2FA, disableChatApiUseCase).test {
            val error = awaitError()
            assertThat(error).isInstanceOf(RuntimeException::class.java)
            assertThat(error).hasMessageThat().contains("saveAccountCredentials error")
        }
        verify(saveAccountCredentialsUseCase).invoke()
        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }
}
