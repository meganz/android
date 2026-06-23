package mega.privacy.android.app.presentation.login

import android.util.Base64
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.content.navigation.FetchNodeProvider
import mega.privacy.android.app.middlelayer.installreferrer.InstallReferrerHandler
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.app.presentation.extensions.newError
import mega.privacy.android.app.presentation.login.mapper.AccountBlockedTypeStringMapper
import mega.privacy.android.app.presentation.login.model.AccountBlockedUiState
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.app.presentation.login.model.RkLink
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.isValid2FA
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ConstantsUrl.recoveryUrl
import mega.privacy.android.app.utils.ConstantsUrl.recoveryUrlWithEmail
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.login.GoogleSignInResult
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.LoginException
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.domain.exception.LoginWrongMultiFactorAuth
import mega.privacy.android.domain.exception.QuerySignupLinkException
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.CheckRecoveryKeyUseCase
import mega.privacy.android.domain.usecase.account.ClearUserCredentialsUseCase
import mega.privacy.android.domain.usecase.account.CreateAccountUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.account.ResendVerificationEmailUseCase
import mega.privacy.android.domain.usecase.account.ResumeCreateAccountUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.environment.GetHistoricalProcessExitReasonsUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.DecodeGoogleIdTokenUseCase
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import mega.privacy.android.domain.usecase.login.LoginWith2FAUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.setting.GetMiscFlagsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import mega.privacy.android.domain.usecase.transfers.ResumeTransfersForNotLoggedInInstanceUseCase
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AccountRegistrationEvent
import mega.privacy.mobile.analytics.event.MultiFactorAuthVerificationFailedEvent
import mega.privacy.mobile.analytics.event.MultiFactorAuthVerificationSuccessEvent
import timber.log.Timber
import javax.inject.Inject

internal const val GOOGLE_SIGN_IN_PENDING_SESSION = "google-sign-in-pending-verification"

