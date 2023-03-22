package mega.privacy.android.domain.usecase.login

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
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

    private val session = "testSession"

    @Before
    fun setUp() {
        underTest = FastLoginUseCase(
            loginRepository = loginRepository,
            initialiseMegaChatUseCase = initialiseMegaChatUseCase,
            chatLogoutUseCase = chatLogoutUseCase,
            resetChatSettingsUseCase = resetChatSettingsUseCase,
            saveAccountCredentialsUseCase = saveAccountCredentialsUseCase,
            loginMutex = mock()
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
            verify(loginRepository).fastLoginFlow(session)
            verify(saveAccountCredentialsUseCase).invoke()
        }

    @Test
    fun `test that fast login invokes chatLogout and resetChatSettings if throws RequireValidation`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenThrow(LoginRequireValidation())

            underTest.invoke(session, false, disableChatApiUseCase).test {
                assertThat(awaitError()).isInstanceOf(LoginRequireValidation::class.java)
            }

            verify(initialiseMegaChatUseCase).invoke(session)
            verify(loginRepository).fastLoginFlow(session)
            verify(chatLogoutUseCase).invoke(disableChatApiUseCase)
            verify(resetChatSettingsUseCase).invoke()
        }

    @Test
    fun `test that fast login does not invoke chatLogout and resetChatSettings if throws LoggedOutFromOtherLocation`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenThrow(LoginLoggedOutFromOtherLocation())

            underTest.invoke(session, false, disableChatApiUseCase).test {
                assertThat(awaitError()).isInstanceOf(LoginLoggedOutFromOtherLocation::class.java)
            }

            verify(initialiseMegaChatUseCase).invoke(session)
            verify(loginRepository).fastLoginFlow(session)
            verifyNoInteractions(chatLogoutUseCase)
            verifyNoInteractions(resetChatSettingsUseCase)
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
            verify(loginRepository).fastLoginFlow(session)
            verify(saveAccountCredentialsUseCase).invoke()
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
        }
}