package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState

internal interface AppEventGateway {

    /**
     * Monitor camera upload progress
     *
     * The value returned is a Pair of
     *
     * [Int] value representing progress between 0 and 100;
     * [Int] value representing pending elements waiting for upload
     */
    val monitorCameraUploadProgress: Flow<Pair<Int, Int>>

    /**
     * Monitor completed transfer
     */
    val monitorCompletedTransfer: Flow<CompletedTransfer>

    /**
     * Broadcast camera upload progress
     *
     * @param progress represents progress between 0 and 100
     * @param pending represents elements waiting for upload
     */
    suspend fun broadcastCameraUploadProgress(progress: Int, pending: Int)


    /**
     * Set the status for SMSVerification
     */
    suspend fun setSMSVerificationShown(isShown: Boolean)

    /**
     * Set the status for account security upgrade
     */
    suspend fun setUpgradeSecurity(isSecurityUpgrade: Boolean)

    /**
     * check whether SMS Verification Shown or not
     */
    suspend fun isSMSVerificationShown(): Boolean

    /**
     * Monitor the offline availability of the file
     */
    fun monitorOfflineFileAvailability(): Flow<Long>

    /**
     * Broadcast the offline availability of the file
     * @param nodeHandle the handle of the node
     */
    suspend fun broadcastOfflineFileAvailability(nodeHandle: Long)

    /**
     * Monitor transfer over quota
     */
    fun monitorTransferOverQuota(): Flow<Boolean>

    /**
     * Monitor storage over quota
     */
    fun monitorStorageOverQuota(): Flow<Boolean>

    /**
     * Broadcast transfer over quota
     * @param isCurrentOverQuota true if the overquota is currently received, false otherwise
     *
     */
    suspend fun broadcastTransferOverQuota(isCurrentOverQuota: Boolean)

    /**
     * Broadcast storage over quota
     *
     */
    suspend fun broadcastStorageOverQuota()

    /**
     * Monitors logout.
     */
    fun monitorLogout(): Flow<Boolean>

    /**
     * Broadcast logout.
     */
    suspend fun broadcastLogout()

    /**
     * Monitor transfer failed
     *
     */
    fun monitorFailedTransfer(): Flow<Boolean>

    /**
     * Monitor transfer failed
     *
     */
    fun monitorSecurityUpgrade(): Flow<Boolean>

    /**
     * Broadcast transfer failed
     *
     */
    suspend fun broadcastFailedTransfer(isFailed: Boolean)

    /**
     * Monitor Finish Activity
     */
    fun monitorFinishActivity(): Flow<Boolean>

    /**
     * Broadcast Finish Activity
     */
    suspend fun broadcastFinishActivity()

    /**
     * Monitors fetch nodes.
     */
    fun monitorFetchNodesFinish(): Flow<Boolean>

    /**
     * Broadcast fetch nodes.
     */
    suspend fun broadcastFetchNodesFinish()

    /**
     * Monitor camera upload folder destination update
     *
     * @return Flow of [CameraUploadsFolderDestinationUpdate]
     */
    fun monitorCameraUploadsFolderDestination(): Flow<CameraUploadsFolderDestinationUpdate>

    /**
     * Broadcast camera upload folder icon update
     *
     * @param data [CameraUploadsFolderDestinationUpdate]
     */
    suspend fun broadcastCameraUploadsFolderDestination(data: CameraUploadsFolderDestinationUpdate)

    /**
     * Broadcast push notification settings
     */
    suspend fun broadcastPushNotificationSettings()

    /**
     * Monitor push notification settings
     */
    fun monitorPushNotificationSettings(): Flow<Boolean>

    /**
     * Broadcast completed transfer
     *
     * @param transfer the completed transfer to be broadcast
     */
    suspend fun broadcastCompletedTransfer(transfer: CompletedTransfer)

    /**
     * Monitor account update.
     *
     * @return Flow of [MyAccountUpdate]
     */
    fun monitorMyAccountUpdate(): Flow<MyAccountUpdate>

    /**
     * Broadcast account update.
     *
     * @param data [MyAccountUpdate]
     */
    suspend fun broadcastMyAccountUpdate(data: MyAccountUpdate)

    /**
     * Monitor transfers finished.
     *
     * @return Flow of [TransfersFinishedState]
     */
    fun monitorTransfersFinished(): Flow<TransfersFinishedState>

    /**
     * Broadcast transfers finished.
     *
     * @param transfersFinishedState [TransfersFinishedState]
     */
    suspend fun broadcastTransfersFinished(transfersFinishedState: TransfersFinishedState)

