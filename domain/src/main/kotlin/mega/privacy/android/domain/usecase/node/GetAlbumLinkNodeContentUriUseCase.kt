package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetAlbumPhotoFileUrlByNodeIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import javax.inject.Inject

/**
 * The use case is used to get the node content uri of a public album link photo.
 *
 * Public album (set preview) nodes are fetched through the main MegaApi (fetchPublicSet) and
 * therefore streamed through the main MegaApi http server - with or without an account session.
 */
class GetAlbumLinkNodeContentUriUseCase @Inject constructor(
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val getAlbumPhotoFileUrlByNodeIdUseCase: GetAlbumPhotoFileUrlByNodeIdUseCase,
) {
    /**
     * Invoke
     *
     * @param nodeId the [NodeId] of the album photo
     * @return [NodeContentUri]
     */
    suspend operator fun invoke(nodeId: NodeId): NodeContentUri {
        val shouldStopHttpServer = if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            true
        } else false
        val url = getAlbumPhotoFileUrlByNodeIdUseCase(nodeId)
            ?: throw IllegalStateException("Album photo local link is null")
        return NodeContentUri.RemoteContentUri(url, shouldStopHttpServer)
    }
}
