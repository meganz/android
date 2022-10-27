package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.exception.ChatNotInitializedException
import mega.privacy.android.domain.exception.LoginAlreadyRunningException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.LoginRepository
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default [LoginRepository] implementation.
 *
 * @property megaApiGateway
 * @property megaApiFolderGateway
 * @property megaChatApiGateway
 * @property ioDispatcher
 */
class DefaultLoginRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LoginRepository {

    override suspend fun initMegaChat(session: String) =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                if (isLoginAlreadyRunning()) {
                    Timber.w("Init chat not allowed as other login is already running.")
                    continuation.resumeWith(Result.failure(LoginAlreadyRunningException()))
                    return@suspendCoroutine
                }

                startLoginProcess()

                var state = megaChatApiGateway.initState

                if (state == MegaChatApi.INIT_NOT_DONE || state == MegaChatApi.INIT_ERROR) {
                    state = megaChatApiGateway.init(session)

                    when (state) {
                        MegaChatApi.INIT_NO_CACHE -> Timber.d("INIT_NO_CACHE")
                        MegaChatApi.INIT_ERROR -> {
                            val exception = ChatNotInitializedException()
                            Timber.e("Init chat error: ${exception.message}. Logout...")
                            megaChatApiGateway.logout()
                            finishLoginProcess()
                            continuation.resumeWith(Result.failure(exception))
                            return@suspendCoroutine
                        }
                        else -> Timber.d("Chat correctly initialized")
                    }
                }

                continuation.resumeWith(Result.success(Unit))
            }
        }

    override suspend fun fastLogin(session: String) =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                Timber.d("Fast login allowed.")
                startLoginProcess()
                megaApiGateway.fastLogin(
                    session,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onFastLoginFinish(continuation)
                    )
                )
            }
        }

    private fun onFastLoginFinish(continuation: Continuation<Unit>) =
        { _: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                Timber.d("Fast login success")
                megaApiFolderGateway.accountAuth = megaApiGateway.accountAuth
                continuation.resumeWith(Result.success(Unit))
            } else {
                Timber.e("Fast login error: ${error.errorString}")
                finishLoginProcess()
                continuation.failWithError(error)
            }
        }

    override suspend fun fetchNodes() =
        withContext(ioDispatcher) {
            suspendCoroutine { continuation ->
                Timber.d("Fetch nodes allowed.")
                megaApiGateway.fetchNodes(
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onFetchNodesFinish(continuation)
                    )
                )
            }
        }

    private fun onFetchNodesFinish(continuation: Continuation<Unit>) =
        { _: MegaRequest, error: MegaError ->
            finishLoginProcess()

            if (error.errorCode == MegaError.API_OK) {
                Timber.d("Fetch nodes success")
                continuation.resumeWith(Result.success(Unit))
            } else {
                Timber.e("Fetch nodes error: ${error.errorString}")
                continuation.failWithError(error)
            }
        }

    /**
     * Checks if there is a login already running.
     *
     * @return True if there is a login already running, false otherwise.
     */
    private fun isLoginAlreadyRunning(): Boolean = MegaApplication.isLoggingIn

    /**
     * Sets isLoggingIn flag to true for starting the login process and not allowing a new one
     * while this is in progress.
     */
    private fun startLoginProcess() {
        MegaApplication.isLoggingIn = true
    }

    /**
     * Sets isLoggingIn flag to false for finishing the login process and allowing a new one
     * when required.
     */
    private fun finishLoginProcess() {
        MegaApplication.isLoggingIn = false
    }
}