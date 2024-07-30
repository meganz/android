package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.main.dialog.shares.RemoveShareResultMapper
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.manager.model.ManagerState
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlertsUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.MonitorUserAlertUpdates
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.GetFullAccountInfoUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.RenameRecoveryKeyFileUseCase
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.GetIncomingContactRequestsUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestUpdatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsFolderDestinationUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.link.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.contact.SaveContactByEmailUseCase
import mega.privacy.android.domain.usecase.environment.MonitorDevicePowerConnectionStateUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.MonitorFinishActivityUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetUsersCallLimitRemindersUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorUpgradeDialogClosedUseCase
import mega.privacy.android.domain.usecase.meeting.SetUsersCallLimitRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.node.RemoveShareUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import mega.privacy.android.domain.usecase.notifications.BroadcastHomeBadgeCountUseCase
import mega.privacy.android.domain.usecase.notifications.GetNumUnreadPromoNotificationsUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodesUseCase
import mega.privacy.android.domain.usecase.offline.StartOfflineSyncWorkerUseCase
import mega.privacy.android.domain.usecase.photos.mediadiscovery.SendStatisticsMediaDiscoveryUseCase
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import mega.privacy.android.domain.usecase.transfers.completed.DeleteOldestCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ManagerViewModel
 *
 * @property monitorUserAlertUpdates Use case for monitoring user alert updates.
 * @property getNumUnreadUserAlertsUseCase Use case for getting the number of unread user alerts.
 * @property getNumUnreadPromoNotificationsUseCase Use case for getting the number of unread promo notifications.
 * @property sendStatisticsMediaDiscoveryUseCase Use case for sending media discovery statistics.
 * @property savedStateHandle Saved state handle for saving and restoring state related to the ViewModel.
 * @property monitorStorageStateEventUseCase Use case for monitoring storage state events.
 * @property getCloudSortOrder Use case for getting the cloud sort order.
 * @property isConnectedToInternetUseCase Use case for checking if the device is connected to the internet.
 * @property getExtendedAccountDetail Use case for getting extended account details.
 * @property getFullAccountInfoUseCase Use case for getting full account info.
 * @property getFeatureFlagValueUseCase Use case for getting the value of a feature flag.
 * @property getUnverifiedIncomingShares Use case for getting unverified incoming shares.
 * @property getUnverifiedOutgoingShares Use case for getting unverified outgoing shares.
 * @property requireTwoFactorAuthenticationUseCase Use case for requiring two factor authentication.
 * @property setCopyLatestTargetPathUseCase Use case for setting the latest target path for copy operations.
 * @property setMoveLatestTargetPathUseCase Use case for setting the latest target path for move operations.
 * @property monitorSecurityUpgradeInApp Use case for monitoring security upgrade in app.
 * @property monitorUserUpdates Use case for monitoring user updates.
 * @property establishCameraUploadsSyncHandlesUseCase Use case for establishing camera uploads sync handles.
 * @property startCameraUploadUseCase Use case for starting camera uploads.
 * @property stopCameraUploadsUseCase Use case for stopping camera uploads.
 * @property saveContactByEmailUseCase Use case for saving a contact by email.
 * @property createShareKeyUseCase Use case for creating a share key.
 * @property getNodeByIdUseCase Use case for getting a node by its ID.
 * @property deleteOldestCompletedTransfersUseCase Use case for deleting the oldest completed transfers.
 * @property getIncomingContactRequestsUseCase Use case for getting incoming contact requests.
 * @property monitorChatArchivedUseCase Use case for monitoring chat archived events.
 * @property restoreNodesUseCase Use case for restoring nodes.
 * @property checkNodesNameCollisionUseCase Use case for checking nodes name collision.
 * @property monitorBackupFolder Use case for monitoring backup folder.
 * @property moveNodesToRubbishUseCase Use case for moving nodes to rubbish.
 * @property deleteNodesUseCase Use case for deleting nodes.
 * @property moveNodesUseCase Use case for moving nodes.
 * @property copyNodesUseCase Use case for copying nodes.
 * @property renameRecoveryKeyFileUseCase Use case for renaming recovery key file.
 * @property removeShareUseCase Use case for removing shares.
 * @property removeShareResultMapper Mapper for mapping the result of remove share operation.
 * @property disableExportNodesUseCase Use case for disabling export nodes.
 * @property removePublicLinkResultMapper Mapper for mapping the result of remove public link operation.
 * @property dismissPsaUseCase Use case for dismissing PSA.
 * @property getRootNodeUseCase Use case for getting the root node.
 * @property getChatLinkContentUseCase Use case for getting the content of a chat link.
 * @property getScheduledMeetingByChatUseCase Use case for getting a scheduled meeting by chat.
 * @property getChatCallUseCase Use case for getting a chat call.
 * @property startMeetingInWaitingRoomChatUseCase Use case for starting a meeting in waiting room chat.
 * @property answerChatCallUseCase Use case for answering a chat call.
 * @property setChatVideoInDeviceUseCase Use case for setting chat video in device.
 * @property rtcAudioManagerGateway Gateway for RTC audio manager.
 * @property chatManagement Management for chat.
 * @property passcodeManagement Management for passcode.
 * @property monitorSyncStalledIssuesUseCase Use case for monitoring sync stalled issues.
 * @property monitorSyncsUseCase Use case for monitoring syncs.
 * @property monitorChatSessionUpdatesUseCase Use case for monitoring chat session updates.
 * @property hangChatCallUseCase Use case for hanging a chat call.
 * @property setUsersCallLimitRemindersUseCase      [SetUsersCallLimitRemindersUseCase]
 * @property getUsersCallLimitRemindersUseCase      [GetUsersCallLimitRemindersUseCase]
 */
