package mega.privacy.android.app.lollipop.managerSections.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.*
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
    private val getStartScreen: GetStartScreen,
    private val shouldHideRecentActivity: ShouldHideRecentActivity,
    private val toggleAutoAcceptQRLinks: ToggleAutoAcceptQRLinks,
    fetchMultiFactorAuthSetting: FetchMultiFactorAuthSetting,
) : ViewModel() {
    private val userAccount = MutableStateFlow(getAccountDetails(false))
    private val state = MutableStateFlow(initialiseState())
    val uiState: StateFlow<SettingsState> = state

    private fun initialiseState(): SettingsState {
        return SettingsState(
            autoAcceptEnabled = false,
            multiFactorAuthEnabled = false,
            canDeleteAccount = canDeleteAccount(getAccountDetails(false))
        )
    }

    val hideRecentActivity: Boolean
        get() = shouldHideRecentActivity()
    val startScreen: Int
        get() = getStartScreen()
    val passcodeLock: Boolean
        get() = refreshPasscodeLockPreference()
    val email: String
        get() = userAccount.value.email
    val isLoggerEnabled: Boolean
        get() = isLoggingEnabled()
    val isChatLoggerEnabled: Boolean
        get() = isChatLoggingEnabled()
    val isCamSyncEnabled: Boolean
        get() = isCameraSyncEnabled()
    val accountType: Int
        get() = userAccount.value.accountTypeIdentifier
    val hasRootNode: Boolean
        get() = rootNodeExists()
    val multiFactorAuthAvailable: Boolean
        get() = isMultiFactorAuthAvailable()


    init {
        viewModelScope.launch {
            state.update { it.copy(autoAcceptEnabled = fetchAutoAcceptQRLinks()) }
            fetchMultiFactorAuthSetting().collect { enabled ->
                state.update { it.copy(multiFactorAuthEnabled = enabled) }
            }
        }

    }

    fun refreshAccount() {
        viewModelScope.launch {
            userAccount.value = getAccountDetails(true)
            state.update { it.copy(canDeleteAccount = canDeleteAccount(userAccount.value)) }
        }
    }

    fun toggleAutoAcceptPreference() {
        viewModelScope.launch {
            kotlin.runCatching {
                toggleAutoAcceptQRLinks()
            }.onSuccess { autoAccept ->
                state.update { it.copy(autoAcceptEnabled = autoAccept) }
            }
        }
    }

}