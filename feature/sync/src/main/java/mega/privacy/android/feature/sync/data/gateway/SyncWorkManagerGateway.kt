package mega.privacy.android.feature.sync.data.gateway

/**
 * Gateway for interacting with the [mega.privacy.android.feature.sync.data.SyncWorker]
 *
 */
internal interface SyncWorkManagerGateway {

    suspend fun enqueueSyncWorkerRequest()

    suspend fun cancelSyncWorkerRequest()
}