package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.StreamingServerRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streaming server repository impl
 *
 * Singleton so the last streamed content type survives across the screens requesting links.
 *
 * @property ioDispatcher
 * @property streamingGateway
 * @constructor Create empty Streaming server repository impl
 */
@Singleton
class StreamingServerRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val streamingGateway: StreamingGateway,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
) : StreamingServerRepository {

    @Volatile
    private var lastStreamedContentType: FileTypeInfo? = null

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
            recordStreamedContentType(node.name)
            streamingGateway.getLocalLink(it)
        }
    }

    override suspend fun getFileStreamingUri(nodeHandle: Long) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeHandle)?.let { megaNode ->
            recordStreamedContentType(megaNode.name)
            streamingGateway.getLocalLink(megaNode)
        }
    }

    override suspend fun getFolderLinkFileStreamingUri(nodeHandle: Long) =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getMegaNodeByHandle(nodeHandle)?.let { megaNode ->
                recordStreamedContentType(megaNode.name)
                megaApiFolderGateway.authorizeNode(megaNode)
            }?.let {
                megaApiGateway.httpServerGetLocalLink(it)
            }
        }

    override suspend fun getFolderLinkFileStreamingUriFromFolderApi(nodeHandle: Long) =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getMegaNodeByHandle(nodeHandle)?.let { megaNode ->
                recordStreamedContentType(megaNode.name)
                megaApiFolderGateway.authorizeNode(megaNode)
            }?.let {
                megaApiFolderGateway.httpServerGetLocalLink(it)
            }
        }

    private fun recordStreamedContentType(fileName: String?) {
        fileName?.let { lastStreamedContentType = fileTypeInfoMapper(it) }
    }

    override fun isLastStreamedContentMedia(): Boolean =
        lastStreamedContentType.let { it is AudioFileTypeInfo || it is VideoFileTypeInfo }
}
