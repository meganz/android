package mega.privacy.android.domain.usecase.login

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.exception.LoginWrongMultiFactorAuth
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
class DefaultLoginWith2FATest {

    private lateinit var underTest: LoginWith2FA

    private val loginRepository = mock<LoginRepository>()
    private val chatLogout = mock<ChatLogout> {
        onBlocking { invoke(disableChatApi) }.thenReturn(Unit)
    }
    private val resetChatSettings = mock<ResetChatSettings>()
    private val disableChatApi = mock<DisableChatApi>()
    private val saveAccountCredentials = mock<SaveAccountCredentials>()

    private val email = "test@email.com"
    private val password = "testPassword"
    private val pin2FA = "123456"

    @Before
    fun setUp() {
        underTest = DefaultLoginWith2FA(
            loginRepository = loginRepository,
            chatLogout = chatLogout,
            resetChatSettings = resetChatSettings,
            saveAccountCredentials = saveAccountCredentials,
            loginMutex = mock()
        )
    }

    @Test(expected = LoginRequireValidation::class)
    fun `test that login invokes chatLogout and resetChatSettings if throws LoginRequireValidation`() =
        runTest {
            whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
                .thenThrow(LoginRequireValidation())

            underTest.invoke(email, password, pin2FA, disableChatApi).test {
                assertThat(awaitError()).isInstanceOf(LoginRequireValidation::class.java)
            }

            verify(loginRepository).initMegaChat()
            verify(loginRepository).login(email, password)
            verify(chatLogout).invoke(disableChatApi)
            verify(resetChatSettings).invoke()
        }

    @Test(expected = LoginWrongMultiFactorAuth::class)
    fun `test that login does not invoke chatLogout and resetChatSettings if throws LoginWrongMultiFactorAuth`() =
        runTest {
            whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
                .thenThrow(LoginWrongMultiFactorAuth())

            underTest.invoke(email, password, pin2FA, disableChatApi).test {
                assertThat(awaitError()).isInstanceOf(LoginWrongMultiFactorAuth::class.java)
            }

            verify(loginRepository).multiFactorAuthLogin(email, password, pin2FA)
            verifyNoInteractions(chatLogout)
            verifyNoInteractions(resetChatSettings)
        }

    @Test(expected = LoginLoggedOutFromOtherLocation::class)
    fun `test that login does not invoke chatLogout and resetChatSettings if throws LoggedOutFromOtherLocation`() =
        runTest {
            whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
                .thenThrow(LoginLoggedOutFromOtherLocation())

            underTest.invoke(email, password, pin2FA, disableChatApi).test {
                assertThat(awaitError()).isInstanceOf(LoginLoggedOutFromOtherLocation::class.java)
            }

            verify(loginRepository).multiFactorAuthLogin(email, password, pin2FA)
            verifyNoInteractions(chatLogout)
            verifyNoInteractions(resetChatSettings)
        }

    @Test
    fun `test that login success without error and invokes saveAccountCredentials`() = runTest {
        whenever(loginRepository.multiFactorAuthLogin(email, password, pin2FA))
            .thenReturn(flowOf(LoginStatus.LoginSucceed))

        underTest.invoke(email, password, pin2FA, disableChatApi).test {
            assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
            cancelAndIgnoreRemainingEvents()
        }

        verify(loginRepository).multiFactorAuthLogin(email, password, pin2FA)
        verify(saveAccountCredentials).invoke()
    }
}