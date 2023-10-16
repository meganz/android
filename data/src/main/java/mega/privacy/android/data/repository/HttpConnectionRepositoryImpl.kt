package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.HttpConnectionGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.HttpConnectionRepository
import java.net.URL
import javax.inject.Inject

/**
 * Implementation of [HttpConnectionRepository]
 */
class HttpConnectionRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val httpConnectionGateway: HttpConnectionGateway,
) : HttpConnectionRepository {
    override suspend fun getDataBytesFromUrl(url: URL): ByteArray? = withContext(ioDispatcher) {
        httpConnectionGateway.getDataBytesFromUrl(url)
    }
}