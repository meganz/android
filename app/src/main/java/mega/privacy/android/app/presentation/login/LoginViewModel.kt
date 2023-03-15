package mega.privacy.android.app.presentation.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.app.psa.PsaManager
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.LoginBlockedAccount
import mega.privacy.android.domain.exception.LoginException
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.exception.LoginWrongMultiFactorAuth
import mega.privacy.android.domain.exception.login.FetchNodesErrorAccess
import mega.privacy.android.domain.exception.login.FetchNodesException
import mega.privacy.android.domain.usecase.CancelTransfers
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.GetAccountCredentials
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.GetSession
import mega.privacy.android.domain.usecase.HasCameraSyncEnabled
import mega.privacy.android.domain.usecase.HasPreferences
import mega.privacy.android.domain.usecase.IsCameraSyncEnabled
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.QuerySignupLink
import mega.privacy.android.domain.usecase.RootNodeExists
import mega.privacy.android.domain.usecase.SaveAccountCredentials
import mega.privacy.android.domain.usecase.login.DisableChatApi
import mega.privacy.android.domain.usecase.login.FastLogin
import mega.privacy.android.domain.usecase.login.FetchNodes
import mega.privacy.android.domain.usecase.login.LocalLogout
import mega.privacy.android.domain.usecase.login.Login
import mega.privacy.android.domain.usecase.login.LoginWith2FA
import mega.privacy.android.domain.usecase.setting.ResetChatSettings
import mega.privacy.android.domain.usecase.transfer.OngoingTransfersExist
import javax.inject.Inject

