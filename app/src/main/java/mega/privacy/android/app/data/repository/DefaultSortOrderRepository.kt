package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.mapper.SortOrderMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.SortOrderRepository
import javax.inject.Inject

/**
 * Default implementation of [SortOrderRepository]
 *
 * @property context
 * @property ioDispatcher
 * @property megaLocalStorageGateway
 * @property sortOrderMapper
 */
class DefaultSortOrderRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    private val sortOrderMapper: SortOrderMapper,
) : SortOrderRepository {

    override suspend fun getCameraSortOrder(): SortOrder = withContext(ioDispatcher) {
        sortOrderMapper(megaLocalStorageGateway.getCameraSortOrder())
    }
}
