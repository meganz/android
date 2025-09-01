package mega.privacy.android.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.appstate.content.mapper.ScreenPreferenceDestinationMapper
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.COOKIES_URI
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.presentation.settings.model.SettingsState
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenSummaryMapper
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.CanDeleteAccount
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.RequestAccountDeletion
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.account.IsMultiFactorAuthEnabledUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioBackgroundPlayEnabledUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.preference.MonitorStartScreenPreferenceDestinationUseCase
import mega.privacy.android.domain.usecase.setting.MonitorContactLinksOptionUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.SetShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.SetSubFolderMediaDiscoveryEnabledUseCase
import mega.privacy.android.domain.usecase.setting.ToggleContactLinksOptionUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.qualifier.DefaultStartScreen
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val canDeleteAccount: CanDeleteAccount,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val isMultiFactorAuthAvailable: IsMultiFactorAuthAvailable,
    private val monitorContactLinksOptionUseCase: MonitorContactLinksOptionUseCase,
    private val startScreen: MonitorStartScreenPreference,
    private val monitorHideRecentActivityUseCase: MonitorHideRecentActivityUseCase,
    private val setHideRecentActivityUseCase: SetHideRecentActivityUseCase,
    private val monitorMediaDiscoveryView: MonitorMediaDiscoveryView,
    private val setMediaDiscoveryView: SetMediaDiscoveryView,
    private val toggleContactLinksOptionUseCase: ToggleContactLinksOptionUseCase,
    private val isMultiFactorAuthEnabledUseCase: IsMultiFactorAuthEnabledUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val requestAccountDeletion: RequestAccountDeletion,
    private val isChatLoggedIn: IsChatLoggedIn,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorPasscodeLockPreferenceUseCase: MonitorPasscodeLockPreferenceUseCase,
    private val setSubFolderMediaDiscoveryEnabledUseCase: SetSubFolderMediaDiscoveryEnabledUseCase,
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
    private val getSessionTransferURLUseCase: GetSessionTransferURLUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val setShowHiddenItemsUseCase: SetShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val setAudioBackgroundPlayEnabledUseCase: SetAudioBackgroundPlayEnabledUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase,
    private val startScreenSummaryMapper: StartScreenSummaryMapper,
    private val mainDestinations: Set<@JvmSuppressWildcards MainNavItem>,
    private val monitorStartScreenPreferenceDestinationUseCase: MonitorStartScreenPreferenceDestinationUseCase,
    private val screenPreferenceDestinationMapper: ScreenPreferenceDestinationMapper,
    @DefaultStartScreen private val defaultStartScreen: NavKey,
) : ViewModel() {
    private val state = MutableStateFlow(initialiseState())
    val uiState: StateFlow<SettingsState> = state
    private val online =
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private var toggleBackgroundPlayJob: Job? = null

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
            startScreenSummary = "",
            hideRecentActivityChecked = false,
            mediaDiscoveryViewState = MediaDiscoveryViewSettings.INITIAL.ordinal,
            email = "",
            accountType = "",
            passcodeLock = false,
            subFolderMediaDiscoveryChecked = true,
            isHiddenNodesEnabled = null,
            showHiddenItems = false,
            accountDetail = null,
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
                monitorContactLinksOptionUseCase().catch { e ->
                    Timber.e(e, "Error when monitoring Auto accept QR settings")
                    emit(false)
                }.map { enabled ->
                    { state: SettingsState -> state.copy(autoAcceptChecked = enabled) }
                },
                flow { emit(isMultiFactorAuthEnabledUseCase()) }
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
                monitorStartScreenSummary(),
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
                flow { emit(isHiddenNodesActive()) }
                    .map { enabled ->
                        { state: SettingsState -> state.copy(isHiddenNodesEnabled = enabled) }
                    },
                monitorShowHiddenItemsUseCase()
                    .map { isEnabled ->
                        { state: SettingsState -> state.copy(showHiddenItems = isEnabled) }
                    },
                monitorAccountDetailUseCase()
                    .map { accountDetail ->
                        { state: SettingsState -> state.copy(accountDetail = accountDetail) }
                    },
            ).catch {
                Timber.e(it)
            }.collect {
                state.update(it)
            }
        }
        viewModelScope.launch {
            monitorMyAccountUpdateUseCase().collect {
                if (it.action == MyAccountUpdate.Action.UPDATE_ACCOUNT_DETAILS) {
                    refreshAccount()
                }
            }
        }
    }

    private fun monitorStartScreenSummary(): Flow<(SettingsState) -> SettingsState> {
        return flow { emit(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) }
            .flatMapLatest { singleActivityFlagEnabled ->
                if (singleActivityFlagEnabled) {
                    monitorStartScreenPreferenceDestinationUseCase()
                        .map { destinationPreference ->
                            screenPreferenceDestinationMapper(destinationPreference)
                                ?: defaultStartScreen
                        }.map { destination ->
                            startScreenSummaryMapper(mainDestinations.first { it.destination == destination })
                        }.map { screenName ->
                            { state: SettingsState -> state.copy(startScreenSummary = screenName) }
                        }
                } else {
                    startScreen()
                        .map { startScreen ->
                            startScreenSummaryMapper(startScreen)
                        }.map { screenName ->
                            { state: SettingsState -> state.copy(startScreenSummary = screenName) }
                        }
                }

            }

    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    /**
     * Get link for Cookie policy page
     */
    suspend fun getCookiePolicyLink() = runCatching {
        val isAdsFeatureEnabled = getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)
        val url =
            if (isAdsFeatureEnabled) getSessionTransferURLUseCase("cookie") else COOKIES_URI
        url
    }.onFailure {
        Timber.e("Failed to fetch session transfer URL for Cookie Policy page: ${it.message}")
    }.getOrDefault(COOKIES_URI)

    private fun monitorPasscodePreference() =
        monitorPasscodeLockPreferenceUseCase().map { enabled ->
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

    /**
     * Retrieves the Multi-Factor Authenticator State
     */
    fun refreshMultiFactorAuthSetting() {
        viewModelScope.launch {
            state.update {
                it.copy(multiFactorAuthChecked = isMultiFactorAuthEnabledUseCase())
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
            toggleContactLinksOptionUseCase()
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

    /**
     * Set show hidden items enabled state
     *
     * @param enabled true if hidden items should be shown
     */
    fun setShowHiddenItemsEnabled(enabled: Boolean) = viewModelScope.launch {
        setShowHiddenItemsUseCase(enabled)
    }

    internal fun toggleBackgroundPlay(isEnable: Boolean) {
        toggleBackgroundPlayJob?.cancel()
        toggleBackgroundPlayJob = viewModelScope.launch {
            setAudioBackgroundPlayEnabledUseCase(isEnable)
        }
    }

    suspend fun getBusinessStatus() = getBusinessStatusUseCase()
}
