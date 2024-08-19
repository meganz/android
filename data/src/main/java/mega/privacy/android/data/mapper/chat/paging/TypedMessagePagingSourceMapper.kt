package mega.privacy.android.data.mapper.chat.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import mega.privacy.android.data.database.chat.ChatDatabase
import mega.privacy.android.data.database.entity.chat.MetaTypedMessageEntity
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import timber.log.Timber
import javax.inject.Inject

/**
 * Typed message paging source mapper
 *
 * @property metaTypedEntityTypedMessageMapper
 */
class TypedMessagePagingSourceMapper @Inject constructor(
    private val metaTypedEntityTypedMessageMapper: MetaTypedEntityTypedMessageMapper,
    private val database: ChatDatabase,
) {

    /**
     * Invoke
     *
     * @param entityPagingSource
     * @return mapped paging source
     */
    operator fun invoke(entityPagingSource: PagingSource<Int, MetaTypedMessageEntity>): PagingSource<Int, TypedMessage> {
        return MappingPagingSource(
            entityPagingSource,
            metaTypedEntityTypedMessageMapper,
            database,
        )
    }

    internal class MappingPagingSource(
        private val originalSource: PagingSource<Int, MetaTypedMessageEntity>,
        private val metaTypedMessageEntityMapper: MetaTypedEntityTypedMessageMapper,
        database: ChatDatabase,
    ) : PagingSource<Int, TypedMessage>() {

        private val invalidationObserver: InvalidationTracker.Observer =
            object : InvalidationTracker.Observer("typed_messages") {
                override fun onInvalidated(tables: Set<String>) {
                    Timber.d("Paging mediator mapper invalidation observer: invalidated")
                    invalidate()
                }
            }

        init {
            database.invalidationTracker.addObserver(invalidationObserver)
        }

        override fun getRefreshKey(state: PagingState<Int, TypedMessage>) =
            state.anchorPosition?.let { maxOf(0, it - (state.config.initialLoadSize / 2)) }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TypedMessage> {
            Timber.d("Paging mediator mapper load: params : $params")
            return when (val originalResult = originalSource.load(params)) {
                is LoadResult.Error -> {
                    Timber.e(originalResult.throwable, "Paging mediator mapper load: error")
                    LoadResult.Error(originalResult.throwable)
                }

                is LoadResult.Invalid -> {
                    Timber.e("Paging mediator mapper load: invalid")
                    LoadResult.Invalid()
                }

                is LoadResult.Page -> {
                    LoadResult.Page(
                        data = originalResult.data.map { metaTypedMessageEntityMapper(it) },
                        prevKey = originalResult.prevKey,
                        nextKey = originalResult.nextKey,
                    )
                }
            }
        }
    }
}
