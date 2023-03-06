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
import mega.privacy.android.domain.usecase.SaveAccountCredentials
import mega.privacy.android.domain.usecase.setting.ResetChatSettings
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultFastLoginTest {

    private lateinit var underTest: FastLogin

    private val loginRepository = mock<LoginRepository>()
    private val initialiseMegaChat = mock<InitialiseMegaChat>()
    private val chatLogout = mock<ChatLogout> {
        onBlocking { invoke(disableChatApi) }.thenReturn(Unit)
    }
    private val resetChatSettings = mock<ResetChatSettings>()
    private val disableChatApi = mock<DisableChatApi>()
    private val saveAccountCredentials = mock<SaveAccountCredentials>()

    private val session = "testSession"

    @Before
    fun setUp() {
        underTest = DefaultFastLogin(
            loginRepository = loginRepository,
            initialiseMegaChat = initialiseMegaChat,
            chatLogout = chatLogout,
            resetChatSettings = resetChatSettings,
            saveAccountCredentials = saveAccountCredentials,
            loginMutex = mock()
        )
    }


    @Test
    fun `test that fast login invokes chatLogout if chat initialization fails with ErrorStatus and finish with success`() =
        runTest {
            whenever(initialiseMegaChat(session)).thenThrow(ChatNotInitializedErrorStatus())
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest.invoke(session, false, disableChatApi).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                cancelAndIgnoreRemainingEvents()
            }

            verify(initialiseMegaChat).invoke(session)
            verify(chatLogout).invoke(disableChatApi)
            verify(loginRepository).fastLoginFlow(session)
            verify(saveAccountCredentials).invoke()
        }

    @Test
    fun `test that fast login invokes chatLogout and resetChatSettings if throws RequireValidation`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenThrow(LoginRequireValidation())

            underTest.invoke(session, false, disableChatApi).test {
                assertThat(awaitError()).isInstanceOf(LoginRequireValidation::class.java)
            }

            verify(initialiseMegaChat).invoke(session)
            verify(loginRepository).fastLoginFlow(session)
            verify(chatLogout).invoke(disableChatApi)
            verify(resetChatSettings).invoke()
        }

    @Test
    fun `test that fast login does not invoke chatLogout and resetChatSettings if throws LoggedOutFromOtherLocation`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenThrow(LoginLoggedOutFromOtherLocation())

            underTest.invoke(session, false, disableChatApi).test {
                assertThat(awaitError()).isInstanceOf(LoginLoggedOutFromOtherLocation::class.java)
            }

            verify(initialiseMegaChat).invoke(session)
            verify(loginRepository).fastLoginFlow(session)
            verifyNoInteractions(chatLogout)
            verifyNoInteractions(resetChatSettings)
        }

    @Test
    fun `test that fast login success without error and invokes saveAccountCredentials`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest.invoke(session, true, disableChatApi).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                cancelAndIgnoreRemainingEvents()
            }

            verify(initialiseMegaChat).invoke(session)
            verify(loginRepository).fastLoginFlow(session)
            verify(saveAccountCredentials).invoke()
        }

    @Test
    fun `test that while fast login, if refreshChatUrl is true, refreshMegaChatUrl is invoked`() =
        runTest {
            whenever(loginRepository.fastLoginFlow(session))
                .thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest.invoke(session, true, disableChatApi).test {
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

            underTest.invoke(session, false, disableChatApi).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                cancelAndIgnoreRemainingEvents()
            }

            verify(loginRepository, never()).refreshMegaChatUrl()
        }
}