/**
 * View Model for Login and registration flows
 *
 * @property state View state as [LoginState]
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase,
    private val querySignupLinkUseCase: QuerySignupLinkUseCase,
    private val cancelTransfersUseCase: CancelTransfersUseCase,
    private val localLogoutUseCase: LocalLogoutUseCase,
    private val loginUseCase: LoginUseCase,
    private val loginWith2FAUseCase: LoginWith2FAUseCase,
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase,
    private val monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase,
    private val saveEphemeralCredentialsUseCase: SaveEphemeralCredentialsUseCase,
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase,
    private val monitorAccountBlockedUseCase: MonitorAccountBlockedUseCase,
    private val getLastRegisteredEmailUseCase: GetLastRegisteredEmailUseCase,
    private val installReferrerHandler: InstallReferrerHandler,
    @LoginMutex val loginMutex: Mutex,
    private val clearUserCredentialsUseCase: ClearUserCredentialsUseCase,
    private val getHistoricalProcessExitReasonsUseCase: GetHistoricalProcessExitReasonsUseCase,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
    private val resumeCreateAccountUseCase: ResumeCreateAccountUseCase,
    private val checkRecoveryKeyUseCase: CheckRecoveryKeyUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val ephemeralCredentialManager: EphemeralCredentialManager,
    private val resumeTransfersForNotLoggedInInstanceUseCase: ResumeTransfersForNotLoggedInInstanceUseCase,
    private val getMiscFlagsUseCase: GetMiscFlagsUseCase,
    private val getDomainNameUseCase: GetDomainNameUseCase,
    private val monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
    private val fetchNodeProvider: FetchNodeProvider,
    private val accountBlockedTypeStringMapper: AccountBlockedTypeStringMapper,
    private val decodeGoogleIdTokenUseCase: DecodeGoogleIdTokenUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
) : ViewModel() {
    private val is2FARequited = savedStateHandle[IS_2FA_REQUIRED] ?: false

    private val _state = MutableStateFlow(
        LoginState(
            is2FARequired = is2FARequited,
        )
    )
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

    private val handledLinks = mutableSetOf<HandledLinks>()

    init {
        Timber.d("LoginViewModel init $this")
        viewModelScope.launch {
            runCatching {
                getHistoricalProcessExitReasonsUseCase()
            }.onFailure {
                Timber.e(it)
            }
        }
        viewModelScope.launch {
            monitorMiscLoadedUseCase().collect {
                _state.update {
                    it.copy(miscFlagLoaded = true)
                }
            }
        }
        setupInitialState()
        getStartScreen()
    }

    private fun getStartScreen() = viewModelScope.launch {
        runCatching { monitorEphemeralCredentialsUseCase().firstOrNull() }
            .onSuccess { ephemeral ->
                if (ephemeral != null && !ephemeral.session.isNullOrEmpty()) {
                    if (ephemeral.session == GOOGLE_SIGN_IN_PENDING_SESSION) {
                        runCatching { clearEphemeralCredentialsUseCase() }
                            .onFailure { Timber.e(it, "[GSIGN] Failed to clear Google ephemeral credentials") }
                        setPendingFragmentToShow(LoginScreen.LoginScreen)
                        return@launch
                    } else {
                        setPendingFragmentToShow(LoginScreen.ConfirmEmail)
                        _state.update { it.copy(temporalEmail = ephemeral.email) }
                        resumeCreateAccount(ephemeral.session.orEmpty())
                        return@launch
                    }
                }
            }

        val visibleFragment = savedStateHandle.get<Int>(Constants.VISIBLE_FRAGMENT)
        LoginScreen.entries.find { it.value == visibleFragment }?.let {
            setPendingFragmentToShow(it)
        }
    }

    /**
     * Reset some states values.
     */
    private fun setupInitialState() {
        viewModelScope.launch {
            merge(
                monitorUserCredentialsUseCase().map { it?.session }.map { session ->
                    { state: LoginState ->
                        resumeTransfersForNotLoggedInInstance(session)
                        if (state.intentState == null) {
                            val is2FARequired = savedStateHandle[IS_2FA_REQUIRED] ?: false
                            val savedEmail = savedStateHandle.get<String>(PENDING_2FA_EMAIL)
                            val savedPassword = savedStateHandle.get<String>(PENDING_2FA_PASSWORD)
                            state.copy(
                                intentState = LoginIntentState.ReadyForInitialSetup,
                                accountSession = state.accountSession?.copy(
                                    session = session,
                                    email = savedEmail
                                ) ?: AccountSession(session = session, email = savedEmail),
                                password = savedPassword,
                                isAccountConfirmed = false,
                                isLoginRequired = !is2FARequired && session == null,
                            )
                        } else {
                            state.copy(
                                accountSession = state.accountSession?.copy(session = session)
                                    ?: AccountSession(session = session)
                            )
                        }
                    }
                }.catch { Timber.e(it) },
                monitorThemeModeUseCase().catch { Timber.e(it) }.map { themeMode ->
                    { state: LoginState ->
                        state.copy(themeMode = themeMode)
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
                    val mappedText = accountBlockedTypeStringMapper(it)
                    triggerAccountBlockedEvent(it.copy(text = mappedText))
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
        _state.update { state -> state.copy(isPendingToShowFragment = triggered(LoginScreen.ConfirmEmail)) }
    }

    /**
     * Sets tour as pending fragment in state.
     */
    private fun setTourAsPendingFragment() {
        _state.update { state -> state.copy(isPendingToShowFragment = triggered(LoginScreen.Tour)) }
    }

    /**
     * Set pending fragment to show
     *
     * @param fragmentType
     */
    fun setPendingFragmentToShow(fragmentType: LoginScreen) {
        _state.update { state ->
            // Clear any stale email/password validation errors when (re-)entering the login form,
            // otherwise they persist after navigating back to the tour and reopening it. See AND-23619.
            val clearErrors = fragmentType == LoginScreen.LoginScreen
            state.copy(
                isPendingToShowFragment = triggered(fragmentType),
                emailError = if (clearErrors) null else state.emailError,
                passwordError = if (clearErrors) null else state.passwordError,
            )
        }
    }

    /**
     * Update state with isPendingToShowFragment as null.
     */
    fun isPendingToShowFragmentConsumed() {
        _state.update { state -> state.copy(isPendingToShowFragment = consumed()) }
    }

    fun resetLoginState() {
        _state.update {
            it.copy(
                isLoginInProgress = false,
                isLoginRequired = true
            )
        }
    }

    /**
     * Stops logging in.
     */
    fun stopLogin(isPerformLocalLogOut: Boolean = true) {
        savedStateHandle[IS_2FA_REQUIRED] = false
        savedStateHandle.remove<String>(PENDING_2FA_EMAIL)
        savedStateHandle.remove<String>(PENDING_2FA_PASSWORD)
        _state.update {
            it.copy(
                accountSession = null,
                password = null,
                is2FARequired = false,
                multiFactorAuthState = null,
                isAccountConfirmed = false,
                temporalEmail = null,
                isLoginRequired = true,
                isLoginInProgress = false,
                loginException = null,
                ongoingTransfersExist = null,
            )
        }
        if (isPerformLocalLogOut) {
            viewModelScope.launch {
                runCatching {
                    localLogoutUseCase(
                        DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi },
                    )
                }.onFailure {
                    Timber.w(it, "Exception in local logout.")
                }
            }
        }
    }

    /**
     * Starts MEGA login with a Google ID token obtained from Credential Manager.
     * Reuses the regular login flow; if the account doesn't exist yet, it's
     * auto-created with the Google sub as the password and login is retried.
     *
     * @param idToken The raw Google ID token JWT.
     */
    fun onGoogleSignIn(idToken: String) {
        if (loginMutex.isLocked) return

        viewModelScope.launch {
            val result = runCatching { decodeGoogleIdTokenUseCase(idToken) }
                .onFailure {
                    Timber.e(it, "[GSIGN] JWT decode failed")
                    setSnackbarMessageId(sharedR.string.google_sign_in_failed)
                }
                .getOrNull() ?: return@launch

            _state.update {
                it.copy(
                    isLoginInProgress = true,
                    is2FARequired = false,
                    accountSession = it.accountSession?.copy(email = result.email)
                        ?: AccountSession(email = result.email),
                    password = result.sub,
                    ongoingTransfersExist = null,
                )
            }

            performGoogleSignInLogin(result)
        }
    }

    private suspend fun performGoogleSignInLogin(result: GoogleSignInResult) {
        fetchNodeProvider.setLoginByAccount()
        runCatching {
            loginUseCase(
                result.email,
                result.sub,
                DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }
            ).collectLatest { status -> status.checkStatus(email = result.email) }
        }.onFailure { exception ->
            if (exception !is LoginException) return@onFailure

            when {
                exception is LoginMultiFactorAuthRequired -> handleGoogleSignIn2FA(result)

                exception is LoginRequireValidation -> {
                    Timber.d("[GSIGN] Account exists but not validated - navigating to ConfirmEmail")
                    sendToConfirmEmailScreen(result.toEphemeralCredentials())
                }

                exception is LoginWrongEmailOrPassword -> autoCreateGoogleAccount(result)

                else -> {
                    _state.update {
                        it.copy(
                            password = null,
                            accountSession = it.accountSession?.copy(email = null),
                        )
                    }
                    exception.loginFailed()
                }
            }
        }
    }

    private fun handleGoogleSignIn2FA(result: GoogleSignInResult) {
        savedStateHandle[IS_2FA_REQUIRED] = true
        savedStateHandle[PENDING_2FA_EMAIL] = result.email
        savedStateHandle[PENDING_2FA_PASSWORD] = result.sub
        _state.update {
            it.copy(
                isLoginInProgress = false,
                isLoginRequired = false,
                is2FARequired = true,
            )
        }
    }

    private suspend fun autoCreateGoogleAccount(result: GoogleSignInResult) {
        Timber.d("[GSIGN] Account doesn't exist - auto-creating")
        runCatching {
            createAccountUseCase(
                email = result.email,
                password = result.sub,
                firstName = result.firstName.orEmpty(),
                lastName = result.lastName.orEmpty(),
            )
        }.onSuccess { credentials ->
            sendToConfirmEmailScreen(credentials)
        }.onFailure { ce ->
            when (ce) {
                is CreateAccountException.AccountAlreadyExists -> {
                    Timber.w(ce, "[GSIGN] Email registered with different password")
                    showGoogleSignInError(sharedR.string.google_sign_in_email_exists)
                }

                else -> {
                    Timber.e(ce, "[GSIGN] createAccount failed")
                    showGoogleSignInError(sharedR.string.google_sign_in_failed)
                }
            }
        }
    }

    private suspend fun sendToConfirmEmailScreen(credentials: EphemeralCredentials) {
        setTemporalCredentials(credentials)
        runCatching {
            clearEphemeralCredentialsUseCase()
            saveEphemeralCredentialsUseCase(
                credentials.copy(session = GOOGLE_SIGN_IN_PENDING_SESSION)
            )
        }.onFailure { Timber.e(it, "[GSIGN] Failed to persist ephemeral credentials") }
        _state.update {
            it.copy(
                isLoginInProgress = false,
                password = null,
                accountSession = it.accountSession?.copy(email = null),
            )
        }
        setIsWaitingForConfirmAccount()
    }

    private fun showGoogleSignInError(@StringRes messageId: Int) {
        _state.update {
            it.copy(
                isLoginInProgress = false,
                isLoginRequired = true,
                password = null,
                accountSession = it.accountSession?.copy(email = null),
                snackbarMessage = triggered(messageId),
            )
        }
    }

    private fun GoogleSignInResult.toEphemeralCredentials() = EphemeralCredentials(
        email = email,
        password = sub,
        session = null,
        firstName = firstName,
        lastName = lastName,
    )

    /**
     * Handles errors surfaced by the Credential Manager launcher (e.g. picker failed).
     */
    fun onGoogleSignInError(throwable: Throwable) {
        Timber.e(throwable, "[GSIGN] Launcher reported failure (${throwable::class.simpleName})")
        setSnackbarMessageId(sharedR.string.google_sign_in_failed)
    }

    /**
     * Updates login error value in state as consumed.
     */
    fun setLoginErrorConsumed() {
        _state.update { it.copy(loginException = null) }
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
    fun checkSignupLink(link: String, timeStamp: Long) {
        // avoid rotating the screen calling this method again
        val handledLink = HandledLinks(link = link, timeStamp = timeStamp)
        if (handledLinks.contains(handledLink)) return
        _state.update { state ->
            state.copy(
                isLoginRequired = false,
                isLoginInProgress = true,
            )
        }
        viewModelScope.launch {
            handledLinks.add(handledLink)
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
                (result.exceptionOrNull() as? QuerySignupLinkException)?.messageId
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
                    )
                }
            } else {
                viewModelScope.launch {
                    when {
                        ongoingTransfersExistUseCase() -> _state.update { state ->
                            state.copy(ongoingTransfersExist = true)
                        }

                        !isConnected -> _state.update { state ->
                            state.copy(
                                isLoginRequired = true,
                                ongoingTransfersExist = null,
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

        _state.update {
            if (typedEmail != null && typedPassword != null) {
                it.copy(
                    isLoginInProgress = true,
                    is2FARequired = false,
                    accountSession = state.value.accountSession?.copy(email = typedEmail)
                        ?: AccountSession(email = typedEmail),
                    password = typedPassword,
                    ongoingTransfersExist = null,
                )
            } else {
                it.copy(
                    isLoginInProgress = true,
                    is2FARequired = false,
                    ongoingTransfersExist = null,
                )
            }
        }

        viewModelScope.launch {
            with(state.value) {
                val email = typedEmail ?: accountSession?.email ?: return@launch
                val password = typedPassword ?: this.password ?: return@launch
                fetchNodeProvider.setLoginByAccount()

                runCatching {
                    loginUseCase(
                        email,
                        password,
                        DisableChatApiUseCase { MegaApplication.getInstance()::disableMegaChatApi }
                    ).collectLatest { status -> status.checkStatus(email = email) }
                }.onFailure { exception ->
                    if (exception !is LoginException) return@onFailure

                    if (exception is LoginMultiFactorAuthRequired) {
                        savedStateHandle[IS_2FA_REQUIRED] = true
                        savedStateHandle[PENDING_2FA_EMAIL] = state.value.accountSession?.email
                        savedStateHandle[PENDING_2FA_PASSWORD] = state.value.password
                        _state.update {
                            it.copy(
                                isLoginInProgress = false,
                                isLoginRequired = false,
                                is2FARequired = true,
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
                    fetchNodeProvider.setLoginByAccount()
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

    private fun LoginException.loginFailed(is2FARequest: Boolean = false) =
        _state.update { loginState ->
            //If LoginBlockedAccount will processed at the `onEvent` when receive an EVENT_ACCOUNT_BLOCKED
            //If LoginLoggedOutFromOtherLocation will be handled in the Activity
            val snackbarMessage = this.newError
                .takeIf {
                    // in the new design we don't show snackbar for these errors
                    this !is LoginTooManyAttempts
                            && this !is LoginWrongEmailOrPassword
                }?.let { triggered(it) }
            loginState.copy(
                isLoginInProgress = false,
                isLoginRequired = true,
                is2FARequired = false,
                loginException = this.takeUnless { this is LoginLoggedOutFromOtherLocation },
                snackbarMessage = snackbarMessage ?: consumed()
            )
        }

    private suspend fun LoginStatus.checkStatus(
        email: String? = null,
    ) = when (this) {
        LoginStatus.LoginStarted -> {
            Timber.d("Login started")
        }

        LoginStatus.LoginSucceed -> {
            Timber.d("Login finished")
            ephemeralCredentialManager.setEphemeralCredential(null)
            _state.update {
                it.copy(
                    isLoginInProgress = false,
                    isLoginRequired = false,
                    is2FARequired = false,
                    multiFactorAuthState = null
                )
            }
            sendAnalyticsEventIfFirstTimeLogin(email)
        }

        LoginStatus.LoginCannotStart -> {
            Timber.d("Login cannot start")
            _state.update {
                it.copy(
                    isLoginInProgress = false,
                    isLoginRequired = true,
                    is2FARequired = false
                )
            }
        }

        is LoginStatus.LoginResumed -> {
            Timber.d("Login resumed")
        }

        is LoginStatus.LoginWaiting -> {
            Timber.d("Login waiting")
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
     * Updates 2FA code in state.
     */
    fun on2FAChanged(twoFA: String) {
        _state.update { state ->
            state.copy(
                multiFactorAuthState = MultiFactorAuthState.Fixed
                    .takeUnless { state.multiFactorAuthState == MultiFactorAuthState.Failed }
            )
        }
        if (twoFA.isValid2FA()) performLoginWith2FA(twoFA)
    }

    /**
     * Updates the state to show a message.
     */
    fun setSnackbarMessageId(@StringRes messageId: Int) =
        _state.update { state -> state.copy(snackbarMessage = triggered(messageId)) }

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
        _state.update {
            it.copy(
                accountBlockedEvent = triggered(
                    AccountBlockedUiState(
                        type = accountBlockedEvent.type,
                        text = accountBlockedEvent.text
                    )
                )
            )
        }
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

    fun onForgotPassword() {
        viewModelScope.launch {
            waitMiscFlagsLoaded()
            val domain = getDomainNameUseCase()
            val typedEmail = _state.value.accountSession?.email
            val url = if (typedEmail.isNullOrEmpty()) {
                recoveryUrl(domain)
            } else {
                val email = runCatching {
                    Base64.encodeToString(typedEmail.toByteArray(), Base64.DEFAULT)
                        .replace("\n", "")
                }.onFailure { Timber.e(it) }
                    .getOrNull() ?: ""

                recoveryUrlWithEmail(domain) + email
            }
            _state.update { it.copy(openUrlEvent = triggered(url)) }
        }
    }

    fun onLostAuthenticationDevice() {
        viewModelScope.launch {
            waitMiscFlagsLoaded()
            val domain = getDomainNameUseCase()
            val url = recoveryUrl(domain)
            _state.update { it.copy(openUrlEvent = triggered(url)) }
        }
    }

    fun onOpenUrlEventConsumed() {
        _state.update { it.copy(openUrlEvent = consumed()) }
    }

    private suspend fun waitMiscFlagsLoaded() {
        if (!_state.value.miscFlagLoaded) {
            getMiscFlagsUseCase()
            _state.first { it.miscFlagLoaded }
        }
    }

    companion object {
        private const val IS_2FA_REQUIRED = "is_2fa_required"
        private const val PENDING_2FA_EMAIL = "pending_2fa_email"
        private const val PENDING_2FA_PASSWORD = "pending_2fa_password"
    }

    data class HandledLinks(val link: String, val timeStamp: Long)
}
