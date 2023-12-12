package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.gateway.SyncPreferencesDatastore
import mega.privacy.android.feature.sync.data.gateway.UserPausedSyncGateway
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

internal class SyncPreferencesRepositoryImpl @Inject constructor(
    private val syncPreferencesDatastore: SyncPreferencesDatastore,
    private val userPausedSyncGateway: UserPausedSyncGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncPreferencesRepository {

    override suspend fun setSyncByWiFi(checked: Boolean) {
        syncPreferencesDatastore.setSyncOnlyByWiFi(checked)
    }

    override fun monitorSyncByWiFi(): Flow<Boolean?> =
        syncPreferencesDatastore.monitorSyncOnlyByWiFi()

    override suspend fun setOnboardingShown(shown: Boolean) {
        syncPreferencesDatastore.setOnboardingShown(shown)
    }

    override suspend fun getOnboardingShown(): Boolean? =
        syncPreferencesDatastore.getOnboardingShown()

    override suspend fun setUserPausedSync(syncId: Long) {
        withContext(ioDispatcher) {
            userPausedSyncGateway.setUserPausedSync(syncId)
        }
    }

    override suspend fun deleteUserPausedSync(syncId: Long) {
        withContext(ioDispatcher) {
            userPausedSyncGateway.deleteUserPausedSync(syncId)
        }
    }

    override suspend fun isSyncPausedByTheUser(syncId: Long): Boolean =
        withContext(ioDispatcher) {
            userPausedSyncGateway
                .getUserPausedSync(syncId) != null
        }
}
