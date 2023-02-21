package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow

internal interface AppEventGateway {

    /**
     * monitor upload service pause State
     */
    val monitorCameraUploadPauseState: Flow<Boolean>

    /**
     * Broadcast upload pause state
     */
    suspend fun broadcastUploadPauseState()

    /**
     * Set the status for SMSVerification
     */
    suspend fun setSMSVerificationShown(isShown: Boolean)

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
