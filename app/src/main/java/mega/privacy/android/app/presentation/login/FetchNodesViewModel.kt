package mega.privacy.android.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.appstate.initialisation.GlobalInitialiser
import mega.privacy.android.app.presentation.extensions.error
import mega.privacy.android.app.presentation.extensions.newError
import mega.privacy.android.app.presentation.login.model.FetchNodesUiState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import mega.privacy.android.domain.exception.LoginException
import mega.privacy.android.domain.exception.login.FetchNodesErrorAccess
import mega.privacy.android.domain.exception.login.FetchNodesException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.chat.IsMegaApiLoggedInUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.FetchNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.requeststatus.EnableRequestStatusMonitorUseCase
import mega.privacy.android.domain.usecase.requeststatus.MonitorRequestStatusProgressEventUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import timber.log.Timber

/**
 *
 * @property state View state as [LoginState]
 */
@HiltViewModel(assistedFactory = FetchNodesViewModel.Factory::class)
class FetchNodesViewModel @AssistedInject constructor(
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val fastLoginUseCase: FastLoginUseCase,
    private val fetchNodesUseCase: FetchNodesUseCase,
    @LoginMutex private val loginMutex: Mutex,
    private val enableRequestStatusMonitorUseCase: EnableRequestStatusMonitorUseCase,
    private val monitorRequestStatusProgressEventUseCase: MonitorRequestStatusProgressEventUseCase,
    private val ephemeralCredentialManager: EphemeralCredentialManager,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val monitorAccountBlockedUseCase: MonitorAccountBlockedUseCase,
    private val isMegaApiLoggedInUseCase: IsMegaApiLoggedInUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val globalInitialiser: GlobalInitialiser,
    @Assisted val args: Args,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(FetchNodesUiState(isFromLogin = args.isFromLogin))
    val state: StateFlow<FetchNodesUiState> = _state

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    private var pendingAction: String? = null

    private val cleanFetchNodesUpdate by lazy { FetchNodesUpdate() }

    private var performFetchNodesJob: Job? = null

    init {
        enableAndMonitorRequestStatusProgressEvent()
        viewModelScope.launch { resetChatSettingsUseCase() }

        viewModelScope.launch {
            val blockedTypes = setOf(
                AccountBlockedType.TOS_COPYRIGHT,
                AccountBlockedType.TOS_NON_COPYRIGHT,
                AccountBlockedType.VERIFICATION_EMAIL,
                AccountBlockedType.SUBUSER_DISABLED
            )

            monitorAccountBlockedUseCase()
                .filter { it.type in blockedTypes }
                .collectLatest {
                    stopFetchingNodes()
                }
        }
        performFetchNodesOrLogin()
    }

    private fun performFetchNodesOrLogin() = runCatching {
        viewModelScope.launch {
            if (args.refreshEvent == RefreshEvent.ChangeEnvironment) {
                Timber.d("Fast login due to ${args.refreshEvent} event")
                fastLogin(session = args.session, refreshChatUrl = true)
            } else if (args.refreshEvent == RefreshEvent.ManualRefresh || args.refreshEvent == RefreshEvent.SdkReload) {
                Timber.d("Refresh event ${args.refreshEvent} - fetch nodes")
                fetchNodes(isRefreshSession = true)
            } else if (isMegaApiLoggedInUseCase()) {
                Timber.d("User is logged in, fetch nodes")
                handlePostLogin(isFastLogin = false)
                fetchNodes(isRefreshSession = false)
            } else {
                // this case mean user logged in and open app again, so we need to fast login
                Timber.d("User is not logged in, fast login")
                fastLogin(session = args.session, refreshChatUrl = false)
            }
        }
    }.onFailure {
        Timber.e(it, "Error performing fetch nodes or login")
    }

    private fun enableAndMonitorRequestStatusProgressEvent() {
        viewModelScope.launch {
            runCatching {
                enableRequestStatusMonitorUseCase()
                monitorRequestStatusProgressEventUseCase()
                    .catch { throwable ->
                        Timber.e(throwable)
                        // Hide progress bar on error
                        _state.update {
                            it.copy(requestStatusProgress = null)
                        }
                    }.collect { progress ->
                        _state.update {
                            it.copy(requestStatusProgress = progress)
                        }
                    }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Fast login.
     */
    private fun fastLogin(session: String, refreshChatUrl: Boolean) {
        _state.update {
            it.copy(
                fetchNodesUpdate = cleanFetchNodesUpdate,
                isFastLoginInProgress = true,
                isFastLogin = true,
                loginTemporaryError = null,
                requestStatusProgress = null,
                snackbarMessage = consumed()
            )
        }

        viewModelScope.launch {
            var retry = 1
            while (loginMutex.isLocked && retry <= 3) {
                Timber.d("Wait for the isLoggingIn lock to be available")
                delay(1000L * retry)
                if (rootNodeExistsUseCase()) {
                    Timber.d("Root node exists")
                    return@launch
                }
                retry++
            }
            if (loginMutex.isLocked) {
                Timber.w("Another login is processing")
                pendingAction = ACTION_OPEN_APP
                return@launch
            }

            performFastLogin(session, refreshChatUrl)
        }
    }

    private fun performFastLogin(session: String, refreshChatUrl: Boolean) =
        viewModelScope.launch {
            runCatching {
                fastLoginUseCase(
                    session,
                    refreshChatUrl
                ) { MegaApplication.getInstance()::disableMegaChatApi }
                    .collectLatest { status -> status.checkStatus(isFastLogin = true) }
            }.onFailure { exception ->
                if (exception !is LoginException) return@onFailure
                exception.loginFailed()
            }
        }

    private fun LoginException.loginFailed() =
        _state.update { loginState ->
            //If LoginBlockedAccount will processed at the `onEvent` when receive an EVENT_ACCOUNT_BLOCKED
            //If LoginLoggedOutFromOtherLocation will be handled in the Activity
            val snackbarMessage = triggered(this.newError)
            loginState.copy(
                snackbarMessage = snackbarMessage
            )
        }

    private fun LoginStatus.checkStatus(isFastLogin: Boolean = false) = when (this) {
        LoginStatus.LoginStarted -> {
            Timber.d("Login started")
            _state.update {
                it.copy(loginTemporaryError = null)
            }
        }

        LoginStatus.LoginSucceed -> {
            // If fast login, state already updated.
            Timber.d("Login finished")
            if (isFastLogin) {
                _state.update {
                    it.copy(
                        loginTemporaryError = null,
                        isFastLoginInProgress = false
                    )
                }
            } else {
                ephemeralCredentialManager.setEphemeralCredential(null)
                _state.update {
                    it.copy(
                        fetchNodesUpdate = cleanFetchNodesUpdate,
                    )
                }
            }
            handlePostLogin(true)
            getUserData()
            fetchNodes()
        }

        LoginStatus.LoginCannotStart -> {
            Timber.d("Login cannot start")
            _state.update {
                it.copy(
                    loginTemporaryError = null,
                )
            }
        }

        is LoginStatus.LoginResumed -> {
            Timber.d("Login resumed")
            _state.update {
                it.copy(loginTemporaryError = null)
            }
        }

        is LoginStatus.LoginWaiting -> {
            Timber.d("Login waiting")
            // Ignore the temporary error if request status event is in progress
            if (!state.value.isRequestStatusInProgress) {
                _state.update {
                    it.copy(loginTemporaryError = this.error)
                }
            } else {
                Unit
            }
        }
    }

    private fun getUserData() {
        applicationScope.launch {
            runCatching {
                getUserDataUseCase()
            }.onFailure { exception ->
                Timber.e(exception, "Error getting user data")
            }
        }
    }

    /**
     * Fetch nodes.
     */
    private fun fetchNodes(isRefreshSession: Boolean = false) {
        viewModelScope.launch {
            if (isRefreshSession) {
                _state.update { it.copy(fetchNodesUpdate = cleanFetchNodesUpdate) }
            }

            Timber.d("fetch nodes started")
            performFetchNodes()
        }
    }

    private fun performFetchNodes() {
        performFetchNodesJob = viewModelScope.launch {
            runCatching {
                fetchNodesUseCase().collectLatest { update ->
                    if (update.progress?.floatValue == 1F) {
                        Timber.d("fetch nodes finished")
                        _state.update {
                            it.copy(
                                fetchNodesUpdate = update
                            )
                        }
                    } else {
                        Timber.d("fetch nodes update")
                        _state.update { it.copy(fetchNodesUpdate = update) }
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
                if (exception !is FetchNodesException) return@launch

                _state.update { state ->
                    val messageId =
                        exception.takeUnless { exception is FetchNodesErrorAccess }

                    state.copy(
                        snackbarMessage = messageId?.let { triggered(exception.error) }
                            ?: consumed()
                    )
                }
            }
        }
    }

    private fun handlePostLogin(isFastLogin: Boolean) {
        globalInitialiser.onPostLogin(session = args.session, isFastLogin)
    }

    private fun stopFetchingNodes() {
        performFetchNodesJob?.cancel()
    }

    companion object Companion {

        /**
         * Intent action for opening app.
         */
        private const val ACTION_OPEN_APP = "OPEN_APP"
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: Args): FetchNodesViewModel
    }

    data class Args(
        val session: String,
        val isFromLogin: Boolean = false,
        val refreshEvent: RefreshEvent? = null,
    )
}
