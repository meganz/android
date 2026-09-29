package mega.privacy.android.data.repository.chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.GiphyApi
import mega.privacy.android.data.mapper.chat.giphy.GiphyDataMapper
import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.chat.GiphyRepository
import javax.inject.Inject

internal class DefaultGiphyRepository @Inject constructor(
    private val giphyApi: GiphyApi,
    private val giphyDataMapper: GiphyDataMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GiphyRepository {

    override suspend fun getTrendingGiphys(limit: Int, offset: Int): List<Giphy> =
        withContext(ioDispatcher) {
            giphyDataMapper(giphyApi.getTrending(limit = limit, offset = offset)?.data.orEmpty())
        }

    override suspend fun searchGiphys(query: String, limit: Int, offset: Int): List<Giphy> =
        withContext(ioDispatcher) {
            giphyDataMapper(
                giphyApi.search(query = query, limit = limit, offset = offset)?.data.orEmpty()
            )
        }
}
