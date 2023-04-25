package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.transfer.CompletedTransfer

internal interface AppEventGateway {

    /**
     * monitor upload service pause State
     */
    val monitorCameraUploadPauseState: Flow<Boolean>

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
     * Broadcast upload pause state
     */
    suspend fun broadcastUploadPauseState()

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
     * Monitor transfer over quota
     */
    fun monitorTransferOverQuota(): Flow<Boolean>

    /**
     * Broadcast transfer over quota
     *
     */
    suspend fun broadcastTransferOverQuota()

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
     * Monitors account update.
     */
    fun monitorAccountUpdate(): Flow<Boolean>

    /**
     * Broadcast account update.
     */
    suspend fun broadcastAccountUpdate()

    /**
     * Monitors paused transfers.
     */
    fun monitorPausedTransfers(): Flow<Boolean>

    /**
     * Broadcast paused transfers.
     */
    suspend fun broadcastPausedTransfers()

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

    fun monitorMyAccountUpdate(): Flow<MyAccountUpdate>

    suspend fun broadcastMyAccountUpdate(data: MyAccountUpdate)
}
