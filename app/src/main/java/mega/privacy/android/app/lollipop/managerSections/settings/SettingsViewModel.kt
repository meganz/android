package mega.privacy.android.app.lollipop.managerSections.settings

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.*
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAccountDetails: GetAccountDetails,
    private val canDeleteAccount: CanDeleteAccount,
    private val refreshUserAccount: RefreshUserAccount,
    private val refreshPasscodeLockPreference: RefreshPasscodeLockPreference,
    private val isLoggingEnabled: IsLoggingEnabled,
    private val isChatLoggingEnabled: IsChatLoggingEnabled,
    private val isCameraSyncEnabled: IsCameraSyncEnabled,
    private val rootNodeExists: RootNodeExists,
    private val isMultiFactorAuthAvailable: IsMultiFactorAuthAvailable,
    private val fetchAutoAcceptQRLinks: FetchAutoAcceptQRLinks,
    private val performMultiFactorAuthCheck: PerformMultiFactorAuthCheck,
    private val getStartScreen: GetStartScreen,
    private val shouldHideRecentActivity: ShouldHideRecentActivity,
    private val toggleAutoAcceptQRLinks: ToggleAutoAcceptQRLinks,
) : ViewModel() {
    val hideRecentActivity: Boolean
        get() = shouldHideRecentActivity()
    val startScreen: Int
        get() = getStartScreen()
    val passcodeLock: Boolean
        get() = refreshPasscodeLockPreference()
    val email: String
        get() = getAccountDetails().email
    val canNotDeleteAccount: Boolean
        get() = !canDeleteAccount()
    val isLoggerEnabled: Boolean
        get() = isLoggingEnabled()
    val isChatLoggerEnabled: Boolean
        get() = isChatLoggingEnabled()
    val isCamSyncEnabled: Boolean
        get() = isCameraSyncEnabled()
    val accountType: Int
        get() = getAccountDetails().accountTypeIdentifier
    val hasRootNode: Boolean
        get() = rootNodeExists()
    val multiFactorAuthAvailable: Boolean
        get() = isMultiFactorAuthAvailable()
    private val autoExceptEnabled = MutableStateFlow<Boolean>(false)
    val isAutoExceptEnabled: StateFlow<Boolean> = autoExceptEnabled

    init {
        viewModelScope.launch {
            autoExceptEnabled.value = fetchAutoAcceptQRLinks()
        }
    }



    fun refreshAccount() = refreshUserAccount()

    fun multiFactorAuthCheck(listener: MegaRequestListenerInterface) =
        performMultiFactorAuthCheck(listener)

    fun toggleAutoAcceptPreference() {
        viewModelScope.launch {
            kotlin.runCatching {
                toggleAutoAcceptQRLinks()
            }.onSuccess {
                autoExceptEnabled.value = it
            }
        }
    }

}