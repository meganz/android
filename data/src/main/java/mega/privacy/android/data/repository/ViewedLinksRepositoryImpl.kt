package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.dao.RecentlyViewedLinkDao
import mega.privacy.android.data.database.entity.RecentlyUsedEntity
import mega.privacy.android.data.database.entity.RecentlyViewedLinkEntity
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.mapper.continuewhereleftoff.RecentlyUsedTypeIdMapper
import mega.privacy.android.data.mapper.viewedlinks.ViewedLinkRawItemMapper
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Default implementation of [ViewedLinksRepository].
 * Persists viewed links using both the recently_used parent table
 * and the recently_viewed_link child table.
 *
 * @property recentlyViewedLinkDao
 * @property viewedLinkRawItemMapper
 * @property recentlyUsedTypeIdMapper
 * @property deviceGateway
 * @property ioDispatcher
 */
internal class ViewedLinksRepositoryImpl @Inject constructor(
    private val recentlyViewedLinkDao: RecentlyViewedLinkDao,
    private val viewedLinkRawItemMapper: ViewedLinkRawItemMapper,
    private val recentlyUsedTypeIdMapper: RecentlyUsedTypeIdMapper,
    private val deviceGateway: DeviceGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewedLinksRepository {

    override fun monitorLinks(): Flow<List<ViewedLink>> =
        recentlyViewedLinkDao
            .monitorViewedLinks()
            .map(viewedLinkRawItemMapper::invoke)
            .flowOn(ioDispatcher)

    override suspend fun saveLink(viewedLink: ViewedLink) = withContext(ioDispatcher) {
        val recentlyUsedEntity = RecentlyUsedEntity(
            nodeHandle = viewedLink.nodeHandle,
            typeId = recentlyUsedTypeIdMapper(viewedLink.type),
            fileName = viewedLink.name,
            lastAccessedTimestamp = viewedLink.accessedTimestamp
                ?: deviceGateway.getCurrentTimeInMillis(),
        )
        val recentlyViewedLinkEntity = RecentlyViewedLinkEntity(
            nodeHandle = viewedLink.nodeHandle,
            linkUrl = viewedLink.linkUrl,
        )
        recentlyViewedLinkDao.saveViewedLink(recentlyUsedEntity, recentlyViewedLinkEntity)
    }

    override suspend fun removeLink(nodeHandle: Long) = withContext(ioDispatcher) {
        recentlyViewedLinkDao.deleteByNodeHandle(nodeHandle)
    }

    override suspend fun clearLinks() = withContext(ioDispatcher) {
        recentlyViewedLinkDao.deleteAll()
    }
}
