package mega.privacy.android.feature.sync.data.gateway

import dagger.Lazy
import mega.privacy.android.data.database.dao.UserPausedSyncsDao
import mega.privacy.android.data.database.entity.UserPausedSyncEntity
import javax.inject.Inject

internal class UserPausedSyncGatewayImpl @Inject constructor(
    private val userPausedSyncDao: Lazy<UserPausedSyncsDao>,
) : UserPausedSyncGateway {

    override suspend fun setUserPausedSync(syncId: Long) {
        userPausedSyncDao.get().insertPausedSync(UserPausedSyncEntity(syncId))
    }

    override suspend fun getUserPausedSync(syncId: Long): UserPausedSyncEntity? =
        userPausedSyncDao.get().getUserPausedSync(syncId)

    override suspend fun deleteUserPausedSync(syncId: Long) {
        userPausedSyncDao.get().deleteUserPausedSync(syncId)
    }
}