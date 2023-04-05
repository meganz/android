package mega.privacy.android.data.repository.account

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.account.business.BusinessAccountStatusMapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.BusinessRepository
import javax.inject.Inject

/**
 * Business repository impl
 */
internal class BusinessRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val businessAccountStatusMapper: BusinessAccountStatusMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BusinessRepository {

    override suspend fun getBusinessStatus() = withContext(ioDispatcher) {
        businessAccountStatusMapper(megaApiGateway.getBusinessStatus())
    }
}