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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
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
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.login.EphemeralCredentials
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
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasPreferencesUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.FetchNodesUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.GetSessionUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import mega.privacy.android.domain.usecase.login.LoginWith2FAUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.SaveEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadWorkerUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
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
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val loggingSettings: LegacyLoggingSettings,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase,
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val hasPreferencesUseCase: HasPreferencesUseCase,
    private val hasCameraSyncEnabledUseCase: HasCameraSyncEnabledUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val querySignupLinkUseCase: QuerySignupLinkUseCase,
    private val cancelTransfersUseCase: CancelTransfersUseCase,
    private val localLogoutUseCase: LocalLogoutUseCase,
    private val loginUseCase: LoginUseCase,
    private val loginWith2FAUseCase: LoginWith2FAUseCase,
    private val fastLoginUseCase: FastLoginUseCase,
    private val fetchNodesUseCase: FetchNodesUseCase,
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase,
    private val saveEphemeralCredentialsUseCase: SaveEphemeralCredentialsUseCase,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val monitorAccountBlockedUseCase: MonitorAccountBlockedUseCase,
    private val getTimelinePhotosUseCase: GetTimelinePhotosUseCase,
    private val startDownloadWorkerUseCase: StartDownloadWorkerUseCase,
    @LoginMutex private val loginMutex: Mutex
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
        get() = isConnectedToInternetUseCase()

    private var pendingAction: String? = null

    private val cleanFetchNodesUpdate by lazy { FetchNodesUpdate() }

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
                            accountSession = state.accountSession?.copy(session = session)
                                ?: AccountSession(session = session),
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
                flowOf(isCameraUploadsEnabledUseCase()).map { isCameraSyncEnabled ->
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

        viewModelScope.launch {
            monitorAccountBlockedUseCase()
                .filter {
                    it.type == AccountBlockedType.TOS_COPYRIGHT
                            || it.type == AccountBlockedType.TOS_NON_COPYRIGHT
                }.collectLatest { stopLogin() }
        }
    }

    /**
     * Check if given feature flag is enabled or not
     */
    fun isFeatureEnabled(feature: Feature) = state.value.enabledFlags.contains(feature)

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
     * Set pending fragment to show
     *
     * @param fragmentType
     */
    fun setPendingFragmentToShow(fragmentType: LoginFragmentType) =
        _state.update { state -> state.copy(isPendingToShowFragment = fragmentType) }

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
                accountSession = null,
                password = null,
                fetchNodesUpdate = null,
                isFirstTime = false,
                isAlreadyLoggedIn = false,
                pressedBackWhileLogin = true,
                is2FAEnabled = false,
                is2FARequired = false,
                twoFAPin = listOf("", "", "", "", "", ""),
                multiFactorAuthState = null,
                isAccountConfirmed = false,
                rootNodesExists = false,
                temporalEmail = null,
                temporalPassword = null,
                hasPreferences = false,
                hasCUSetting = false,
                isCUSettingEnabled = false,
                isLocalLogoutInProgress = true,
                isLoginRequired = true,
                isLoginInProgress = false,
                loginException = null,
                ongoingTransfersExist = null,
                isCheckingSignupLink = false
            )
        }
        viewModelScope.launch {
            runCatching {
                localLogoutUseCase(
                    DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi },
                )
            }.onFailure {
                Timber.w("Exception in local logout.", it)
            }
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
            var newAccountSession: AccountSession? = null
            val messageId = if (result.isSuccess) {
                accountConfirmed = true
                newAccountSession = state.value.accountSession?.copy(email = result.getOrNull())
                    ?: AccountSession(email = result.getOrNull())
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
                    accountSession = newAccountSession,
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
            val typedEmail = accountSession?.email?.lowercase()?.trim()
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
        if (loginMutex.isLocked) {
            return
        }

        LoginActivity.isBackFromLoginPage = false

        _state.update {
            if (typedEmail != null && typedPassword != null) {
                it.copy(
                    isLoginInProgress = true,
                    is2FARequired = false,
                    accountSession = state.value.accountSession?.copy(email = typedEmail)
                        ?: AccountSession(email = typedEmail),
                    password = typedPassword,
                    ongoingTransfersExist = null,
                    pressedBackWhileLogin = false,
                )
            } else {
                it.copy(
                    isLoginInProgress = true,
                    is2FARequired = false,
                    ongoingTransfersExist = null,
                    pressedBackWhileLogin = false,
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
                    if (exception !is LoginException) return@onFailure

                    if (exception is LoginMultiFactorAuthRequired) {
                        _state.update {
                            it.copy(
                                isLoginInProgress = false,
                                is2FAEnabled = true,
                                isLoginRequired = false,
                                is2FARequired = true,
                                isFirstTime2FA = triggered
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
    private fun performLoginWith2FA(pin2FA: String) {
        if (loginMutex.isLocked) {
            return
        }

        viewModelScope.launch {
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

            if (loginMutex.isLocked) {
                Timber.w("Another login is processing")
                pendingAction = ACTION_OPEN_APP
                return@launch
            }

            performFastLogin(refreshChatUrl)
        }
    }

    private fun performFastLogin(refreshChatUrl: Boolean) = viewModelScope.launch {
        runCatching {
            fastLoginUseCase(
                state.value.accountSession?.session ?: return@launch,
                refreshChatUrl,
                DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }
            ).collectLatest { status -> status.checkStatus(true) }
        }.onFailure { exception ->
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
            Timber.d("Login finished")
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
            Timber.d("Login cannot start")
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
                _state.update { it.copy(fetchNodesUpdate = cleanFetchNodesUpdate) }
            }

            Timber.d("fetch nodes started")
            performFetchNodes()
        }
    }

    private fun performFetchNodes() = viewModelScope.launch {
        runCatching {
            fetchNodesUseCase().collectLatest { update ->
                if (update.progress?.floatValue == 1F) {
                    Timber.d("fetch nodes finished")
                    prefetchTimeline()

                    _state.update {
                        it.copy(
                            intentState = LoginIntentState.ReadyForFinalSetup,
                            fetchNodesUpdate = update
                        )
                    }

                    if (getFeatureFlagValueUseCase(AppFeatures.DownloadWorker)) {
                        /*In case the app crash or restarts, we need to restart the worker
                        in order to monitor current transfers and update the related notification.*/
                        startDownloadWorkerUseCase()
                    }
                } else {
                    Timber.d("fetch nodes update")
                    _state.update { it.copy(fetchNodesUpdate = update) }
                }
            }
        }.onFailure { exception ->
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

    private suspend fun prefetchTimeline() {
        if (!getFeatureFlagValueUseCase(AppFeatures.PrefetchTimeline)) return
        getTimelinePhotosUseCase()
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

    /**
     * Sets snackbarMessage in state as consumed.
     */
    fun onSnackbarMessageConsumed() =
        _state.update { state -> state.copy(snackbarMessage = consumed()) }

    /**
     * Stop camera upload
     */
    fun stopCameraUploads() =
        viewModelScope.launch {
            runCatching { stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable) }
                .onFailure { Timber.e(it) }
        }

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
    fun on2FAChanged(twoFA: String) = twoFA.getTwoFactorAuthentication()?.let {
        updateTwoFAState(it)
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

    /**
     * Sets isFirstTime2FA as consumed.
     */
    fun onFirstTime2FAConsumed() =
        _state.update { state -> state.copy(isFirstTime2FA = consumed) }

    /**
     * Get ephemeral
     *
     */
    suspend fun getEphemeral() =
        runCatching { monitorEphemeralCredentialsUseCase().firstOrNull() }.getOrNull()

    /**
     * Set temporal email
     *
     * @param email
     */
    fun setTemporalEmail(email: String) {
        viewModelScope.launch {
            runCatching {
                val ephemeral = monitorEphemeralCredentialsUseCase().firstOrNull() ?: return@launch
                clearEphemeralCredentialsUseCase()
                saveEphemeralCredentialsUseCase(ephemeral.copy(email = email))
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Save ephemeral
     *
     * @param ephemeral
     */
    fun saveEphemeral(ephemeral: EphemeralCredentials) {
        viewModelScope.launch {
            runCatching {
                clearEphemeralCredentialsUseCase()
                saveEphemeralCredentialsUseCase(ephemeral)
            }.onFailure { Timber.e(it) }
        }
    }

    fun clearEphemeral() {
        viewModelScope.launch {
            runCatching { clearEphemeralCredentialsUseCase() }
                .onFailure { Timber.e(it) }
        }
    }

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
