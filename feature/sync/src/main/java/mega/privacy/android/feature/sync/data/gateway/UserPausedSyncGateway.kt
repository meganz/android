package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.UserPausedSyncEntity

internal interface UserPausedSyncGateway {

    suspend fun setUserPausedSync(syncId: Long)

    suspend fun getUserPausedSync(syncId: Long): UserPausedSyncEntity?

    suspend fun deleteUserPausedSync(syncId: Long)

    fun monitorUserPausedSyncIds(): Flow<Set<Long>>
}