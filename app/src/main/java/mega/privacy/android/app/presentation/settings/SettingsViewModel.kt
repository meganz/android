package mega.privacy.android.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.ABTestFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.COOKIES_URI
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.presentation.settings.model.SettingsState
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.domain.usecase.CanDeleteAccount
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSettingUseCase
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.RefreshPasscodeLockPreference
import mega.privacy.android.domain.usecase.RequestAccountDeletion
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.SetChatLogsEnabled
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.SetSdkLogsEnabled
import mega.privacy.android.domain.usecase.ToggleAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.SetSubFolderMediaDiscoveryEnabledUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val canDeleteAccount: CanDeleteAccount,
    private val refreshPasscodeLockPreference: RefreshPasscodeLockPreference,
    areSdkLogsEnabled: AreSdkLogsEnabled,
    areChatLogsEnabled: AreChatLogsEnabled,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val isMultiFactorAuthAvailable: IsMultiFactorAuthAvailable,
    private val monitorAutoAcceptQRLinks: MonitorAutoAcceptQRLinks,
    private val startScreen: MonitorStartScreenPreference,
    private val monitorHideRecentActivityUseCase: MonitorHideRecentActivityUseCase,
    private val setHideRecentActivityUseCase: SetHideRecentActivityUseCase,
    private val monitorMediaDiscoveryView: MonitorMediaDiscoveryView,
    private val setMediaDiscoveryView: SetMediaDiscoveryView,
    private val toggleAutoAcceptQRLinks: ToggleAutoAcceptQRLinks,
    private val fetchMultiFactorAuthSettingUseCase: FetchMultiFactorAuthSettingUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val requestAccountDeletion: RequestAccountDeletion,
    private val isChatLoggedIn: IsChatLoggedIn,
    private val setSdkLogsEnabled: SetSdkLogsEnabled,
    private val setChatLoggingEnabled: SetChatLogsEnabled,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorPasscodeLockPreferenceUseCase: MonitorPasscodeLockPreferenceUseCase,
    private val setSubFolderMediaDiscoveryEnabledUseCase: SetSubFolderMediaDiscoveryEnabledUseCase,
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
    private val getSessionTransferURLUseCase: GetSessionTransferURLUseCase,
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
            cameraUploadsEnabled = true,
            cameraUploadsOn = false,
            chatEnabled = true,
            callsEnabled = true,
            startScreen = 0,
            hideRecentActivityChecked = false,
            mediaDiscoveryViewState = MediaDiscoveryViewSettings.INITIAL.ordinal,
            email = "",
            accountType = "",
            passcodeLock = false,
            subFolderMediaDiscoveryChecked = true,
            cookiePolicyLink = null,
        )
    }

    init {
        viewModelScope.launch {
            merge(
                monitorPasscodePreference(),
                flow { emit(isCameraUploadsEnabledUseCase()) }.map { enabled ->
                    { state: SettingsState -> state.copy(cameraUploadsOn = enabled) }
                },
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
                                cameraUploadsEnabled = online,
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
                monitorHideRecentActivityUseCase()
                    .map { hide ->
                        { state: SettingsState -> state.copy(hideRecentActivityChecked = hide) }
                    },
                monitorMediaDiscoveryView()
                    .map { viewState ->
                        { state: SettingsState ->
                            state.copy(
                                mediaDiscoveryViewState = viewState
                                    ?: MediaDiscoveryViewSettings.INITIAL.ordinal
                            )
                        }
                    },
                monitorSubFolderMediaDiscoverySettingsUseCase()
                    .map { viewState ->
                        { state: SettingsState ->
                            state.copy(
                                subFolderMediaDiscoveryChecked = viewState
                            )
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
        getCookiePolicyLink()
    }

    /**
     * Get link for Cookie policy page
     */
    private fun getCookiePolicyLink() = viewModelScope.launch {
        runCatching {
            val isAdsFeatureEnabled = getFeatureFlagValueUseCase(AppFeatures.InAppAdvertisement) &&
                    getFeatureFlagValueUseCase(ABTestFeatures.ads) &&
                    getFeatureFlagValueUseCase(ABTestFeatures.adse)
            val url =
                if (isAdsFeatureEnabled) getSessionTransferURLUseCase("cookie") else COOKIES_URI
            state.update { it.copy(cookiePolicyLink = url) }
        }.onFailure {
            Timber.e("Failed to fetch session transfer URL for Cookie Policy page: ${it.message}")
        }
    }

    private suspend fun monitorPasscodePreference() =
        if (getFeatureFlagValueUseCase(AppFeatures.PasscodeBackend)) {
            monitorPasscodeLockPreferenceUseCase()
        } else {
            flow { emit(refreshPasscodeLockPreference()) }
        }.map { enabled ->
            { state: SettingsState -> state.copy(passcodeLock = enabled) }
        }

    /**
     * Refresh Camera Uploads On/Off state
     */
    fun refreshCameraUploadsOn() = viewModelScope.launch {
        runCatching { isCameraUploadsEnabledUseCase() }
            .onSuccess { isEnabled ->
                state.update { it.copy(cameraUploadsOn = isEnabled) }
            }
            .onFailure {
                Timber.e(it)
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
        setHideRecentActivityUseCase(hide)
    }

    /**
     * Set media discovery view setting
     *
     * @param state [MediaDiscoveryViewSettings]
     */
    fun mediaDiscoveryView(state: Int) = viewModelScope.launch {
        setMediaDiscoveryView(state)
    }

    /**
     * Set sub folder sub folder media discovery  setting enabled
     *
     * @param enabled true if enabled sub folder media discovery
     */
    fun setSubFolderMediaDiscoveryEnabled(enabled: Boolean) = viewModelScope.launch {
        setSubFolderMediaDiscoveryEnabledUseCase(enabled)
    }

    suspend fun fetchPasscodeEnabled() = refreshPasscodeLockPreference()
}
