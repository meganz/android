package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetBackupsNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.main.dialog.shares.RemoveShareResultMapper
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.manager.model.ManagerState
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.*
import mega.privacy.android.domain.usecase.account.GetFullAccountInfoUseCase
import mega.privacy.android.domain.usecase.account.GetIncomingContactRequestsUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.RenameRecoveryKeyFileUseCase
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.billing.GetActiveSubscriptionUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreCameraUploadsFoldersInRubbishBinUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsFolderDestinationUseCase
import mega.privacy.android.domain.usecase.chat.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.contact.SaveContactByEmailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.MonitorFinishActivityUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallEndedUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.node.RemoveShareUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import mega.privacy.android.domain.usecase.photos.mediadiscovery.SendStatisticsMediaDiscoveryUseCase
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import mega.privacy.android.domain.usecase.transfers.completed.DeleteOldestCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncsUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Manager view model
 *
 * @property monitorGlobalUpdates
 * @property getBackupsNode
 * @property getNumUnreadUserAlertsUseCase
 * @property hasBackupsChildren
 * @property sendStatisticsMediaDiscoveryUseCase
 * @property savedStateHandle
 * @property monitorStorageStateEventUseCase
 * @param monitorCameraUploadsFolderDestinationUseCase
 * @property getPrimarySyncHandleUseCase
 * @property getSecondarySyncHandleUseCase
 * @property areCameraUploadsFoldersInRubbishBinUseCase
 * @property getCloudSortOrder
 * @property monitorConnectivityUseCase
 * @property getExtendedAccountDetail
 * @property getFullAccountInfoUseCase
 * @property getActiveSubscriptionUseCase
 * @property getFeatureFlagValueUseCase
 * @property getUnverifiedIncomingShares
 * @property getUnverifiedOutgoingShares
 * @property monitorUserUpdates
 * @property startCameraUploadUseCase
 * @property stopCameraUploadsUseCase
 * @property deleteOldestCompletedTransfersUseCase
 * @property getIncomingContactRequestsUseCase
 * @param monitorNodeUpdates
 * @param monitorContactUpdates monitor contact update when credentials verification occurs to update shares count
 * @param monitorContactRequestUpdates
 * @param monitorFinishActivityUseCase
 * @param monitorOfflineNodeAvailabilityUseCase monitor the offline availability of the file to update the UI
 * @param getNumUnreadChatsUseCase  monitor number of unread chats
 * @property monitorBackupFolder
 * @property getScheduledMeetingByChat  [GetScheduledMeetingByChat]
 * @property getChatCallUseCase [GetChatCallUseCase]
 * @property startMeetingInWaitingRoomChatUseCase [StartMeetingInWaitingRoomChatUseCase]
 * @property answerChatCallUseCase [AnswerChatCallUseCase]
 * @property setChatVideoInDeviceUseCase [SetChatVideoInDeviceUseCase]
 * @property rtcAudioManagerGateway [RTCAudioManagerGateway]
 * @property chatManagement [ChatManagement]
 * @property passcodeManagement [PasscodeManagement]
 * @property monitorChatSessionUpdatesUseCase [MonitorChatSessionUpdatesUseCase]
 * @property hangChatCallUseCase [HangChatCallUseCase]
 * @property monitorCallRecordingConsentEventUseCase [MonitorCallRecordingConsentEventUseCase]
 * @property monitorCallEndedUseCase [MonitorCallEndedUseCase]
 * @property getNodeByHandle [GetNodeByHandle]
 */
