package mega.privacy.android.data.gateway

/**
 *  Functions for starting work requests
 */
interface WorkManagerGateway {

    /**
     *  Enqueue unique work request to delete oldest completed transfers from local storage
     */
    suspend fun enqueueDeleteOldestCompletedTransfersWorkRequest()

    /**
     * Enqueue unique work request to start download worker to monitor the download transfers as a foreground service
     */
    fun enqueueDownloadsWorkerRequest()
}
