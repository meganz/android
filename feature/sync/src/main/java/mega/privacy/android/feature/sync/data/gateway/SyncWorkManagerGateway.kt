package mega.privacy.android.feature.sync.data.gateway

import androidx.work.NetworkType

/**
 * Gateway for interacting with the [mega.privacy.android.feature.sync.data.SyncWorker]
 *
 */
internal interface SyncWorkManagerGateway {

    suspend fun enqueueSyncWorkerRequest(
        frequencyInMinutes: Int,
        networkType: NetworkType,
    )

    suspend fun cancelSyncWorkerRequest()
}