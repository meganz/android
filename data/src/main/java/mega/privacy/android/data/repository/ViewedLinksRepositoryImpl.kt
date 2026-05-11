package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.dao.RecentlyViewedLinkDao
import mega.privacy.android.data.database.entity.RecentlyViewedLinkEntity
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.mapper.viewedlinks.RecentlyViewedLinkTypeIdMapper
import mega.privacy.android.data.mapper.viewedlinks.ViewedLinkRawItemMapper
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Default implementation of [ViewedLinksRepository].
 * Persists viewed links to the self-contained recently_viewed_link table.
 *
 * @property recentlyViewedLinkDao
 * @property viewedLinkRawItemMapper
 * @property recentlyViewedLinkTypeIdMapper
 * @property deviceGateway
 * @property ioDispatcher
 */
internal class ViewedLinksRepositoryImpl @Inject constructor(
    private val recentlyViewedLinkDao: RecentlyViewedLinkDao,
    private val viewedLinkRawItemMapper: ViewedLinkRawItemMapper,
    private val recentlyViewedLinkTypeIdMapper: RecentlyViewedLinkTypeIdMapper,
    private val deviceGateway: DeviceGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewedLinksRepository {

    override fun monitorLinks(): Flow<List<ViewedLink>> =
        recentlyViewedLinkDao
            .monitorViewedLinks()
            .map(viewedLinkRawItemMapper::invoke)
            .flowOn(ioDispatcher)

    override suspend fun saveLink(viewedLink: ViewedLink) = withContext(ioDispatcher) {
        val accessedTimestamp = viewedLink.accessedTimestamp
            ?: deviceGateway.getCurrentTimeInMillis()
        val recentlyViewedLinkEntity = RecentlyViewedLinkEntity(
            nodeHandle = viewedLink.nodeHandle,
            typeId = recentlyViewedLinkTypeIdMapper(viewedLink.type),
            nodeName = viewedLink.name,
            linkUrl = viewedLink.linkUrl,
            lastAccessedTimestamp = accessedTimestamp,
        )
        recentlyViewedLinkDao.insertOrUpdateLink(recentlyViewedLinkEntity)
    }

    override suspend fun removeLink(nodeHandle: Long) = withContext(ioDispatcher) {
        recentlyViewedLinkDao.deleteByNodeHandle(nodeHandle)
    }

    override suspend fun clearLinks() = withContext(ioDispatcher) {
        recentlyViewedLinkDao.deleteAll()
    }
}