@HiltViewModel
class ManagerViewModel @Inject constructor(
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    monitorContactUpdates: MonitorContactUpdates,
    private val monitorUserAlertUpdates: MonitorUserAlertUpdates,
    monitorContactRequestUpdatesUseCase: MonitorContactRequestUpdatesUseCase,
    private val getNumUnreadUserAlertsUseCase: GetNumUnreadUserAlertsUseCase,
    private val getNumUnreadPromoNotificationsUseCase: GetNumUnreadPromoNotificationsUseCase,
    private val sendStatisticsMediaDiscoveryUseCase: SendStatisticsMediaDiscoveryUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    monitorCameraUploadsFolderDestinationUseCase: MonitorCameraUploadsFolderDestinationUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val getExtendedAccountDetail: GetExtendedAccountDetail,
    private val getFullAccountInfoUseCase: GetFullAccountInfoUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getUnverifiedIncomingShares: GetUnverifiedIncomingShares,
    private val getUnverifiedOutgoingShares: GetUnverifiedOutgoingShares,
    monitorFinishActivityUseCase: MonitorFinishActivityUseCase,
    private val requireTwoFactorAuthenticationUseCase: RequireTwoFactorAuthenticationUseCase,
    private val setCopyLatestTargetPathUseCase: SetCopyLatestTargetPathUseCase,
    private val setMoveLatestTargetPathUseCase: SetMoveLatestTargetPathUseCase,
    private val monitorSecurityUpgradeInApp: MonitorSecurityUpgradeInApp,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase,
    private val startCameraUploadUseCase: StartCameraUploadUseCase,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val saveContactByEmailUseCase: SaveContactByEmailUseCase,
    private val createShareKeyUseCase: CreateShareKeyUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
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
    private val removeOfflineNodesUseCase: RemoveOfflineNodesUseCase,
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
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase,
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
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val setUsersCallLimitRemindersUseCase: SetUsersCallLimitRemindersUseCase,
    private val getUsersCallLimitRemindersUseCase: GetUsersCallLimitRemindersUseCase,
    private val broadcastHomeBadgeCountUseCase: BroadcastHomeBadgeCountUseCase,
    private val monitorUpgradeDialogClosedUseCase: MonitorUpgradeDialogClosedUseCase,
    private val monitorDevicePowerConnectionStateUseCase: MonitorDevicePowerConnectionStateUseCase,
    private val startOfflineSyncWorkerUseCase: StartOfflineSyncWorkerUseCase,
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

    private val _numUnreadUserAlerts = MutableStateFlow(
        Pair(
            UnreadUserAlertsCheckType.NOTIFICATIONS_TITLE,
            0
        )
    )

    private val legacyNumUnreadUserAlerts = SingleLiveEvent<Pair<UnreadUserAlertsCheckType, Int>>()

    /**
     * The number of unread user alerts
     */
    val numUnreadUserAlerts = _numUnreadUserAlerts.asStateFlow()

    private var monitorCallInChatJob: Job? = null

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    private val isFirstLogin = savedStateHandle.getStateFlow(
        key = IS_FIRST_LOGIN_KEY,
        initialValue = false
    )

    init {
        checkUsersCallLimitReminders()
        getApiFeatureFlag()

        viewModelScope.launch {
            val order = getCloudSortOrder()
            combine(
                isFirstLogin,
                flowOf(getUnverifiedIncomingShares(order) + getUnverifiedOutgoingShares(order))
                    .map { it.size },
            ) { firstLogin: Boolean, pendingShares: Int ->
                { state: ManagerState ->
                    state.copy(
                        isFirstLogin = firstLogin,
                        pendingActionsCount = state.pendingActionsCount + pendingShares,
                    )
                }
            }.collectLatest {
                _state.update(it)
            }
        }

        viewModelScope.launch {
            monitorNodeUpdatesUseCase().collect {
                onReceiveNodeUpdate(true)
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

        monitorCallInChatJob?.cancel()
        monitorCallInChatJob = viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .collect {
                    it.apply {
                        when (status) {
                            ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.GenericNotification ->
                                if (termCode == ChatCallTermCodeType.CallUsersLimit
                                    && _state.value.isCallUnlimitedProPlanFeatureFlagEnabled
                                ) {
                                    setUsersCallLimitReminder(true)
                                    _state.update { state -> state.copy(callEndedDueToFreePlanLimits = true) }
                                } else if (termCode == ChatCallTermCodeType.CallDurationLimit && _state.value.isCallUnlimitedProPlanFeatureFlagEnabled) {
                                    if (it.isOwnClientCaller) {
                                        _state.update { state ->
                                            state.copy(
                                                shouldUpgradeToProPlan = true
                                            )
                                        }
                                    }
                                } else if (termCode == ChatCallTermCodeType.TooManyParticipants) {
                                    _state.update { state ->
                                        state.copy(
                                            message = InfoToShow.SimpleString(
                                                R.string.call_error_too_many_participants
                                            )
                                        )
                                    }
                                }

                            else -> {}
                        }
                    }
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
            monitorContactRequestUpdatesUseCase()
                .collect {
                    Timber.d("Contact Request Updates")
                    updateIncomingContactRequests()
                    updateContactRequests(it)
                }
        }

        viewModelScope.launch {
            monitorUserAlertUpdates()
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
            monitorSyncsUseCase().catch { Timber.e(it) }.collect { syncFolders ->
                val isServiceEnabled = syncFolders.isNotEmpty()
                _state.update {
                    it.copy(
                        androidSyncServiceEnabled = isServiceEnabled,
                    )
                }
            }
        }

        viewModelScope.launch {
            monitorMyAccountUpdateEvent
                .filter {
                    it.storageState == StorageState.Green || it.storageState == StorageState.Orange
                }
                .collect {
                    startCameraUploads()
                }
        }

        viewModelScope.launch {
            monitorUpgradeDialogClosedUseCase().catch {
                Timber.e(it)
            }.collect {
                _state.update { it.copy(shouldUpgradeToProPlan = false) }
            }
        }

        viewModelScope.launch {
            monitorDevicePowerConnectionStateUseCase().catch {
                Timber.e(
                    "An error occurred while monitoring the Device Power Connection State",
                    it,
                )
            }.collect { state ->
                Timber.d("The Device Power Connection State is $state")
                if (state == DevicePowerConnectionState.Connected) {
                    startCameraUploadUseCase()
                }
            }
        }
    }

    /**
     * Start offline sync worker
     * It syncs offline local files with database entries
     */
    fun startOfflineSyncWorker() {
        viewModelScope.launch {
            startOfflineSyncWorkerUseCase()
        }
    }

    /**
     * Get call unlimited pro plan api feature flag
     */
    private fun getApiFeatureFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.CallUnlimitedProPlan)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { flag ->
                _state.update { state ->
                    state.copy(
                        isCallUnlimitedProPlanFeatureFlagEnabled = flag,
                    )
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

    /**
     * Notifies about the number of unread user alerts once.
     *
     * @return [SingleLiveEvent] with the number of unread user alerts.
     */
    fun onGetNumUnreadUserAlerts(): SingleLiveEvent<Pair<UnreadUserAlertsCheckType, Int>> =
        legacyNumUnreadUserAlerts

    /**
     * Checks the number of unread user alerts.
     */
    fun checkNumUnreadUserAlerts(type: UnreadUserAlertsCheckType) {
        viewModelScope.launch {
            runCatching {
                val promoNotificationCount =
                    if (getFeatureFlagValueUseCase(AppFeatures.PromoNotifications))
                        getNumUnreadPromoNotificationsUseCase()
                    else
                        0
                val totalCount =
                    getNumUnreadUserAlertsUseCase() + promoNotificationCount
                _numUnreadUserAlerts.update { Pair(type, totalCount) }
                legacyNumUnreadUserAlerts.value = Pair(type, totalCount)
                val incomingContactRequests = incomingContactRequests.value.size
                broadcastHomeBadgeCountUseCase(totalCount + incomingContactRequests)
            }.onFailure {
                Timber.e("Failed to get the number of unread user alerts or promo notifications with error: $it")
            }
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
        savedStateHandle[IS_FIRST_LOGIN_KEY] = newIsFirstLogin
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
     * Consume show free plan participants limit dialog event
     *
     */
    fun onConsumeShowFreePlanParticipantsLimitDialogEvent() {
        setUsersCallLimitReminder(enabled = false)
        _state.update { state -> state.copy(callEndedDueToFreePlanLimits = false) }
    }

    /**
     * Enable or disable users call limit reminder
     */
    fun setUsersCallLimitReminder(enabled: Boolean) = viewModelScope.launch {
        runCatching {
            setUsersCallLimitRemindersUseCase(if (enabled) UsersCallLimitReminders.Enabled else UsersCallLimitReminders.Disabled)
        }.onFailure { exception ->
            Timber.e("An error occurred when setting the call limit reminder", exception)
        }
    }

    /**
     * Consume ShouldUpgradeToProPlan
     *
     */
    fun onConsumeShouldUpgradeToProPlan() {
        _state.update { state -> state.copy(shouldUpgradeToProPlan = false) }
    }

    /**
     * Start camera uploads
     */
    private fun startCameraUploads() = viewModelScope.launch {
        startCameraUploadUseCase()
    }

    /**
     * Stop and disable camera uploads
     */
    fun stopAndDisableCameraUploads() = viewModelScope.launch {
        runCatching {
            stopCameraUploadsUseCase(restartMode = CameraUploadsRestartMode.StopAndDisable)
        }.onFailure { Timber.d(it) }
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
        require(node != null) { "Cannot create a share key for a null node" }
        val typedNode = getNodeByIdUseCase(NodeId(node.handle))
        require(typedNode is FolderNode) { "Cannot create a share key for a non-folder node" }
        createShareKeyUseCase(typedNode)
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
                _state.update { it.copy(nodeNameCollisionsResult = result) }
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
                _state.update { it.copy(nodeNameCollisionsResult = result) }
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
        _state.update { it.copy(nodeNameCollisionsResult = null) }
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
     * Remove offline nodes
     *
     * @param nodeHandles
     */
    fun removeOfflineNodes(nodeHandles: List<Long>) {
        viewModelScope.launch {
            runCatching {
                removeOfflineNodesUseCase(nodeHandles.map { NodeId(it) })
            }.onFailure {
                Timber.e(it)
            }
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
                        message = InfoToShow.RawString(message),
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
                        message = InfoToShow.RawString(message),
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
        nodeSourceType: NodeSourceType,
    ): Long = when (nodeSourceType) {
        NodeSourceType.CLOUD_DRIVE -> browserParentHandle
        NodeSourceType.INCOMING_SHARES -> incomingParentHandle
        NodeSourceType.OUTGOING_SHARES -> outgoingParentHandle
        NodeSourceType.LINKS -> linksParentHandle
        NodeSourceType.RUBBISH_BIN -> rubbishBinParentHandle
        NodeSourceType.BACKUPS -> backupsParentHandle
        NodeSourceType.HOME, NodeSourceType.OTHER -> getRootNodeUseCase()?.id?.longValue
            ?: MegaApiJava.INVALID_HANDLE
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
                        getScheduledMeetingByChatUseCase(chatId)
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
        )
    }

    /**
     * When navigating to Device Center, this holds the previous Bottom Navigation item, so that the
     * User can go back to it after leaving Device Center
     *
     * @param previousItem The previous Bottom Navigation Item, which can be null to signify that
     * it is not set up
     */
    fun setDeviceCenterPreviousBottomNavigationItem(previousItem: Int?) {
        _state.update { it.copy(deviceCenterPreviousBottomNavigationItem = previousItem) }
    }

    /**
     * Check users call limit reminders
     */
    private fun checkUsersCallLimitReminders() {
        viewModelScope.launch {
            getUsersCallLimitRemindersUseCase().collectLatest { result ->
                _state.update { it.copy(usersCallLimitReminders = result) }
            }
        }
    }

    /**
     * Update search query
     *
     * @param query Search query
     */
    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }


    /**
     * Uploads a file to the specified destination.
     *
     * @param file The file to upload.
     * @param destination The destination where the file will be uploaded.
     */
    fun uploadFile(
        file: File,
        destination: Long,
    ) {
        uploadFiles(
            mapOf(file.absolutePath to null),
            NodeId(destination)
        )
    }

    /**
     * Uploads a list of files to the specified destination.
     *
     * @param shareInfo The files as [ShareInfo] to upload.
     * @param destination The destination where the files will be uploaded.
     */
    fun uploadShareInfo(
        shareInfo: List<ShareInfo>,
        destination: Long,
    ) {
        val pathsAndNames = shareInfo.map { it.fileAbsolutePath }.associateWith { null }

        uploadFiles(pathsAndNames, NodeId(destination))
    }

    private fun uploadFiles(
        pathsAndNames: Map<String, String?>,
        destinationId: NodeId,
    ) {
        _state.update { state ->
            state.copy(
                uploadEvent = triggered(
                    TransferTriggerEvent.StartUpload.Files(
                        pathsAndNames = pathsAndNames,
                        destinationId = destinationId,
                    )
                )
            )
        }
    }

    /**
     * Consume upload event
     */
    fun consumeUploadEvent() {
        _state.update { it.copy(uploadEvent = consumed()) }
    }

    internal companion object {
        internal const val IS_FIRST_LOGIN_KEY = "EXTRA_FIRST_LOGIN"
        private const val INVALID_HANDLE = -1L
    }
}
