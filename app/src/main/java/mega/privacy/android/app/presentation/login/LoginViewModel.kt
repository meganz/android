package mega.privacy.android.app.presentation.login

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.middlelayer.installreferrer.InstallReferrerHandler
import mega.privacy.android.app.presentation.extensions.error
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.app.presentation.extensions.newError
import mega.privacy.android.app.presentation.login.LoginViewModel.Companion.ACTION_FORCE_RELOAD_ACCOUNT
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.app.presentation.login.model.RkLink
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.getTwoFactorAuthentication
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.getUpdatedTwoFactorAuthentication
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.exception.LoginException
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.domain.exception.LoginWrongMultiFactorAuth
import mega.privacy.android.domain.exception.QuerySignupLinkException
import mega.privacy.android.domain.exception.login.FetchNodesErrorAccess
import mega.privacy.android.domain.exception.login.FetchNodesException
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.CheckRecoveryKeyUseCase
import mega.privacy.android.domain.usecase.account.ClearUserCredentialsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorLoggedOutFromAnotherLocationUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.ResendVerificationEmailUseCase
import mega.privacy.android.domain.usecase.account.ResumeCreateAccountUseCase
import mega.privacy.android.domain.usecase.account.SetLoggedOutFromAnotherLocationUseCase
import mega.privacy.android.domain.usecase.account.ShouldShowUpgradeAccountUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasPreferencesUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.environment.GetHistoricalProcessExitReasonsUseCase
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.ClearLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.FetchNodesUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.GetSessionUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import mega.privacy.android.domain.usecase.login.LoginWith2FAUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.notifications.ShouldShowNotificationReminderUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.requeststatus.EnableRequestStatusMonitorUseCase
import mega.privacy.android.domain.usecase.requeststatus.MonitorRequestStatusProgressEventUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import mega.privacy.android.domain.usecase.transfers.ResumeTransfersForNotLoggedInInstanceUseCase
import mega.privacy.android.domain.usecase.transfers.paused.CheckIfTransfersShouldBePausedUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import mega.privacy.mobile.analytics.event.AccountRegistrationEvent
import mega.privacy.mobile.analytics.event.MultiFactorAuthVerificationFailedEvent
import mega.privacy.mobile.analytics.event.MultiFactorAuthVerificationSuccessEvent
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
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
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
    private val establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase,
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase,
    private val saveEphemeralCredentialsUseCase: SaveEphemeralCredentialsUseCase,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val monitorAccountBlockedUseCase: MonitorAccountBlockedUseCase,
    private val getTimelinePhotosUseCase: GetTimelinePhotosUseCase,
    private val getLastRegisteredEmailUseCase: GetLastRegisteredEmailUseCase,
    private val clearLastRegisteredEmailUseCase: ClearLastRegisteredEmailUseCase,
    private val installReferrerHandler: InstallReferrerHandler,
    @LoginMutex val loginMutex: Mutex,
    private val clearUserCredentialsUseCase: ClearUserCredentialsUseCase,
    private val getHistoricalProcessExitReasonsUseCase: GetHistoricalProcessExitReasonsUseCase,
    private val enableRequestStatusMonitorUseCase: EnableRequestStatusMonitorUseCase,
    private val monitorRequestStatusProgressEventUseCase: MonitorRequestStatusProgressEventUseCase,
    private val checkIfTransfersShouldBePausedUseCase: CheckIfTransfersShouldBePausedUseCase,
    private val isFirstLaunchUseCase: IsFirstLaunchUseCase,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
    private val resumeCreateAccountUseCase: ResumeCreateAccountUseCase,
    private val checkRecoveryKeyUseCase: CheckRecoveryKeyUseCase,
    monitorLoggedOutFromAnotherLocationUseCase: MonitorLoggedOutFromAnotherLocationUseCase,
    private val setLoggedOutFromAnotherLocationUseCase: SetLoggedOutFromAnotherLocationUseCase,
    private val shouldShowNotificationReminderUseCase: ShouldShowNotificationReminderUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val shouldShowUpgradeAccountUseCase: ShouldShowUpgradeAccountUseCase,
    private val ephemeralCredentialManager: EphemeralCredentialManager,
    private val resumeTransfersForNotLoggedInInstanceUseCase: ResumeTransfersForNotLoggedInInstanceUseCase,
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

    /**
     * Monitor if the user is logged out from another location.
     */
    val monitorLoggedOutFromAnotherLocation = monitorLoggedOutFromAnotherLocationUseCase()

    private var pendingAction: String? = null

    private val cleanFetchNodesUpdate by lazy { FetchNodesUpdate() }

    private var performFetchNodesJob: Job? = null

    init {
        enableAndMonitorRequestStatusProgressEvent()
        viewModelScope.launch {
            runCatching {
                getHistoricalProcessExitReasonsUseCase()
            }.onFailure {
                Timber.e(it)
            }
        }
        setupInitialState()
        getStartScreen()
    }

    private fun getStartScreen() = viewModelScope.launch {
        runCatching { monitorEphemeralCredentialsUseCase().firstOrNull() }
            .onSuccess { ephemeral ->
                if (ephemeral != null && !ephemeral.session.isNullOrEmpty()) {
                    setPendingFragmentToShow(LoginFragmentType.ConfirmEmail)
                    _state.update { it.copy(temporalEmail = ephemeral.email) }
                    resumeCreateAccount(ephemeral.session.orEmpty())
                    return@launch
                }
            }

        val session = getSession()
        val visibleFragment =
            savedStateHandle.get<Int>(Constants.VISIBLE_FRAGMENT) ?: run {
                if (session.isNullOrEmpty()) Constants.TOUR_FRAGMENT else Constants.LOGIN_FRAGMENT
            }

        setPendingFragmentToShow(LoginFragmentType.entries.find { it.value == visibleFragment }
            ?: LoginFragmentType.Login)
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
     * Reset some states values.
     */
    private fun setupInitialState() {
        viewModelScope.launch {
            merge(
                flow { emit(getSessionUseCase()) }.map { session ->
                    { state: LoginState ->
                        resumeTransfersForNotLoggedInInstance(session)

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
                }.catch { Timber.e(it) },
                flow { emit(rootNodeExistsUseCase()) }.map { exists ->
                    { state: LoginState -> state.copy(rootNodesExists = exists) }
                }.catch { Timber.e(it) },
                flow { emit(hasPreferencesUseCase()) }.map { hasPreferences ->
                    { state: LoginState -> state.copy(hasPreferences = hasPreferences) }
                }.catch { Timber.e(it) },
                flow { emit(hasCameraSyncEnabledUseCase()) }.map { hasCameraSyncEnabled ->
                    { state: LoginState -> state.copy(hasCUSetting = hasCameraSyncEnabled) }
                }.catch { Timber.e(it) },
                flow { emit(isCameraUploadsEnabledUseCase()) }.map { isCameraSyncEnabled ->
                    { state: LoginState -> state.copy(isCUSettingEnabled = isCameraSyncEnabled) }
                }.catch { Timber.e(it) },
                monitorFetchNodesFinishUseCase().catch { Timber.e(it) }.map {
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
                flow { emit(isFirstLaunchUseCase()) }.catch { Timber.e(it) }.map { isFirstLaunch ->
                    { state: LoginState ->
                        state.copy(isFirstTimeLaunch = isFirstLaunch)
                    }
                },
                monitorThemeModeUseCase().catch { Timber.e(it) }.map { themeMode ->
                    { state: LoginState ->
                        state.copy(themeMode = themeMode)
                    }
                },
                flow {
                    emit(shouldShowNotificationReminderUseCase())
                }.catch { Timber.e(it) }
                    .map { shouldShowNotificationPermission ->
                        { state: LoginState ->
                            state.copy(shouldShowNotificationPermission = shouldShowNotificationPermission)
                        }
                    },
            ).collect {
                _state.update(it)
            }
        }

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
                    if (it.type == AccountBlockedType.VERIFICATION_EMAIL) resetLoginState() else stopLogin()
                }
        }
    }

    private fun resumeTransfersForNotLoggedInInstance(session: String?) {
        if (session == null) {
            viewModelScope.launch {
                runCatching { resumeTransfersForNotLoggedInInstanceUseCase() }
                    .onFailure { Timber.e(it) }
            }
        }
    }

    /**
     * Sets confirm email fragment as pending in state.
     */
    fun setIsWaitingForConfirmAccount() {
        _state.update { state -> state.copy(isPendingToShowFragment = LoginFragmentType.ConfirmEmail) }
    }

    /**
     * Sets tour as pending fragment in state.
     */
    private fun setTourAsPendingFragment() {
        _state.update { state -> state.copy(isPendingToShowFragment = LoginFragmentType.Tour) }
    }

    /**
     * Set pending fragment to show
     *
     * @param fragmentType
     */
    fun setPendingFragmentToShow(fragmentType: LoginFragmentType) {
        _state.update { state -> state.copy(isPendingToShowFragment = fragmentType) }
    }

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

    fun resetLoginState() {
        performFetchNodesJob?.cancel()
        _state.update {
            it.copy(
                fetchNodesUpdate = null,
                isLoginInProgress = false,
                isLoginRequired = true
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
                Timber.w(it, "Exception in local logout.")
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
    fun setTemporalCredentials(credentials: EphemeralCredentials) {
        ephemeralCredentialManager.setEphemeralCredential(credentials)
    }

    /**
     * True if there is a not null email and a not null password, false otherwise.
     */
    fun checkTemporalCredentials(): Boolean {
        val ephemeralCredentials = ephemeralCredentialManager.getEphemeralCredential()
        return if (ephemeralCredentials != null && !ephemeralCredentials.email.isNullOrEmpty() && !ephemeralCredentials.password.isNullOrEmpty()) {
            performLogin(ephemeralCredentials.email, ephemeralCredentials.password)
            true
        } else {
            false
        }
    }

    /**
     * Checks a signup link.
     */
    fun checkSignupLink(link: String) {
        // avoid rotating the screen calling this method again
        if (state.value.intentState == LoginIntentState.AlreadySet
            || state.value.isCheckingSignupLink
        ) return
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
            var isLoginInProgress = false
            val messageId = if (result.isSuccess) {
                accountConfirmed = true
                newAccountSession = state.value.accountSession?.copy(email = result.getOrNull())
                    ?: AccountSession(email = result.getOrNull())
                if (checkTemporalCredentials()) {
                    isLoginInProgress = true
                    null
                } else {
                    R.string.account_confirmed
                }
            } else {
                (result.exceptionOrNull() as QuerySignupLinkException).messageId
            }

            _state.update { state ->
                val isAccountConfirmed =
                    if (accountConfirmed == true) true else state.isAccountConfirmed

                state.copy(
                    isLoginRequired = true,
                    isLoginInProgress = isLoginInProgress,
                    temporalEmail = newAccountSession?.email,
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
                    ).collectLatest { status -> status.checkStatus(email = email) }
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
                    val email = accountSession?.email ?: return@launch
                    loginWith2FAUseCase(
                        email,
                        password ?: return@launch,
                        pin2FA,
                        DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }
                    ).collectLatest { status ->
                        status.checkStatus(email = email)
                        if (status == LoginStatus.LoginSucceed) {
                            _state.update { it.copy(multiFactorAuthState = MultiFactorAuthState.Passed) }
                            Analytics.tracker.trackEvent(MultiFactorAuthVerificationSuccessEvent)
                        }
                    }
                }.onFailure { exception ->
                    if (exception !is LoginException) return@onFailure

                    if (exception is LoginWrongMultiFactorAuth) {
                        Analytics.tracker.trackEvent(MultiFactorAuthVerificationFailedEvent)
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
                isFastLoginInProgress = true,
                fetchNodesUpdate = cleanFetchNodesUpdate
            )
        }

        viewModelScope.launch {
            getAccountCredentialsUseCase()?.updateCredentials() ?: return@launch
            var retry = 1
            while (loginMutex.isLocked && retry <= 3) {
                Timber.d("Wait for the isLoggingIn lock to be available")
                delay(1000L * retry)
                if (rootNodeExistsUseCase()) {
                    Timber.d("Root node exists")
                    _state.update { it.copy(intentState = LoginIntentState.ReadyForFinalSetup) }
                    return@launch
                }
                retry++
            }
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
            ).collectLatest { status -> status.checkStatus(isFastLogin = true) }
        }.onFailure { exception ->
            if (exception !is LoginException) return@onFailure
            exception.loginFailed()
        }
    }

    private fun LoginException.loginFailed(is2FARequest: Boolean = false) =
        _state.update { loginState ->
            //If LoginBlockedAccount will processed at the `onEvent` when receive an EVENT_ACCOUNT_BLOCKED
            //If LoginLoggedOutFromOtherLocation will be handled in the Activity
            val snackbarMessage = this.newError
                .takeIf {
                    // in the new design we don't show snackbar for these errors
                    this !is LoginTooManyAttempts
                            && this !is LoginWrongEmailOrPassword
                            && this !is LoginLoggedOutFromOtherLocation
                }?.let { triggered(it) }
            loginState.copy(
                isLoginInProgress = false,
                isLoginRequired = true,
                is2FAEnabled = is2FARequest,
                is2FARequired = false,
                fetchNodesUpdate = null,
                loginException = this,
                snackbarMessage = snackbarMessage ?: consumed()
            )
        }

    private suspend fun LoginStatus.checkStatus(
        isFastLogin: Boolean = false,
        email: String? = null,
    ) = when (this) {
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
                        loginTemporaryError = null,
                        isLoginInProgress = false,
                        isLoginRequired = false,
                        is2FARequired = false,
                        isAlreadyLoggedIn = true,
                        isFastLoginInProgress = false,
                        fetchNodesUpdate = cleanFetchNodesUpdate,
                        multiFactorAuthState = null
                    )
                }
            }
            if (!isFastLogin) {
                shouldShowUpgradeAccount()
            }
            val isSingleActivityEnabled = runCatching {
                getFeatureFlagValueUseCase(AppFeatures.SingleActivity)
            }.getOrDefault(false)
            if (!isSingleActivityEnabled) {
                fetchNodes()
            }
            sendAnalyticsEventIfFirstTimeLogin(email)
        }

        LoginStatus.LoginCannotStart -> {
            Timber.d("Login cannot start")
            _state.update {
                it.copy(
                    loginTemporaryError = null,
                    isLoginInProgress = false,
                    isFastLoginInProgress = false,
                    isLoginRequired = true,
                    is2FAEnabled = false,
                    is2FARequired = false
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

    /**
     * Should show upgrade account
     */
    suspend fun shouldShowUpgradeAccount() {
        _state.update { it.copy(shouldShowUpgradeAccount = shouldShowUpgradeAccountUseCase()) }
        Timber.d("Should show upgrade account: ${state.value.shouldShowUpgradeAccount}")
    }

    /**
     * Fetch nodes.
     */
    fun fetchNodes(isRefreshSession: Boolean = false) {
        viewModelScope.launch {
            if (isRefreshSession) {
                getAccountCredentialsUseCase()?.updateCredentials() ?: return@launch
            }

            MegaApplication.getInstance().checkEnabledCookies()

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
                        prefetchTimeline()
                        _state.update {
                            it.copy(
                                intentState = LoginIntentState.ReadyForFinalSetup,
                                fetchNodesUpdate = update
                            )
                        }
                        startWorkers()
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
                        exception.takeUnless { exception is FetchNodesErrorAccess || state.pressedBackWhileLogin }

                    state.copy(
                        isLoginInProgress = false,
                        isLoginRequired = true,
                        is2FAEnabled = false,
                        is2FARequired = false,
                        snackbarMessage = messageId?.let { triggered(exception.error) }
                            ?: consumed()
                    )
                }
            }
        }
    }

    private suspend fun prefetchTimeline() {
        runCatching {
            if (!getFeatureFlagValueUseCase(AppFeatures.PrefetchTimeline)) return@runCatching
            getTimelinePhotosUseCase()
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun startWorkers() {
        /* In case the app crash or restarts, we need to sync some tasks */
        val syncTasks = listOf<suspend () -> Unit>(
            { establishCameraUploadsSyncHandlesUseCase() },
            { checkIfTransfersShouldBePausedUseCase() }
        )

        syncTasks.forEach { task ->
            runCatching {
                task()
            }.onFailure { error ->
                Timber.e(error, "Task failed")
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
    } ?: run {
        _state.update { state ->
            state.copy(
                multiFactorAuthState = MultiFactorAuthState.Fixed
                    .takeUnless { state.multiFactorAuthState == MultiFactorAuthState.Failed })
        }
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
     * Get session
     */
    private suspend fun getSession() =
        runCatching { getSessionUseCase() }.getOrNull()

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

    /**
     * Clear ephemeral
     *
     */
    fun clearEphemeral() {
        viewModelScope.launch {
            runCatching { clearEphemeralCredentialsUseCase() }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Send analytics event if the current logged email matches
     * with the last registration attempted email
     */
    fun sendAnalyticsEventIfFirstTimeLogin(loggedEmail: String?) {
        if (loggedEmail.isNullOrEmpty()) return
        viewModelScope.launch {
            val lastRegisteredEmail =
                runCatching { getLastRegisteredEmailUseCase() }.getOrNull()
            if (loggedEmail == lastRegisteredEmail) {
                runCatching {
                    installReferrerHandler.getDetails()
                }.onSuccess { details ->
                    Analytics.tracker.trackEvent(
                        AccountRegistrationEvent(
                            referrerUrl = details.referrerUrl,
                            referrerClickTime = details.referrerClickTime,
                            appInstallTime = details.appInstallTime
                        )
                    )
                }.onFailure {
                    Timber.e(it)
                }
                runCatching {
                    clearLastRegisteredEmailUseCase()
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    /**
     * Clear user credentials
     *
     */
    fun clearUserCredentials() {
        viewModelScope.launch {
            clearUserCredentialsUseCase()
        }
    }

    fun triggerAccountBlockedEvent(accountBlockedEvent: AccountBlockedEvent) {
        _state.update { it.copy(accountBlockedEvent = triggered(accountBlockedEvent)) }
    }

    fun resetAccountBlockedEvent() {
        _state.update { it.copy(accountBlockedEvent = consumed()) }
    }

    fun resetResendVerificationEmailEvent() {
        _state.update { it.copy(resendVerificationEmailEvent = consumed()) }
    }

    fun resendVerificationEmail() = viewModelScope.launch {
        runCatching {
            resendVerificationEmailUseCase()
        }.onSuccess {
            _state.update {
                it.copy(
                    resendVerificationEmailEvent = triggered(true)
                )
            }
        }.onFailure { throwable ->
            Timber.e(throwable)
            _state.update {
                it.copy(
                    resendVerificationEmailEvent = triggered(false)
                )
            }
        }
    }

    /**
     * Check recovery key
     *
     * @param link the recovery key link
     * @param recoveryKey the recovery key
     */
    fun checkRecoveryKey(link: String, recoveryKey: String) = viewModelScope.launch {
        runCatching {
            checkRecoveryKeyUseCase(link, recoveryKey)
        }.onSuccess {
            _state.update {
                it.copy(
                    checkRecoveryKeyEvent = triggered(Result.success(RkLink(link, recoveryKey)))
                )
            }
        }.onFailure { throwable ->
            Timber.e(throwable)
            _state.update {
                it.copy(
                    checkRecoveryKeyEvent = triggered(Result.failure(throwable))
                )
            }
        }
    }

    /**
     * Check if the account is blocked
     */
    fun onCheckRecoveryKeyEventConsumed() {
        _state.update { it.copy(checkRecoveryKeyEvent = consumed()) }
    }

    /**
     * Resume create account
     */
    suspend fun resumeCreateAccount(session: String) {
        runCatching {
            resumeCreateAccountUseCase(session)
        }.onFailure {
            cancelCreateAccount()
        }
    }

    fun cancelCreateAccount() {
        clearEphemeral()
        clearUserCredentials()
        setTourAsPendingFragment()
    }

    /**
     * Set handled logged out from another location
     */
    fun setHandledLoggedOutFromAnotherLocation() {
        viewModelScope.launch {
            setLoggedOutFromAnotherLocationUseCase(false)
        }
    }

    /**
     * On request recovery key
     *
     * @param link the recovery key link
     */
    fun onRequestRecoveryKey(link: String) {
        _state.update { it.copy(recoveryKeyLink = link) }
    }

    /**
     * On recovery key consumed
     */
    fun onRecoveryKeyConsumed() {
        _state.update { it.copy(recoveryKeyLink = null) }
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
