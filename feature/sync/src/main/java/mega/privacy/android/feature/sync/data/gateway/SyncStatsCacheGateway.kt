package mega.privacy.android.feature.sync.data.gateway

import nz.mega.sdk.MegaSyncStats

internal interface SyncStatsCacheGateway {

    fun getSyncStatsById(syncId: Long): MegaSyncStats?

    fun setSyncStats(syncStats: MegaSyncStats)
}