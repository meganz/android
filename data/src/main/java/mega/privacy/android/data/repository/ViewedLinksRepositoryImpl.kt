package mega.privacy.android.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_VIEWED_LINK
import mega.privacy.android.data.database.dao.RecentlyViewedLinkDao
import mega.privacy.android.data.database.entity.RecentlyViewedLinkEntity
import mega.privacy.android.data.database.entity.ViewedLinkRawItem
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.mapper.viewedlinks.RecentlyViewedLinkTypeIdMapper
import mega.privacy.android.data.mapper.viewedlinks.ViewedLinkRawItemMapper
import mega.privacy.android.data.preferences.ViewedLinksSortPreferenceDataStore
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
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
 * @property sortPreferenceDataStore
 * @property ioDispatcher
 */
internal class ViewedLinksRepositoryImpl @Inject constructor(
    private val recentlyViewedLinkDao: RecentlyViewedLinkDao,
    private val viewedLinkRawItemMapper: ViewedLinkRawItemMapper,
    private val recentlyViewedLinkTypeIdMapper: RecentlyViewedLinkTypeIdMapper,
    private val deviceGateway: DeviceGateway,
    private val sortPreferenceDataStore: ViewedLinksSortPreferenceDataStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewedLinksRepository {

    override fun getViewedLinksPagingSource(
        sortField: ViewedLinksSortField,
        sortDirection: SortDirection,
    ): PagingSource<Int, ViewedLink> {
        val column = when (sortField) {
            ViewedLinksSortField.Name -> "node_name COLLATE NOCASE"
            ViewedLinksSortField.LastAccessed -> "last_accessed_timestamp"
        }
        val direction = when (sortDirection) {
            SortDirection.Ascending -> "ASC"
            SortDirection.Descending -> "DESC"
        }
        val query = SimpleSQLiteQuery(
            "SELECT node_handle, type_id, node_name, link_url, last_accessed_timestamp " +
                    "FROM $TABLE_RECENTLY_VIEWED_LINK ORDER BY $column $direction"
        )
        return MappingPagingSource(
            source = recentlyViewedLinkDao.getViewedLinksPagingSource(query),
            mapper = viewedLinkRawItemMapper,
            ioDispatcher = ioDispatcher,
        )
    }

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

    override fun monitorSortPreference() =
        sortPreferenceDataStore.monitorSortPreference().flowOn(ioDispatcher)

    override suspend fun setSortPreference(
        sortField: ViewedLinksSortField,
        sortDirection: SortDirection,
    ) = withContext(ioDispatcher) {
        sortPreferenceDataStore.setSortPreference(sortField, sortDirection)
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
