package mega.privacy.android.domain.usecase.login

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.ChatNotInitializedUnknownStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.SaveAccountCredentials
import mega.privacy.android.domain.usecase.setting.ResetChatSettings
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultLoginTest {

    private lateinit var underTest: Login

    private val loginRepository = mock<LoginRepository>()
    private val chatLogout = mock<ChatLogout> {
        onBlocking { invoke(disableChatApi) }.thenReturn(Unit)
    }
    private val resetChatSettings = mock<ResetChatSettings>()
    private val disableChatApi = mock<DisableChatApi>()
    private val saveAccountCredentials = mock<SaveAccountCredentials>()

    private val email = "test@email.com"
    private val password = "testPassword"

    @Before
    fun setUp() {
        underTest = DefaultLogin(
            loginRepository = loginRepository,
            chatLogout = chatLogout,
            resetChatSettings = resetChatSettings,
            saveAccountCredentials = saveAccountCredentials,
            loginMutex = mock()
        )
    }

    @Test
    fun `test that login invokes chatLogout if chat initialization fails with ErrorStatus and finish with success`() =
        runTest {
            whenever(loginRepository.initMegaChat()).thenThrow(ChatNotInitializedErrorStatus())
            whenever(loginRepository.login(email, password))
                .thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest.invoke(email, password, disableChatApi).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                cancelAndIgnoreRemainingEvents()
            }

            verify(loginRepository).initMegaChat()
            verify(chatLogout).invoke(disableChatApi)
            verify(loginRepository).login(email, password)
            verify(saveAccountCredentials).invoke()
        }

    @Test
    fun `test that login returns LoginCannotStart if chat initialization fails with UnknownStatus`() =
        runTest {
            whenever(loginRepository.initMegaChat()).thenThrow(ChatNotInitializedUnknownStatus())

            underTest.invoke(email, password, disableChatApi).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginCannotStart)
                cancelAndIgnoreRemainingEvents()
            }

            verify(loginRepository).initMegaChat()
        }

    @Test
    fun `test that login invokes chatLogout and resetChatSettings if throws RequireValidation`() =
        runTest {
            whenever(loginRepository.login(email, password))
                .thenThrow(LoginRequireValidation())

            underTest.invoke(email, password, disableChatApi).test {
                assertThat(awaitError()).isInstanceOf(LoginRequireValidation::class.java)
            }

            verify(loginRepository).initMegaChat()
            verify(loginRepository).login(email, password)
            verify(chatLogout).invoke(disableChatApi)
            verify(resetChatSettings).invoke()
        }

    @Test
    fun `test that login does not invoke chatLogout and resetChatSettings if throws LoggedOutFromOtherLocation`() =
        runTest {
            whenever(loginRepository.login(email, password))
                .thenThrow(LoginLoggedOutFromOtherLocation())

            underTest.invoke(email, password, disableChatApi).test {
                assertThat(awaitError()).isInstanceOf(LoginLoggedOutFromOtherLocation::class.java)
            }

            verify(loginRepository).initMegaChat()
            verify(loginRepository).login(email, password)
            verifyNoInteractions(chatLogout)
            verifyNoInteractions(resetChatSettings)
        }

    @Test
    fun `test that login success without error and invokes saveAccountCredentials`() = runTest {
        whenever(loginRepository.login(email, password))
            .thenReturn(flowOf(LoginStatus.LoginSucceed))

        underTest.invoke(email, password, disableChatApi).test {
            assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
            cancelAndIgnoreRemainingEvents()
        }

        verify(loginRepository).initMegaChat()
        verify(loginRepository).login(email, password)
        verify(saveAccountCredentials).invoke()
    }
}