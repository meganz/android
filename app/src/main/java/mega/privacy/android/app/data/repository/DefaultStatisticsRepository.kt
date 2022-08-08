package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Default [StatisticsRepository] implementation
 */
class DefaultStatisticsRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
) : StatisticsRepository {
    override suspend fun sendEvent(eventID: Int, message: String) = withContext(ioDispatcher) {
        megaApiGateway.sendEvent(eventID, message)
    }
}