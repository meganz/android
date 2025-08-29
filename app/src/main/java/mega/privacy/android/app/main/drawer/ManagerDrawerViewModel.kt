package mega.privacy.android.app.main.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.usecase.HasBackupsChildren
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.chat.GetCurrentUserStatusUseCase
import mega.privacy.android.domain.usecase.contact.MonitorMyChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.notifications.GetEnabledNotificationsUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ManagerDrawerViewModel @Inject constructor(
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val getCurrentUserStatusUseCase: GetCurrentUserStatusUseCase,
    private val hasBackupsChildren: HasBackupsChildren,
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorMyChatOnlineStatusUseCase: MonitorMyChatOnlineStatusUseCase,
    private val monitorVerificationStatus: MonitorVerificationStatus,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getEnabledNotificationsUseCase: GetEnabledNotificationsUseCase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ManagerDrawerUiState())
    val state = _state.asStateFlow()

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

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
        shouldShowPromoTag()
        monitorFetchNodesFinish()
        monitorStorageState()
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
            monitorNodeUpdatesUseCase().collect { updatedNodes ->
                val backupsNodeHandle = state.value.backupsNodeHandle
                if (backupsNodeHandle != NodeId(-1L)) {
                    // Check if the backups node is updated
                    updatedNodes.changes.keys.find { backupsNodeHandle == it.parentId }
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
                getBackupsNodeUseCase()
            }.onSuccess { backupsNode ->
                _state.update { it.copy(backupsNodeHandle = backupsNode?.id ?: NodeId(-1L)) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorCurrentUserStatus() {
        viewModelScope.launch {
            monitorMyChatOnlineStatusUseCase().collect { onlineStatus ->
                _state.update { it.copy(userChatStatus = onlineStatus.status) }
            }
        }
    }

    private fun getCurrentUserStatus() {
        viewModelScope.launch {
            runCatching {
                getCurrentUserStatusUseCase()
            }.onSuccess { status ->
                _state.update { it.copy(userChatStatus = status) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun shouldShowPromoTag() {
        viewModelScope.launch {
            runCatching {
                val promoNotificationCount = getEnabledNotificationsUseCase().size
                _state.update { it.copy(showPromoTag = promoNotificationCount > 0) }
            }
        }
    }

    private fun monitorFetchNodesFinish() {
        viewModelScope.launch {
            monitorFetchNodesFinishUseCase()
                .catch { Timber.e(it) }
                .collect { checkRootNode() }
        }
    }

    private fun monitorStorageState() {
        viewModelScope.launch {
            monitorStorageStateUseCase()
                .catch { Timber.e(it) }
                .collectLatest { storageState ->
                    _state.update {
                        it.copy(storageState = storageState)
                    }
                }
        }
    }
}
