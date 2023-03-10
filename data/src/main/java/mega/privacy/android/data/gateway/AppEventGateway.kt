package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow

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
}
