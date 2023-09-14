package mega.privacy.android.app.main.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetBackupsNode
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.usecase.HasBackupsChildren
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.GetCurrentUserStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorMyChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ManagerDrawerViewModel @Inject constructor(
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val getCurrentUserStatusUseCase: GetCurrentUserStatusUseCase,
    private val hasBackupsChildren: HasBackupsChildren,
    private val getBackupsNode: GetBackupsNode,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val monitorMyChatOnlineStatusUseCase: MonitorMyChatOnlineStatusUseCase,
    private val monitorVerificationStatus: MonitorVerificationStatus,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ManagerDrawerUiState())
    val state = _state.asStateFlow()

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    /**
     * Get latest [StorageState]
     */
    fun getStorageState() = monitorStorageStateEventUseCase.getState()

    /**
     * Monitor My Account Update event
     */
    val monitorMyAccountUpdateEvent = monitorMyAccountUpdateUseCase()

    init {
        checkRootNode()
        getCurrentUserStatus()
        monitorCurrentUserStatus()
        loadBackupNode()
        checkBackupChildren()
        monitorNodeUpdatesEvent()
        observerVerificationStatus()
        observerConnectivityEvent()
        loadFeatureFlags()
    }

    private fun loadFeatureFlags() {
        viewModelScope.launch {
            _state.update { it.copy(enabledFlags = getEnabledFeatures()) }
        }
    }

    private fun observerConnectivityEvent() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it) }
                .collect { isConnected ->
                    _state.update { it.copy(isConnected = isConnected) }
                }
        }
    }

    private fun checkRootNode() {
        viewModelScope.launch {
            runCatching {
                rootNodeExistsUseCase()
            }.onSuccess { exists ->
                _state.update { it.copy(isRootNodeExist = exists) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun observerVerificationStatus() {
        viewModelScope.launch {
            monitorVerificationStatus()
                .catch { Timber.e(it) }
                .onEach {
                    Timber.d("Verification status returned: $it")
                }
                .collect { status ->
                    _state.update { it.copy(canVerifyPhoneNumber = status is UnVerified && status.canRequestOptInVerification) }
                }
        }
    }

    private fun monitorNodeUpdatesEvent() {
        viewModelScope.launch {
            monitorNodeUpdates().collect { updatedNodes ->
                val backupsNode = state.value.backUpNodeHandle
                if (backupsNode != -1L) {
                    // Check if the back up node is updated
                    updatedNodes.changes.keys.find { backupsNode == it.parentId.longValue }
                        ?.run { checkBackupChildren() }
                }
            }
        }
    }

    private fun checkBackupChildren() {
        viewModelScope.launch {
            runCatching {
                hasBackupsChildren()
            }.onSuccess { hasChildren ->
                _state.update { it.copy(hasBackupsChildren = hasChildren) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun loadBackupNode() {
        viewModelScope.launch {
            runCatching {
                getBackupsNode()
            }.onSuccess { node ->
                _state.update { it.copy(backUpNodeHandle = node?.handle ?: -1L) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorCurrentUserStatus() {
        viewModelScope.launch {
            monitorMyChatOnlineStatusUseCase().collect { onlineStatus ->
                _state.update { it.copy(userStatus = onlineStatus.status) }
            }
        }
    }

    private fun getCurrentUserStatus() {
        viewModelScope.launch {
            runCatching {
                getCurrentUserStatusUseCase()
            }.onSuccess { status ->
                _state.update { it.copy(userStatus = status) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private suspend fun getEnabledFeatures(): Set<Feature> {
        return setOfNotNull(
            AppFeatures.AndroidSync.takeIf { getFeatureFlagValueUseCase(it) },
            AppFeatures.DeviceCenter.takeIf { getFeatureFlagValueUseCase(it) },
        )
    }
}