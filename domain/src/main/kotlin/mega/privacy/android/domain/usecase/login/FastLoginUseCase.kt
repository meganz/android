package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import javax.inject.Inject

/**
 * Use case for fast login.
 */
class FastLoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val initialiseMegaChatUseCase: InitialiseMegaChatUseCase,
    private val chatLogoutUseCase: ChatLogoutUseCase,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     * @param session Account session.
     * @param refreshChatUrl True if should refresh chat api URL, false otherwise.
     * @param disableChatApi True if should call [DisableChatApiUseCase]
     * @return Flow of [LoginStatus].
     */
    operator fun invoke(
        session: String,
        refreshChatUrl: Boolean,
        disableChatApi: Boolean
    ) = callbackFlow {
        // MegaChat::init() can run in parallel with fastLogin, but any call touching the
        // chat client (refreshMegaChatUrl, chatLogout) must wait for it to finish, and
        // fetchNodes must not be triggered until it has finished because the SDK listener
        // is registered at the end of MegaChat::init(). Hence the join() calls below.
        var initialiseChatJob: Job? = null
        runCatching {
            loginMutex.lock()

            initialiseChatJob = launch {
                runCatching {
                    runCatching { initialiseMegaChatUseCase(session) }
                        .onFailure { exception ->
                            if (exception is ChatNotInitializedErrorStatus) {
                                chatLogoutUseCase(disableChatApi)
                            }
                        }

                    if (refreshChatUrl) {
                        loginRepository.refreshMegaChatUrl()
                    }
                }.onFailure {
                    close(it)
                }
            }

            loginRepository.fastLoginFlow(session)
                .catch {
                    initialiseChatJob.join()
                    if (it !is LoginLoggedOutFromOtherLocation) {
                        chatLogoutUseCase(disableChatApi)
                        resetChatSettingsUseCase()
                    }
                    close(it)
                }
                .collectLatest { loginStatus ->
                    if (loginStatus == LoginStatus.LoginSucceed) {
                        saveAccountCredentialsUseCase()
                        initialiseChatJob.join()
                    }
                    trySend(loginStatus)
                    if (loginStatus == LoginStatus.LoginSucceed) {
                        close()
                    }
                }
        }.onFailure {
            close(it)
        }

        // MegaChat::init() cannot be interrupted once started, so wait for the chat job on
        // every exit path (including cancellation) to guarantee awaitClose does not release
        // the login mutex while the chat client is still initialising.
        withContext(NonCancellable) { initialiseChatJob?.join() }

        awaitClose {
            unlockLoginMutex()
        }
    }

    private fun unlockLoginMutex() = runCatching { loginMutex.unlock() }

}