@HiltViewModel
class ManagerViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    monitorContactUpdates: MonitorContactUpdates,
    private val monitorGlobalUpdates: MonitorGlobalUpdates,
    monitorContactRequestUpdates: MonitorContactRequestUpdates,
    private val getBackupsNode: GetBackupsNode,
    private val getNumUnreadUserAlertsUseCase: GetNumUnreadUserAlertsUseCase,
    private val hasBackupsChildren: HasBackupsChildren,
    private val sendStatisticsMediaDiscoveryUseCase: SendStatisticsMediaDiscoveryUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    monitorCameraUploadsFolderDestinationUseCase: MonitorCameraUploadsFolderDestinationUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val areCameraUploadsFoldersInRubbishBinUseCase: AreCameraUploadsFoldersInRubbishBinUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val getExtendedAccountDetail: GetExtendedAccountDetail,
    private val getFullAccountInfoUseCase: GetFullAccountInfoUseCase,
    private val getActiveSubscriptionUseCase: GetActiveSubscriptionUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getUnverifiedIncomingShares: GetUnverifiedIncomingShares,
    private val getUnverifiedOutgoingShares: GetUnverifiedOutgoingShares,
    monitorFinishActivityUseCase: MonitorFinishActivityUseCase,
    private val requireTwoFactorAuthenticationUseCase: RequireTwoFactorAuthenticationUseCase,
    private val setCopyLatestTargetPathUseCase: SetCopyLatestTargetPathUseCase,
    private val setMoveLatestTargetPathUseCase: SetMoveLatestTargetPathUseCase,
    private val monitorSecurityUpgradeInApp: MonitorSecurityUpgradeInApp,
    private val listenToNewMediaUseCase: ListenToNewMediaUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val saveContactByEmailUseCase: SaveContactByEmailUseCase,
    private val createShareKey: CreateShareKey,
    private val deleteOldestCompletedTransfersUseCase: DeleteOldestCompletedTransfersUseCase,
    private val getIncomingContactRequestsUseCase: GetIncomingContactRequestsUseCase,
    monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase,
    monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    monitorOfflineNodeAvailabilityUseCase: MonitorOfflineFileAvailabilityUseCase,
    private val monitorChatArchivedUseCase: MonitorChatArchivedUseCase,
    private val restoreNodesUseCase: RestoreNodesUseCase,
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val deleteNodesUseCase: DeleteNodesUseCase,
    private val moveNodesUseCase: MoveNodesUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
    private val renameRecoveryKeyFileUseCase: RenameRecoveryKeyFileUseCase,
    private val removeShareUseCase: RemoveShareUseCase,
    private val removeShareResultMapper: RemoveShareResultMapper,
    getNumUnreadChatsUseCase: GetNumUnreadChatsUseCase,
    private val disableExportNodesUseCase: DisableExportNodesUseCase,
    private val removePublicLinkResultMapper: RemovePublicLinkResultMapper,
    private val dismissPsaUseCase: DismissPsaUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getChatLinkContentUseCase: GetChatLinkContentUseCase,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val chatManagement: ChatManagement,
    private val passcodeManagement: PasscodeManagement,
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val monitorCallRecordingConsentEventUseCase: MonitorCallRecordingConsentEventUseCase,
    private val monitorCallEndedUseCase: MonitorCallEndedUseCase,
    private val getNodeByHandle: GetNodeByHandle,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(ManagerState())

    /**
     * public UI State
     */
    val state: StateFlow<ManagerState> = _state

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent = monitorConnectivityUseCase()

    /**
     * Monitor Finish Activity event
     */
    val monitorFinishActivityEvent = monitorFinishActivityUseCase()

    /**
     * Monitor My Account Update event
     */
    val monitorMyAccountUpdateEvent = monitorMyAccountUpdateUseCase()

    /**
     * Monitor Camera Upload Folder Icon Update event
     */
    val monitorCameraUploadFolderIconUpdateEvent = monitorCameraUploadsFolderDestinationUseCase()

    /**
     * Monitor offline file availability event
     */
    val monitorOfflineNodeAvailabilityEvent = monitorOfflineNodeAvailabilityUseCase()

    /**
     * Monitor number of unread chats
     */
    val monitorNumUnreadChats = getNumUnreadChatsUseCase()

    private val _incomingContactRequests = MutableStateFlow<List<ContactRequest>>(emptyList())

    /**
     * The latest incoming contact requests
     */
    val incomingContactRequests = _incomingContactRequests.asStateFlow()

    private val _stalledIssuesCount = MutableStateFlow(0)

    /**
     * Sync stalled issues count
     */
    val stalledIssuesCount = _stalledIssuesCount.asStateFlow()

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    private val isFirstLogin = savedStateHandle.getStateFlow(
        key = isFirstLoginKey,
        initialValue = false
    )

    private var monitorChatSessionUpdatesJob: Job? = null

    init {
        viewModelScope.launch {
            val order = getCloudSortOrder()
            combine(
                isFirstLogin,
                flowOf(getUnverifiedIncomingShares(order) + getUnverifiedOutgoingShares(order))
                    .map { it.size },
                flowOf(getEnabledFeatures()),
            ) { firstLogin: Boolean, pendingShares: Int, features: Set<Feature> ->
                { state: ManagerState ->
                    state.copy(
                        isFirstLogin = firstLogin,
                        pendingActionsCount = state.pendingActionsCount + pendingShares,
                        enabledFlags = features
                    )
                }
            }.collectLatest {
                _state.update(it)
            }
        }

        viewModelScope.launch {
            monitorNodeUpdates().collect {
                val nodeList = it.changes.keys.toList()
                onReceiveNodeUpdate(true)
                checkCameraUploadFolder(nodeList)
                checkUnverifiedSharesCount()
            }
        }
        viewModelScope.launch {
            monitorContactUpdates().collectLatest { updates ->
                if (updates.changes.values.any { it.contains(UserChanges.AuthenticationInformation) }) {
                    checkUnverifiedSharesCount()
                }
            }
        }
        viewModelScope.launch {
            monitorSecurityUpgradeInApp().collect {
                if (it) {
                    setShouldAlertUserAboutSecurityUpgrade(true)
                }
            }
        }
        viewModelScope.launch {
            listenToNewMediaUseCase()
            deleteOldestCompletedTransfersUseCase()
        }
        viewModelScope.launch {
            monitorUserUpdates()
                .catch { Timber.w("Exception monitoring user updates: $it") }
                .filter { it == UserChanges.CameraUploadsFolder }
                .collect {
                    Timber.d(
                        "The Camera Uploads Sync Handles have been changed in the API + " +
                                "Refresh the Sync Handles"
                    )
                    runCatching { establishCameraUploadsSyncHandlesUseCase() }
                        .onFailure { Timber.e(it) }
                }
        }
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect {
                _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = true) }
            }
        }
        viewModelScope.launch {
            updateIncomingContactRequests()
        }

        viewModelScope.launch {
            monitorChatArchivedUseCase().conflate().collect { chatTitle ->
                _state.update { it.copy(titleChatArchivedEvent = chatTitle) }
            }
        }
        viewModelScope.launch {
            monitorBackupFolder()
                .catch { Timber.w("Exception monitoring backups folder: $it") }
                .map {
                    // Default to an Invalid Handle of -1L when an error occurs
                    it.getOrDefault(NodeId(-1L))
                }.collectLatest { backupsFolderNodeId ->
                    _state.update { it.copy(userRootBackupsFolderHandle = backupsFolderNodeId) }
                    // This will be removed in a later refactor
                    MegaNodeUtil.myBackupHandle = backupsFolderNodeId.longValue
                }
        }
        viewModelScope.launch {
            monitorContactRequestUpdates()
                .collect {
                    Timber.d("Contact Request Updates")
                    updateIncomingContactRequests()
                    updateContactRequests(it)
                }
        }
        @Suppress("DEPRECATION")
        viewModelScope.launch {
            monitorGlobalUpdates()
                .filterIsInstance<GlobalUpdate.OnUserAlertsUpdate>()
                .collect {
                    Timber.d("onUserAlertsUpdate")
                    updateIncomingContactRequests()
                    checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE_AND_TOOLBAR_ICON)
                }
        }

        viewModelScope.launch {
            monitorSyncStalledIssuesUseCase().catch { Timber.e(it) }.collect {
                _stalledIssuesCount.value = it.size
            }
        }

        viewModelScope.launch {
            val androidSyncEnabled =
                getFeatureFlagValueUseCase(AppFeatures.AndroidSync)

            if (androidSyncEnabled) {
                monitorSyncsUseCase().catch { Timber.e(it) }.collect { syncFolders ->
                    val isServiceEnabled = syncFolders.isNotEmpty()
                    _state.update { it.copy(androidSyncServiceEnabled = isServiceEnabled) }
                }
            }
        }

        viewModelScope.launch {
            monitorCallRecordingConsentEventUseCase().conflate()
                .collect { isRecordingConsentAccepted ->
                    _state.update {
                        it.copy(
                            showRecordingConsentDialog = false,
                            isRecordingConsentAccepted = isRecordingConsentAccepted
                        )
                    }
                }
        }

        viewModelScope.launch {
            monitorCallEndedUseCase().conflate().collect { chatId ->
                if (chatId == state.value.callInProgressChatId) {
                    resetCallRecordingState()
                }
            }
        }
    }

    /**
     * A shorthand way of retrieving the [ManagerState]
     *
     * @return the [ManagerState]
     */
    fun state() = _state.value

    private suspend fun getEnabledFeatures(): Set<Feature> {
        return setOfNotNull(
            AppFeatures.QRCodeCompose.takeIf { getFeatureFlagValueUseCase(it) },
            AppFeatures.DeviceCenter.takeIf { getFeatureFlagValueUseCase(it) },
        )
    }

    /**
     * Cache up-to-date incoming contact requests in view model
     */
    private suspend fun updateIncomingContactRequests() {
        runCatching { getIncomingContactRequestsUseCase() }
            .onSuccess { requests ->
                _incomingContactRequests.update { requests }
            }.onFailure {
                Timber.e(it)
            }
    }

    /**
     * Set a flag to know if the current navigation level is the first one
     *
     * @param isFirstNavigationLevel true if the current navigation level corresponds to the first level
     */
    fun setIsFirstNavigationLevel(isFirstNavigationLevel: Boolean) = viewModelScope.launch {
        _state.update { it.copy(isFirstNavigationLevel = isFirstNavigationLevel) }
    }

    /**
     * Set the current shares tab to the UI state
     *
     * @param tab shares tab to set
     */
    fun setSharesTab(tab: SharesTab) = viewModelScope.launch {
        _state.update { it.copy(sharesTab = tab) }
    }

    /**
     * Notify that the node update has been handled by the UI
     */
    fun nodeUpdateHandled() {
        onReceiveNodeUpdate(false)
    }

    /**
     *  Get the unverified shares count and set state
     */
    private suspend fun checkUnverifiedSharesCount() {
        val sortOrder = getCloudSortOrder()
        val unverifiedIncomingShares = getUnverifiedIncomingShares(sortOrder).size
        val unverifiedOutgoingShares = getUnverifiedOutgoingShares(sortOrder).size
        _state.update { it.copy(pendingActionsCount = unverifiedIncomingShares + unverifiedOutgoingShares) }
    }

    /**
     * Set the ui one-off event when a node update is received
     *
     * @param update true if a node update has been received
     */
    private fun onReceiveNodeUpdate(update: Boolean) = viewModelScope.launch {
        _state.update { it.copy(nodeUpdateReceived = update) }
    }

    private val numUnreadUserAlerts = SingleLiveEvent<Pair<UnreadUserAlertsCheckType, Int>>()

    /**
     * Notifies about the number of unread user alerts once.
     *
     * @return [SingleLiveEvent] with the number of unread user alerts.
     */
    fun onGetNumUnreadUserAlerts(): SingleLiveEvent<Pair<UnreadUserAlertsCheckType, Int>> =
        numUnreadUserAlerts

    /**
     * Checks the number of unread user alerts.
     */
    fun checkNumUnreadUserAlerts(type: UnreadUserAlertsCheckType) {
        viewModelScope.launch {
            numUnreadUserAlerts.value = Pair(type, getNumUnreadUserAlertsUseCase())
        }
    }

    /**
     * Fire a Media Discovery stats event
     */
    fun onMediaDiscoveryOpened(mediaHandle: Long) {
        viewModelScope.launch {
            sendStatisticsMediaDiscoveryUseCase(mediaHandle)
        }
    }

    /**
     * Set first login status
     */
    fun setIsFirstLogin(newIsFirstLogin: Boolean) {
        savedStateHandle[isFirstLoginKey] = newIsFirstLogin
    }

    /**
     * Get latest [StorageState]
     */
    fun getStorageState() = monitorStorageStateEventUseCase.getState()

    /**
     * Get Cloud Sort Order
     */
    suspend fun getOrder() = getCloudSortOrder()

    /**
     * After nodes on Cloud Drive changed or some nodes are moved to rubbish bin,
     * need to check if CU and MU folders have been moved to rubbish bin
     * If true, then stop the camera uploads process and reschedule
     *
     * @param updatedNodes  Nodes which have changed.
     */
    private suspend fun checkCameraUploadFolder(updatedNodes: List<Node>?) {
        val primaryHandle = getPrimarySyncHandleUseCase()
        val secondaryHandle = getSecondarySyncHandleUseCase()

        updatedNodes?.firstOrNull {
            it.id.longValue == primaryHandle || it.id.longValue == secondaryHandle
        } ?: return

        val areCameraFoldersInRubbishBin =
            areCameraUploadsFoldersInRubbishBinUseCase(
                primaryHandle,
                secondaryHandle
            )
        if (areCameraFoldersInRubbishBin) {
            stopCameraUploads(shouldReschedule = true)
        }
    }

    /**
     * Get extended account detail
     */
    fun askForExtendedAccountDetails() {
        Timber.d("askForExtendedAccountDetails")
        viewModelScope.launch {
            getExtendedAccountDetail(
                forceRefresh = true,
                sessions = true,
                purchases = false,
                transactions = false,
            )
        }
    }

    /**
     * Ask for full account info
     *
     */
    fun askForFullAccountInfo() {
        Timber.d("askForFullAccountInfo")
        viewModelScope.launch {
            runCatching {
                getFullAccountInfoUseCase()
            }.onFailure {
                Timber.w("Exception getting account info.", it)
            }
        }
    }

    /**
     * Checks if 2FA alert should be shown
     * @param newAccount checks if its a new user
     * @param firstLogin checks if its a first login
     */
    fun checkToShow2FADialog(newAccount: Boolean, firstLogin: Boolean) = viewModelScope.launch {
        runCatching {
            requireTwoFactorAuthenticationUseCase(
                newAccount = newAccount,
                firstLogin = firstLogin,
            )
        }.onSuccess {
            _state.update { state -> state.copy(show2FADialog = it) }
        }.onFailure {
            Timber.w("Exception checking 2FA.", it)
        }
    }

    /**
     * Mark handle show2f a dialog
     */
    fun markHandleShow2FADialog() {
        _state.update { state -> state.copy(show2FADialog = false) }
    }

    /**
     * Set last used path of copy as target path for next copy
     */
    private fun setCopyTargetPath(path: Long) {
        viewModelScope.launch {
            runCatching { setCopyLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Set last used path of move as target path for next move
     */
    private fun setMoveTargetPath(path: Long) {
        viewModelScope.launch {
            runCatching { setMoveLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Set the security upgrade alert state
     *
     * @param shouldShow true if the security upgrade alert needs to be shown
     */
    fun setShouldAlertUserAboutSecurityUpgrade(shouldShow: Boolean) {
        _state.update {
            it.copy(shouldAlertUserAboutSecurityUpgrade = shouldShow)
        }
    }

    /**
     * on Consume Push notification settings updated event
     */
    fun onConsumePushNotificationSettingsUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = false) }
        }
    }

    /**
     * Start camera upload
     */
    fun startCameraUpload() = viewModelScope.launch {
        startCameraUploadUseCase()
    }

    /**
     * Stop camera upload
     *
     * @param shouldReschedule true if the Camera Uploads should be rescheduled at a later time
     */
    fun stopCameraUploads(shouldReschedule: Boolean) = viewModelScope.launch {
        runCatching { stopCameraUploadsUseCase(shouldReschedule = shouldReschedule) }
            .onFailure { Timber.d(it) }
    }

    /**
     * Add new contact
     *
     * @param email
     */
    private fun addNewContact(email: String?) {
        if (email.isNullOrEmpty()) return
        viewModelScope.launch {
            runCatching {
                saveContactByEmailUseCase(email)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Init share key
     *
     * @param node
     */
    suspend fun initShareKey(node: MegaNode?) = runCatching {
        node?.let { createShareKey(it) }
    }.onFailure {
        Timber.e(it)
    }

    /**
     * Consume chat archive event
     */
    fun onChatArchivedEventConsumed() =
        _state.update { it.copy(titleChatArchivedEvent = null) }


    /**
     * Restore nodes
     *
     * @param nodes
     */
    fun restoreNodes(nodes: Map<Long, Long>) {
        viewModelScope.launch {
            val result = runCatching {
                restoreNodesUseCase(nodes)
            }
            _state.update { it.copy(restoreNodeResult = result) }
        }
    }

    /**
     * Mark handle restore node result
     *
     */
    fun markHandleRestoreNodeResult() {
        _state.update { it.copy(restoreNodeResult = null) }
    }

    /**
     * Check restore nodes name collision
     *
     * @param nodes
     */
    fun checkRestoreNodesNameCollision(nodes: List<MegaNode>) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionUseCase(
                    nodes.associate { it.handle to it.restoreHandle },
                    NodeNameCollisionType.RESTORE
                )
            }.onSuccess { result ->
                _state.update { it.copy(nodeNameCollisionResult = result) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Check move nodes name collision
     *
     * @param nodes
     * @param targetNode
     */
    fun checkNodesNameCollision(
        nodes: List<Long>,
        targetNode: Long,
        type: NodeNameCollisionType,
    ) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionUseCase(
                    nodes.associateWith { targetNode },
                    type
                )
            }.onSuccess { result ->
                _state.update { it.copy(nodeNameCollisionResult = result) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Move nodes
     *
     * @param nodes
     */
    fun moveNodes(nodes: Map<Long, Long>) {
        viewModelScope.launch {
            val result = runCatching {
                moveNodesUseCase(nodes)
            }.onSuccess {
                setMoveTargetPath(nodes.values.first())
            }.onFailure {
                Timber.e(it)
            }
            _state.update { state -> state.copy(moveRequestResult = result) }
        }
    }

    /**
     * Copy nodes
     *
     * @param nodes
     */
    fun copyNodes(nodes: Map<Long, Long>) {
        viewModelScope.launch {
            val result = runCatching {
                copyNodesUseCase(nodes)
            }.onSuccess {
                setCopyTargetPath(nodes.values.first())
            }.onFailure {
                Timber.e(it)
            }
            _state.update { state -> state.copy(moveRequestResult = result) }
        }
    }

    /**
     * Mark handle node name collision result
     *
     */
    fun markHandleNodeNameCollisionResult() {
        _state.update { it.copy(nodeNameCollisionResult = null) }
    }

    private fun updateContactRequests(requests: List<ContactRequest>) {
        Timber.d("updateContactRequests")
        requests.forEach { req ->
            if (req.isOutgoing) {
                Timber.d("SENT REQUEST")
                Timber.d("STATUS: %s, Contact Handle: %d", req.status, req.handle)
                if (req.status === ContactRequestStatus.Accepted) {
                    addNewContact(req.targetEmail)
                }
            } else {
                Timber.d("RECEIVED REQUEST")
                Timber.d("STATUS: %s Contact Handle: %d", req.status, req.handle)
                if (req.status === ContactRequestStatus.Accepted) {
                    addNewContact(req.sourceEmail)
                }
            }
        }
        checkNumUnreadUserAlerts(UnreadUserAlertsCheckType.NAVIGATION_TOOLBAR_ICON)
    }

    /**
     * Move nodes to rubbish
     *
     * @param nodeHandles
     */
    fun moveNodesToRubbishBin(nodeHandles: List<Long>) {
        viewModelScope.launch {
            val result = runCatching {
                moveNodesToRubbishUseCase(nodeHandles)
            }.onFailure {
                Timber.e(it)
            }
            _state.update { state -> state.copy(moveRequestResult = result) }
        }
    }

    /**
     * Delete nodes
     *
     * @param nodeHandles
     */
    fun deleteNodes(nodeHandles: List<Long>) {
        viewModelScope.launch {
            val result = runCatching {
                deleteNodesUseCase(nodeHandles.map { NodeId(it) })
            }.onFailure {
                Timber.e(it)
            }
            _state.update { state -> state.copy(moveRequestResult = result) }
        }
    }

    /**
     * Mark handle move request result
     *
     */
    fun markHandleMoveRequestResult() {
        _state.update { it.copy(moveRequestResult = null) }
    }

    /**
     * Rename Recovery Key file if needed
     *
     * @param relativePath    Relative path of the file
     * @param newName         New name for the file
     */
    fun renameRecoveryKeyFileIfNeeded(relativePath: String, newName: String) {
        viewModelScope.launch {
            runCatching {
                renameRecoveryKeyFileUseCase(relativePath, newName)
            }.onSuccess {
                if (it) {
                    Timber.d("Old $relativePath file has been renamed to $newName")
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }


    /**
     * Remove shares
     *
     */
    fun removeShares(nodeIds: List<Long>) {
        viewModelScope.launch {
            runCatching {
                removeShareUseCase(nodeIds.map { NodeId(it) })
            }.onFailure {
                Timber.e(it)
            }.onSuccess { result ->
                val message = removeShareResultMapper(result)
                _state.update { state ->
                    state.copy(
                        message = message,
                    )
                }
            }
        }
    }

    /**
     * Disable export nodes
     *
     */
    fun disableExport(nodeIds: List<Long>) {
        viewModelScope.launch {
            runCatching {
                disableExportNodesUseCase(nodeIds.map { NodeId(it) })
            }.onFailure {
                Timber.e(it)
            }.onSuccess { result ->
                val message = removePublicLinkResultMapper(result)
                _state.update { state ->
                    state.copy(
                        message = message,
                    )
                }
            }
        }
    }

    /**
     * Mark handled message
     *
     */
    fun markHandledMessage() {
        _state.update { it.copy(message = null) }
    }

    /**
     * Dismiss psa
     *
     * @param psaId
     */
    fun dismissPsa(psaId: Int) = viewModelScope.launch { dismissPsaUseCase(psaId) }

    /**
     * Get the parent handle from where the search is performed
     *
     * @param browserParentHandle
     * @param rubbishBinParentHandle
     * @param backupsParentHandle
     * @param incomingParentHandle
     * @param outgoingParentHandle
     * @param linksParentHandle
     *
     * @return the parent handle from where the search is performed
     */
    suspend fun getParentHandleForSearch(
        browserParentHandle: Long,
        rubbishBinParentHandle: Long,
        backupsParentHandle: Long,
        incomingParentHandle: Long,
        outgoingParentHandle: Long,
        linksParentHandle: Long,
        searchType: SearchType,
    ): Long = when (searchType) {
        SearchType.CLOUD_DRIVE -> browserParentHandle
        SearchType.INCOMING_SHARES -> incomingParentHandle
        SearchType.OUTGOING_SHARES -> outgoingParentHandle
        SearchType.LINKS -> linksParentHandle
        SearchType.RUBBISH_BIN -> rubbishBinParentHandle
        SearchType.BACKUPS -> backupsParentHandle
        SearchType.OTHER -> getRootNodeUseCase()?.id?.longValue ?: MegaApiJava.INVALID_HANDLE
    }

    /**
     * Check link
     *
     * @param link
     */
    fun checkLink(link: String?) {
        viewModelScope.launch {
            if (link.isNullOrEmpty()) return@launch
            val result = runCatching { getChatLinkContentUseCase(link) }
                .onFailure {
                    Timber.e(it)
                }
            _state.update { state -> state.copy(chatLinkContent = result) }
        }
    }

    /**
     * Mark handle check link result
     */
    fun markHandleCheckLinkResult() {
        _state.update { it.copy(chatLinkContent = null) }
    }

    /**
     * Start or answer a meeting with waiting room as a host
     *
     * @param chatId   Chat ID
     */
    fun startOrAnswerMeetingWithWaitingRoomAsHost(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                val call = getChatCallUseCase(chatId)
                val scheduledMeetingStatus = when (call?.status) {
                    ChatCallStatus.UserNoPresent -> ScheduledMeetingStatus.NotJoined(call.duration)

                    ChatCallStatus.Connecting,
                    ChatCallStatus.Joining,
                    ChatCallStatus.InProgress,
                    -> ScheduledMeetingStatus.Joined(call.duration)

                    else -> ScheduledMeetingStatus.NotStarted
                }
                if (scheduledMeetingStatus is ScheduledMeetingStatus.NotStarted) {
                    runCatching {
                        getScheduledMeetingByChat(chatId)
                    }.onSuccess { scheduledMeetingList ->
                        scheduledMeetingList?.first()?.schedId?.let { schedId ->
                            startSchedMeetingWithWaitingRoom(
                                chatId = chatId, schedIdWr = schedId
                            )
                        }
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }
                } else {
                    answerCall(chatId = chatId)
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Start scheduled meeting with waiting room
     *
     * @param chatId    Chat ID
     * @param schedIdWr Scheduled meeting ID
     */
    private fun startSchedMeetingWithWaitingRoom(chatId: Long, schedIdWr: Long) =
        viewModelScope.launch {
            Timber.d("Start scheduled meeting with waiting room")
            runCatching {
                startMeetingInWaitingRoomChatUseCase(
                    chatId = chatId,
                    schedIdWr = schedIdWr,
                    enabledVideo = false,
                    enabledAudio = true
                )
            }.onSuccess { call ->
                call?.let {
                    call.chatId.takeIf { it != INVALID_HANDLE }?.let {
                        Timber.d("Meeting started")
                        openCall(call)
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Answer call
     *
     * @param chatId    Chat Id.
     */
    private fun answerCall(chatId: Long) {
        chatManagement.addJoiningCallChatId(chatId)

        viewModelScope.launch {
            Timber.d("Answer call")
            runCatching {
                setChatVideoInDeviceUseCase()
                answerChatCallUseCase(chatId = chatId, video = false, audio = true)
            }.onSuccess { call ->
                call?.apply {
                    chatManagement.removeJoiningCallChatId(chatId)
                    rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                    CallUtil.clearIncomingCallNotification(callId)
                    openCall(call)
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Open call
     *
     * @param call  [ChatCall]
     */
    private fun openCall(call: ChatCall) {
        chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
        chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
        CallUtil.openMeetingInProgress(
            MegaApplication.getInstance().applicationContext,
            call.chatId,
            true,
            passcodeManagement,
            state().isSessionOnRecording
        )
    }

    /**
     * Sets showRecordingConsentDialog as consumed.
     */
    fun setShowRecordingConsentDialogConsumed() =
        _state.update { state -> state.copy(showRecordingConsentDialog = false) }

    /**
     * Sets isRecordingConsentAccepted.
     */
    fun setIsRecordingConsentAccepted(value: Boolean) =
        _state.update { state -> state.copy(isRecordingConsentAccepted = value) }

    /**
     * Hang chat call
     */
    fun hangChatCall(chatId: Long) = viewModelScope.launch {
        runCatching {
            getChatCallUseCase(chatId)?.let { chatCall ->
                hangChatCallUseCase(chatCall.callId)
            }
        }.onSuccess {
            resetCallRecordingState()
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Monitor chat session updates
     *
     * @param chatId    Chat ID to monitor
     */
    fun startMonitorChatSessionUpdates(chatId: Long) {
        _state.update { it.copy(callInProgressChatId = chatId) }
        monitorChatSessionUpdatesJob = viewModelScope.launch {
            monitorChatSessionUpdatesUseCase()
                .filter { it.chatId == chatId }
                .collectLatest { result ->
                    result.session?.let { session ->
                        session.changes?.apply {
                            if (contains(ChatSessionChanges.SessionOnRecording)) {
                                _state.update { state ->
                                    state.copy(
                                        isSessionOnRecording = session.isRecording,
                                        showRecordingConsentDialog = if (!state.isRecordingConsentAccepted) session.isRecording else false
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Stop monitor chat session updates
     */
    fun stopMonitorChatSessionUpdates() {
        monitorChatSessionUpdatesJob?.cancel()
    }

    /**
     * Reset call recording status properties
     */
    fun resetCallRecordingState() {
        _state.update {
            it.copy(
                callInProgressChatId = -1L,
                isSessionOnRecording = false,
                showRecordingConsentDialog = false,
                isRecordingConsentAccepted = false
            )
        }
    }

    /**
     * Retrieves the corresponding [MegaNode] from the given handle
     *
     * @param nodeHandle The Node Handle
     * @return A potentially nullable [MegaNode]
     */
    suspend fun retrieveMegaNode(nodeHandle: Long): MegaNode? = getNodeByHandle(nodeHandle)

    internal companion object {
        internal const val isFirstLoginKey = "EXTRA_FIRST_LOGIN"
        private const val INVALID_HANDLE = -1L
    }
}
