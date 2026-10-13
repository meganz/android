package mega.privacy.android.data.gateway.zipremote

import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.domain.entity.zipbrowser.RemoteZipArchiveSource
import javax.inject.Inject

internal class RemoteArchiveUrlGatewayImpl @Inject constructor(
    private val streamingGateway: StreamingGateway,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
) : RemoteArchiveUrlGateway {

    override suspend fun urlFor(source: RemoteZipArchiveSource): String? {
        ensureServerRunning()
        val handle = source.nodeId.longValue
        // A folder link node is served by the account's server once the folder api authorises it,
        // which is what the other folder link viewers already do.
        val node = if (source.isFolderLinkNode) {
            megaApiFolderGateway.getMegaNodeByHandle(handle)
                ?.let { megaApiFolderGateway.authorizeNode(it) }
        } else {
            megaApiGateway.getMegaNodeByHandle(handle)
        } ?: return null
        return streamingGateway.getLocalLink(node)
    }

    override suspend fun ensureServerRunning() {
        if (streamingGateway.getPort() == 0) streamingGateway.startServer()
    }
}
