package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.domain.exception.ChatNotInitializedException
import mega.privacy.android.domain.exception.LoginAlreadyRunningException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.LoginRepository
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
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

    @Singleton
    override var allowBackgroundLogin: Boolean = true

    override suspend fun fastLogin(session: String) =
        withContext<Unit>(ioDispatcher) {
            suspendCoroutine { continuation ->
                if (allowBackgroundLogin) {
                    allowBackgroundLogin = false
                    megaApiGateway.fastLogin(
                        session,
                        OptionalMegaRequestListenerInterface(
                            onRequestFinish = onFastLoginFinish(continuation)
                        )
                    )
                } else {
                    continuation.resumeWith(Result.failure(LoginAlreadyRunningException()))
                }
            }
        }

    private fun onFastLoginFinish(continuation: Continuation<Unit>) =
        { _: MegaRequest, error: MegaError ->
            allowBackgroundLogin = true
            if (error.errorCode == MegaError.API_OK) {
                megaApiFolderGateway.accountAuth = megaApiGateway.accountAuth
                continuation.resumeWith(Result.success(Unit))
            } else {
                continuation.failWithError(error)
            }
        }

    override suspend fun fetchNodes() =
        withContext<Unit>(ioDispatcher) {
            suspendCoroutine { continuation ->
                megaApiGateway.fetchNodes(
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onFetchNodesFinish(continuation)
                    )
                )
            }
        }

    private fun onFetchNodesFinish(continuation: Continuation<Unit>) =
        { _: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(Result.success(Unit))
            } else {
                continuation.failWithError(error)
            }
        }

    override suspend fun initMegaChat(session: String) =
        withContext<Unit>(ioDispatcher) {
            suspendCoroutine { continuation ->
                var state = megaChatApiGateway.initState

                if (state == MegaChatApi.INIT_NOT_DONE || state == MegaChatApi.INIT_ERROR) {
                    state = megaChatApiGateway.init(session)

                    when (state) {
                        MegaChatApi.INIT_NO_CACHE -> Timber.d("INIT_NO_CACHE")
                        MegaChatApi.INIT_ERROR -> megaChatApiGateway.logout()
                        else -> Timber.d("Chat correctly initialized")
                    }
                }

                if (state == MegaChatApi.INIT_ERROR) {
                    continuation.resumeWith(Result.failure(ChatNotInitializedException()))
                } else {
                    continuation.resumeWith(Result.success(Unit))
                }
            }
        }
}