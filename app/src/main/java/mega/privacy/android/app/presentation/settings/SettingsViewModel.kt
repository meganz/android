package mega.privacy.android.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.presentation.settings.model.SettingsState
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.domain.usecase.CanDeleteAccount
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSetting
import mega.privacy.android.domain.usecase.GetAccountDetails
import mega.privacy.android.domain.usecase.GetPreference
import mega.privacy.android.domain.usecase.IsCameraSyncEnabled
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsHideRecentActivityEnabled
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.PutPreference
import mega.privacy.android.domain.usecase.RefreshPasscodeLockPreference
import mega.privacy.android.domain.usecase.RequestAccountDeletion
import mega.privacy.android.domain.usecase.RootNodeExists
import mega.privacy.android.domain.usecase.SetChatLogsEnabled
import mega.privacy.android.domain.usecase.SetSdkLogsEnabled
import mega.privacy.android.domain.usecase.ToggleAutoAcceptQRLinks
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
    private val startScreen: MonitorStartScreenPreference,
    private val isHideRecentActivityEnabled: IsHideRecentActivityEnabled,
    private val toggleAutoAcceptQRLinks: ToggleAutoAcceptQRLinks,
    fetchMultiFactorAuthSetting: FetchMultiFactorAuthSetting,
    monitorConnectivity: MonitorConnectivity,
    private val requestAccountDeletion: RequestAccountDeletion,
    private val isChatLoggedIn: IsChatLoggedIn,
    private val setSdkLogsEnabled: SetSdkLogsEnabled,
    private val setChatLoggingEnabled: SetChatLogsEnabled,
    private val putStringPreference: PutPreference<String>,
    private val putStringSetPreference: PutPreference<MutableSet<String>>,
    private val putIntPreference: PutPreference<Int>,
    private val putLongPreference: PutPreference<Long>,
    private val putFloatPreference: PutPreference<Float>,
    private val putBooleanPreference: PutPreference<Boolean>,
    private val getStringPreference: GetPreference<String?>,
    private val getStringSetPreference: GetPreference<MutableSet<String>?>,
    private val getIntPreference: GetPreference<Int>,
    private val getLongPreference: GetPreference<Long>,
    private val getFloatPreference: GetPreference<Float>,
    private val getBooleanPreference: GetPreference<Boolean>,
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
            callsEnabled = true,
            startScreen = 0,
            hideRecentActivityChecked = false,
            email = "",
            accountType = ""
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
                        { state: SettingsState -> state.copy(startScreen = screen.id) }
                    },
                isHideRecentActivityEnabled()
                    .map { hide ->
                        { state: SettingsState -> state.copy(hideRecentActivityChecked = hide) }
                    },
                isChatLoggedIn()
                    .combine(online) { loggedIn, online -> loggedIn && online }
                    .map { enabled ->
                        { state: SettingsState ->
                            state.copy(
                                chatEnabled = enabled,
                                callsEnabled = enabled,
                            )
                        }
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
                accountType = userAccount.accountTypeString
            )
        }

    fun refreshAccount() = viewModelScope.launch {
        state.update(updateAccountState(getAccountDetails(true)))
    }

    fun toggleAutoAcceptPreference() = viewModelScope.launch {
        runCatching {
            toggleAutoAcceptQRLinks()
        }
    }

    suspend fun deleteAccount() = runCatching { requestAccountDeletion() }
        .fold(
            { true },
            { e ->
                Timber.e(e, "Error when asking for the cancellation link")
                false
            }
        )

    fun disableLogger() = if (sdkLogsEnabled.value) {
        viewModelScope.launch { setSdkLogsEnabled(false) }
        true
    } else {
        false
    }

    fun disableChatLogger() = if (chatLogsEnabled.value) {
        viewModelScope.launch { setChatLoggingEnabled(false) }
        true
    } else {
        false
    }

    fun enableLogger() = viewModelScope.launch { setSdkLogsEnabled(true) }

    fun enableChatLogger() = viewModelScope.launch { setChatLoggingEnabled(true) }

    fun putString(key: String?, value: String?) {
        viewModelScope.launch { putStringPreference(key, value) }
    }

    fun putStringSet(key: String?, values: MutableSet<String>?) {
        viewModelScope.launch { putStringSetPreference(key, values) }
    }

    fun putInt(key: String?, value: Int) {
        viewModelScope.launch { putIntPreference(key, value) }
    }

    fun putLong(key: String?, value: Long) {
        viewModelScope.launch { putLongPreference(key, value) }
    }

    fun putFloat(key: String?, value: Float) {
        viewModelScope.launch { putFloatPreference(key, value) }
    }

    fun putBoolean(key: String?, value: Boolean) {
        viewModelScope.launch { putBooleanPreference(key, value) }
    }

    fun getString(key: String?, defValue: String?) =
        runBlocking { getStringPreference(key, defValue).firstOrNull() }

    fun getStringSet(key: String?, defValue: MutableSet<String>?) =
        runBlocking { getStringSetPreference(key, defValue).firstOrNull() }

    fun getInt(key: String?, defValue: Int) =
        runBlocking { getIntPreference(key, defValue).first() }

    fun getLong(key: String?, defValue: Long) =
        runBlocking { getLongPreference(key, defValue).first() }

    fun getFloat(key: String?, defValue: Float) =
        runBlocking { getFloatPreference(key, defValue).first() }

    fun getBoolean(key: String?, defValue: Boolean) =
        runBlocking { getBooleanPreference(key, defValue).first() }
}