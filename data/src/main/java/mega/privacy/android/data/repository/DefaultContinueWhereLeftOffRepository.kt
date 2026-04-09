package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.dao.RecentlyUsedDao
import mega.privacy.android.data.database.dao.TextEditorScrollDao
import mega.privacy.android.data.database.entity.RecentlyUsedEntity
import mega.privacy.android.data.database.entity.TextEditorScrollEntity
import mega.privacy.android.data.mapper.continuewhereleftoff.ContinueWhereLeftOffItemMapper
import mega.privacy.android.data.mapper.continuewhereleftoff.RecentlyUsedTypeIdMapper
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.continuewhereleftoff.TextEditorScroll
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [ContinueWhereLeftOffRepository].
 * Aggregates data from [RecentlyUsedDao] and [TextEditorScrollDao].
 */
@Singleton
internal class DefaultContinueWhereLeftOffRepository @Inject constructor(
    private val recentlyUsedDao: RecentlyUsedDao,
    private val textEditorScrollDao: TextEditorScrollDao,
    private val continueWhereLeftOffItemMapper: ContinueWhereLeftOffItemMapper,
    private val recentlyUsedTypeIdMapper: RecentlyUsedTypeIdMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ContinueWhereLeftOffRepository {

    override fun monitorContinueWhereLeftOffItems(
        limit: Int,
    ): Flow<List<ContinueWhereLeftOffItem>> =
        recentlyUsedDao
            .monitorRecentlyUsedItems(limit)
            .map { list -> list.map(continueWhereLeftOffItemMapper::invoke) }
            .flowOn(ioDispatcher)

    override suspend fun saveRecentlyUsedItem(
        nodeHandle: Long,
        type: RecentlyUsedType,
        fileName: String,
    ) = withContext(ioDispatcher) {
        recentlyUsedDao.insertAndPrune(
            entity = RecentlyUsedEntity(
                nodeHandle = nodeHandle,
                typeId = recentlyUsedTypeIdMapper.toTypeId(type),
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
