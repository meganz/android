package mega.privacy.android.feature.sync.domain.repository

import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.SyncDebris

internal interface SyncDebrisRepository {

    suspend fun clear()

    suspend fun getSyncDebrisForSyncs(syncs: List<FolderPair>): List<SyncDebris>
}