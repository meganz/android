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
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
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
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasPreferencesUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.FetchNodesUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.GetSessionUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import mega.privacy.android.domain.usecase.login.LoginWith2FAUseCase
import mega.privacy.android.domain.usecase.login.MonitorAccountUpdateUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import mega.privacy.android.domain.usecase.transfer.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.OngoingTransfersExistUseCase
import javax.inject.Inject

/**
 * View Model for [LoginFragment]
 *
 * @property state View state as [LoginState]
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val loggingSettings: LegacyLoggingSettings,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase,
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val hasPreferencesUseCase: HasPreferencesUseCase,
    private val hasCameraSyncEnabledUseCase: HasCameraSyncEnabledUseCase,
    private val isCameraSyncEnabledUseCase: IsCameraSyncEnabledUseCase,
    private val querySignupLinkUseCase: QuerySignupLinkUseCase,
    private val cancelTransfersUseCase: CancelTransfersUseCase,
    private val localLogoutUseCase: LocalLogoutUseCase,
    private val loginUseCase: LoginUseCase,
    private val loginWith2FAUseCase: LoginWith2FAUseCase,
    private val fastLoginUseCase: FastLoginUseCase,
    private val fetchNodesUseCase: FetchNodesUseCase,
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val monitorAccountUpdateUseCase: MonitorAccountUpdateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    /**
     * Get latest value of StorageState.
     */
    fun getStorageState() = monitorStorageStateEventUseCase.getState()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

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
                flowOf(getSessionUseCase()).map { session ->
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
                flowOf(hasPreferencesUseCase()).map { hasPreferences ->
                    { state: LoginState -> state.copy(hasPreferences = hasPreferences) }
                },
                flowOf(hasCameraSyncEnabledUseCase()).map { hasCameraSyncEnabled ->
                    { state: LoginState -> state.copy(hasCUSetting = hasCameraSyncEnabled) }
                },
                flowOf(isCameraSyncEnabledUseCase()).map { isCameraSyncEnabled ->
                    { state: LoginState -> state.copy(isCUSettingEnabled = isCameraSyncEnabled) }
                }
            ).collect {
                _state.update(it)
            }
        }

        viewModelScope.launch { resetChatSettingsUseCase() }
        monitorFetchNodes()
        monitorAccountUpdates()
    }

    private fun monitorFetchNodes() = viewModelScope.launch {
        monitorFetchNodesFinishUseCase().collectLatest {
            with(state.value.pendingAction) {
                when (this) {
                    ACTION_FORCE_RELOAD_ACCOUNT -> {
                        _state.update { it.copy(isPendingToFinishActivity = true) }
                    }
                    ACTION_OPEN_APP -> {
                        _state.update { it.copy(intentState = LoginIntentState.ReadyForFinalSetup) }
                    }
                }
            }
        }
    }

    private fun monitorAccountUpdates() = viewModelScope.launch {
        monitorAccountUpdateUseCase().collectLatest {
            if (state.value.isPendingToShowFragment == LoginFragmentType.ConfirmEmail) {
                _state.update { it.copy(isPendingToShowFragment = LoginFragmentType.Login) }
            }
        }
    }


    /**
     * Sets confirm email fragment as pending in state.
     */
    fun setIsWaitingForConfirmAccount() =
        _state.update { state -> state.copy(isPendingToShowFragment = LoginFragmentType.ConfirmEmail) }

    /**
     * Sets tour as pending fragment in state.
     */
    fun setTourAsPendingFragment() =
        _state.update { state -> state.copy(isPendingToShowFragment = LoginFragmentType.Tour) }

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
            localLogoutUseCase(
                DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi },
                ClearPsa { PsaManager::stopChecking }
            )
            _state.update { it.copy(isLocalLogoutInProgress = false) }
            MegaApplication.isLoggingIn = false
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
        getAccountCredentialsUseCase()?.let { credentials ->
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
                accountSession = saveAccountCredentialsUseCase(),
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
        if (!getFeatureFlagValueUseCase(AppFeatures.PermanentLogging)) {
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
        if (!getFeatureFlagValueUseCase(AppFeatures.PermanentLogging)) {
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
        val result = runCatching { querySignupLinkUseCase(link) }
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
    fun launchCancelTransfers() = viewModelScope.launch { cancelTransfersUseCase() }

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
                    loginUseCase(
                        email,
                        password,
                        DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }
                    ).collectLatest { status -> status.checkStatus(email, password) }
                }.onFailure { exception ->
                    MegaApplication.isLoggingIn = false
                    if (exception !is LoginException) return@onFailure

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
                loginWith2FAUseCase(
                    accountSession?.email ?: return@launch,
                    password ?: return@launch,
                    pin2FA,
                    DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }
                ).collectLatest { status ->
                    status.checkStatus()
                }
            }.onFailure { exception ->
                MegaApplication.isLoggingIn = false
                if (exception !is LoginException) return@onFailure

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
            fastLoginUseCase(
                state.value.accountSession?.session ?: return@launch,
                refreshChatUrl,
                DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }
            ).collectLatest { status -> status.checkStatus() }
        }.onFailure { exception ->
            MegaApplication.isLoggingIn = false
            if (exception !is LoginException) return@onFailure
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
            fetchNodesUseCase().collectLatest { update ->
                if (update.progress?.floatValue == 1F) {
                    MegaApplication.isLoggingIn = false
                    _state.update {
                        it.copy(
                            intentState = LoginIntentState.ReadyForFinalSetup,
                            fetchNodesUpdate = update
                        )
                    }
                } else {
                    _state.update { it.copy(fetchNodesUpdate = update) }
                }
            }
        }.onFailure {
            MegaApplication.isLoggingIn = false
            if (it !is FetchNodesException) return@launch

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
        _state.update { state -> state.copy(ongoingTransfersExist = ongoingTransfersExistUseCase()) }
    }

    /**
     * Sets to null ongoingTransfersExistUseCase in state.
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

    companion object {
        /**
         * Intent action for showing the login fetching nodes.
         */
        const val ACTION_FORCE_RELOAD_ACCOUNT = "FORCE_RELOAD"

        /**
         * Intent action for opening app.
         */
        const val ACTION_OPEN_APP = "OPEN_APP"
    }
}
