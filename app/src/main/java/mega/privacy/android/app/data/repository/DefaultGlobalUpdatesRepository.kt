package mega.privacy.android.app.data.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.repository.GlobalUpdatesRepository
import javax.inject.Inject

class DefaultGlobalUpdatesRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
) : GlobalUpdatesRepository {

    override fun monitorGlobalUpdates(): Flow<GlobalUpdate> = megaApiGateway.globalUpdates
}