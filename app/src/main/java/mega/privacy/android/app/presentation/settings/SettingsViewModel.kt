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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.presentation.settings.model.SettingsState
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.domain.usecase.CanDeleteAccount
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSettingUseCase
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetPreference
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.MonitorHideRecentActivity
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.PutPreference
import mega.privacy.android.domain.usecase.RefreshPasscodeLockPreference
import mega.privacy.android.domain.usecase.RequestAccountDeletion
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.SetChatLogsEnabled
import mega.privacy.android.domain.usecase.SetHideRecentActivity
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.SetSdkLogsEnabled
import mega.privacy.android.domain.usecase.ToggleAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.camerauploads.IsCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val canDeleteAccount: CanDeleteAccount,
    private val refreshPasscodeLockPreference: RefreshPasscodeLockPreference,
    areSdkLogsEnabled: AreSdkLogsEnabled,
    areChatLogsEnabled: AreChatLogsEnabled,
    private val isCameraSyncEnabledUseCase: IsCameraSyncEnabledUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val isMultiFactorAuthAvailable: IsMultiFactorAuthAvailable,
    private val monitorAutoAcceptQRLinks: MonitorAutoAcceptQRLinks,
    private val startScreen: MonitorStartScreenPreference,
    private val monitorHideRecentActivity: MonitorHideRecentActivity,
    private val setHideRecentActivity: SetHideRecentActivity,
    private val monitorMediaDiscoveryView: MonitorMediaDiscoveryView,
    private val setMediaDiscoveryView: SetMediaDiscoveryView,
    private val toggleAutoAcceptQRLinks: ToggleAutoAcceptQRLinks,
    private val fetchMultiFactorAuthSettingUseCase: FetchMultiFactorAuthSettingUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
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
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
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
            mediaDiscoveryViewState = MediaDiscoveryViewSettings.INITIAL.ordinal,
            email = "",
            accountType = ""
        )
    }

    val passcodeLock: Boolean
        get() = refreshPasscodeLockPreference()
    val isCamSyncEnabled: Boolean
        get() = isCameraSyncEnabledUseCase()


    init {
        viewModelScope.launch {
            merge(
                flow { emit(getAccountDetailsUseCase(false)) }.map {
                    updateAccountState(it)
                },
                flow { emit(isMultiFactorAuthAvailable()) }
                    .map { available ->
                        { state: SettingsState -> state.copy(multiFactorVisible = available) }
                    },
                monitorAutoAcceptQRLinks().catch { e ->
                    Timber.e(e, "Error when monitoring Auto accept QR settings")
                    emit(false)
                }.map { enabled ->
                    { state: SettingsState -> state.copy(autoAcceptChecked = enabled) }
                },
                flow { emit(fetchMultiFactorAuthSettingUseCase()) }
                    .map { enabled ->
                        { state: SettingsState -> state.copy(multiFactorAuthChecked = enabled) }
                    },
                online
                    .map { it && rootNodeExistsUseCase() }
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
                monitorHideRecentActivity()
                    .map { hide ->
                        { state: SettingsState -> state.copy(hideRecentActivityChecked = hide) }
                    },
                monitorMediaDiscoveryView()
                    .map { viewState ->
                        { state: SettingsState ->
                            state.copy(mediaDiscoveryViewState = viewState
                                ?: MediaDiscoveryViewSettings.INITIAL.ordinal)
                        }
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
            ).catch {
                Timber.e(it)
            }.collect {
                state.update(it)
            }
        }

    }

    fun refreshMultiFactorAuthSetting() {
        viewModelScope.launch {
            state.update {
                it.copy(multiFactorAuthChecked = fetchMultiFactorAuthSettingUseCase())
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
        runCatching {
            getAccountDetailsUseCase(true)
        }.onSuccess {
            state.update(updateAccountState(it))
        }.onFailure {
            Timber.e(it)
        }
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

    /**
     * Set hide recent activity setting
     *
     * @param hide true if hide recent activity
     */
    fun hideRecentActivity(hide: Boolean) = viewModelScope.launch {
        setHideRecentActivity(hide)
    }

    /**
     * Set media discovery view setting
     *
     * @param state [MediaDiscoveryViewSettings]
     */
    fun mediaDiscoveryView(state: Int) = viewModelScope.launch {
        setMediaDiscoveryView(state)
    }

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