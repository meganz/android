package mega.privacy.android.data.gateway

import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

class DefaultStreamingGateway @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) : StreamingGateway {

    override suspend fun getLocalLink(node: MegaNode): String? =
        megaApi.httpServerGetLocalLink(node)

    override suspend fun getPort() = megaApi.httpServerIsRunning()

    override suspend fun startServer() = megaApi.httpServerStart()

    override suspend fun setMaxBufferSize(bufferSize: Int) {
        megaApi.httpServerSetMaxBufferSize(bufferSize)
    }

    override suspend fun stopServer() {
        megaApi.httpServerStop()
    }

}