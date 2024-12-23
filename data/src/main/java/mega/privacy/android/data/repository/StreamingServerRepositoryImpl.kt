package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.StreamingServerRepository
import javax.inject.Inject

/**
 * Streaming server repository impl
 *
 * @property ioDispatcher
 * @property streamingGateway
 * @constructor Create empty Streaming server repository impl
 */
class StreamingServerRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val streamingGateway: StreamingGateway,
    private val megaApiGateway: MegaApiGateway,
) : StreamingServerRepository {
    override suspend fun startServer() {
        withContext(ioDispatcher) {
            if (streamingGateway.getPort() == 0) streamingGateway.startServer()
        }
    }

    override suspend fun stopServer() {
        withContext(ioDispatcher) {
            streamingGateway.stopServer()
        }
    }

    override suspend fun setMaxBufferSize(bufferSize: Int) {
        withContext(ioDispatcher) {
            streamingGateway.setMaxBufferSize(bufferSize)
        }
    }

    override suspend fun getFileStreamingUri(node: Node) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(node.id.longValue)?.let {
            streamingGateway.getLocalLink(it)
        }
    }
}