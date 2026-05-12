package mega.privacy.android.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.dao.RecentlyViewedLinkDao
import mega.privacy.android.data.database.entity.RecentlyViewedLinkEntity
import mega.privacy.android.data.database.entity.ViewedLinkRawItem
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

    override fun getViewedLinksPagingSource(): PagingSource<Int, ViewedLink> =
        MappingPagingSource(
            source = recentlyViewedLinkDao.getViewedLinksPagingSource(),
            mapper = viewedLinkRawItemMapper,
            ioDispatcher = ioDispatcher,
        )

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

    private class MappingPagingSource(
        private val source: PagingSource<Int, ViewedLinkRawItem>,
        private val mapper: ViewedLinkRawItemMapper,
        private val ioDispatcher: CoroutineDispatcher,
    ) : PagingSource<Int, ViewedLink>() {

        init {
            source.registerInvalidatedCallback { invalidate() }
        }

        override val jumpingSupported: Boolean
            get() = source.jumpingSupported

        override fun getRefreshKey(state: PagingState<Int, ViewedLink>): Int? =
            state.anchorPosition?.let { anchor ->
                state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
            }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ViewedLink> =
            when (val result = source.load(params)) {
                is LoadResult.Error -> LoadResult.Error(result.throwable)
                is LoadResult.Invalid -> LoadResult.Invalid()
                is LoadResult.Page -> LoadResult.Page(
                    data = withContext(ioDispatcher) {
                        result.data.map(mapper::invoke)
                    },
                    prevKey = result.prevKey,
                    nextKey = result.nextKey,
                )
            }
    }
}
