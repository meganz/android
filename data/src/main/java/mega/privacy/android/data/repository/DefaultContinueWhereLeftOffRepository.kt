package mega.privacy.android.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_RECENTLY_USED
import mega.privacy.android.data.database.dao.RecentlyUsedDao
import mega.privacy.android.data.database.dao.TextEditorScrollDao
import mega.privacy.android.data.database.entity.RecentlyUsedEntity
import mega.privacy.android.data.database.entity.TextEditorScrollEntity
import mega.privacy.android.data.mapper.continuewhereleftoff.ContinueWhereLeftOffItemMapper
import mega.privacy.android.data.mapper.continuewhereleftoff.RecentlyUsedTypeIdMapper
import mega.privacy.android.data.preferences.ContinueWhereLeftOffSortPreferenceDataStore
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [ContinueWhereLeftOffRepository].
 * Aggregates data from [RecentlyUsedDao] and [TextEditorScrollDao].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
internal class DefaultContinueWhereLeftOffRepository @Inject constructor(
    private val recentlyUsedDao: RecentlyUsedDao,
    private val textEditorScrollDao: TextEditorScrollDao,
    private val continueWhereLeftOffItemMapper: ContinueWhereLeftOffItemMapper,
    private val recentlyUsedTypeIdMapper: RecentlyUsedTypeIdMapper,
    private val sortPreferenceDataStore: ContinueWhereLeftOffSortPreferenceDataStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ContinueWhereLeftOffRepository {

    override fun monitorContinueWhereLeftOffItems(
        limit: Int,
        sortField: ContinueWhereLeftOffSortField?,
        sortDirection: SortDirection?,
    ): Flow<List<ContinueWhereLeftOffItem>> {
        val sortSource = if (sortField != null && sortDirection != null) {
            flowOf(sortField to sortDirection)
        } else {
            sortPreferenceDataStore.monitorSortPreference()
        }
        return sortSource
            .flatMapLatest { (field, direction) -> monitorItems(limit, field, direction) }
            .map { list -> list.map(continueWhereLeftOffItemMapper::invoke) }
            .flowOn(ioDispatcher)
    }

    override fun monitorSortPreference(): Flow<Pair<ContinueWhereLeftOffSortField, SortDirection>> =
        sortPreferenceDataStore.monitorSortPreference().flowOn(ioDispatcher)

    override suspend fun setSortPreference(
        sortField: ContinueWhereLeftOffSortField,
        sortDirection: SortDirection,
    ) = withContext(ioDispatcher) {
        sortPreferenceDataStore.setSortPreference(sortField, sortDirection)
    }

    private fun monitorItems(
        limit: Int,
        sortField: ContinueWhereLeftOffSortField,
        sortDirection: SortDirection,
    ): Flow<List<RecentlyUsedEntity>> {
        val column = when (sortField) {
            ContinueWhereLeftOffSortField.Name -> "file_name COLLATE NOCASE"
            ContinueWhereLeftOffSortField.Timestamp -> "last_accessed_timestamp"
        }
        val direction = when (sortDirection) {
            SortDirection.Ascending -> "ASC"
            SortDirection.Descending -> "DESC"
        }
        val query = SimpleSQLiteQuery(
            "SELECT * FROM $TABLE_RECENTLY_USED ORDER BY $column $direction LIMIT ?",
            arrayOf(limit),
        )
        return recentlyUsedDao.monitorItems(query)
    }

    override suspend fun saveRecentlyUsedItem(
        nodeHandle: Long,
        type: RecentlyUsedType,
        fileName: String,
    ) = withContext(ioDispatcher) {
        recentlyUsedDao.insertAndPrune(
            entity = RecentlyUsedEntity(
                nodeHandle = nodeHandle,
                typeId = recentlyUsedTypeIdMapper(type),
                fileName = fileName,
                lastAccessedTimestamp = System.currentTimeMillis()
            ),
            maxItems = RecentlyUsedDao.MAX_RECENTLY_USED_ITEMS,
        )
    }

    override suspend fun savePosition(nodeHandle: Long) = withContext(ioDispatcher) {
        val existing = recentlyUsedDao.getByNodeHandle(nodeHandle) ?: return@withContext
        recentlyUsedDao.insertOrUpdate(
            existing.copy(lastAccessedTimestamp = System.currentTimeMillis())
        )
    }

    override suspend fun removeRecentlyUsedItem(nodeHandle: Long) = withContext(ioDispatcher) {
        recentlyUsedDao.deleteByNodeHandle(nodeHandle)
    }

    override suspend fun clearAllRecentlyUsedItems() = withContext(ioDispatcher) {
        recentlyUsedDao.deleteAll()
    }

    override suspend fun saveTextEditorScroll(textEditorScroll: TextEditorScroll) =
        withContext(ioDispatcher) {
            textEditorScrollDao.insertOrUpdate(
                TextEditorScrollEntity(
                    nodeHandle = textEditorScroll.nodeHandle,
                    cursorPosition = textEditorScroll.cursorPosition,
                    scrollSpot = textEditorScroll.scrollFraction
                )
            )
        }

    override suspend fun getTextEditorScroll(nodeHandle: Long): TextEditorScroll? =
        withContext(ioDispatcher) {
            textEditorScrollDao.getByNodeHandle(nodeHandle)?.let {
                TextEditorScroll(
                    nodeHandle = it.nodeHandle,
                    cursorPosition = it.cursorPosition,
                    scrollFraction = it.scrollSpot
                )
            }
        }

    override suspend fun deleteTextEditorScroll(nodeHandle: Long) = withContext(ioDispatcher) {
        textEditorScrollDao.deleteByNodeHandle(nodeHandle)
    }

}
