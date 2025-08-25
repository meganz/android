package mega.privacy.android.data.repository

import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getChatRequestListener
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.facade.security.SetLogoutFlagWrapper
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.CredentialsPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.SessionTransferURLListenerInterface
import mega.privacy.android.data.mapper.login.FetchNodesUpdateMapper
import mega.privacy.android.data.mapper.login.TemporaryWaitingErrorMapper
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatLoggingOutException
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.ChatNotInitializedUnknownStatus
import mega.privacy.android.domain.exception.LoginBlockedAccount
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginUnknownStatus
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.domain.exception.LoginWrongMultiFactorAuth
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.exception.login.FetchNodesBlockedAccount
import mega.privacy.android.domain.exception.login.FetchNodesErrorAccess
import mega.privacy.android.domain.exception.login.FetchNodesUnknownStatus
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.security.LoginRepository
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
 * @property appEventGateway
 */
internal class DefaultLoginRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val appEventGateway: AppEventGateway,
    private val fetchNodesUpdateMapper: FetchNodesUpdateMapper,
    private val temporaryWaitingErrorMapper: TemporaryWaitingErrorMapper,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val setLogoutFlagWrapper: SetLogoutFlagWrapper,
    private val credentialsPreferencesGateway: Lazy<CredentialsPreferencesGateway>,
) : LoginRepository {

    override suspend fun initMegaChat(session: String) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                var state = megaChatApiGateway.initState

                when (state) {
                    MegaChatApi.INIT_NOT_DONE, MegaChatApi.INIT_ERROR -> {
                        state = megaChatApiGateway.init(session)

                        when (state) {
                            MegaChatApi.INIT_NO_CACHE -> Timber.d("INIT_NO_CACHE")
                            MegaChatApi.INIT_ERROR -> {
                                val exception = ChatNotInitializedErrorStatus()
                                Timber.e("Init chat error: ${exception.message}. Logout...")
                                megaChatApiGateway.logout(null)
                                continuation.resumeWith(Result.failure(exception))
                                return@suspendCancellableCoroutine
                            }

                            else -> Timber.d("Chat correctly initialized")
                        }
                    }

                    MegaChatApi.INIT_TERMINATED -> {
                        Timber.w("Chat with terminated state, a logout is in progress.")
                        continuation.resumeWith(Result.failure(ChatLoggingOutException()))
                        return@suspendCancellableCoroutine
                    }
                }

                continuation.resumeWith(Result.success(Unit))
            }
        }


    override suspend fun fastLogin(session: String) {
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                Timber.d("Fast login allowed.")
                val listener =
                    continuation.getRequestListener("fastLogin") { }
                megaApiGateway.fastLogin(
                    session,
                    listener
                )
            }
            megaApiFolderGateway.setAccountAuth(megaApiGateway.getAccountAuth())
        }
    }

    override suspend fun fetchNodes() {
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                Timber.d("Fetch nodes allowed.")
                val listener =
                    continuation.getRequestListener("fetchNodes") { onFetchNodesFinish(continuation) }
                megaApiGateway.fetchNodes(listener)
            }
        }
    }

    private fun onFetchNodesFinish(continuation: Continuation<Unit>) =
        { _: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                Timber.d("Fetch nodes success")
                continuation.resumeWith(Result.success(Unit))
            } else {
                Timber.e("Fetch nodes error: ${error.errorString}")
                continuation.failWithError(error, "onFetchNodesFinish")
            }
        }

    override fun monitorLogout() = appEventGateway.monitorLogout()

    override suspend fun broadcastLogout() = appEventGateway.broadcastLogout()

    override suspend fun localLogout() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("localLogout") {}

            megaApiGateway.localLogout(listener)

        }
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("logout") { }

            megaApiGateway.logout(listener)

        }
        credentialsPreferencesGateway.get().clear()
    }

    override suspend fun chatLogout() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getChatRequestListener("chatLogout") { }

            megaChatApiGateway.logout(listener)
        }
    }

    override fun monitorFinishActivity() = appEventGateway.monitorFinishActivity()

    override suspend fun broadcastFinishActivity() = appEventGateway.broadcastFinishActivity()

    override suspend fun initMegaChat() = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            var state = megaChatApiGateway.initState
            Timber.d("MegaChat init state: $state")

            when (state) {
                MegaChatApi.INIT_NOT_DONE, MegaChatApi.INIT_ERROR -> {
                    state = megaChatApiGateway.init(null)

                    when (state) {
                        MegaChatApi.INIT_WAITING_NEW_SESSION -> {
                            continuation.resumeWith(Result.success(Unit))
                        }

                        MegaChatApi.INIT_ERROR -> {
                            val exception = ChatNotInitializedErrorStatus()
                            Timber.e("Init chat error: ${exception.message}. Logout...")
                            continuation.resumeWith(Result.failure(exception))
                        }

                        else -> {
                            continuation.resumeWith(
                                Result.failure(ChatNotInitializedUnknownStatus())
                            )
                        }
                    }
                }

                else -> {
                    continuation.resumeWith(Result.success(Unit))
                }
            }
        }
    }

    private fun loginRequest(
        loginRequest: (OptionalMegaRequestListenerInterface) -> Unit,
    ) = callbackFlow {
        var timerJob: Job? = null
        val listener = OptionalMegaRequestListenerInterface(
            onRequestStart = { trySend(LoginStatus.LoginStarted) },
            onRequestTemporaryError = { request, error ->
                Timber.w("onRequestTemporaryError: %s%d", request.requestString, error.errorCode)
                if (timerJob?.isActive != true) {
                    timerJob = applicationScope.launch(ioDispatcher) {
                        runCatching {
                            delay(WAITING_ERROR_TIMER)
                            val waitingReason = megaApiGateway.getWaitingReason()
                            val temporaryError = waitingReason.let {
                                Timber.w("Waiting, retry reason for ${request.requestString}: $it")
                                temporaryWaitingErrorMapper(it)
                            }
                            trySend(LoginStatus.LoginWaiting(temporaryError))
                        }
                    }
                }
            },
            onRequestUpdate = { request ->
                Timber.d("onRequestUpdate: ${request.requestString}")
                timerJob?.cancel()
                trySend(LoginStatus.LoginResumed)
            },
            onRequestFinish = { _, error ->
                timerJob?.cancel()
                when (error.errorCode) {
                    MegaError.API_OK -> {
                        trySend(LoginStatus.LoginSucceed)
                        close()
                    }

                    MegaError.API_ESID -> {
                        Timber.w("Logged out from other location.")
                        close(LoginLoggedOutFromOtherLocation())
                    }

                    MegaError.API_EFAILED, MegaError.API_EEXPIRED -> {
                        Timber.w("Wrong 2FA code.")
                        close(LoginWrongMultiFactorAuth())
                    }

                    MegaError.API_EMFAREQUIRED -> {
                        Timber.w("Require 2FA.")
                        close(LoginMultiFactorAuthRequired())
                    }

                    MegaError.API_ENOENT -> {
                        Timber.w("Wrong email or password")
                        close(LoginWrongEmailOrPassword())
                    }

                    MegaError.API_ETOOMANY -> {
                        Timber.w("Too many attempts")
                        close(LoginTooManyAttempts())
                    }

                    MegaError.API_EINCOMPLETE -> {
                        Timber.w("Account not validated")
                        close(LoginRequireValidation())
                    }

                    MegaError.API_EBLOCKED -> {
                        Timber.w("Blocked account")
                        close(LoginBlockedAccount())
                    }

                    else -> {
                        Timber.w("MegaRequest.TYPE_LOGIN error $error")
                        close(LoginUnknownStatus(error.toException("loginRequest")))
                    }
                }
            }
        )

        loginRequest.invoke(listener)
        awaitClose()
    }.onEach {
        if (it == LoginStatus.LoginSucceed) {
            megaApiFolderGateway.setAccountAuth(megaApiGateway.getAccountAuth())
        }
    }.flowOn(ioDispatcher)

    override fun login(email: String, password: String) =
        loginRequest { megaApiGateway.login(email, password, it) }


    override fun multiFactorAuthLogin(
        email: String,
        password: String,
        pin: String,
    ) = loginRequest { megaApiGateway.multiFactorAuthLogin(email, password, pin, it) }

    override suspend fun refreshMegaChatUrl() = withContext(ioDispatcher) {
        megaChatApiGateway.refreshUrl()
    }

    override fun fastLoginFlow(session: String) =
        loginRequest { megaApiGateway.fastLogin(session, it) }

    override fun fetchNodesFlow(): Flow<FetchNodesUpdate> = callbackFlow {
        var timerJob: Job? = null
        val listener = OptionalMegaRequestListenerInterface(
            onRequestStart = { Timber.d("onRequestStart: Fetch nodes") },
            onRequestUpdate = { request ->
                timerJob?.cancel()
                trySend(fetchNodesUpdateMapper(request))
            },
            onRequestTemporaryError = { request, error ->
                Timber.w("onRequestTemporaryError: %s%d", request.requestString, error.errorCode)
                if (timerJob?.isActive != true) {
                    val fetchNodesUpdate = fetchNodesUpdateMapper(request)
                    trySend(fetchNodesUpdate)
                    timerJob = applicationScope.launch(ioDispatcher) {
                        runCatching {
                            delay(WAITING_ERROR_TIMER)
                            val temporaryError = megaApiGateway.getWaitingReason().let {
                                Timber.w("Waiting, retry reason for ${request.requestString}: $it")
                                temporaryWaitingErrorMapper(it)
                            }
                            trySend(fetchNodesUpdate.copy(temporaryError = temporaryError))
                        }
                    }
                }
            },
            onRequestFinish = { request, error ->
                timerJob?.cancel()
                when (error.errorCode) {
                    MegaError.API_OK -> {
                        trySend(fetchNodesUpdateMapper(null))
                        close()
                    }

                    MegaError.API_EACCESS -> {
                        Timber.e("Error API_EACCESS")
                        close(FetchNodesErrorAccess(error.toException("fetchNodesFlow")))
                    }

                    MegaError.API_EBLOCKED -> {
                        Timber.w("Suspended account - Reason: ${request.number}")
                        close(FetchNodesBlockedAccount())
                    }

                    else -> {
                        close(FetchNodesUnknownStatus(error.toException("fetchNodesFlow")))
                    }
                }

            }
        )

        megaApiGateway.fetchNodes(listener)
        awaitClose()
    }.flowOn(ioDispatcher)

    override fun monitorFetchNodesFinish() = appEventGateway.monitorFetchNodesFinish()

    override suspend fun broadcastFetchNodesFinish() = appEventGateway.broadcastFetchNodesFinish()

    override suspend fun shouldShowPasswordReminderDialog(atLogin: Boolean) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener =
                    OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                        when (error.errorCode) {
                            MegaError.API_OK, MegaError.API_ENOENT -> continuation.resumeWith(
                                Result.success(request.flag)
                            )

                            else -> continuation.failWithError(
                                error,
                                "shouldShowPasswordReminderDialog"
                            )
                        }
                    })
                megaApiGateway.shouldShowPasswordReminderDialog(atLogin, listener)
            }
        }

    override fun setLogoutInProgressFlag(inProgress: Boolean) = setLogoutFlagWrapper(inProgress)

    override suspend fun getSessionTransferURL(path: String): String? {
        return withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = SessionTransferURLListenerInterface { link ->
                    continuation.resumeWith(Result.success(link))
                }
                megaApiGateway.getSessionTransferURL(path, listener)
            }
        }
    }

    override suspend fun resendSignupLink(email: String, fullName: String?): String =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        when (error.errorCode) {
                            MegaError.API_OK -> continuation.resumeWith(Result.success(request.email))
                            MegaError.API_EEXIST -> continuation.resumeWith(
                                Result.failure(CreateAccountException.AccountAlreadyExists)
                            )

                            MegaError.API_ETOOMANY -> continuation.resumeWith(
                                Result.failure(CreateAccountException.TooManyAttemptsException)
                            )

                            else -> continuation.failWithError(error, "resendSignupLink")
                        }
                    }
                )
                megaApiGateway.resendSignupLink(email, fullName, listener)
            }
        }

    companion object {
        internal const val WAITING_ERROR_TIMER = 10000L
    }
}