    /**
     * Monitor chat archived.
     *
     * @return Flow [String]
     */
    fun monitorChatArchived(): Flow<String>

    /**
     * Broadcast chat archived.
     *
     * @param chatTitle [String]
     */
    suspend fun broadcastChatArchived(chatTitle: String)

    /**
     * Monitor home badge count.
     *
     * @return Flow of the number of pending actions the current logged in account has.
     */
    fun monitorHomeBadgeCount(): Flow<Int>

    /**
     * Broadcast home badge count.
     *
     * @param badgeCount Number of pending actions the current logged in account has.
     */
    suspend fun broadcastHomeBadgeCount(badgeCount: Int)

    /**
     * Monitor if successfully joined to a chat.
     *
     * @return Flow [Boolean]
     */
    fun monitorJoinedSuccessfully(): Flow<Boolean>

    /**
     * Broadcast if successfully joined to a chat.
     */
    suspend fun broadcastJoinedSuccessfully()

    /**
     * Monitor if should leave a chat.
     *
     * @return Flow [Long] ID of the chat to leave.
     */
    fun monitorLeaveChat(): Flow<Long>

    /**
     * Broadcast that should leave a chat.
     *
     * @param chatId [Long] ID of the chat to leave.
     */
    suspend fun broadcastLeaveChat(chatId: Long)

    /**
     * Monitors when transfers management have to stop.
     *
     * @return Flow [Boolean]
     */
    fun monitorStopTransfersWork(): Flow<Boolean>

    /**
     * Broadcasts if transfers management have to stop.
     */
    suspend fun broadcastStopTransfersWork()

    /**
     * Broadcast refresh session
     *
     */
    suspend fun broadcastRefreshSession()

    /**
     * Monitor refresh session
     *
     * @return
     */
    fun monitorRefreshSession(): Flow<Unit>

    /**
     * Monitor chat signal presence, monitor if any signal is available
     */
    fun monitorChatSignalPresence(): Flow<Unit>

    /**
     * Broadcast chat signal presence if network signal is available
     */
    suspend fun broadcastChatSignalPresence()

    /**
     * Broadcast blocked account.
     *
     * @param accountBlockedDetail [AccountBlockedDetail]
     */
    suspend fun broadcastAccountBlocked(accountBlockedDetail: AccountBlockedDetail)

    /**
     * Monitors blocked account.
     */
    fun monitorAccountBlocked(): Flow<AccountBlockedDetail>

    /**
     * Monitor scheduled meeting canceled.
     *
     * @return Flow [Int]
     */
    fun monitorScheduledMeetingCanceled(): Flow<Int>

    /**
     * Broadcast scheduled meeting canceled.
     *
     * @param messageResId [Int]
     */
    suspend fun broadcastScheduledMeetingCanceled(messageResId: Int)

    /**
     * Monitor CameraUploadSettingsAction.
     *
     * @return Flow [CameraUploadsSettingsAction]
     */
    fun monitorCameraUploadsSettingsActions(): Flow<CameraUploadsSettingsAction>

    /**
     * Broadcast CameraUploadSettingsAction.
     *
     * @param action [CameraUploadsSettingsAction]
     */
    suspend fun broadCastCameraUploadSettingsActions(action: CameraUploadsSettingsAction)


    /**
     * Monitor BackupInfoType.
     *
     * @return Flow [BackupInfoType]
     */
    fun monitorBackupInfoType(): Flow<BackupInfoType>

    /**
     * Broadcast BackupInfoType.
     *
     * @param backupInfoType [BackupInfoType]
     */
    suspend fun broadCastBackupInfoType(backupInfoType: BackupInfoType)

    /**
     * Broadcast business account expired
     */
    suspend fun broadcastBusinessAccountExpired()

    /**
     * Monitor business account expired events
     *
     * @return a flow that emits each time a new business account expired error is received
     */
    fun monitorBusinessAccountExpired(): Flow<Unit>

    /**
     * Broadcast call recording consent event (accepted/rejected)
     *
     * @param isRecordingConsentAccepted True if recording consent has been accepted or False otherwise.
     */
    suspend fun broadcastCallRecordingConsentEvent(isRecordingConsentAccepted: Boolean)

    /**
     * Monitor call recording consent event (accepted/rejected)
     *
     * @return Flow of Boolean. True if consent has been accepted or False otherwise.
     */
    fun monitorCallRecordingConsentEvent(): Flow<Boolean>
}
