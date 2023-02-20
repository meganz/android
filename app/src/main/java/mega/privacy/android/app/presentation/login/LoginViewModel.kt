package mega.privacy.android.app.presentation.login

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.LoginState.Companion.CLICKS_TO_ENABLE_LOGS
import mega.privacy.android.app.psa.PsaManager
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.account.AccountSession
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
import mega.privacy.android.domain.usecase.login.LocalLogout
import mega.privacy.android.domain.usecase.setting.ResetChatSettings
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
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    // All these SingleLiveEvents will be contemplated in state and removed once migrated to Compose.
    private val querySignupLinkFinished = SingleLiveEvent<Result<String>>()

    /**
     * Notifies about querySignupLink request finish.
     */
    fun onQuerySignupLinkFinished(): LiveData<Result<String>> = querySignupLinkFinished

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

        initChatSettings()
    }

    /**
     * Reset chat settings.
     */
    fun initChatSettings() = viewModelScope.launch { resetChatSettings() }

    /**
     * Updates state with a new intentAction.
     *
     * @param intentAction Intent action.
     */
    fun setIntentAction(intentAction: String) {
        _state.update { it.copy(intentAction = intentAction) }
    }

    /**
     * Updates isFirstFetchNodesUpdate as true in state.
     */
    fun updateFetchNodesUpdate() {
        _state.update { it.copy(isFirstFetchNodesUpdate = false) }
    }

    /**
     * Updates pressBackWhileLogin value in state.
     */
    fun updatePressedBackWhileLogin(pressedBackWhileLogin: Boolean) {
        _state.update { it.copy(pressedBackWhileLogin = pressedBackWhileLogin) }
    }

    /**
     * Updates accountConfirmationLink value in state.
     */
    fun updateAccountConfirmationLink(accountConfirmationLink: String?) {
        _state.update { it.copy(accountConfirmationLink = accountConfirmationLink) }
    }

    /**
     * Updates isFetchingNodes value in state.
     */
    fun updateIsFetchingNodes(isFetchingNodes: Boolean) {
        _state.update { it.copy(isFetchingNodes = isFetchingNodes) }
    }

    /**
     * Updates isAlreadyLoggedIn value in state.
     */
    fun updateIsAlreadyLoggedIn(isAlreadyLoggedIn: Boolean) {
        _state.update { it.copy(isAlreadyLoggedIn = isAlreadyLoggedIn) }
    }

    /**
     * Updates was2FAErrorShown and is2FAErrorShown values as true in state.
     */
    fun setWas2FAErrorShown() {
        _state.update { it.copy(was2FAErrorShown = true, is2FAErrorShown = true) }
    }

    /**
     * Updates is2FAErrorShown value as false in state.
     */
    fun setIs2FAErrorNotShown() {
        _state.update { it.copy(is2FAErrorShown = false) }
    }

    /**
     * Updates is2FAEnabled value as true in state.
     */
    fun setIs2FAEnabled() {
        _state.update { it.copy(is2FAEnabled = true) }
    }

    /**
     * Updates isAccountConfirmed value in state.
     */
    fun updateIsAccountConfirmed(isAccountConfirmed: Boolean) {
        _state.update { it.copy(isAccountConfirmed = isAccountConfirmed) }
    }

    /**
     * Updates isPinLongClick value in state.
     */
    fun updateIsPinLongClick(isPinLongClick: Boolean) {
        _state.update { it.copy(isPinLongClick = isPinLongClick) }
    }

    /**
     * Updates isRefreshApiServer value in state.
     */
    fun updateIsRefreshApiServer(isRefreshApiServer: Boolean) {
        _state.update { it.copy(isRefreshApiServer = isRefreshApiServer) }
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
     * Updates email and password values in state.
     */
    fun updateCredentials(email: String?, password: String?) {
        val accountSession = state.value.accountSession

        _state.update {
            it.copy(
                accountSession = accountSession?.copy(email = email)
                    ?: AccountSession(email = email),
                password = password
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
     * Set temporal email and password values in state as current email and password.
     */
    fun setTemporalCredentialsAsCurrentCredentials() = with(state.value) {
        updateCredentials(temporalEmail, temporalPassword)
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
     * Decrements the required value for enabling/disabling Karere logs.
     *
     * @param activity Required [Activity]
     */
    fun clickKarereLogs(activity: Activity) = with(state.value) {
        if (pendingClicksKarere == 1) {
            viewModelScope.launch {
                if (!getFeatureFlagValue(AppFeatures.PermanentLogging)) {
                    if (loggingSettings.areKarereLogsEnabled()) {
                        loggingSettings.setStatusLoggerKarere(activity, false)
                    } else {
                        (activity as LoginActivity).showConfirmationEnableLogsKarere()
                    }
                }
            }
            _state.update { it.copy(pendingClicksKarere = CLICKS_TO_ENABLE_LOGS) }
        } else {
            _state.update { it.copy(pendingClicksKarere = pendingClicksKarere - 1) }
        }
    }

    /**
     * Decrements the required value for enabling/disabling SDK logs.
     *
     * @param activity Required [Activity]
     */
    fun clickSDKLogs(activity: Activity) = with(state.value) {
        if (pendingClicksSDK == 1) {
            viewModelScope.launch {
                if (!getFeatureFlagValue(AppFeatures.PermanentLogging)) {
                    if (loggingSettings.areSDKLogsEnabled()) {
                        loggingSettings.setStatusLoggerSDK(activity, false)
                    } else {
                        (activity as LoginActivity).showConfirmationEnableLogsSDK()
                    }
                }
            }
            _state.update { it.copy(pendingClicksSDK = CLICKS_TO_ENABLE_LOGS) }
        } else {
            _state.update { it.copy(pendingClicksSDK = pendingClicksSDK - 1) }
        }
    }

    /**
     * Checks a signup link.
     */
    fun checkSignupLink(link: String) = viewModelScope.launch {
        querySignupLinkFinished.value = kotlin.runCatching { querySignupLink(link) }
    }

    /**
     * Cancels all transfers, uploads and downloads.
     */
    fun launchCancelTransfers() = viewModelScope.launch { cancelTransfers() }

    /**
     * Local logout.
     */
    fun performLocalLogout() = viewModelScope.launch {
        localLogout(ClearPsa { PsaManager::stopChecking })
    }
}
