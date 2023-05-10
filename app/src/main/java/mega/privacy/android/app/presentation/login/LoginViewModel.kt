package mega.privacy.android.app.presentation.login

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.presentation.extensions.error
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.getTwoFactorAuthentication
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.getUpdatedTwoFactorAuthentication
import mega.privacy.android.app.psa.PsaManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.exception.LoginBlockedAccount
import mega.privacy.android.domain.exception.LoginException
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.exception.LoginWrongMultiFactorAuth
import mega.privacy.android.domain.exception.QuerySignupLinkException
import mega.privacy.android.domain.exception.login.FetchNodesErrorAccess
import mega.privacy.android.domain.exception.login.FetchNodesException
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
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
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import mega.privacy.android.domain.usecase.transfer.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.OngoingTransfersExistUseCase
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadUseCase
import timber.log.Timber
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
    private val scheduleCameraUploadUseCase: ScheduleCameraUploadUseCase,
    private val stopCameraUploadUseCase: StopCameraUploadUseCase,
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

    private var pendingAction: String? = null

    private val cleanFetchNodesUpdate by lazy { FetchNodesUpdate() }

    init {
        viewModelScope.launch { getEnabledFeatures() }
    }

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
                            isAlreadyLoggedIn = session != null,
                            isLoginRequired = session == null,
                        )
                    }
                },
                flowOf(rootNodeExistsUseCase()).map { exists ->
                    { state: LoginState -> state.copy(rootNodesExists = exists) }
                },
                flowOf(hasPreferencesUseCase()).map { hasPreferences ->
                    { state: LoginState -> state.copy(hasPreferences = hasPreferences) }
                },
                flowOf(hasCameraSyncEnabledUseCase()).map { hasCameraSyncEnabled ->
                    { state: LoginState -> state.copy(hasCUSetting = hasCameraSyncEnabled) }
                },
                flowOf(isCameraSyncEnabledUseCase()).map { isCameraSyncEnabled ->
                    { state: LoginState -> state.copy(isCUSettingEnabled = isCameraSyncEnabled) }
                },
                monitorFetchNodesFinishUseCase().map {
                    with(pendingAction) {
                        when (this) {
                            ACTION_FORCE_RELOAD_ACCOUNT -> {
                                { state: LoginState -> state.copy(isPendingToFinishActivity = true) }
                            }

                            ACTION_OPEN_APP -> {
                                { state: LoginState -> state.copy(intentState = LoginIntentState.ReadyForFinalSetup) }
                            }

                            else -> {
                                { state: LoginState -> state }
                            }
                        }
                    }
                },
            ).collect {
                _state.update(it)
            }
        }

        viewModelScope.launch { resetChatSettingsUseCase() }

        checkTemporalCredentials()
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
     * Update state with isPendingToShowFragment as null.
     */
    fun isPendingToShowFragmentConsumed() {
        _state.update { state -> state.copy(isPendingToShowFragment = null) }
    }

    /**
     * Sets [ACTION_FORCE_RELOAD_ACCOUNT] as pendingAction.
     */
    fun setForceReloadAccountAsPendingAction() {
        pendingAction = ACTION_FORCE_RELOAD_ACCOUNT
        MegaApplication.isLoggingIn = true
        _state.update { state ->
            state.copy(
                intentState = LoginIntentState.AlreadySet,
                isLoginInProgress = false,
                isLoginRequired = false,
                is2FARequired = false,
                isAlreadyLoggedIn = true,
                fetchNodesUpdate = cleanFetchNodesUpdate
            )
        }
    }

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
                ClearPsa { PsaManager::clearPsa }
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
     * Updates login error value in state as consumed.
     */
    fun setLoginErrorConsumed() {
        _state.update { it.copy(loginException = null) }
    }

    private fun UserCredentials.updateCredentials() {
        val accountSession = state.value.accountSession
        _state.update {
            it.copy(
                accountSession = accountSession?.copy(email = email, session = session)
                    ?: AccountSession(email = email, session)
            )
        }
    }

    /**
     * Updates temporal email and password values in state.
     */
    fun setTemporalCredentials(email: String?, password: String?) {
        _state.update { it.copy(temporalEmail = email, temporalPassword = password) }
    }

    /**
     * True if there is a not null email and a not null password, false otherwise.
     */
    private fun checkTemporalCredentials() = with(state.value) {
        if (temporalEmail != null && temporalPassword != null) {
            performLogin(temporalEmail, temporalPassword)
        }
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
    fun checkSignupLink(link: String) {
        _state.update { state ->
            state.copy(
                isLoginRequired = false,
                isLoginInProgress = true,
                isCheckingSignupLink = true
            )
        }
        viewModelScope.launch {
            val result = runCatching { querySignupLinkUseCase(link) }
            var accountConfirmed: Boolean? = null
            val messageId = if (result.isSuccess) {
                accountConfirmed = true
                R.string.account_confirmed
            } else {
                (result.exceptionOrNull() as QuerySignupLinkException).messageId
            }

            _state.update { state ->
                val isAccountConfirmed =
                    if (accountConfirmed == true) true else state.isAccountConfirmed

                state.copy(
                    isLoginRequired = true,
                    isLoginInProgress = false,
                    isAccountConfirmed = isAccountConfirmed,
                    intentState = LoginIntentState.AlreadySet,
                    isCheckingSignupLink = false,
                    snackbarMessage = messageId?.let { triggered(it) } ?: consumed()
                )
            }
        }
    }

    /**
     * Update email in state.
     */
    fun onEmailChanged(typedEmail: String) {
        val newAccountSession = state.value.accountSession?.copy(email = typedEmail)
            ?: AccountSession(email = typedEmail)

        _state.update { state ->
            state.copy(
                accountSession = newAccountSession,
                emailError = null,
                snackbarMessage = consumed()
            )
        }
    }

    /**
     * Update password in state.
     */
    fun onPasswordChanged(typedPassword: String) = _state.update { state ->
        state.copy(password = typedPassword, passwordError = null, snackbarMessage = consumed())
    }

    /**
     * Check typed values before perform login.
     */
    fun onLoginClicked(cancelTransfers: Boolean) {
        if (cancelTransfers) {
            viewModelScope.launch { cancelTransfersUseCase() }
        }

        with(state.value) {
            val typedEmail = accountSession?.email
            val emailError = when {
                typedEmail.isNullOrEmpty() -> LoginError.EmptyEmail
                !Constants.EMAIL_ADDRESS.matcher(typedEmail).matches() -> LoginError.NotValidEmail
                else -> null
            }
            val passwordError = LoginError.EmptyPassword.takeUnless { !password.isNullOrEmpty() }

            if (emailError != null || passwordError != null) {
                _state.update { state ->
                    state.copy(
                        emailError = emailError,
                        passwordError = passwordError,
                        pressedBackWhileLogin = false,
                    )
                }
            } else {
                viewModelScope.launch {
                    when {
                        ongoingTransfersExistUseCase() -> _state.update { state ->
                            state.copy(ongoingTransfersExist = true, pressedBackWhileLogin = false)
                        }

                        !isConnected -> _state.update { state ->
                            state.copy(
                                isLoginRequired = true,
                                ongoingTransfersExist = null,
                                pressedBackWhileLogin = false,
                                snackbarMessage = triggered(R.string.error_server_connection_problem)
                            )
                        }

                        else -> performLogin()
                    }
                }
            }
        }
    }

    /**
     * Login.
     */
    private fun performLogin(typedEmail: String? = null, typedPassword: String? = null) {
        if (MegaApplication.isLoggingIn) {
            return
        }

        MegaApplication.isLoggingIn = true

        _state.update {
            if (typedEmail != null && typedPassword != null) {
                it.copy(
                    isLoginInProgress = true,
                    is2FARequired = false,
                    accountSession = state.value.accountSession?.copy(email = typedEmail)
                        ?: AccountSession(email = typedEmail),
                    password = typedPassword,
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

        viewModelScope.launch {
            with(state.value) {
                val email = typedEmail ?: accountSession?.email ?: return@launch
                val password = typedPassword ?: this.password ?: return@launch

                runCatching {
                    loginUseCase(
                        email,
                        password,
                        DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }
                    ).collectLatest { status -> status.checkStatus() }
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
    private fun performLoginWith2FA(pin2FA: String) = viewModelScope.launch {
        MegaApplication.isLoggingIn = true
        _state.update { state -> state.copy(multiFactorAuthState = MultiFactorAuthState.Checking) }

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
    fun fastLogin(refreshChatUrl: Boolean) {
        _state.update {
            it.copy(
                isLoginInProgress = false,
                isLoginRequired = false,
                is2FARequired = false,
                isAlreadyLoggedIn = true,
                fetchNodesUpdate = cleanFetchNodesUpdate
            )
        }

        viewModelScope.launch {
            getAccountCredentialsUseCase()?.updateCredentials() ?: return@launch

            if (MegaApplication.isLoggingIn) {
                Timber.w("Another login is processing")
                pendingAction = ACTION_OPEN_APP
                return@launch
            }

            performFastLogin(refreshChatUrl)
        }
    }

    private fun performFastLogin(refreshChatUrl: Boolean) = viewModelScope.launch {
        MegaApplication.isLoggingIn = true
        runCatching {
            fastLoginUseCase(
                state.value.accountSession?.session ?: return@launch,
                refreshChatUrl,
                DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }
            ).collectLatest { status -> status.checkStatus(true) }
        }.onFailure { exception ->
            MegaApplication.isLoggingIn = false
            if (exception !is LoginException) return@onFailure
            exception.loginFailed()
        }
    }

    private fun LoginException.loginFailed(is2FARequest: Boolean = false) =
        _state.update { loginState ->
            //If LoginBlockedAccount will processed at the `onEvent` when receive an EVENT_ACCOUNT_BLOCKED
            //If LoginLoggedOutFromOtherLocation will be handled in the Activity
            val error = this.error
                .takeUnless { this is LoginLoggedOutFromOtherLocation || this is LoginBlockedAccount }
            loginState.copy(
                isLoginInProgress = false,
                isLoginRequired = true,
                is2FAEnabled = is2FARequest,
                is2FARequired = false,
                loginException = this.takeIf { exception -> exception is LoginLoggedOutFromOtherLocation },
                snackbarMessage = error?.let { triggered(it) } ?: consumed()
            )
        }

    private fun LoginStatus.checkStatus(isFastLogin: Boolean = false) = when (this) {
        LoginStatus.LoginStarted -> {
            Timber.d("Login started")
        }

        LoginStatus.LoginSucceed -> {
            //If fast login, state already updated.
            if (!isFastLogin) {
                _state.update {
                    it.copy(
                        isLoginInProgress = false,
                        isLoginRequired = false,
                        is2FARequired = false,
                        isAlreadyLoggedIn = true,
                        fetchNodesUpdate = cleanFetchNodesUpdate,
                        multiFactorAuthState = null
                    )
                }
            }
            fetchNodes()
        }

        LoginStatus.LoginCannotStart -> {
            _state.update {
                it.copy(
                    isLoginInProgress = false,
                    isLoginRequired = true,
                    is2FAEnabled = false,
                    is2FARequired = false
                )
            }
        }
    }

    /**
     * Fetch nodes.
     */
    fun fetchNodes(isRefreshSession: Boolean = false) {
        viewModelScope.launch {
            getAccountCredentialsUseCase()?.updateCredentials() ?: return@launch

            MegaApplication.getInstance().checkEnabledCookies()

            if (isRefreshSession) {
                MegaApplication.isLoggingIn = true
                _state.update { it.copy(fetchNodesUpdate = cleanFetchNodesUpdate) }
            }

            performFetchNodes()
        }
    }

    private fun performFetchNodes() = viewModelScope.launch {
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
        }.onFailure { exception ->
            MegaApplication.isLoggingIn = false
            if (exception !is FetchNodesException) return@launch

            _state.update { state ->
                val messageId =
                    exception.takeUnless { exception is FetchNodesErrorAccess || state.pressedBackWhileLogin }

                state.copy(
                    isLoginInProgress = false,
                    isLoginRequired = true,
                    is2FAEnabled = false,
                    is2FARequired = false,
                    snackbarMessage = messageId?.let { triggered(exception.error) } ?: consumed()
                )
            }
        }
    }

    /**
     * Sets to null ongoingTransfersExistUseCase in state.
     */
    fun resetOngoingTransfers() =
        _state.update { state -> state.copy(ongoingTransfersExist = null) }

    /**
     * Intent set.
     */
    fun intentSet() {
        _state.update { state -> state.copy(intentState = LoginIntentState.AlreadySet) }
    }

    private suspend fun getEnabledFeatures() {
        val enabledFeatures = setOfNotNull(
            AppFeatures.FolderLinkCompose.takeIf { getFeatureFlagValueUseCase(it) }
        )
        _state.update { it.copy(enabledFlags = enabledFeatures) }
    }

    /**
     * Check if given feature flag is enabled or not
     */
    fun isFeatureEnabled(feature: Feature) = state.value.enabledFlags.contains(feature)


    /**
     * Sets snackbarMessage in state as consumed.
     */
    fun onSnackbarMessageConsumed() =
        _state.update { state -> state.copy(snackbarMessage = consumed()) }

    /**
     * Schedule camera upload
     */
    fun scheduleCameraUpload() = viewModelScope.launch { scheduleCameraUploadUseCase() }

    /**
     * Stop camera upload
     */
    fun stopCameraUpload() = viewModelScope.launch { stopCameraUploadUseCase() }

    /**
     * Updates a pin of the 2FA code in state.
     */
    fun on2FAPinChanged(pin: String, index: Int) {
        val updated2FA =
            state.value.twoFAPin.getUpdatedTwoFactorAuthentication(pin = pin, index = index)

        updateTwoFAState(updated2FA)
        updated2FA.getTwoFactorAuthentication()?.apply {
            performLoginWith2FA(this)
        }
    }

    /**
     * Updates 2FA code in state.
     */
    fun on2FAChanged(twoFA: String) = twoFA.getTwoFactorAuthentication()?.apply {
        updateTwoFAState(this)
        performLoginWith2FA(twoFA)
    }

    private fun updateTwoFAState(twoFA: List<String>) {
        _state.update { state ->
            state.copy(
                twoFAPin = twoFA,
                multiFactorAuthState = MultiFactorAuthState.Fixed
                    .takeUnless { state.multiFactorAuthState == MultiFactorAuthState.Failed }
            )
        }
    }

    /**
     * Updates the state to show a message.
     */
    fun setSnackbarMessageId(@StringRes messageId: Int) =
        _state.update { state -> state.copy(snackbarMessage = triggered(messageId)) }

    companion object {
        /**
         * Intent action for showing the login fetching nodes.
         */
        const val ACTION_FORCE_RELOAD_ACCOUNT = "FORCE_RELOAD"

        /**
         * Intent action for opening app.
         */
        private const val ACTION_OPEN_APP = "OPEN_APP"
    }
}
