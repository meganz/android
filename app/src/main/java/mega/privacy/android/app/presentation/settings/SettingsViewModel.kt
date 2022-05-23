package mega.privacy.android.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.presentation.settings.model.SettingsState
import mega.privacy.android.app.utils.LogUtil
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAccountDetails: GetAccountDetails,
    private val canDeleteAccount: CanDeleteAccount,
    private val refreshPasscodeLockPreference: RefreshPasscodeLockPreference,
    areSdkLogsEnabled: AreSdkLogsEnabled,
    areChatLogsEnabled: AreChatLogsEnabled,
    private val isCameraSyncEnabled: IsCameraSyncEnabled,
    private val rootNodeExists: RootNodeExists,
    private val isMultiFactorAuthAvailable: IsMultiFactorAuthAvailable,
    private val monitorAutoAcceptQRLinks: MonitorAutoAcceptQRLinks,
    private val startScreen: GetStartScreen,
    private val isHideRecentActivityEnabled: IsHideRecentActivityEnabled,
    private val toggleAutoAcceptQRLinks: ToggleAutoAcceptQRLinks,
    fetchMultiFactorAuthSetting: FetchMultiFactorAuthSetting,
    monitorConnectivity: MonitorConnectivity,
    private val requestAccountDeletion: RequestAccountDeletion,
    private val isChatLoggedIn: IsChatLoggedIn,
    private val setSdkLogsEnabled: SetSdkLogsEnabled,
    private val setChatLoggingEnabled: SetChatLogsEnabled,
) : ViewModel() {
    private val state = MutableStateFlow(initialiseState())
    val uiState: StateFlow<SettingsState> = state
    private val online =
        monitorConnectivity().shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val sdkLogsEnabled =
        areSdkLogsEnabled().stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val chatLogsEnabled =
        areChatLogsEnabled().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private fun initialiseState(): SettingsState {
        return SettingsState(
            autoAcceptEnabled = false,
            autoAcceptChecked = false,
            multiFactorAuthChecked = false,
            multiFactorEnabled = false,
            multiFactorVisible = false,
            deleteAccountVisible = false,
            deleteEnabled = false,
            cameraUploadEnabled = true,
            chatEnabled = true,
            startScreen = 0,
            hideRecentActivityChecked = false,
            email = "",
            accountType = 0
        )
    }

    val passcodeLock: Boolean
        get() = refreshPasscodeLockPreference()
    val isCamSyncEnabled: Boolean
        get() = isCameraSyncEnabled()


    init {
        viewModelScope.launch {
            merge(
                flowOf(getAccountDetails(false)).map {
                    updateAccountState(it)
                },
                flowOf(isMultiFactorAuthAvailable())
                    .map { available ->
                        { state: SettingsState -> state.copy(multiFactorVisible = available) }
                    },
                monitorAutoAcceptQRLinks().catch { e ->
                    Timber.e(e, "Error when monitoring Auto accept QR settings")
                    emit(false)
                }.map { enabled ->
                    { state: SettingsState -> state.copy(autoAcceptChecked = enabled) }
                },
                fetchMultiFactorAuthSetting()
                    .map { enabled ->
                        { state: SettingsState -> state.copy(multiFactorAuthChecked = enabled) }
                    },
                online
                    .map { it && rootNodeExists() }
                    .map { online ->
                        { state: SettingsState ->
                            state.copy(
                                cameraUploadEnabled = online,
                                autoAcceptEnabled = online,
                                multiFactorEnabled = online,
                                deleteEnabled = online,
                            )
                        }
                    },
                startScreen()
                    .map { screen ->
                        { state: SettingsState -> state.copy(startScreen = screen) }
                    },
                isHideRecentActivityEnabled()
                    .map { hide ->
                        { state: SettingsState -> state.copy(hideRecentActivityChecked = hide) }
                    },
                isChatLoggedIn()
                    .combine(online) { loggedIn, online -> loggedIn && online }
                    .map { enabled ->
                        { state: SettingsState -> state.copy(chatEnabled = enabled) }
                    },
            ).collect {
                state.update(it)
            }

        }

    }

    private fun updateAccountState(userAccount: UserAccount) =
        { state: SettingsState ->
            state.copy(
                deleteAccountVisible = canDeleteAccount(userAccount),
                email = userAccount.email,
                accountType = userAccount.accountTypeIdentifier
            )
        }

    fun refreshAccount() {
        viewModelScope.launch {
            state.update(updateAccountState(getAccountDetails(true)))
        }
    }

    fun toggleAutoAcceptPreference() {
        viewModelScope.launch {
            kotlin.runCatching {
                toggleAutoAcceptQRLinks()
            }
        }
    }

    suspend fun deleteAccount(): Boolean {
        return kotlin.runCatching { requestAccountDeletion() }
            .fold(
                { true },
                { e ->
                    LogUtil.logError("Error when asking for the cancellation link: ${e.message}")
                    false
                }
            )
    }

    fun disableLogger(): Boolean {
        return if (sdkLogsEnabled.value) {
            viewModelScope.launch { setSdkLogsEnabled(false) }
            true
        } else {
            false
        }
    }

    fun disableChatLogger(): Boolean {
        return if (chatLogsEnabled.value) {
            viewModelScope.launch { setChatLoggingEnabled(false) }
            true
        } else {
            false
        }
    }

    fun enableLogger() {
        viewModelScope.launch { setSdkLogsEnabled(true) }
    }

    fun enableChatLogger() {
        viewModelScope.launch { setChatLoggingEnabled(true) }
    }

}