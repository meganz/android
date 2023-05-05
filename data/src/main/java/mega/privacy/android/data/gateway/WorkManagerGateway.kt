package mega.privacy.android.data.gateway

/**
 *  Functions for starting work requests
 */
interface WorkManagerGateway {

    /**
     *  Enqueue unique work request to delete oldest completed transfers from local storage
     */
    suspend fun enqueueDeleteOldestCompletedTransfersWorkRequest()
}
