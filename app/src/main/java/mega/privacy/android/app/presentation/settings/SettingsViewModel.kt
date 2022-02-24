package mega.privacy.android.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.presentation.settings.model.SettingsState
import mega.privacy.android.app.utils.LogUtil
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAccountDetails: GetAccountDetails,
    private val canDeleteAccount: CanDeleteAccount,
    private val refreshPasscodeLockPreference: RefreshPasscodeLockPreference,
    private val isLoggingEnabled: IsLoggingEnabled,
    private val isChatLoggingEnabled: IsChatLoggingEnabled,
    private val isCameraSyncEnabled: IsCameraSyncEnabled,
    private val rootNodeExists: RootNodeExists,
    private val isMultiFactorAuthAvailable: IsMultiFactorAuthAvailable,
    private val fetchAutoAcceptQRLinks: FetchAutoAcceptQRLinks,
    private val startScreen: GetStartScreen,
    private val isHideRecentActivityEnabled: IsHideRecentActivityEnabled,
    private val toggleAutoAcceptQRLinks: ToggleAutoAcceptQRLinks,
    fetchMultiFactorAuthSetting: FetchMultiFactorAuthSetting,
    isOnline: IsOnline,
    private val requestAccountDeletion: RequestAccountDeletion,
    private val isChatLoggedIn: IsChatLoggedIn,
    private val setLoggingEnabled: SetLoggingEnabled,
    private val setChatLoggingEnabled: SetChatLoggingEnabled,
) : ViewModel() {
    private val userAccount = MutableStateFlow(getAccountDetails(false))
    private val state = MutableStateFlow(initialiseState())
    val uiState: StateFlow<SettingsState> = state
    private val online = isOnline().shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

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
        )
    }

    val passcodeLock: Boolean
        get() = refreshPasscodeLockPreference()
    val email: String
        get() = userAccount.value.email
    val isCamSyncEnabled: Boolean
        get() = isCameraSyncEnabled()
    val accountType: Int
        get() = userAccount.value.accountTypeIdentifier


    init {
        viewModelScope.launch {
            merge(
                userAccount.map {
                    { state: SettingsState -> state.copy(deleteAccountVisible = canDeleteAccount(it)) }
                },
                flowOf(isMultiFactorAuthAvailable())
                    .map { available ->
                        { state: SettingsState -> state.copy(multiFactorVisible = available) }
                    },
                flowOf(kotlin.runCatching{ fetchAutoAcceptQRLinks() }.getOrDefault(false))
                    .map { enabled ->
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
                    .map{ screen ->
                        { state: SettingsState -> state.copy(startScreen = screen)}
                    },
                isHideRecentActivityEnabled()
                    .map{ hide ->
                        { state: SettingsState -> state.copy(hideRecentActivityChecked = hide)}
                    },
                isChatLoggedIn()
                    .combine(online){ loggedIn, online -> loggedIn && online}
                    .map { enabled ->
                        { state: SettingsState -> state.copy(chatEnabled = enabled)}
                    },
            ).collect {
                state.update(it)
            }

        }

    }

    fun refreshAccount() {
        viewModelScope.launch {
            userAccount.value = getAccountDetails(true)
        }
    }

    fun toggleAutoAcceptPreference() {
        viewModelScope.launch {
            kotlin.runCatching {
                toggleAutoAcceptQRLinks()
            }.onSuccess { autoAccept ->
                state.update { it.copy(autoAcceptChecked = autoAccept) }
            }
        }
    }

    suspend fun deleteAccount(): Boolean {
        return kotlin.runCatching { requestAccountDeletion() }
            .fold(
                { true },
                { e ->
                    LogUtil.logError( "Error when asking for the cancellation link: ${e.message}")
                    false
                }
            )
    }

    fun disableLogger(): Boolean {
        return if (isLoggingEnabled()){
            setLoggingEnabled(false)
            true
        } else{
            false
        }
    }

    fun disableChatLogger(): Boolean {
        return if (isChatLoggingEnabled()){
            setChatLoggingEnabled(false)
            true
        } else{
            false
        }
    }

    fun enableLogger() {
        setLoggingEnabled(true)
    }

    fun enableChatLogger() {
        setChatLoggingEnabled(true)
    }

}