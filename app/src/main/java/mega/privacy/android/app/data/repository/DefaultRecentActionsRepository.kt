package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * Default implementation of [RecentActionsRepository]
 */
class DefaultRecentActionsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecentActionsRepository {

    override suspend fun getRecentActions(): List<MegaRecentActionBucket> =
        withContext(ioDispatcher) { megaApiGateway.getRecentActions() }

}