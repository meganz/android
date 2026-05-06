package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.ChatNotInitializedUnknownStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.security.GoogleSignInRepository
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.account.CreateAccountUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import javax.inject.Inject

/**
 * Use case that orchestrates the Google Sign-In flow:
 * 1. Launch Google Sign-In to get email + sub
 * 2. Attempt MEGA login with email + sub (as password)
 * 3. On account-not-found: auto-create account, then retry login
 * 4. On account-exists with different password: propagate exception
 * 5. Handle 2FA requirement
 */
class GoogleSignInUseCase @Inject constructor(
    private val googleSignInRepository: GoogleSignInRepository,
    private val loginRepository: LoginRepository,
    private val createAccountUseCase: CreateAccountUseCase,
    private val chatLogoutUseCase: ChatLogoutUseCase,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase,
    private val chatAnonymousLogoutUseCase: ChatAnonymousLogoutUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     * @param idToken The raw Google ID token JWT obtained from Credential Manager.
     * @param disableChatApiUseCase [DisableChatApiUseCase]
     * @return Flow of [LoginStatus].
     */
    operator fun invoke(
        idToken: String,
        disableChatApiUseCase: DisableChatApiUseCase,
    ) = callbackFlow {
        runCatching {
            val googleResult = googleSignInRepository.signIn(idToken)
            val email = googleResult.email
            val password = googleResult.sub
            val firstName = googleResult.firstName.orEmpty()
            val lastName = googleResult.lastName.orEmpty()

            loginMutex.lock()

            runCatching { chatAnonymousLogoutUseCase() }
            runCatching { loginRepository.initMegaChat() }
                .onFailure { exception ->
                    when (exception) {
                        is ChatNotInitializedErrorStatus -> {
                            chatLogoutUseCase(disableChatApiUseCase)
                        }

                        is ChatNotInitializedUnknownStatus -> {
                            send(LoginStatus.LoginCannotStart)
                            close(exception)
                            return@callbackFlow
                        }
                    }
                }

            performLogin(email, password, firstName, lastName, disableChatApiUseCase)
        }.onFailure {
            close(it)
        }

        awaitClose {
            unlockLoginMutex()
        }
    }

    private suspend fun ProducerScope<LoginStatus>.performLogin(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        disableChatApiUseCase: DisableChatApiUseCase,
        isRetry: Boolean = false,
    ) {
        loginRepository.login(email, password).catch { throwable ->
            if (throwable is LoginWrongEmailOrPassword && !isRetry) {
                createAccountUseCase(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                )
                performLogin(email, password, firstName, lastName, disableChatApiUseCase, isRetry = true)
            } else {
                if (throwable !is LoginLoggedOutFromOtherLocation
                    && throwable !is LoginMultiFactorAuthRequired
                ) {
                    chatLogoutUseCase(disableChatApiUseCase)
                    resetChatSettingsUseCase()
                }
                close(throwable)
            }
        }.collectLatest { loginStatus ->
            if (loginStatus == LoginStatus.LoginSucceed) {
                runCatching { saveAccountCredentialsUseCase() }
            }
            trySend(loginStatus)
            if (loginStatus == LoginStatus.LoginSucceed) {
                close()
            }
        }
    }

    private fun unlockLoginMutex() = runCatching { loginMutex.unlock() }
}
