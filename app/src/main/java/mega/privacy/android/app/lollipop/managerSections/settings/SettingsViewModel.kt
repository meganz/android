package mega.privacy.android.app.lollipop.managerSections.settings

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaRequestListenerInterface

class SettingsViewModel @ViewModelInject constructor(
    private val getAccountDetails: GetAccountDetails,
    private val canDeleteAccount: CanDeleteAccount,
    private val refreshUserAccount: RefreshUserAccount,
    private val refreshPasscodeLockPreference: RefreshPasscodeLockPreference,
    private val isLoggingEnabled: IsLoggingEnabled,
    private val isChatLoggingEnabled: IsChatLoggingEnabled,
    private val isCameraSyncEnabled: IsCameraSyncEnabled,
    private val rootNodeExists: RootNodeExists,
    private val isMultiFactorAuthAvailable: IsMultiFactorAuthAvailable,
    private val fetchContactLinksOption: FetchContactLinksOption,
    private val performMultiFactorAuthCheck: PerformMultiFactorAuthCheck,
    private val getStartScreen: GetStartScreen,
    private val shouldHideRecentActivity: ShouldHideRecentActivity,
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
    val isKarereLoggerEnabled: Boolean
        get() = isChatLoggingEnabled()
    val isCamSyncEnabled: Boolean
        get() = isCameraSyncEnabled()
    val accountType: Int
        get() = getAccountDetails().accountTypeIdentifier
    val hasRootNode: Boolean
        get() = rootNodeExists()
    val multiFactorAuthAvailable: Boolean
        get() = isMultiFactorAuthAvailable()

    fun refreshAccount() = refreshUserAccount()

    fun getContactLinksOption(listener: MegaRequestListenerInterface) =
        fetchContactLinksOption(listener)

    fun multiFactorAuthCheck(listener: MegaRequestListenerInterface) =
        performMultiFactorAuthCheck(listener)

}