/**
 * View Model for [LoginFragment]
 *
 * @property state View state as [LoginState]
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val monitorConnectivity: MonitorConnectivity,
    private val rootNodeExistsUseCase: RootNodeExists,
    private val getFeatureFlagValue: GetFeatureFlagValue,
    private val loggingSettings: LegacyLoggingSettings,
    private val resetChatSettings: ResetChatSettings,
    private val saveAccountCredentials: SaveAccountCredentials,
    private val getAccountCredentials: GetAccountCredentials,
    private val getSession: GetSession,
    private val hasPreferences: HasPreferences,
    private val hasCameraSyncEnabled: HasCameraSyncEnabled,
    private val isCameraSyncEnabled: IsCameraSyncEnabled,
    private val querySignupLink: QuerySignupLink,
    private val cancelTransfers: CancelTransfers,
    private val localLogout: LocalLogout,
    private val login: Login,
    private val loginWith2FA: LoginWith2FA,
    private val fastLogin: FastLogin,
    private val fetchNodes: FetchNodes,
    private val ongoingTransfersExist: OngoingTransfersExist,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    /**
     * Get latest value of StorageState.
     */
    fun getStorageState() = monitorStorageStateEvent.getState()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Checks if root node exists.
     *
     * @return True if root node exists, false otherwise.
     */
    fun rootNodeExists() = runBlocking { rootNodeExistsUseCase() }

    /**
     * Reset some states values.
     */
    fun setupInitialState() {
        viewModelScope.launch {
            merge(
                flowOf(getSession()).map { session ->
                    { state: LoginState ->
                        state.copy(
                            intentState = LoginIntentState.ReadyForInitialSetup,
                            accountSession = AccountSession(session = session),
                            is2FAEnabled = false,
                            isAccountConfirmed = false,
                            pressedBackWhileLogin = false,
                            isFirstTime = session == null,
                            isAlreadyLoggedIn = session != null
                        )
                    }
                },
                flowOf(hasPreferences()).map { hasPreferences ->
                    { state: LoginState -> state.copy(hasPreferences = hasPreferences) }
                },
                flowOf(hasCameraSyncEnabled()).map { hasCameraSyncEnabled ->
                    { state: LoginState -> state.copy(hasCUSetting = hasCameraSyncEnabled) }
                },
                flowOf(isCameraSyncEnabled()).map { isCameraSyncEnabled ->
                    { state: LoginState -> state.copy(isCUSettingEnabled = isCameraSyncEnabled) }
                }
            ).collect {
                _state.update(it)
            }
        }

        viewModelScope.launch { resetChatSettings() }
    }

    /**
     * Updates state with a new intentAction.
     *
     * @param pendingAction Intent action.
     */
    fun setPendingAction(pendingAction: String) {
        _state.update { it.copy(pendingAction = pendingAction) }
    }

    /**
     * Updates pressBackWhileLogin value in state.
     */
    fun updatePressedBackWhileLogin(pressedBackWhileLogin: Boolean) {
        _state.update { it.copy(pressedBackWhileLogin = pressedBackWhileLogin) }
    }

    /**
     * Sets querySignupLinkResult as null in state.
     */
    fun querySignupLinkResultShown() = _state.update { it.copy(querySignupLinkResult = null) }

    /**
     * Stops logging in.
     */
    fun stopLogin() {
        MegaApplication.isLoggingIn = false
        _state.update {
            it.copy(
                pressedBackWhileLogin = true,
                isAlreadyLoggedIn = false,
                fetchNodesUpdate = null,
                is2FARequired = false,
                is2FAEnabled = false,
                isLoginInProgress = false,
                isLocalLogoutInProgress = true,
                isLoginRequired = true,
            )
        }
        viewModelScope.launch {
            localLogout(
                DisableChatApi { MegaApplication.getInstance()::disableMegaChatApi },
                ClearPsa { PsaManager::stopChecking }
            )
            _state.update { it.copy(isLocalLogoutInProgress = false) }
        }
    }

    /**
     * Updates isAccountConfirmed value in state.
     */
    fun updateIsAccountConfirmed(isAccountConfirmed: Boolean) {
        _state.update { it.copy(isAccountConfirmed = isAccountConfirmed) }
    }

    /**
     * Updates error value in state as null.
     */
    fun setErrorShown() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Updates email and session values in state.
     */
    fun updateEmailAndSession(): Boolean = runBlocking {
        getAccountCredentials()?.let { credentials ->
            val accountSession = state.value.accountSession
            _state.update {
                it.copy(
                    accountSession = accountSession?.copy(
                        email = credentials.email,
                        session = credentials.session
                    ) ?: AccountSession(
                        email = credentials.email,
                        credentials.session,
                    )
                )
            }
            true
        } ?: false
    }

    /**
     * Updates temporal email and password values in state.
     */
    fun setTemporalCredentials(email: String?, password: String?) {
        _state.update { it.copy(temporalEmail = email, temporalPassword = password) }
    }

    /**
     * Set temporal email and password values in state as current email and password.
     */
    fun setTemporalCredentialsAsCurrentCredentials() = with(state.value) {
        _state.update {
            it.copy(
                accountSession = accountSession?.copy(email = temporalEmail)
                    ?: AccountSession(email = temporalEmail),
                password = temporalPassword
            )
        }
    }

    /**
     * True if there is a not null email and a not null password, false otherwise.
     */
    fun areThereValidTemporalCredentials() = with(state.value) {
        temporalEmail != null && temporalPassword != null
    }

    /**
     * Saves credentials
     */
    fun saveCredentials() = viewModelScope.launch {
        _state.update {
            it.copy(
                accountSession = saveAccountCredentials(),
                isAlreadyLoggedIn = true
            )
        }
    }

    /**
     * Enables Karere logs if not enabled. Disables them if already enabled.
     *
     * @param activity Required [Activity]
     */
    fun checkAndUpdateKarereLogs(activity: Activity) = viewModelScope.launch {
        if (!getFeatureFlagValue(AppFeatures.PermanentLogging)) {
            if (loggingSettings.areKarereLogsEnabled()) {
                loggingSettings.setStatusLoggerKarere(activity, false)
            } else {
                (activity as LoginActivity).showConfirmationEnableLogsKarere()
            }
        }
    }

    /**
     * Decrements the required value for enabling/disabling SDK logs.
     *
     * @param activity Required [Activity]
     */
    fun checkAndUpdateSDKLogs(activity: Activity) = viewModelScope.launch {
        if (!getFeatureFlagValue(AppFeatures.PermanentLogging)) {
            if (loggingSettings.areSDKLogsEnabled()) {
                loggingSettings.setStatusLoggerSDK(activity, false)
            } else {
                (activity as LoginActivity).showConfirmationEnableLogsSDK()
            }
        }
    }

    /**
     * Checks a signup link.
     */
    fun checkSignupLink(link: String) = viewModelScope.launch {
        val result = runCatching { querySignupLink(link) }
        _state.update { state ->
            state.copy(
                querySignupLinkResult = result,
                intentState = LoginIntentState.AlreadySet
            )
        }
    }

    /**
     * Cancels all transfers, uploads and downloads.
     */
    fun launchCancelTransfers() = viewModelScope.launch { cancelTransfers() }

    /**
     * Login.
     */
    fun performLogin(typedEmail: String? = null, typedPassword: String? = null) {
        if (MegaApplication.isLoggingIn) {
            return
        }

        MegaApplication.isLoggingIn = true
        viewModelScope.launch {
            with(state.value) {
                val email = typedEmail ?: accountSession?.email ?: return@launch
                val password = typedPassword ?: this.password ?: return@launch

                runCatching {
                    login(
                        email,
                        password,
                        DisableChatApi { MegaApplication.getInstance()::disableMegaChatApi }
                    ).collectLatest { status -> status.checkStatus(email, password) }
                }.onFailure { exception ->
                    if (exception !is LoginException) return@onFailure
                    MegaApplication.isLoggingIn = false

                    if (exception is LoginMultiFactorAuthRequired) {
                        _state.update {
                            it.copy(
                                isLoginInProgress = false,
                                is2FAEnabled = true,
                                isLoginRequired = false,
                                is2FARequired = true
                            )
                        }
                    } else {
                        exception.loginFailed()
                    }
                }
            }
        }
    }

    /**
     * Login with 2FA.
     */
    fun performLoginWith2FA(pin2FA: String) = viewModelScope.launch {
        MegaApplication.isLoggingIn = true

        with(state.value) {
            runCatching {
                loginWith2FA(
                    accountSession?.email ?: return@launch,
                    password ?: return@launch,
                    pin2FA,
                    DisableChatApi { MegaApplication.getInstance()::disableMegaChatApi }
                ).collectLatest { status ->
                    status.checkStatus()
                }
            }.onFailure { exception ->
                if (exception !is LoginException) return@onFailure
                MegaApplication.isLoggingIn = false

                if (exception is LoginWrongMultiFactorAuth) {
                    _state.update {
                        it.copy(
                            isLoginInProgress = false,
                            is2FARequired = true,
                            multiFactorAuthState = MultiFactorAuthState.Failed
                        )
                    }
                } else {
                    exception.loginFailed(true)
                }
            }
        }
    }

    /**
     * Fast login.
     */
    fun performFastLogin(refreshChatUrl: Boolean) = viewModelScope.launch {
        MegaApplication.isLoggingIn = true
        runCatching {
            fastLogin(
                state.value.accountSession?.session ?: return@launch,
                refreshChatUrl,
                DisableChatApi { MegaApplication.getInstance()::disableMegaChatApi }
            ).collectLatest { status -> status.checkStatus() }
        }.onFailure { exception ->
            if (exception !is LoginException) return@onFailure
            MegaApplication.isLoggingIn = false
            exception.loginFailed()
        }
    }

    private fun LoginException.loginFailed(is2FARequest: Boolean = false) =
        _state.update {
            it.copy(
                isLoginInProgress = false,
                isLoginRequired = true,
                is2FAEnabled = is2FARequest,
                is2FARequired = false,
                //If LoginBlockedAccount will processed at the `onEvent` when receive an EVENT_ACCOUNT_BLOCKED
                error = this.takeUnless { exception -> exception is LoginBlockedAccount }
            )
        }

    private fun LoginStatus.checkStatus(email: String? = null, password: String? = null) {
        when (this) {
            LoginStatus.LoginStarted -> {
                _state.update {
                    if (email != null && password != null) {
                        it.copy(
                            isLoginInProgress = true,
                            is2FARequired = false,
                            accountSession = state.value.accountSession?.copy(email = email)
                                ?: AccountSession(email = email),
                            password = password,
                            ongoingTransfersExist = null
                        )
                    } else {
                        it.copy(
                            isLoginInProgress = true,
                            is2FARequired = false,
                            ongoingTransfersExist = null
                        )
                    }

                }
            }
            LoginStatus.LoginSucceed -> {
                _state.update {
                    it.copy(
                        isLoginInProgress = false,
                        isLoginRequired = false,
                        is2FARequired = false,
                        isAlreadyLoggedIn = true,
                        fetchNodesUpdate = FetchNodesUpdate()
                    )
                }
                performFetchNodes()
            }
            else -> {
                //LoginStatus.LoginCannotStart no action required.
            }
        }
    }

    /**
     * Fetch nodes.
     */
    fun performFetchNodes(isRefreshSession: Boolean = false) = viewModelScope.launch {
        if (isRefreshSession && !updateEmailAndSession()) return@launch

        MegaApplication.getInstance().checkEnabledCookies()

        if (isRefreshSession) {
            MegaApplication.isLoggingIn = true
            _state.update { it.copy(fetchNodesUpdate = FetchNodesUpdate()) }
        }

        runCatching {
            fetchNodes().collectLatest { update ->
                _state.update { it.copy(fetchNodesUpdate = update) }

                if (update.progress?.floatValue == 1F) {
                    MegaApplication.isLoggingIn = false
                    _state.update { it.copy(intentState = LoginIntentState.ReadyForFinalSetup) }
                }
            }
        }.onFailure {
            if (it !is FetchNodesException) return@launch

            MegaApplication.isLoggingIn = false
            _state.update { state ->
                state.copy(
                    isLoginInProgress = false,
                    isLoginRequired = true,
                    is2FAEnabled = false,
                    is2FARequired = false,
                    error = it.takeUnless { it is FetchNodesErrorAccess || state.pressedBackWhileLogin }
                )
            }
        }
    }

    /**
     * Checks if there are ongoing transfers.
     */
    fun checkOngoingTransfers() = viewModelScope.launch {
        _state.update { state -> state.copy(ongoingTransfersExist = ongoingTransfersExist()) }
    }

    /**
     * Sets to null ongoingTransfersExist in state.
     */
    fun resetOngoingTransfers() =
        _state.update { state -> state.copy(ongoingTransfersExist = null) }

    /**
     * Checks and updates 2FA error.
     */
    fun checkAndUpdate2FAState() = state.value.multiFactorAuthState?.apply {
        _state.update { state ->
            state.copy(
                multiFactorAuthState =
                if (this == MultiFactorAuthState.Failed) MultiFactorAuthState.Fixed
                else null
            )
        }
    }

    /**
     * Intent set.
     */
    fun intentSet() {
        _state.update { state -> state.copy(intentState = LoginIntentState.AlreadySet) }
    }
}
