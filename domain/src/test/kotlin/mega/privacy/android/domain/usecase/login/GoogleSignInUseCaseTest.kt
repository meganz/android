package mega.privacy.android.domain.usecase.login

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.login.GoogleSignInResult
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.exception.login.GoogleSignInException
import mega.privacy.android.domain.repository.security.GoogleSignInRepository
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.account.CreateAccountUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoogleSignInUseCaseTest {

    private lateinit var underTest: GoogleSignInUseCase

    private val googleSignInRepository = mock<GoogleSignInRepository>()
    private val loginRepository = mock<LoginRepository>()
    private val createAccountUseCase = mock<CreateAccountUseCase>()
    private val chatLogoutUseCase = mock<ChatLogoutUseCase>()
    private val resetChatSettingsUseCase = mock<ResetChatSettingsUseCase>()
    private val saveAccountCredentialsUseCase = mock<SaveAccountCredentialsUseCase>()
    private val chatAnonymousLogoutUseCase = mock<ChatAnonymousLogoutUseCase>()
    private val loginMutex = mock<Mutex>()
    private val disableChatApiUseCase = mock<DisableChatApiUseCase>()

    private val googleSignInResult = GoogleSignInResult(
        email = "test@gmail.com",
        sub = "google-sub-12345",
        firstName = "John",
        lastName = "Doe",
    )

    private val ephemeralCredentials = EphemeralCredentials(
        email = "test@gmail.com",
        password = "google-sub-12345",
        session = "test-session",
        firstName = "John",
        lastName = "Doe",
    )

    @BeforeAll
    fun setUp() {
        underTest = GoogleSignInUseCase(
            googleSignInRepository = googleSignInRepository,
            loginRepository = loginRepository,
            createAccountUseCase = createAccountUseCase,
            chatLogoutUseCase = chatLogoutUseCase,
            resetChatSettingsUseCase = resetChatSettingsUseCase,
            saveAccountCredentialsUseCase = saveAccountCredentialsUseCase,
            chatAnonymousLogoutUseCase = chatAnonymousLogoutUseCase,
            loginMutex = loginMutex,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            googleSignInRepository,
            loginRepository,
            createAccountUseCase,
            chatLogoutUseCase,
            resetChatSettingsUseCase,
            saveAccountCredentialsUseCase,
            chatAnonymousLogoutUseCase,
            loginMutex,
        )
    }

    @Test
    fun `test that invoke emits LoginSucceed when Google sign-in and login succeed`() =
        runTest {
            whenever(googleSignInRepository.signIn()).thenReturn(googleSignInResult)
            whenever(
                loginRepository.login(
                    googleSignInResult.email,
                    googleSignInResult.sub,
                )
            ).thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest(disableChatApiUseCase).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke creates account and retries login when account not found`() =
        runTest {
            whenever(googleSignInRepository.signIn()).thenReturn(googleSignInResult)
            whenever(
                loginRepository.login(
                    googleSignInResult.email,
                    googleSignInResult.sub,
                )
            ).thenReturn(
                flow { throw LoginWrongEmailOrPassword() },
                flowOf(LoginStatus.LoginSucceed),
            )
            whenever(
                createAccountUseCase(
                    email = googleSignInResult.email,
                    password = googleSignInResult.sub,
                    firstName = googleSignInResult.firstName.orEmpty(),
                    lastName = googleSignInResult.lastName.orEmpty(),
                )
            ).thenReturn(ephemeralCredentials)

            underTest(disableChatApiUseCase).test {
                assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
                awaitComplete()
            }

            verify(createAccountUseCase).invoke(
                email = googleSignInResult.email,
                password = googleSignInResult.sub,
                firstName = googleSignInResult.firstName.orEmpty(),
                lastName = googleSignInResult.lastName.orEmpty(),
            )
        }

    @Test
    fun `test that invoke throws CreateAccountException AccountAlreadyExists when account exists with different password`() =
        runTest {
            whenever(googleSignInRepository.signIn()).thenReturn(googleSignInResult)
            whenever(
                loginRepository.login(
                    googleSignInResult.email,
                    googleSignInResult.sub,
                )
            ).thenReturn(flow { throw LoginWrongEmailOrPassword() })
            whenever(
                createAccountUseCase(
                    email = googleSignInResult.email,
                    password = googleSignInResult.sub,
                    firstName = googleSignInResult.firstName.orEmpty(),
                    lastName = googleSignInResult.lastName.orEmpty(),
                )
            ).thenAnswer { throw CreateAccountException.AccountAlreadyExists }

            underTest(disableChatApiUseCase).test {
                assertThat(awaitError())
                    .isInstanceOf(CreateAccountException.AccountAlreadyExists::class.java)
            }
        }

    @Test
    fun `test that invoke throws LoginMultiFactorAuthRequired when 2FA required`() =
        runTest {
            whenever(googleSignInRepository.signIn()).thenReturn(googleSignInResult)
            whenever(
                loginRepository.login(
                    googleSignInResult.email,
                    googleSignInResult.sub,
                )
            ).thenReturn(flow { throw LoginMultiFactorAuthRequired() })

            underTest(disableChatApiUseCase).test {
                assertThat(awaitError())
                    .isInstanceOf(LoginMultiFactorAuthRequired::class.java)
            }
        }

    @Test
    fun `test that invoke throws GoogleSignInException Cancelled when user cancels`() =
        runTest {
            whenever(googleSignInRepository.signIn()).thenThrow(GoogleSignInException.Cancelled)

            underTest(disableChatApiUseCase).test {
                assertThat(awaitError())
                    .isInstanceOf(GoogleSignInException.Cancelled::class.java)
            }
        }

    @Test
    fun `test that invoke throws GoogleSignInException Unknown on Google failure`() =
        runTest {
            whenever(googleSignInRepository.signIn())
                .thenThrow(GoogleSignInException.Unknown("test error"))

            underTest(disableChatApiUseCase).test {
                assertThat(awaitError())
                    .isInstanceOf(GoogleSignInException.Unknown::class.java)
            }
        }

    @Test
    fun `test that invoke calls saveAccountCredentialsUseCase on login success`() =
        runTest {
            whenever(googleSignInRepository.signIn()).thenReturn(googleSignInResult)
            whenever(
                loginRepository.login(
                    googleSignInResult.email,
                    googleSignInResult.sub,
                )
            ).thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest(disableChatApiUseCase).test {
                awaitItem()
                awaitComplete()
            }

            verify(saveAccountCredentialsUseCase).invoke()
        }

    @Test
    fun `test that invoke acquires and releases loginMutex`() =
        runTest {
            whenever(googleSignInRepository.signIn()).thenReturn(googleSignInResult)
            whenever(
                loginRepository.login(
                    googleSignInResult.email,
                    googleSignInResult.sub,
                )
            ).thenReturn(flowOf(LoginStatus.LoginSucceed))

            underTest(disableChatApiUseCase).test {
                awaitItem()
                awaitComplete()
            }

            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }
}
