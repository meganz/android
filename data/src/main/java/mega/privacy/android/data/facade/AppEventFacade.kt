package mega.privacy.android.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.qualifier.ApplicationScope
import javax.inject.Inject

/**
 * Default implementation of [AppEventGateway]
 */
internal class AppEventFacade @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
) : AppEventGateway {

    private val _monitorCameraUploadProgress = MutableSharedFlow<Pair<Int, Int>>()
    private val cameraUploadsFolderDestinationUpdate =
        MutableSharedFlow<CameraUploadsFolderDestinationUpdate>()
    private val _transferOverQuota = MutableSharedFlow<Boolean>()
    private val _storageOverQuota = MutableSharedFlow<Boolean>()
    private val _fileAvailableOffline = MutableSharedFlow<Long>()
    private val logout = MutableSharedFlow<Boolean>()
    private val fetchNodesFinish = MutableSharedFlow<Boolean>()
    private val _transferFailed = MutableSharedFlow<Boolean>()
    private val transfersFinished = MutableSharedFlow<TransfersFinishedState>()
    private val pushNotificationSettingsUpdate = MutableSharedFlow<Boolean>()
    private val myAccountUpdate = MutableSharedFlow<MyAccountUpdate>()
    private val chatArchived = MutableSharedFlow<String>()
    private val homeBadgeCount = MutableSharedFlow<Int>()
    private val isJoinedSuccessfullyToChat = MutableSharedFlow<Boolean>()
    private val leaveChat = MutableSharedFlow<Long>()
    private val stopTransfersWork = MutableSharedFlow<Boolean>()
    private val chatSignalPresence = MutableSharedFlow<Unit>()
    private val accountBlocked = MutableSharedFlow<AccountBlockedDetail>()
    private val scheduledMeetingCanceled = MutableSharedFlow<Int>()

    private val _isSMSVerificationShownState = MutableStateFlow(false)
    private val _finishActivity = MutableSharedFlow<Boolean>()

    private val updateUpgradeSecurityState = MutableStateFlow(false)
    private val _monitorCompletedTransfer = MutableSharedFlow<CompletedTransfer>()
    private val _monitorRefreshSession = MutableSharedFlow<Unit>()
    private val _monitorBackupInfoType = MutableSharedFlow<BackupInfoType>()

    private val _monitorCameraUploadsSettingsActions =
        MutableSharedFlow<CameraUploadsSettingsAction>()
    private val _businessAccountExpired = MutableSharedFlow<Unit>()

    override val monitorCameraUploadProgress =
        _monitorCameraUploadProgress.toSharedFlow(appScope)

    override val monitorCompletedTransfer =
        _monitorCompletedTransfer.toSharedFlow(appScope)

    override suspend fun broadcastCameraUploadProgress(progress: Int, pending: Int) {
        _monitorCameraUploadProgress.emit(Pair(progress, pending))
    }

    override suspend fun setSMSVerificationShown(isShown: Boolean) {
        _isSMSVerificationShownState.value = isShown
    }

    override suspend fun isSMSVerificationShown(): Boolean = _isSMSVerificationShownState.value

    override fun monitorOfflineFileAvailability(): Flow<Long> =
        _fileAvailableOffline.asSharedFlow()

    override suspend fun broadcastOfflineFileAvailability(nodeHandle: Long) {
        _fileAvailableOffline.emit(nodeHandle)
    }

    override fun monitorTransferOverQuota(): Flow<Boolean> = _transferOverQuota.asSharedFlow()

    override suspend fun broadcastTransferOverQuota(isCurrentOverQuota: Boolean) {
        _transferOverQuota.emit(isCurrentOverQuota)
    }

    override fun monitorStorageOverQuota(): Flow<Boolean> = _storageOverQuota.asSharedFlow()

    override suspend fun broadcastStorageOverQuota() {
        _storageOverQuota.emit(true)
    }

    override fun monitorLogout(): Flow<Boolean> = logout.asSharedFlow()

    override suspend fun broadcastLogout() = logout.emit(true)

    override fun monitorFailedTransfer(): Flow<Boolean> = _transferFailed.asSharedFlow()

    override fun monitorSecurityUpgrade(): Flow<Boolean> =
        updateUpgradeSecurityState.asStateFlow()

    override suspend fun setUpgradeSecurity(isSecurityUpgrade: Boolean) {
        updateUpgradeSecurityState.value = isSecurityUpgrade
    }

    override suspend fun broadcastFailedTransfer(isFailed: Boolean) {
        _transferFailed.emit(isFailed)
    }

    override fun monitorFinishActivity() = _finishActivity.toSharedFlow(appScope)

    override suspend fun broadcastFinishActivity() = _finishActivity.emit(true)

    override fun monitorFetchNodesFinish() = fetchNodesFinish.asSharedFlow()

    override suspend fun broadcastFetchNodesFinish() = fetchNodesFinish.emit(true)

    override suspend fun broadcastPushNotificationSettings() =
        pushNotificationSettingsUpdate.emit(true)

    override fun monitorPushNotificationSettings() =
        pushNotificationSettingsUpdate.toSharedFlow(appScope)

    override suspend fun broadcastCompletedTransfer(transfer: CompletedTransfer) =
        _monitorCompletedTransfer.emit(transfer)

    override fun monitorMyAccountUpdate(): Flow<MyAccountUpdate> =
        myAccountUpdate.toSharedFlow(appScope)

    override suspend fun broadcastMyAccountUpdate(data: MyAccountUpdate) =
        myAccountUpdate.emit(data)

    override fun monitorTransfersFinished(): Flow<TransfersFinishedState> =
        transfersFinished.toSharedFlow(appScope)

    override suspend fun broadcastTransfersFinished(transfersFinishedState: TransfersFinishedState) =
        transfersFinished.emit(transfersFinishedState)

    override fun monitorCameraUploadsFolderDestination(): Flow<CameraUploadsFolderDestinationUpdate> =
        cameraUploadsFolderDestinationUpdate.toSharedFlow(appScope)

    override suspend fun broadcastCameraUploadsFolderDestination(data: CameraUploadsFolderDestinationUpdate) =
        cameraUploadsFolderDestinationUpdate.emit(data)

    override fun monitorChatArchived(): Flow<String> = chatArchived.toSharedFlow(appScope)

    override suspend fun broadcastChatArchived(chatTitle: String) = chatArchived.emit(chatTitle)

    override fun monitorHomeBadgeCount() =
        homeBadgeCount.shareIn(appScope, started = SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun broadcastHomeBadgeCount(badgeCount: Int) =
        homeBadgeCount.emit(badgeCount)

    override fun monitorJoinedSuccessfully(): Flow<Boolean> =
        isJoinedSuccessfullyToChat.toSharedFlow(appScope)

    override suspend fun broadcastJoinedSuccessfully() =
        isJoinedSuccessfullyToChat.emit(true)

    override fun monitorLeaveChat(): Flow<Long> = leaveChat.toSharedFlow(appScope)

    override suspend fun broadcastLeaveChat(chatId: Long) = leaveChat.emit(chatId)

    override fun monitorStopTransfersWork() = stopTransfersWork.toSharedFlow(appScope)

    override suspend fun broadcastStopTransfersWork() = stopTransfersWork.emit(true)

    override suspend fun broadcastRefreshSession() = _monitorRefreshSession.emit(Unit)

    override fun monitorRefreshSession() = _monitorRefreshSession.asSharedFlow()

    override suspend fun broadcastChatSignalPresence() = chatSignalPresence.emit(Unit)

    override fun monitorChatSignalPresence(): Flow<Unit> = chatSignalPresence.asSharedFlow()

    override suspend fun broadcastAccountBlocked(accountBlockedDetail: AccountBlockedDetail) =
        accountBlocked.emit(accountBlockedDetail)

    override fun monitorAccountBlocked() = accountBlocked.asSharedFlow()
    override fun monitorScheduledMeetingCanceled(): Flow<Int> =
        scheduledMeetingCanceled.toSharedFlow(appScope)

    override suspend fun broadcastScheduledMeetingCanceled(messageResId: Int) =
        scheduledMeetingCanceled.emit(messageResId)

    override fun monitorCameraUploadsSettingsActions() =
        _monitorCameraUploadsSettingsActions.asSharedFlow()

    override suspend fun broadCastCameraUploadSettingsActions(action: CameraUploadsSettingsAction) {
        _monitorCameraUploadsSettingsActions.emit(action)
    }

    override fun monitorBackupInfoType() = _monitorBackupInfoType.asSharedFlow()

    override suspend fun broadCastBackupInfoType(backupInfoType: BackupInfoType) {
        _monitorBackupInfoType.emit(backupInfoType)
    }

    override suspend fun broadcastBusinessAccountExpired() {
        _businessAccountExpired.emit(Unit)
    }

    override fun monitorBusinessAccountExpired() = _businessAccountExpired.asSharedFlow()
}

private fun <T> Flow<T>.toSharedFlow(
    scope: CoroutineScope,
) = shareIn(scope, started = SharingStarted.WhileSubscribed())
