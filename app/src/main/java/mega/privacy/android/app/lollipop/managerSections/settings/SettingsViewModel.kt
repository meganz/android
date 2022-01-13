package mega.privacy.android.app.lollipop.managerSections.settings

import android.util.Log
import androidx.lifecycle.*
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

    val hideRecentActivity: Boolean
        get() = shouldHideRecentActivity()
    val startScreen: Int
        get() = getStartScreen()
    val passcodeLock: Boolean
        get() = refreshPasscodeLockPreference()
    val email: String
        get() = userAccount.value.email
    val displayDeleteAccountOption: StateFlow<Boolean> =
        userAccount.mapLatest {
            canDeleteAccount(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            canDeleteAccount(userAccount.value)
        )

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

    private val autoAcceptEnabled = MutableStateFlow(false)
    val isAutoAcceptEnabled: StateFlow<Boolean> = autoAcceptEnabled
    val isMultiFactorEnabled: StateFlow<Boolean> =
        fetchMultiFactorAuthSetting()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = false
            )

    init {
        viewModelScope.launch {
            autoAcceptEnabled.value = fetchAutoAcceptQRLinks()
        }
    }

    fun refreshAccount() {
        userAccount.value = getAccountDetails(true)
    }

    fun toggleAutoAcceptPreference() {
        viewModelScope.launch {
            kotlin.runCatching {
                toggleAutoAcceptQRLinks()
            }.onSuccess {
                autoAcceptEnabled.value = it
            }
        }
    }

}