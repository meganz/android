package mega.privacy.android.feature.sync.data.gateway

import nz.mega.sdk.MegaSyncStats
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class SyncStatsCacheGatewayImpl @Inject constructor() : SyncStatsCacheGateway {

    private val syncStatsInMemoryCache = ConcurrentHashMap<Long, MegaSyncStats>()

    override fun getSyncStatsById(syncId: Long): MegaSyncStats? =
        syncStatsInMemoryCache[syncId]

    override fun setSyncStats(syncStats: MegaSyncStats) {
        syncStatsInMemoryCache[syncStats.backupId] = syncStats
    }
}