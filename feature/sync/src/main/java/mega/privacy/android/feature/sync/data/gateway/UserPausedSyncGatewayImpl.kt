package mega.privacy.android.feature.sync.data.gateway

import mega.privacy.android.data.database.dao.UserPausedSyncsDao
import mega.privacy.android.data.database.entity.UserPausedSyncEntity
import javax.inject.Inject

internal class UserPausedSyncGatewayImpl @Inject constructor(
    private val userPausedSyncDao: UserPausedSyncsDao,
) : UserPausedSyncGateway {

    override suspend fun setUserPausedSync(syncId: Long) {
        userPausedSyncDao.insertPausedSync(UserPausedSyncEntity(syncId))
    }

    override suspend fun getUserPausedSync(syncId: Long): UserPausedSyncEntity? =
        userPausedSyncDao.getUserPausedSync(syncId)

    override suspend fun deleteUserPausedSync(syncId: Long) {
        userPausedSyncDao.deleteUserPausedSync(syncId)
    }
}